[简体中文](README.md) · [English](README.en.md)

# FirePhenix

<img src="artwork/firephenix/phoenix-carrying-mail-glass.png" alt="Glass phoenix carrying a letter" width="144">

An Outlook-focused personal email client based on [Thunderbird for Android](https://github.com/thunderbird/thunderbird-android), with sender aliases, translation, summaries, and a Liquid Glass interface. The upstream baseline is Thunderbird 24.0 (`73a7b7fffcaed6db51c5b0aaa90e4ee012549563`). This is a development version.

FirePhenix version: `0.1.0-alpha.2`, Android `versionCode`: `35`. Debug builds append `-SNAPSHOT`. The product version is separate from the upstream version.

## Downloads

- [GitHub Releases](https://github.com/PurpleCoderBlue/FirePhenix-Android/releases): no APK release has been published yet; debug builds can be built locally.
- [Komi Store](https://github.com/komi-store/komi-store), formerly GitHub Store, can retrieve applications from GitHub Releases. Add this repository after an APK release is available. This does not imply an existing store listing.

Thunderbird's official Google Play and F-Droid listings distribute the upstream application, not FirePhenix.

## Features

|     Area      |                                                                     Implementation                                                                      |
|---------------|---------------------------------------------------------------------------------------------------------------------------------------------------------|
| Mail          | Upstream accounts, folders, search, attachments, drafts, composition, and synchronization, plus local account display names                             |
| Appearance    | Liquid Glass, system/light/dark themes, bundled or custom backgrounds, automatic or manual accent colors, separate glass and background blur            |
| Translation   | One tap translates within the original layout; another restores the original; bilingual mode, cancellation, regeneration, and encrypted caching         |
| Summaries     | Separate service configuration; the summary button toggles the generated summary; quoted history is excluded by default                                 |
| Services      | Qwen, OpenAI, DeepSeek, custom OpenAI-compatible services, and Anthropic; translation also supports Microsoft Translator and Google Cloud Translation   |
| Automation    | Separate sender lists for translation and summaries; incomplete, spam, and oversized messages are excluded from automatic processing                    |
| Aliases       | Explicitly add existing aliases, set defaults and favorites, and reuse the existing identity/draft/reply flow; ambiguous choices require confirmation   |
| Remote images | Allow/block sender lists and bundled brand defaults; the catalogue updates with the app; Anthropic is blocked by default and its image button is hidden |
| Layout        | Single column on narrow windows; translation and summary settings occupy separate columns on wide windows                                               |

Minimum Android API: 23. Refraction and related optical effects require Android 13 / API 33 or newer; older systems use a fallback. The launcher icon is a static glass-style asset.

## Configuration and privacy

See the [configuration guide](docs/firephenix-configuration.md) for details (Chinese).

- Appearance: Settings → General settings → Display → Appearance.
- Services: Settings → General settings → Interaction → Translation and summary. Select a default service, long-press to edit/delete, or use the final list row to add one. Copying settings between actions is explicit; each copy is saved independently.
- No API keys are bundled. Service settings and processing caches use Android Keystore AES-256-GCM encryption in private no-backup storage and are excluded from ordinary settings exports.
- Manual processing, or opening a complete eligible message whose sender was explicitly added to an automation list, sends the selected text to the configured service. Attachments and images are not uploaded for AI processing. Availability and charges depend on the service, model, and account.
- Qwen's preset base URL is `https://maas.qianwenaiapi.com/compatible-mode/v1`; the client appends `/chat/completions`. Use your own key and available model. Do not put keys in URLs.
- New accounts default to downloading message bodies up to 5 MiB. Existing explicit limits are preserved. This does not authorize remote images or attachments and does not change the AI automation limit.
- Remote images depend only on the four sender lists and bundled catalogue, with explicit blocking taking priority. Outlook/device contacts do not grant image access. A catalogue match is not proof of sender authenticity.

## Building

Use JDK 21, Android SDK Platform 37, and the included Gradle Wrapper 9.7.0. Target SDK is 36. Open the repository root in Android Studio and select `app-thunderbird` / `fossDebug`. The retained `app-k9mail` module is not the FirePhenix product entry point.

```bash
git clone https://github.com/PurpleCoderBlue/FirePhenix-Android.git
cd FirePhenix-Android
./gradlew :app-thunderbird:assembleFossDebug --max-workers=2 \
  '-Dorg.gradle.jvmargs=-Xmx3g -XX:MaxMetaspaceSize=1g -Dfile.encoding=UTF-8'
```

Configure `JAVA_HOME`, `ANDROID_HOME`, or an ignored `local.properties` locally. Do not commit machine-specific SDK paths.

Output: `app-thunderbird/build/outputs/apk/foss/debug/app-thunderbird-foss-debug.apk`. Debug package: `app.firephenix.android.debug`. Release application ID: `app.firephenix.android`.

Forks must use their own OAuth registrations and signing keys. Check the package, certificate hash, and redirect URI together. OAuth client IDs are public identifiers, not client secrets. The existing debug callback only matches its registered development certificate. Release OAuth registration is still being prepared; Beta, Daily, and other providers are not fully configured for independent distribution. See the [upstream fork requirements](README.thunderbird.md#forking).

## Verification and limitations

These are local checks recorded on October 17, 2026, not a claim that every device and feature has passed acceptance:

- Foss Debug builds and relevant lint/static checks passed in the previous UI verification. The complete `test lint detekt spotlessCheck` run passed.
- The account-name null regression was fixed: all 756 `legacy:core` tests and 8 account-module tests passed. All 109 Bolt JVM tests passed in the default Chinese environment after removing hardcoded English assertions. See the [UI verification record](docs/engineering/technical-designs/0009-firephenix-alpha2-verification.md).
- Installation, reply-to-composer navigation, and return navigation were checked on one Honor CHG-W60 tablet. No email was sent. Other devices, window sizes, and long-running background behavior need further acceptance.
- The full Qwen HTML processing pipeline was tested using a public HTML fixture of 20 paragraphs (3,451 characters) plus a link: translation took about 9.1 seconds and summarization about 1.4 seconds; cached reads took about 16/35 milliseconds. Paragraphs and links were preserved. No private email was sent. Other providers mainly have protocol tests; latency depends on the model, service, and text.
- Repeated view traversal during transitions was reduced. The same settings-navigation sequence recorded 4.71% slow frames in this round. Slow frames remain; consistent full-frame-rate rendering is not claimed.
- Alias `From` / `Sender` / `Reply-To` headers and real recipient reply routing have not completed acceptance.
- FCM and Honor Push only have API boundaries; no SDK, token registration, or push gateway is deployed. Existing upstream synchronization remains in use.
- A local long-term signing key and signed candidate are available. Release OAuth registration/login validation, public APK publication, and store distribution remain incomplete.

See the [diagnosis record](docs/engineering/technical-designs/0007-firephenix-diagnosis.md). Device screenshots, private emails, logs, keys, local configuration, and build outputs do not belong in the source repository.

## Upstream updates

The product stays on the Thunderbird stable baseline, currently `THUNDERBIRD_24_0`. Do not synchronize upstream `main`, Beta, or Daily changes. A new official stable release must be evaluated and tested on a separate upgrade branch before updating this product. Do not use GitHub's Sync fork to import the upstream development branch. See the [Git policy](docs/firephenix-git-policy.md).

## Development and feedback

- [Report an issue](https://github.com/PurpleCoderBlue/FirePhenix-Android/issues) with the version, OS, reproduction steps, and redacted logs. Do not attach message bodies, addresses, keys, or tokens.
- Follow the [development setup](docs/contributing/development-environment.md), [testing guide](docs/contributing/testing-guide.md), [architecture](docs/architecture/README.md), and [AGENTS.md](AGENTS.md).
- Upstream signing, publishing, notification, and bot workflows are restricted to upstream repositories. This fork has no automatic release pipeline configured.
- See [SECURITY.md](SECURITY.md) for security reporting.

## License and credits

Licensed under [Apache License 2.0](LICENSE), retaining upstream authorship, licenses, and third-party notices. The original upstream introduction remains in [README.thunderbird.md](README.thunderbird.md).

- [Thunderbird for Android / K-9 Mail](https://github.com/thunderbird/thunderbird-android): mail and application foundation.
- [Kyant Backdrop / AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass): glass materials and examples; notices are in [third_party](components/ui/bolt/third_party).
- [Read Frog](https://github.com/mengxi-ream/read-frog): reference for scheduling, batching, and caching; independently implemented in Kotlin without copying GPL source.

Phoenix artwork and generation notes are in [artwork/firephenix](artwork/firephenix).
