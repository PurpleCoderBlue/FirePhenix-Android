# FirePhenix Microsoft 应用注册

日期：2026-10-14。用户已明确授权修改 Microsoft OAuth、登记应用，并在提交时确认同意注册页的 Microsoft 平台策略。用户自行完成控制台登录；未代替用户登录邮箱、同意邮件访问或发送邮件。

## FirePhenix 独立包名更新（2026-10-14）

用户已确认使用 `app.firephenix.android` 作为应用 ID，调试版为 `app.firephenix.android.debug`，测试应用 ID 为 `app.firephenix.android.tests`。源码命名空间保留 `net.thunderbird.android`，不影响安装标识。

调试版 Microsoft 回调为 `msauth://app.firephenix.android.debug/0mSO1xhKsrgx%2B%2FX6ZN3aIoNgiOM%3D`。2026-10-14 已在 Entra 为现有 Client ID `7f186507-2d63-4f98-8f55-7ca2f6baad3d` 保存新包名及签名哈希 `0mSO1xhKsrgx+/X6ZN3aIoNgiOM=`，替换原 `net.thunderbird.android.debug` 回调。保存后页面已显示新地址；截图 `/private/tmp/firephenix-new-package-microsoft-redirect.png` 避开顶部私人账号区域。

Beta、Daily、Release 的自有 OAuth 注册仍未完成；其他提供商的上游 OAuth 配置也尚未替换，不能据此发布这些构建。新包名不会自动继承旧包名的本地账户数据。

独立包名验证：已生成 Foss Debug APK，`aapt dump badging` 确认安装标识为 `app.firephenix.android.debug`，默认名称为 `FirePhenix`、中文为“火凤凰”。APK DEX 已验证包含新 Microsoft 回调且不含旧调试回调，签名 SHA-1 为 `d2648ed7184ab2b831fbf5fa64ddda22836088e3`，对应上述 Base64 哈希。该 APK 的 SHA-256 为 `ae6d35278927535997d0802fad275a9d7a1898d51422ab6548958db83ef32861`。应用模块 `spotlessCheck` 通过（17 秒）；完整 Lint 已停止，未完成，不作为通过依据。日志分别为 `/private/tmp/firephenix-package-rename-build.log` 和 `/private/tmp/firephenix-package-rename-format.log`。本次未安装到手机或验证登录。

IDE 本机模块文件及引用已从 `.idea/MailApp.iml` 改为 `.idea/FirePhenix.iml`，Android Studio MCP 已返回模块名 `FirePhenix`。Git 远程仍指向上游，没有提交或推送。

## 当前已保存的注册参数

|           参数            |                                     值                                      |
|-------------------------|----------------------------------------------------------------------------|
| 显示名称                    | `FirePhenix`，中文产品名称为“火凤凰”                                                  |
| Application (client) ID | `7f186507-2d63-4f98-8f55-7ca2f6baad3d`                                     |
| 支持账户                    | 任何 Entra ID 租户 + 个人 Microsoft 帐户                                           |
| 平台                      | Android                                                                    |
| 当前调试包名                  | `app.firephenix.android.debug`                                             |
| 当前调试 APK 签名哈希           | `0mSO1xhKsrgx+/X6ZN3aIoNgiOM=`                                             |
| 编码后的重定向 URI             | `msauth://app.firephenix.android.debug/0mSO1xhKsrgx%2B%2FX6ZN3aIoNgiOM%3D` |

以上 Client ID 和回调来自已登录的 Microsoft Entra 控制台，不是 Client Secret。没有创建机密或注册收费服务。
签名哈希由已成功构建的 APK 使用 Android SDK `apksigner verify --print-certs-pem` 获取证书后，计算证书 DER 的 SHA-1，再按 Base64 编码得出；微软 Android 平台配置页已显示对应保存结果。

当前仅修改 `app-thunderbird/src/debug/kotlin/net/thunderbird/android/auth/TbOAuthConfigurationFactory.kt` 中的 Microsoft Client ID 和回调；复用原有 AppAuth 浏览器授权码流程、端点和 scope。
Beta、Daily、Release 的签名与回调尚未配置为自有发布注册，其他邮件提供商的上游 OAuth 参数也尚未替换；不能据此发布这些构建。

## 验证边界

- 控制台已显示注册成功及 Android 回调保存成功。
- 本机截图：`/private/tmp/thunderbird-plus-registration-success.jpg`、`/private/tmp/thunderbird-plus-android-redirect.jpg`；临时文件可能在系统清理后失效。
- 当前只配置 OAuth 参数；个人 Outlook 登录、令牌刷新、IMAP/SMTP 和别名 From 均未实测。
- 原有委托 scope 包含 `IMAP.AccessAsUser.All`、`SMTP.Send`、`offline_access` 和基础身份 scope；它们不等于已经获得邮箱访问授权。后续用户同意请求时再进行权限核对。
- 控制台显示未验证发布者的提示。微软文档说明，在启用风险升级同意策略的组织中，新多租户应用请求超出基本登录的权限时，其他租户普通用户的同意可能被阻止。未验证任何企业租户可用性，不把注册成功当作 Microsoft 365 企业邮箱已兼容。
- 调试包名与签名用于当前开发验证；包名、签名证书或发布渠道变化后，应重新核对并登记回调。

## 官方依据

- [注册应用及账户类型](https://learn.microsoft.com/en-us/entra/identity-platform/quickstart-register-app)
- [Android 包名、签名哈希和回调配置](https://learn.microsoft.com/en-us/entra/identity-platform/tutorial-mobile-app-android-prepare-app?tabs=workforce-tenant)
- [IMAP / SMTP OAuth 委托 scope](https://learn.microsoft.com/en-us/exchange/client-developer/legacy-protocols/how-to-authenticate-an-imap-pop-smtp-application-by-using-oauth/)
- [发布者验证及组织同意限制](https://learn.microsoft.com/en-us/entra/identity-platform/publisher-verification-overview)

## 调试构建验证

### 仅名称、包名及必要 Microsoft 配置的原版基线

用户要求恢复原版功能后，已核对当前 HEAD 等于官方 `THUNDERBIRD_24_0`（`73a7b7fffcaed6db51c5b0aaa90e4ee012549563`）。8,165 个受管理文件中仅 6 个有改动，均为应用名称、安装包名和调试版 Microsoft OAuth；其余 8,159 个文件与上游一致。另有中文名称资源及开发验证记录。未加入自动翻译、推送适配、别名扩展或 UI 重构，本机 SDK 配置保留用于构建。

通过 Android Studio MCP 终端执行 `:app-thunderbird:assembleFossDebug :app-thunderbird:lintFossDebug :app-thunderbird:spotlessCheck`，结果 `BUILD SUCCESSFUL in 2m 3s`（4,877 tasks）。日志 `/private/tmp/firephenix-minimal-baseline-build.log`。最终 APK 包名、英文/中文名称、证书、DEX Client ID 与新回调均已核验；SHA-256 仍为 `ae6d35278927535997d0802fad275a9d7a1898d51422ab6548958db83ef32861`。本次没有安装到手机，也未登录邮箱或验证收发。正式发布签名及 Beta/Daily/Release 自有 OAuth 仍未配置。

### 历史构建

Microsoft 配置修改后，`:app-thunderbird:assembleFossDebug :app-thunderbird:spotlessCheck` 已成功，耗时 1 分 10 秒。重新打包的 DEX 已核对自有 Client ID 与回调存在，原 Microsoft Client ID 不在 DEX 中；APK 证书哈希与注册值一致。该次 APK SHA-256 为 `66b5e90a9b0756d897a349157ff6e5da15521a95e86c1f81178d197a4c51ad20`，已通过 ADB 保留数据更新安装。随后进行了名称资源修改，最终 APK 哈希需以该修改后的构建为准。

## 显示名称和项目目录更新

用户于 2026-10-14 将名称改为英文 `FirePhenix`、中文“火凤凰”。已在同一个微软应用的“品牌打造和属性”页面保存 `FirePhenix`，未新建另一应用。原始注册显示名称为 `Thunderbird++`；上述旧注册截图记录初始创建过程，当前名称截图位于 `/private/tmp/firephenix-microsoft-name.jpg`。

Android 显示名称与品牌名称已更新：默认语言为 `FirePhenix`，中文资源为“火凤凰”。外层项目目录已迁移到 `<project-parent>`；Android Studio 打开 `android/`。迁移核验 8,169 个源码及新增文件内容、Git HEAD、状态、配置和 21,509 条历史均保持一致。
