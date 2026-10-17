package net.thunderbird.feature.account.settings.impl.alias

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.toImmutableList
import net.thunderbird.components.ui.bolt.atom.button.ButtonFilled
import net.thunderbird.components.ui.bolt.atom.button.ButtonIcon
import net.thunderbird.components.ui.bolt.atom.button.ButtonText
import net.thunderbird.components.ui.bolt.atom.icon.Icons
import net.thunderbird.components.ui.bolt.atom.text.TextBodyMedium
import net.thunderbird.components.ui.bolt.atom.text.TextLabelSmall
import net.thunderbird.components.ui.bolt.atom.textfield.TextFieldOutlinedSuggestions
import net.thunderbird.components.ui.bolt.organism.BasicDialog
import net.thunderbird.components.ui.bolt.organism.TopAppBarWithBackButton
import net.thunderbird.components.ui.bolt.template.Scaffold
import net.thunderbird.core.ui.theme.api.FeatureThemeProvider
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import net.thunderbird.feature.account.settings.api.R as AliasR

internal class AliasManagementActivity : ComponentActivity() {
    private val themeProvider: FeatureThemeProvider by inject()
    private val viewModel: AliasManagementViewModel by viewModel {
        parametersOf(requireNotNull(intent.getStringExtra("accountId")))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            themeProvider.WithTheme {
                AliasManagementContent(
                    viewModel = viewModel,
                    onBack = ::finish,
                    chooseAccount = intent.getStringExtra("accountId").orEmpty().isBlank(),
                    onAccount = { id ->
                        val accountIntent = Intent(this, AliasManagementActivity::class.java)
                        accountIntent.putExtra("accountId", id)
                        startActivity(accountIntent)
                        finish()
                    },
                )
            }
        }
    }
}

@Composable
private fun AliasManagementContent(
    viewModel: AliasManagementViewModel,
    onBack: () -> Unit,
    chooseAccount: Boolean,
    onAccount: (String) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val dispatch = viewModel::event
    AliasManagementScreen(state, dispatch, onBack, chooseAccount, onAccount)
}

@Composable
internal fun AliasManagementScreen(
    state: AliasManagementState,
    dispatch: (AliasManagementEvent) -> Unit,
    onBack: () -> Unit,
    chooseAccount: Boolean = false,
    onAccount: (String) -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBarWithBackButton(stringResource(AliasR.string.account_settings_api_alias_title), onBack)
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).imePadding(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 840.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextBodyMedium(stringResource(AliasR.string.account_settings_api_alias_notice))
                if (state.failed) TextBodyMedium(stringResource(AliasR.string.account_settings_api_alias_error))
                if (chooseAccount) {
                    state.accounts.forEach { account -> ButtonText(account.label, { onAccount(account.id) }) }
                    return@Column
                }
                state.aliases.forEach { alias -> AliasItem(alias, state.busy, dispatch) }
                TextFieldOutlinedSuggestions(
                    value = state.address,
                    onValueChange = { dispatch(AliasManagementEvent.Address(it)) },
                    options = state.candidates.toImmutableList(),
                    label = stringResource(AliasR.string.account_settings_api_alias_existing_address),
                    enabled = !state.busy,
                )
                ButtonFilled(
                    stringResource(AliasR.string.account_settings_api_alias_add),
                    { dispatch(AliasManagementEvent.Add) },
                    enabled = !state.busy && state.address.isNotBlank(),
                )
            }
        }
    }
}

@Composable
private fun AliasItem(
    alias: net.thunderbird.feature.account.settings.alias.SenderAlias,
    busy: Boolean,
    dispatch: (AliasManagementEvent) -> Unit,
) {
    val address = alias.identity.email.orEmpty()
    var managing by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TextBodyMedium(address, modifier = Modifier.weight(1f))
        if (alias.isDefault) TextLabelSmall(stringResource(AliasR.string.account_settings_api_alias_default))
        if (alias.isFavorite) TextLabelSmall("★")
        ButtonIcon(
            onClick = { managing = true },
            imageVector = Icons.Outlined.MoreVert,
            contentDescription = stringResource(AliasR.string.account_settings_api_alias_manage),
            enabled = !busy,
        )
    }
    if (managing) {
        BasicDialog(
            headlineText = address,
            onDismissRequest = { managing = false },
            buttons = {},
            content = {
                Column(Modifier.padding(16.dp)) {
                    AliasActions(alias, busy) { event ->
                        managing = false
                        dispatch(event)
                    }
                }
            },
        )
    }
}

@Composable
private fun AliasActions(
    alias: net.thunderbird.feature.account.settings.alias.SenderAlias,
    busy: Boolean,
    dispatch: (AliasManagementEvent) -> Unit,
) {
    val address = alias.identity.email.orEmpty()
    Column {
        ButtonText(
            stringResource(
                if (alias.isDefault) {
                    AliasR.string.account_settings_api_alias_default
                } else {
                    AliasR.string.account_settings_api_alias_make_default
                },
            ),
            { dispatch(AliasManagementEvent.Default(address)) },
            enabled = !alias.isDefault && !busy,
        )
        ButtonText(
            stringResource(
                if (alias.isFavorite) {
                    AliasR.string.account_settings_api_alias_unfavorite
                } else {
                    AliasR.string.account_settings_api_alias_favorite
                },
            ),
            {
                dispatch(
                    AliasManagementEvent.Favorite(
                        address,
                        !alias.isFavorite,
                    ),
                )
            },
            enabled = !busy,
        )
        ButtonText(
            stringResource(AliasR.string.account_settings_api_alias_remove),
            { dispatch(AliasManagementEvent.Remove(address)) },
            enabled = !alias.isPrimary && !busy,
        )
    }
}
