[简体中文](README.md) · [English](README.en.md)

# FirePhenix · 火凤凰

<img src="artwork/firephenix/phoenix-carrying-mail-glass.png" alt="液态玻璃火凤凰叼着邮件" width="144">

基于 [Thunderbird for Android](https://github.com/thunderbird/thunderbird-android) 的个人邮件客户端，围绕 Outlook 多别名、邮件翻译与总结和液态玻璃界面进行开发。当前基线为 Thunderbird 24.0（`73a7b7fffcaed6db51c5b0aaa90e4ee012549563`）。

这是独立衍生项目，与 Mozilla、Thunderbird 官方没有隶属或背书关系。目前为 alpha 预发布版本。

FirePhenix 应用版本为 `0.1.0-alpha.2`，内部 `versionCode` 为 `35`；调试构建追加 `-SNAPSHOT`。产品版本独立于上游 Thunderbird 24.0 基线。

## 获取应用

- [下载 0.1.0-alpha.2](https://github.com/PurpleCoderBlue/FirePhenix-Android/releases/tag/v0.1.0-alpha.2)：2026-10-17 发布的长期签名预发布 APK，内部版本号 35；附件提供 SHA-256 校验文件。
- [Komi Store](https://github.com/komi-store/komi-store)（原 GitHub Store）：开源的 GitHub Release 应用获取与更新工具。可在其中添加本仓库读取 GitHub Release；商店兼容性仍待验证，不表示已上架。

Thunderbird 官方的 Google Play、F-Droid 下载入口对应上游应用，不是 FirePhenix 的发行渠道。

## 功能

|  范围  |                                                当前实现                                                |
|------|----------------------------------------------------------------------------------------------------|
| 邮件   | 复用上游账户、文件夹、搜索、附件、草稿、撰写与同步能力，增加账户显示昵称                                                               |
| 界面   | 统一液态玻璃外观；跟随系统、浅色及深色；内置背景、自选图片、自动取色与手动主题色；玻璃模糊和背景模糊分别调整                                             |
| 翻译   | 一键翻译并在原邮件排版中显示，再点恢复原文；支持双语、取消、重试与加密缓存                                                              |
| 总结   | 独立服务配置；同一个总结按钮切换显隐，优先处理最新内容                                                                        |
| 服务   | 千问、OpenAI、DeepSeek、自定义 OpenAI 兼容服务、Anthropic；翻译另支持 Microsoft Translator 和 Google Cloud Translation |
| 自动处理 | 翻译与总结分别维护发件人名单，打开邮件时匹配；自动排除垃圾邮件、不完整及超长正文                                                           |
| 别名   | 手动管理已有账户别名、默认值与收藏；复用发件身份、草稿和回复流程，歧义时要求选择                                                           |
| 远程图片 | 允许与禁止发件人名单；内置品牌目录仅随应用版本更新；Anthropic 默认禁止并隐藏加载按钮                                                    |
| 多尺寸  | 窄窗口单栏；宽窗口按可用尺寸适配，服务设置分别显示翻译栏与总结栏                                                                   |

Android 最低 API 23；运行时折射等光学效果需要 Android 13 / API 33 或更高，旧系统使用替代呈现。桌面图标为静态玻璃风格资源。

## 配置与隐私

详细步骤见 [配置说明](docs/firephenix-configuration.md)。

- 外观入口：设置 → 常规设置 → 显示 → 外观。
- 服务入口：设置 → 常规设置 → 互动 → 翻译与总结。点选默认服务，长按编辑或删除，列表末项添加；翻译和总结配置可明确复制后分别保存。
- 不内置 API 密钥。完整服务配置和处理缓存使用 Android Keystore 的 AES-256-GCM 加密，存放于 App 私有 no-backup 目录，不进入普通设置导出。
- 手动处理或手动加入且匹配的发件人名单，会把所选正文发送给配置的服务；不默认上传附件或图片。API 可用性和费用取决于所填服务、模型及账户。
- 千问预设基础地址为 `https://maas.qianwenaiapi.com/compatible-mode/v1`，客户端追加 `/chat/completions`。填写实际可用模型和自己的密钥，不把密钥放进 URL。
- 新账户正文自动下载上限为 5 MiB；已有账户显式保存的下载值不会被覆盖。此设置不授权下载远程图片或附件，也不改变 AI 自动处理上限。

## 构建

使用 JDK 21、Android SDK Platform 37 和仓库自带 Gradle Wrapper 9.7.0。目标 SDK 为 36。Android Studio 打开仓库根目录，选择 `app-thunderbird` 的 `fossDebug` 变体。`app-k9mail` 是保留的上游模块，不是 FirePhenix 的产品入口。

```bash
git clone https://github.com/PurpleCoderBlue/FirePhenix-Android.git
cd FirePhenix-Android
./gradlew :app-thunderbird:assembleFossDebug --max-workers=2 \
  '-Dorg.gradle.jvmargs=-Xmx3g -XX:MaxMetaspaceSize=1g -Dfile.encoding=UTF-8'
```

配置本机 `JAVA_HOME`、`ANDROID_HOME` 或未纳入 Git 的 `local.properties`；不要把本机 SDK 路径写入受管理配置。

输出：`app-thunderbird/build/outputs/apk/foss/debug/app-thunderbird-foss-debug.apk`。调试包名为 `app.firephenix.android.debug`，正式应用 ID 为 `app.firephenix.android`。

自行构建时必须使用自己的 OAuth 注册和签名，核对包名、证书哈希及 redirect URI。仓库中的 OAuth Client ID 是公开标识，不是 Client Secret；现有调试回调只匹配开发时登记的签名。本项目已在自有 Microsoft 注册中分别登记 Debug 和长期签名 Release 的 Android 回调；Beta、Daily 及其他邮件提供商的自有 OAuth 尚未配置完整。详见 [上游派生要求](README.thunderbird.md#forking)。

## 验证与已知限制

以下为 2026-10-17 本地实际检查记录的摘要，不代表完整测试套件或所有设备验收通过：

- Foss Debug APK 构建及 Lint 通过；相关 Detekt、Kotlin/Kotlin Gradle 格式检查通过。
- 通知账号名称空值回归已修复，legacy:core 全部 756 项测试通过；账号模块 8 项通过；Bolt 109 项在默认中文环境通过，移除了英文文案依赖。全量 test、lint、detekt、spotlessCheck 已通过；详见[本轮界面回归记录](docs/engineering/technical-designs/0009-firephenix-alpha2-verification.md)。
- 已在一台荣耀 CHG-W60 平板验证安装、回复菜单进入撰写及返回；未代发邮件。其他手机、平板、窗口和长期后台运行仍需验证。
- 千问完整 HTML 处理链路已用 20 段公开正文（3451 字符，另含链接文本）的 HTML 样本验证：翻译约 9.1 秒、总结约 1.4 秒；缓存重复读取分别约 16／35 毫秒。段落和链接保留。未发送私人邮件；其他服务主要完成协议测试，实际耗时取决于服务、模型和正文。
- 已减少动画期间重复视图遍历；同套设置导航本轮采样慢帧为 4.71%，仍有慢帧，不能宣称所有页面已经满帧。
- Outlook 别名的真实收件端 `From` / `Sender` / `Reply-To` 和回复路由尚未验收。
- 远程图片只按四个发件人名单及随应用发布的内置目录判断，手动禁止优先；不读取 Outlook 或设备联系人，也不以联系人身份放行图片。
- FCM／荣耀 Push 仅预留 API 边界，没有接入 SDK、注册令牌或部署推送网关；现有同步继续复用上游。
- 长期签名 APK 已公开为 alpha.2 预发布；正式包已在真机通过 Microsoft OAuth 返回应用并成功发现邮箱文件夹，Debug 回调保留。未发送邮件；商店发行尚未完成。

诊断详情见 [问题与验证记录](docs/engineering/technical-designs/0007-firephenix-diagnosis.md)。原始真机截图、邮件、日志、密钥、本机配置与构建产物不纳入源码仓库。

## 上游升级策略

固定采用 Thunderbird 正式版基线，不同步官方 `main`、Beta 或 Daily 开发修改。当前为 `THUNDERBIRD_24_0`。只有新的正式版发布后，才在独立升级分支评估、合并及验证；通过后再更新本项目主分支。不要直接使用 GitHub 的 Sync fork 同步默认开发分支。详见 [Git 与上游升级约定](docs/firephenix-git-policy.md)。

## 开发与反馈

- [提交问题](https://github.com/PurpleCoderBlue/FirePhenix-Android/issues)：请附版本、系统、复现步骤及脱敏日志，不要附邮件正文、地址、密钥或令牌。
- [开发环境](docs/contributing/development-environment.md)、[测试指南](docs/contributing/testing-guide.md)、[架构](docs/architecture/README.md)与 [AGENTS.md](AGENTS.md) 继续遵循上游约定。
- 上游自动签名、发布、通知和机器人工作流仅允许在上游仓库运行；本仓库没有配置自动发行。
- 安全说明见 [SECURITY.md](SECURITY.md)。

## 开源与致谢

本项目遵循 [Apache License 2.0](LICENSE)，保留上游作者、许可证及第三方声明。原上游介绍保存在 [README.thunderbird.md](README.thunderbird.md)。

- [Thunderbird for Android / K-9 Mail](https://github.com/thunderbird/thunderbird-android)：邮件与应用基础。
- [Kyant Backdrop / AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass)：玻璃材质与示例；复用部分的声明见 [third_party](components/ui/bolt/third_party)。
- [Read Frog / 陪读蛙](https://github.com/mengxi-ream/read-frog)：参考请求调度、批处理及缓存方法，在 Kotlin 中独立实现；未复制其 GPL 源码。

凤凰图标资源与生成记录见 [artwork/firephenix](artwork/firephenix)。
