# MailApp Android 上游基线验证

验证日期：2026-10-14。操作通过 Android Studio MCP 的集成终端进行，未修改上游受管理源码、OAuth 配置或全局代理设置。

## 构建

- 上游：`THUNDERBIRD_24_0`，提交 `73a7b7fffcaed6db51c5b0aaa90e4ee012549563`。
- 工程：`<project-root>`。
- Gradle wrapper：9.7.0；实际 daemon JDK：Temurin 21.0.12。
- 构建时自动安装 SDK Platform 37.0 revision 2 和 Build-Tools 36.0.0，使用现有已接受的 SDK 许可证。
- 成功命令：

```sh
env JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
  ANDROID_HOME='<android-sdk>' \
  ./gradlew :app-thunderbird:assembleFossDebug --max-workers=2 \
  '-Dorg.gradle.jvmargs=-Xmx3g -Dfile.encoding=UTF-8' \
  -Dhttp.proxyHost= -Dhttps.proxyHost= --info --console=plain
```

结果：`BUILD SUCCESSFUL in 9m 5s`，`2191 actionable tasks: 1718 executed, 460 from cache, 13 up-to-date`。
详细日志位于本机 `/private/tmp/mailapp-android-baseline-info.log`，属于临时诊断文件。
前一次构建为开启详细诊断而主动终止，其 daemon disappeared 记录不作为源码构建失败结论。

命令中的空代理主机参数没有证明全部连接绕过代理：实测该次构建存在到 `127.0.0.1:7890` 的连接。
官方 Google Maven 同一 1 MiB 文件片段的 curl 测试约为直连 447 KiB/s、代理 433 KiB/s；该采样不是整体 Gradle 下载速度保证。

## APK 与真机启动

- APK：`app-thunderbird/build/outputs/apk/foss/debug/app-thunderbird-foss-debug.apk`，约 45 MiB。
- applicationId：`net.thunderbird.android.debug`；versionName：`24.0-SNAPSHOT`；versionCode：33，依据同目录 `output-metadata.json`。
- SHA-256：`1bf8c8f3d6b5aeda3d87d24ba1c5d17bc949672f02ebe21bc3b057ba143ba4b8`。
- 用户选择荣耀手机验证；ADB 报告机型 `MEP_AN00`，Android 16 / API 36。
- 安装前 `pm path net.thunderbird.android.debug` 未返回已有包；`adb install` 返回 `Success`。
- 不带 MAIN action/category 的显式启动命令返回 Activity 不存在；以下标准启动 Intent 返回 `Status: ok`，当前 Activity 为 `app.k9mail.feature.launcher.FeatureLauncherActivity`，应用进程存在：

```sh
adb shell am start -W --user 0 -a android.intent.action.MAIN \
  -c android.intent.category.LAUNCHER -p net.thunderbird.android.debug
```

通过 `adb exec-out screencap -p` 获取并检查截图，确认中文欢迎页、开始按钮和导入 K-9 设置入口正常显示。
本机截图：`/private/tmp/mailapp-baseline-onboarding.png`。
手机 crash buffer 输出只有厂商封装头，未获得可读的崩溃条目；不能据此宣称已完成稳定性验证。

## 未验证部分

- 自有 Microsoft 应用注册和 OAuth 配置，Outlook 登录、收发、别名 From 与附件。
- 自动翻译、翻译设置与缓存：尚未实现。
- 荣耀后台同步、通知、耗电和长期稳定性。
- 单元测试、Lint、Detekt、Spotless：本次只构建未修改的上游基线，未执行这些检查。
- 当前 AVD 缺少所配置的 Android 36.1 ARM64 系统镜像，启动失败；用户已选择真机验证，未补装镜像。

此 APK 是上游调试基线，不是完成定制功能的发布版本。
