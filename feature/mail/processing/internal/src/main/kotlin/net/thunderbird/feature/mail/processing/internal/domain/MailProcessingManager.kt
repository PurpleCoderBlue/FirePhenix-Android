package net.thunderbird.feature.mail.processing.internal.domain

import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.thunderbird.feature.mail.processing.api.ProcessingAction
import net.thunderbird.feature.mail.processing.api.ProcessingMessage
import net.thunderbird.feature.mail.processing.api.ServiceProfile
import net.thunderbird.feature.mail.processing.internal.data.ProcessingCredentials
import net.thunderbird.feature.mail.processing.internal.data.ProcessingException
import net.thunderbird.feature.mail.processing.internal.data.ProcessingFailure
import net.thunderbird.feature.mail.processing.internal.data.ProcessingService
import net.thunderbird.feature.mail.processing.internal.data.ProcessingStorage
import net.thunderbird.feature.mail.processing.internal.data.contentHash

internal data class ProcessingSelection(
    val profileId: String,
    val action: ProcessingAction,
    val model: String = "",
    val language: String = "zh-Hans",
    val includeQuotedContext: Boolean = false,
)

internal class MailProcessingManager(
    private val storage: ProcessingStorage,
    private val service: ProcessingService,
    private val applicationScope: CoroutineScope,
    private val requestDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val requestScope = CoroutineScope(
        applicationScope.coroutineContext + SupervisorJob(applicationScope.coroutineContext[Job]),
    )
    private val mutex = Mutex()
    private val requestPermits = Semaphore(MAX_PARALLEL_REQUESTS)

    private data class ActiveRequest(val result: Deferred<String>, val progress: StateFlow<Pair<Int, Int>>)

    private val requests = mutableMapOf<String, ActiveRequest>()

    private fun credentials(selection: ProcessingSelection): ProcessingCredentials {
        val saved = storage.credentials(selection.profileId)?.takeIf { it.profile.action == selection.action }
            ?: throw ProcessingException(ProcessingFailure.NOT_CONFIGURED)
        val profile = if (selection.model.isBlank()) saved.profile else saved.profile.copy(model = selection.model)
        return ProcessingCredentials(profile, saved.secret)
    }

    fun requestKey(message: ProcessingMessage, selection: ProcessingSelection): String =
        cacheKey(message, selection, credentials(selection).profile)

    private fun cacheKey(message: ProcessingMessage, selection: ProcessingSelection, profile: ServiceProfile): String =
        contentHash(message.accountId) + ":" + contentHash(
            listOf(
                message.accountId,
                message.messageId,
                message.folder,
                message.html,
                message.isComplete.toString(),
                Json.encodeToString(profile),
                selection.language,
                if (selection.action == ProcessingAction.SUMMARIZE) selection.includeQuotedContext.toString() else "",
            ).joinToString("\u0000"),
        )

    suspend fun process(
        message: ProcessingMessage,
        selection: ProcessingSelection,
        force: Boolean,
        onStarted: (String) -> Unit = {},
        onProgress: (Int, Int) -> Unit,
    ): String {
        if (!message.isComplete) throw ProcessingException(ProcessingFailure.INCOMPLETE)
        val credentials = credentials(selection)
        val key = cacheKey(message, selection, credentials.profile)
        val cacheGeneration = storage.cacheGeneration
        val request = mutex.withLock {
            requests[key]?.takeUnless { it.result.isCompleted }?.let { return@withLock it }
            val progress = MutableStateFlow(0 to 0)
            val result = requestScope.async(requestDispatcher) {
                if (!force) storage.cached(message.accountId, key)?.let { return@async it }
                val result = when (selection.action) {
                    ProcessingAction.TRANSLATE -> translate(message, credentials, selection.language) { done, total ->
                        progress.value =
                            done to total
                    }

                    ProcessingAction.SUMMARIZE -> summarize(message, credentials, selection) { done, total ->
                        progress.value =
                            done to total
                    }
                }
                if (storage.cacheGeneration ==
                    cacheGeneration
                ) {
                    storage.cache(message.accountId, key, result, cacheGeneration)
                }
                result
            }
            ActiveRequest(result, progress).also { active ->
                requests[key] = active
                result.invokeOnCompletion {
                    applicationScope.launch { mutex.withLock { if (requests[key] === active) requests.remove(key) } }
                }
            }
        }
        onStarted(key)
        return coroutineScope {
            val observer = launch { request.progress.collect { (done, total) -> onProgress(done, total) } }
            try {
                request.result.await()
            } finally {
                observer.cancel()
            }
        }
    }

    suspend fun cancel(key: String) {
        mutex.withLock { requests[key]?.result?.cancel() }
    }

    fun clearAccount(accountId: String) {
        requestScope.launch {
            val prefix = contentHash(accountId) + ":"
            val active = mutex.withLock { requests.filterKeys { it.startsWith(prefix) }.values.map { it.result } }
            active.forEach { it.cancel() }
            active.joinAll()
            storage.clearAccount(accountId)
        }
    }

    private suspend fun execute(
        credentials: ProcessingCredentials,
        texts: List<String>,
        language: String,
    ): List<String> = requestPermits.withPermit {
        val current = storage.credentials(credentials.profile.id)
        if (current?.profile?.revision != credentials.profile.revision) {
            throw ProcessingException(ProcessingFailure.NOT_CONFIGURED)
        }
        service.execute(credentials.profile, credentials.secret, texts, language)
    }

    private suspend fun translate(
        message: ProcessingMessage,
        credentials: ProcessingCredentials,
        language: String,
        onProgress: (Int, Int) -> Unit,
    ): String {
        val plan = HtmlProcessingPlan(message.html)
        if (plan.texts.isEmpty()) throw ProcessingException(ProcessingFailure.OUTPUT)
        val batches = plan.batches()
        val results = arrayOfNulls<List<String>>(batches.size)
        val next = AtomicInteger(0)
        val progress = Mutex()
        var completed = 0
        onProgress(0, batches.size)
        coroutineScope {
            List(minOf(MAX_PARALLEL_REQUESTS, batches.size)) {
                async {
                    while (true) {
                        val index = next.getAndIncrement()
                        if (index >= batches.size) break
                        val batch = batches[index]
                        val unique = batch.distinct()
                        val translated = execute(credentials, unique, language)
                        if (translated.size != unique.size) throw ProcessingException(ProcessingFailure.OUTPUT)
                        val mapped = unique.zip(translated).toMap()
                        results[index] = batch.map { mapped.getValue(it) }
                        progress.withLock {
                            completed++
                            onProgress(completed, batches.size)
                        }
                    }
                }
            }.awaitAll()
        }
        return plan.translatedHtml(results.flatMap { checkNotNull(it) })
    }

    private suspend fun summarize(
        message: ProcessingMessage,
        credentials: ProcessingCredentials,
        selection: ProcessingSelection,
        onProgress: (Int, Int) -> Unit,
    ): String {
        val text = HtmlProcessingPlan.summaryText(message.html, selection.includeQuotedContext)
        if (text.isBlank()) throw ProcessingException(ProcessingFailure.OUTPUT)
        var chunks = splitText(text)
        var round = 0
        while (true) {
            val progress = Mutex()
            var completed = 0
            onProgress(0, chunks.size)
            val summaries = coroutineScope {
                chunks.map { chunk ->
                    async {
                        // execute() shares the same two-request limit as translation.
                        execute(credentials, listOf(chunk), selection.language).single().also {
                            progress.withLock { onProgress(++completed, chunks.size) }
                        }
                    }
                }.awaitAll()
            }
            if (summaries.size == 1) {
                onProgress(chunks.size, chunks.size)
                return summaries.single()
            }
            val combined = summaries.joinToString("\n\n")
            val next = splitText(combined)
            round++
            if (round > MAX_SUMMARY_ROUNDS || combined.length >= chunks.sumOf { it.length }) {
                throw ProcessingException(ProcessingFailure.INCOMPLETE)
            }
            chunks = next
        }
    }

    private companion object {
        const val MAX_SUMMARY_ROUNDS = 8
        const val MAX_PARALLEL_REQUESTS = 2
    }
}
