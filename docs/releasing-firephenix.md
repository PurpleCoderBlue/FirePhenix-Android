# FirePhenix 发行准备

当前准备版本：`0.1.0-alpha.2`，`versionCode = 35`，FOSS Release，应用 ID `app.firephenix.android`。
Debug 使用不同包名 `app.firephenix.android.debug`，现有调试数据不会因正式包构建而迁移或删除。

## 长期签名

签名私钥和密码只保存在本机被 Git 忽略的 `.signing/` 目录，不上传源码、GitHub Secrets、Release 或构建日志。
请在本机之外保留加密备份；后续升级需继续使用同一签名密钥。

公开证书 SHA-256：

```text
09837b9db26bd0a003358a1bd1b0df8154086c912dd693becb27a6ce078de000
```

构建约定读取 `.signing/tb.release.upload.properties`。自行派生时使用自己的密钥和注册，下面只是占位模板，不包含真实密码：

```properties
tb.release.storeFile=../.signing/your-release.p12
tb.release.storePassword=REPLACE_LOCALLY
tb.release.keyAlias=your-key-alias
tb.release.keyPassword=REPLACE_LOCALLY
```

```bash
./gradlew :app-thunderbird:assembleFossRelease --max-workers=2
```

产物：`app-thunderbird/build/outputs/apk/foss/release/app-thunderbird-foss-release.apk`。
使用 Android SDK `apksigner verify --verbose --print-certs` 验证签名，并单独生成 SHA-256 校验文件；APK 和校验文件作为 GitHub Release 附件，不能提交到源码 Git。

## Microsoft 正式包回调

沿用已有 FirePhenix 应用注册，不重建账号、不新增权限或 Client Secret。
之前登记的是 Debug 包及调试证书，不适用于本次新签名正式包。

在现有注册的“身份验证 → 添加平台 → Android”中增加：

|       字段       |                                  值                                   |
|----------------|----------------------------------------------------------------------|
| 包名             | `app.firephenix.android`                                             |
| Signature hash | `Ok/e2+RhERw1tisbMAXsEp5dOGc=`                                       |
| 回调 URI         | `msauth://app.firephenix.android/Ok%2Fe2%2BRhERw1tisbMAXsEp5dOGc%3D` |

保留当前 Debug 回调，不能用正式包条目覆盖它。正式源码必须使用同一注册及上述回调；包内签名和回调值需一起核对。
上述证书哈希与 Client ID 是公开标识，不是私钥或 Client Secret。

依据：[Microsoft Android 回调配置](https://learn.microsoft.com/en-us/entra/identity-platform/tutorial-mobile-app-android-prepare-app?tabs=workforce-tenant)、[上游 Fork 约定](../README.thunderbird.md#forking)。

后台当前停在登录页面，尚未完成新增登记，也未验收正式包 Outlook 登录。验证前只准备安装包，不公开为可用发行版。
Beta、Daily 及其他提供商 OAuth 也不能借此视为已完成自有注册。

## 发布检查

1. 全量测试、Lint、Detekt、Spotless 和所涉 JVM／真机回归通过，明确仍有慢帧等验收边界。
2. 审阅源码暂存清单、历史增量及安装包，不包含个人文件、邮件、日志、测试入口、密钥或密码。
3. 构建包的版本、签名证书、Microsoft 回调和对应源码提交一致；发布检查记录实际 APK 校验值。
4. `origin/main` 保留历史正常快进，不强推或同步上游开发代码。
5. GitHub 使用 alpha 预发布标签，仅上传明确核对过的 APK 与校验文件；未完成登录验证时不公开发行。
6. 发布后核对远端提交、附件名称、下载校验值与签名；中文 README 为默认，顶部切换英语。

