# Technical Design: 火凤凰 / FirePhenix Android 第一版

- ADR: [API/Internal 边界](../adr/0009-api-internal-split.md)
- Status: **Proposed** — 用户需求已确认；上游基线已构建并在荣耀手机首次启动，邮件服务及新增功能尚未验证。
- 调研日期：2026-10-14。
- 上游基线：`THUNDERBIRD_24_0`，提交 `73a7b7fffcaed6db51c5b0aaa90e4ee012549563`。

## Summary

首版服务个人 Outlook 账户，在荣耀 500 Pro 上运行，使用 M1 MacBook Air 开发。用户确认需要切换同一微软账户的多个邮箱别名作为 From 地址；翻译使用用户已有的 OpenAI 兼容 API，地址、模型和密钥尚未配置。日历、联系人等功能可以通过外部浏览器入口解决。以上产品范围来自本次用户确认。

用户最新确认名称为中文“火凤凰”、英文“FirePhenix”，采用该拼写；此前暂定的“雷鸟++ / Thunderbird++”已被替换。微软注册显示名称同步采用英文。

保留成熟的原生邮件收发、多账户、附件、搜索和通知能力。新增自动翻译、原文/译文/双语阅读和翻译设置。当前不部署 Push Gateway。

## Current State

### 开源基础选择

选择 Thunderbird Android 24.0 稳定版。[官方发布](https://github.com/thunderbird/thunderbird-android/releases/tag/THUNDERBIRD_24_0)和[README](../../../README.md)提供发布及项目依据。仓库使用 Apache-2.0，保留原 [LICENSE](../../../LICENSE) 与 [NOTICE](../../../NOTICE)。Android 版支持 IMAP/POP/SMTP；其功能不能直接等同于桌面版的日历和 Exchange 能力。[Android FAQ](https://support.mozilla.org/en-US/kb/thunderbird-android-8-faq)

备选 FairEmail 支持标准邮件协议，但其 Android 客户端同样没有解决统一多平台客户端问题；Twake Mail/Tmail 提供 Flutter 多平台构建，却要求 JMAP 服务器配置。基于本次“原生 Outlook 邮件、当前不引入服务器”的要求，采用 Thunderbird 是工程判断。[FairEmail 项目及 FAQ](https://github.com/M66B/FairEmail)、[Tmail README](https://github.com/linagora/tmail-flutter)

上游逐步采用 Kotlin Multiplatform，当前仍有 legacy Android 阅读界面。共享模块方向有利于以后复用部分代码，但不能据此承诺现成的 Mac/iOS 客户端。[上游架构](https://thunderbird.github.io/thunderbird-android/docs/latest/architecture/index.html)

### 已核实的集成位置

- 邮件正文通过 `legacy/ui/legacy/.../MessageContainerView.kt` 的 `displayMessageViewContainer()` 展示；附件解析器及图片加载策略由现有阅读链路管理。
- 阅读界面已有通过 Compose 桥接 feature API 的模式，例如 `MessageReaderBottomSheet`；新逻辑应遵循 [legacy 集成规范](../../architecture/legacy-module-integration.md)。
- `app-common` 负责实现装配；新模块采用 `:api` / `:internal` 边界、Koin、Coroutines/Flow、MVI 和 Bolt 组件，遵循 [AGENTS.md](../../../AGENTS.md) 与 ADR-0009。

### Outlook 及别名边界

个人 Outlook 的 IMAP 为 `outlook.office365.com:993`（TLS），SMTP 为 `smtp-mail.outlook.com:587`（STARTTLS），要求 OAuth2；微软文档说明 POP/IMAP 默认关闭，应由用户开启。[微软协议设置](https://support.microsoft.com/en-us/outlook/pop-imap-and-smtp-settings-for-outlook-com)

Thunderbird Android 已有管理发件身份和在 From 处切换身份的能力，优先复用。[官方身份配置说明](https://support.mozilla.org/en-US/kb/thundermail-and-thunderbird-android)

微软说明 Outlook.com 可以使用账户别名发信，但该文档只描述其自身撰写界面，不能证明第三方 SMTP 必定保留别名 From。[微软别名说明](https://support.microsoft.com/en-us/outlook/send-email-from-a-different-address-in-outlook-com) 首版必须实测收件方原始邮件及回复地址，失败时先查明协议限制，再向用户确认是否需要改变后端方案。

Fork 必须使用自己的 OAuth 配置和回调地址。[上游 Forking 要求](../../../README.md#forking) 用户已明确授权修改 Microsoft OAuth 并提交应用注册；2026-10-14 已创建自有微软应用，按用户最新名称改为 `FirePhenix`，保存当前调试 APK 对应的 Android 回调。当前仅调试版 Microsoft 配置切换为自有注册；其他构建类型和提供商仍是上游配置，不能作为自有发布版发布。参数及证据见 [微软注册记录](0003-mailapp-microsoft-registration.md)。[微软 OAuth 文档](https://learn.microsoft.com/en-us/exchange/client-developer/legacy-protocols/how-to-authenticate-an-imap-pop-smtp-application-by-using-oauth/)

## Proposed Design

### 自动翻译

1. 新增独立翻译 feature，公开稳定的翻译、设置与阅读控件 API；实现与服务协议放在 `:internal`。通过 `app-common` 装配，legacy 仅添加必要的桥接调用。
2. 在本机检测邮件语言；默认目标为简体中文。中文、无法可靠识别的短文本和没有可翻译文字的邮件保留原文及手动入口。初选 bundled 语言检测方案，避免依赖 Google Play Services 动态安装检测模型；具体依赖需在基线构建后验证。[ML Kit 语言检测](https://developers.google.com/ml-kit/language/identification/android)
3. 第一适配器使用 Chat Completions 格式的 OpenAI 兼容 API；endpoint、model、API key 由用户配置。官方协议只作为格式依据，第三方兼容性必须另行验证。[Chat Completions 参考](https://developers.openai.com/api/reference/resources/chat/subresources/completions/methods/create)
4. 从已经进入上游安全阅读链路的 HTML 提取正文文字。保留表格、链接属性、CID 图片和原有结构；不发送附件、图片、CSS 或脚本。模型只返回文字数据，按节点数量和类型校验，再作为转义文字写回 DOM，不能让模型直接生成可执行 HTML。
5. 保留原文对象；译文和双语模式仅影响显示，不改变本地原邮件、转发内容或回复引用。
6. 缓存按账户、内容摘要、目标语言、服务地址、模型和实现版本隔离；提供清除入口和容量限制。缓存与密钥排除在备份及普通设置导出之外；密钥使用 Android Keystore 加密，禁止正文、密钥和账户地址进入日志。
7. 未完成配置或用户未开启翻译时不发送请求。服务错误、取消、长邮件超限和不符合协议的输出保留原文，不静默切换其他服务；避免自动重试产生重复费用。

### 背景同步和通知

复用上游同步、通知和 IMAP 推送链路；其推送需要后台服务，Android 省电模式也会影响工作调度。没有服务器时不能承诺无需后台运行的即时云端邮件提醒。[上游推送说明](https://support.mozilla.org/en-US/kb/configure-push-email-thunderbird-android)、[Android Doze](https://developer.android.com/training/monitoring-device-state/doze-standby)

首版验证通知权限、网络恢复、重启、Doze 和荣耀手机实际后台行为；必要设置通过标准 Android 设置入口引导，厂商私有入口应以设备验证为依据。

FCM 和荣耀 Push 当前属于扩展预留；没有配置和 Gateway 时显示未启用状态，不能标为已实现实时邮件推送。未来客户端收到可信推送信号后进入现有同步队列，厂商 SDK 隔离为可选实现，服务端凭据不进入 APK。[FCM 服务端要求](https://firebase.google.com/docs/cloud-messaging/server-environment)、[荣耀客户端集成](https://developer.honor.com/en/doc/guides/101080)、[荣耀服务端下行](https://developer.honor.com/en/doc/guides/101087)

### 外部入口

提供个人 Outlook 日历和联系人网页入口，使用外部浏览器。邮件读写继续走原生协议；没有实现或宣称日历、联系人双向同步。

## Migration and Rollout

1. 编译、运行未修改的上游基线，再做功能扩展。当前源码已经完整检出，全部 8,165 个受管理文件按 Git 内容及模式核验，初始工作区干净。2026-10-14 已通过 Android Studio MCP 完成 `:app-thunderbird:assembleFossDebug`，并在荣耀手机安装、启动至中文首次设置界面；详见 [基线验证记录](0003-mailapp-baseline-validation.md)。
2. 按上游版本构建，保持 JDK、SDK、Gradle 要求；不为当前环境擅自降级。稳定版要求 JDK 21+、Gradle 9.7.0、compile SDK 37，详见 [环境指南](../../contributing/development-environment.md)、[wrapper](../../../gradle/wrapper/gradle-wrapper.properties)、[构建配置](../../../build-plugin/src/main/kotlin/ThunderbirdProjectConfig.kt)。
3. 建立自有应用标识和 OAuth 配置；Microsoft 注册、登录及任何实际测试发信由用户明确授权后进行。
4. 验证个人 Outlook 收发及别名，再验证翻译和阅读体验。
5. 验证荣耀设备通知行为，生成带有明确功能状态与限制说明的可安装 APK。

## Testing and Verification

- 基线：Gradle 构建成功且产生 APK；通过 Android Studio MCP 检查模块和构建结果，并在模拟器或设备启动。IDE 成功连接或空构建结果不能替代 APK 及启动验证。
- Outlook：OAuth 登录、刷新 token、同步、收发、附件和账户切换。实际测试发信先取得收件地址及发送授权。
- 别名：至少两个已授权账户别名；检查收件方 `From`、`Sender`、`Reply-To`，并验证回复能到达所选别名。`Return-Path` 单独检查，不将它与显示 From 混为一谈。
- 翻译：HTML 表格、内联格式、CID、链接、中文/混合语言、长文本、乱码和空正文；恶意模型输出只作为转义文字。
- 缓存：账户和内容隔离、配置变化失效、清除和容量限制。
- 网络：通过本地测试服务覆盖超时、取消、授权失败、限流和不完整返回；未获授权不调用真实收费服务。
- 通知：模拟器和荣耀 500 Pro 的锁屏、Doze、重启和网络恢复；未做真机测试时明确说明。
- 质量：运行受影响模块的测试、Lint、Detekt 和 Spotless；只对改动文件进行格式修复。

## Open Technical Questions

- 用户服务的 API 地址、具体协议差异、模型、密钥配置与真实翻译调用授权尚未提供。
- 自有 Microsoft 应用注册与调试版回调已建立；实际个人 Outlook OAuth 登录、委托权限同意和收发仍需用户参与验证，尚未宣称成功。
- Outlook 实际 SMTP 发信与别名保留行为尚未验证。
- 荣耀手机已通过 ADB 连接，报告 Android 16 / API 36；MagicOS 版本与实际邮件后台行为尚未验证。
- 基线 APK 已构建、安装并首次启动；SDK Platform 37.0 revision 2 与 Build-Tools 36.0.0 已安装。此结果不代表 Outlook、翻译或通知验证完成。

本文件记录调研、设计及验证计划，以上未完成项均不代表功能已经实现。
