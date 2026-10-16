# FirePhenix · 火凤凰

<img src="artwork/firephenix/phoenix-carrying-mail-glass.png" alt="液态玻璃火凤凰叼着邮件" width="144">

基于 [Thunderbird for Android](https://github.com/thunderbird/thunderbird-android) 的个人邮件客户端，围绕 Outlook 多别名、邮件翻译与总结和液态玻璃界面进行开发。当前基线为 Thunderbird 24.0（`73a7b7fffcaed6db51c5b0aaa90e4ee012549563`）。

这是独立衍生项目，与 Mozilla、Thunderbird 官方没有隶属或背书关系。目前为开发版本，尚未完成正式发行准备。

FirePhenix 应用版本为 `0.1.0-alpha.1`，内部 `versionCode` 为 `34`；调试构建追加 `-SNAPSHOT`。产品版本独立于上游 Thunderbird 24.0 基线。

## 获取应用

- [本仓库 Releases](https://github.com/PurpleCoderBlue/FirePhenix-Android/releases)：截至 2026-10-16 尚未发布 APK Release，可先按下文构建调试版。
- [Komi Store](https://github.com/komi-store/komi-store)（原 GitHub Store）：开源的 GitHub Release 应用获取与更新工具。本仓库发布带 APK 的 Release 后，可在其中添加本仓库；目前不表示已上架或可以下载。

Thunderbird 官方的 Google Play、F-Droid 下载入口对应上游应用，不是 FirePhenix 的发行渠道。

## 功能

|  范围  |                                                当前实现                                                |
|------|----------------------------------------------------------------------------------------------------|
| 邮件   | 复用上游账户、文件夹、搜索、附件、草稿、撰写与同步能力，增加账户显示昵称                                                               |
| 界面   | 统一液态玻璃外观；跟随系统、浅色及深色；内置背景、自选图片、自动取色与手动主题色；玻璃模糊和背景模糊分别调整                                             |
| 翻译   | 一键翻译并在原邮件排版中显示，再点恢复原文；支持双语、取消、重试与加密缓存                                                              |
| 总结   | 独立服务配置，正文内展示可收起的总结；优先处理最新内容                                                                        |
| 服务   | 千问、OpenAI、DeepSeek、自定义 OpenAI 兼容服务、Anthropic；翻译另支持 Microsoft Translator 和 Google Cloud Translation |
| 自动处理 | 翻译与总结分别设置开关和规则，打开邮件时匹配；自动排除垃圾邮件、不完整及超长正文                                                           |
| 别名   | 手动管理已有账户别名、默认值与收藏；复用发件身份、草稿和回复流程，歧义时要求选择                                                           |
| 远程图片 | 精确发件人允许规则；未获允许时阻止自动加载，保留单封邮件的手动显示入口                                                                |
| 多尺寸  | 窄窗口单栏；宽窗口按可用尺寸适配，服务设置分别显示翻译栏与总结栏                                                                   |

Android 最低 API 23；运行时折射等光学效果需要 Android 13 / API 33 或更高，旧系统使用替代呈现。桌面图标为静态玻璃风格资源。

## 配置与隐私

详细步骤见 [配置说明](docs/firephenix-configuration.md)。

- 外观入口：设置 → 常规设置 → 显示 → 外观。
- 服务入口：设置 → 常规设置 → 互动 → 翻译与总结。点选默认服务，长按编辑或删除，列表末项添加；翻译和总结配置可明确复制后分别保存。
- 不内置 API 密钥。完整服务配置和处理缓存使用 Android Keystore 的 AES-256-GCM 加密，存放于 App 私有 no-backup 目录，不进入普通设置导出。
- 手动处理或已启用且匹配的自动规则，会把所选正文发送给配置的服务；不默认上传附件或图片。API 可用性和费用取决于所填服务、模型及账户。
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

自行构建时必须使用自己的 OAuth 注册和签名，核对包名、证书哈希及 redirect URI。仓库中的 OAuth Client ID 是公开标识，不是 Client Secret；现有调试回调只匹配开发时登记的签名。Beta、Daily、Release 及其他邮件提供商的自有 OAuth 尚未配置完整，不能直接作为正式发行版本使用。详见 [上游派生要求](README.thunderbird.md#forking)。

## 验证与已知限制

以下为 2026-10-16 前本地实际检查记录的摘要，不代表完整测试套件或所有设备验收通过：

- Foss Debug APK 构建及 Lint 通过；相关 Detekt、Kotlin/Kotlin Gradle 格式检查通过。
- 最近一轮回复弹层、邮件阅读、主题及抽屉范围测试共 86 项通过。扩大 Bolt 测试后仍有 5 项因 `No ThemeColorScheme provided` 失败，尚未解决；没有关闭检查。
- 已在一台荣耀 CHG-W60 平板验证安装、回复菜单进入撰写及返回；未代发邮件。其他手机、平板、窗口和长期后台运行仍需验证。
- 千问仅完成固定公开文本的真实翻译／总结测试；约 1116 字符示例完整返回约 3.1 秒。不能据此保证整封邮件耗时，其他服务主要完成协议测试。
- 长邮件和转场性能仍在改进；最近邮件样例仍有慢帧，不能宣称已经完全流畅。
- Outlook 别名的真实收件端 `From` / `Sender` / `Reply-To` 和回复路由尚未验收。
- Outlook 联系人读取未接通，未增加 `Contacts.Read` 授权；当前远程图片自动允许条件实际只有发件人规则。联系人适配边界已预留，不能当作已可用。
- FCM／荣耀 Push 仅预留 API 边界，没有接入 SDK、注册令牌或部署推送网关；现有同步继续复用上游。
- 正式签名、发行版 OAuth、APK Release 及商店发行尚未完成。

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
