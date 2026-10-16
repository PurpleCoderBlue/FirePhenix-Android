# FirePhenix 液态玻璃资源复用与验证

日期：2026-10-15。本文件记录本轮玻璃外观改造；首轮交付记录保留在 0005。最终 APK、截图、检查状态及校验值以 [本轮交付清单](../../../artifacts/firephenix-liquid-glass/README.md) 为准。

> 更新：以下保留先前迭代记录。2026-10-15 用户已明确改为仅保留液态玻璃；两种模糊、API 与动画的新诊断以 [后续诊断](0007-firephenix-diagnosis.md) 和当前交付清单为准。

## 依据与资源

产品依据为 [最终需求](0004-firephenix-final-requirements.md) 及用户后续要求：参考 Liquid Glass Design 的资源和示例，复用现成材质实现，保留原版 UI。

- [资源库](https://liquidglassdesign.com/resources) 与 [iOS/iPadOS 27 Figma Kit 介绍](https://liquidglassdesign.com/resources/ios-and-ipados-27-figma-kit)：已查看介绍及组件总览图。Figma 原文件请求返回 403，未导入该原文件，也未打包 Apple 图片或 SF Symbols。
- [明暗按钮参考](https://liquidglassdesign.com/gallery/liquid-glass-button-ios-26-light-dark)：用于对照胶囊轮廓、边缘高光和明暗材质。
- 运行时复用 [Kyant0/AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass) 的 Backdrop 效果和控件示例，固定 `backdrop:2.0.0`、`shapes:1.2.0`。该版本的 Compose 依赖为 1.11.0，项目版本目录配置仍为 1.11.1；APK 实际解析出的 Compose 版本为 1.11.2，与此前交付包一致。最终 APK 的 46 项框架版本元数据逐项比对见交付清单。
- [来源、版本及许可](../../../components/ui/bolt/third_party/kyant-backdrop/README.md) 和 [源文件校验记录](../../../components/ui/bolt/third_party/kyant-backdrop/provenance.json) 保留在仓库，APK 附带 Apache-2.0 许可证与署名。

## 实现决策

在已有 Bolt／旧 View 设计系统内接入。玻璃外观使用现成 lens 折射、色散、vibrancy、高光、阴影及示例动画；没有生成玻璃贴图，也没有另写一套折射公式。旧 View 桥接复用发布版着色器，修改包名和常量名，保留公式。

玻璃表面覆盖按钮、输入框、表面、导航栏、抽屉、标签栏、菜单、弹窗和底部面板。开关及滑块复用示例外形和动画，并适配受控状态、禁用、取消、键盘与无障碍操作。滑块的触摸绑定到稳定的整个轨道区域，避免移动中的滑块柄影响拖动坐标。

页面底层背景采用可切换的内置背景或用户自选图片，导航栏可记录页面内容。旧 View 记录本 App 常规 View 的 GPU 显示列表，并排除玻璃控件前景。WebView、SurfaceView、TextureView 不提交第二次绘制，采样仅保留这些区域的背景填充；HTML 正文仍在原来的窗口绘制流程中显示。正文与图片的原有安全渲染链路保留，不使用全界面模糊或全局文字半透明。

按用户最后补充要求，原版外观隐藏整组玻璃模糊控件、预览及说明；选择外观不更改已保存的模糊值。模糊范围仍为 0–32，默认 16。按用户后续要求，0 为清透玻璃，32 为只保留背景大致颜色的强磨砂。强度映射为 `96dp * (level / 32)^2`，普通材质底色透明度从 8% 平滑增加到 78%，中间值 16 对应 24dp 模糊。Compose 和旧 View 使用同一映射；有明确语义颜色的旧 View 按钮保留其颜色对比度。预览使用现有 Bolt 图标和本地化文字作真实采样源，左侧露出清晰背景，右侧使用与全局控件同一个玻璃渲染器，不绘制模糊示意贴图。Android 13/API 33 起使用运行时折射。较旧系统保留可读替代呈现，设置页说明能力限制。原版在关闭玻璃时沿用原来的 Material/Bolt 默认形状、颜色、输入处理及布局。默认材质参数在确认启用玻璃后才读取 Bolt 主题，保留原版输入框无需显式 Bolt 主题的用法。

外观切换时保持场景和 Scaffold 内容的父容器结构稳定，不再迁移带焦点的输入组件；原版与玻璃分支共享相同的内容位置。旧 View 装饰保存并恢复背景、内边距和阴影高度，弹窗主题仅在玻璃外观应用；没有修改账户、邮件内容、服务地址、模型或凭据。

## 验证中修复的问题

- 补齐当前邮件处理代码缺失的 BILLING 文案分支，以及现有测试要求的 HTTP 400/402 错误分类；保留其他当前修改，并保存修改前源码。
- 修复开关回调读到旧状态、禁用时仍响应、纵向滚动误触、键盘操作缺失和切换外观时输入状态丢失。
- 真机发现材质采样越过控件边界形成矩形色块，收紧采样范围并移除重复预览场景后已在真机截图复查。
- 扩展 JVM 回归发现原版输入框因默认参数提前读取主题而报错，改为仅在玻璃分支解析默认材质颜色。
- 补齐 API 33 绘制调用的显式版本防护，不提高最低 Android 版本，不关闭检查。
- 用户报告整页邮件闪烁后，以真机静态阅读页录像复现：237/237 次连续帧比较出现大面积变化；调试暂停玻璃二次采样的对照为 1/177 次。新增嵌套 WebView 回归测试，在旧采样路径上明确得到“预期一次、实际两次绘制”，再将独立渲染表面排除；正式补丁的真机复测结果以交付清单为准。
- 修复滚动内容越过父容器裁剪区域污染导航栏采样：尊重 `clipChildren` 和 `clipToPadding`。新增回归在修复前明确失败（预期红色、实际黑色）；真机顶栏记录区域的纯黑像素比例从 65.17% 降为 0。
- 模糊 0/32 原预览的平均 RGB 差仅 0.259/255，不能将滑块数字变化算作视觉验收。调整强度及预览源后的真机像素与边缘细节对比见交付清单。
- 大模糊核产生透明像素时，锐利原背景仍会从材质下方漏出。给光学层补充不透底的材质底层，前景仍在其上正常绘制。真机最大值选定预览区域的边缘能量比从 0.09285 降到 0.00313（以 0 强度为基准）；扣除透明度变化影响后仍只剩 1.31%，左侧未覆盖背景的边缘量比为 1.0。不将单纯变暗算作强毛玻璃验收。
- 最终设备回归发现：迁移带焦点的只读下拉输入框会在 Android 35/36 触发 `MagnifierNode` 脱离节点异常，造成外观页退出。新增 API 35 回归先复现了与设备相同的异常，再将场景及 Scaffold 改为稳定的父容器结构；修复后的测试通过。设备连续切换和进程连续性证据以最终交付清单为准。

## 验证与交付边界

实际执行结果、失败后的复跑、设备信息及截图索引统一记录在本轮交付清单中。已有源码和 Git 历史保留；未推送、发布或部署。

原版浅色、深色设置页已与修改前截图逐像素对照；排除系统时间等区域后，各比较 5,705,856 像素，早期 `0fd6032c` 候选包均为零差异；最新修订的复测结果见交付清单。该结果只证明所记录页面和状态，不代替全部页面或设备的穷举验收。

真实 API 调用、Outlook 别名实际发信和未完成的设备场景仍需单独验收，不能由协议模拟测试或材质库集成推定成功。未授权代发邮件，也未增加服务费用或新权限。

## 最后补充的界面要求

- 原版外观隐藏整组背景模糊、重置、预览与相关说明，仅保留已存储的玻璃强度值（控件不显示），返回玻璃时恢复显示。
- 远程特殊文件夹与本地特殊文件夹按同一用途本地化：收件箱、发件箱、草稿、已发送、已删除、归档、垃圾邮件。用户自建普通文件夹和服务器 ID／路径保持原样。
- 账户抽屉顶部新增“修改账户显示昵称”入口，打开已有账户常规设置并复用原有名称校验和保存。显式自定义昵称优先；旧名称为空或等于邮箱地址时，根据精确已知邮箱域或已配置的标准收信主机显示 Outlook／Gmail，无法识别则保留邮箱显示。只计算显示值，不迁移覆盖既有账户名称，不改发送身份。
- 贴屏的大面积矩形玻璃表面不再被替换成 24dp 圆角，不绘制人工白色高光外框，由实际窗口边缘裁切。小型圆角玻璃控件仍保留光学高光。原版渲染分支不因此改变。
- 旧设置页的 Preference 复选框由既有 Bolt Switch／Kyant LiquidToggle 显示。保留原始检查状态、变更监听、否决及持久化，通过原控件的点击路径执行更改；桥接只用于 Preference 的 widget_frame，切回原版或销毁 Activity 时恢复原控件并释放 Compose 内容。
- 设置分组标题的实心黑底只在玻璃外观下透明化；原版仍使用保存的原背景。
- 语言选择沿用现有语言值和保存流程；玻璃模式下使用居中弹窗，宽度不超过 480dp，列表高度不超过 360dp 或窗口高度的 55%，支持上下滚动。原版仍走原来的选择流程。
- 底层背景提供墨蓝、暖灰和凤凰拼色，默认墨蓝为平色，替换原来的大范围平滑渐变，以避免该渐变在用户设备上的明显色带。内置平色不能证明设备对其他图片也不会出现色带。
- 自选图片通过系统文档选择入口导入一次，不申请广泛存储或持久 URI 权限。输入限制 32MiB／一亿像素，解码后长边不超过 2560 像素，以 PNG 保存在应用 noBackup 私有目录，文件名按内容 SHA-256 生成；不保留输入文件的元数据，不放进普通设置导出。选择取消或导入失败不覆盖原选择；缺失图片回退到墨蓝并提示重新选择。
- Compose 及旧 View 使用相同背景文件和窗口坐标裁切，避免小型设置开关各自缩放整张图片造成拼接不一致。背景设置入口只在玻璃外观下显示。

## 显示页滚动崩溃排查

用户报告“常规设置 → 显示 → 下滑”后黑屏退回。荣耀 CHG-W60 的本 App 退出记录确认 11:08–11:17 存在多次 `APP CRASH(NATIVE)`，其中有 `status=11`；未取得相应原生回溯，因此不能宣称已定位到具体驱动函数。

代码检查发现，RecyclerView 临时分离的控件可能保留同一个光学背景，但采样注册此前已被清理。修复保留弱注册直到对象自然释放，并为光学背景加标记：即使临时注册集合缺失，采样也不能再次录入光学背景自身。新增回归覆盖嵌套列表项在注册集合为空时不被二次录制，硬件崩溃仍须由真机验收。

候选包 `38f86cf7c287` 已在上述荣耀真机完成两轮各 24 次上下滚动，显示页及 PID 12224 保持，没有新增进程退出事件。第一轮脚本误把 dumpsys 的“保存时间”变化算成退出变化，已修正为比较 timestamp／PID／process／reason／status；第二轮严格比较通过。该候选结果不替代后续最终 APK 的复测，最后版本及检查结果以交付清单为准。

## 服务保存与阅读交互后续修订

空可选 JSON 参数按空对象处理，字段校验与保存状态放到保存按钮附近。2026-10-15 荣耀真机的真实按钮流程成功保存虚构配置，用户随后反馈“看起来能保存了”。该结果不代表真实服务调用通过。

服务设置按翻译／总结分组；宽窗口双栏，手机和窄窗口上下排列。默认服务列表支持点选、长按编辑／删除、末项添加；自动开关和规则位于对应组内。规则排序仅影响同一动作的优先级。

玻璃阅读页翻译按钮首次执行并在原邮件布局显示译文，再次恢复原文，切换已有结果不重复请求；总结位于同组。更多弹窗保留双语、服务、语言、重新生成、取消及旧对话选项。下载完整邮件与显示远程图片同在发件人栏，复用原监听和可见性状态。

已查阅用户指定的 [陪读蛙源码](https://github.com/mengxi-ream/read-frog/tree/f2e72180008812358b501e105b6a5ca66ff53b4e/src/utils/host/translate) 中 translation-text-swap.ts、translation-modes.ts、恢复原文测试及 GPL-3.0 许可，参考文本节点替换与原文保留思路，继续复用本项目 Jsoup 处理计划。未复制该 GPL 源码，未在邮件 WebView 启用 JavaScript。[沉浸式翻译的官方仓库](https://github.com/immersive-translate/immersive-translate) 当前用于发行和反馈，不能视作当前扩展完整源码。

最终构建和设备验收见交付记录，不以源码存在推定视觉通过。

## 2026-10-15 follow-up: sender image rules, bounded translation, mail background

The user confirmed the remote-image condition is sender rule OR Outlook contact. Sender rules are exact, enabled, full From mailboxes; multiple or missing senders fail closed. Shared business behavior applies to both appearances. The general Interaction settings and account Reading settings share one editor. Existing ALWAYS/device-contact settings no longer bypass this policy in FirePhenix; other white-label applications keep their original policy. Outlook contacts remain externally blocked on Contacts.Read rather than being silently replaced with local device contacts.

The previous serial translation path was reproduced by TranslationQueueTest before replacing it with two bounded workers, an app-wide semaphore, batch-local deduplication, ordered reconstruction and existing in-flight/cache handling. Shared cancellation and failed partial output are covered by additional regressions. This is a local simulated throughput result, not a claim about actual model latency. Read Frog source at f2e72180008812358b501e105b6a5ca66ff53b4e informed the queue/batching/context/cache comparison. No GPL source was copied. Additional cross-batch/subject data transfer was rejected by automatic approval and was not implemented; per-batch context uses only the content already being translated.

The important-inline-background regression failed on the old CSS-only path. A presentation pass in the existing HTML cleaner now appends transparent background and theme foreground declarations at inline priority, preserving URLs, image sources and mail structure. Glass rendering also disables WebView algorithmic darkening. Original rendering bypasses this pass. Final physical acceptance must be recorded separately from these local regressions.

Native reader icon buttons now have explicit centered 24 dp icons inside 48 dp targets. All native glass page/popup motion resources use the shared 220 ms duration; Activity overrides complement existing Fragment transitions. New-account automatic body download defaults to 5 MiB; stored account limits and independent remote-image/AI thresholds are preserved.

## 2026-10-15: header, navigation, launcher and frame diagnostics

The native AppBar root cause was reproduced with a real Material 1.14 AppBarLayout: assigning a transparent ColorDrawable is converted to a MaterialShapeDrawable and lift-on-scroll recolors it. Glass now uses a null AppBar backing, keeping the optical Toolbar and restoring the saved Original backing when the style is removed. The Material pixel regression failed before this change and passes after it. Physical CHG-W60 captures on candidate 32fa97a2085f show the scrolled header retaining the same blue RGB mean, with zero dark-neutral pixels in the measured region.

Five production Compose NavHosts share GlassPageMotion (220 ms, FastOutSlowIn, directional slide/fade); native Fragment, Navigation Component and Activity routes use GlassMotion and the same motion resources. The mail ViewSwitcher now also uses GlassMotion for its animation-enabled policy and four motion resources. Original motion remains unchanged. Widget expansion/toggle gestures are not page navigation.

Backdrop sampling traverses native ViewGroups without a second dispatchDraw, preventing duplicate child animation/Compose submission. External renderers such as WebView remain excluded. Embedded Preference toggles obtain an offscreen wallpaper source without painting a rectangular wallpaper behind their control; the physical Display screenshot verifies the rectangle is absent.

Switching launcher aliases in the foreground reproduced a return to Honor Launcher while retaining the same App PID. The device removes the associated task despite DONT_KILL_APP. LauncherAppearanceController keeps UI changes immediate and updates only this App's aliases after all its Activities have stopped for one second; a quick recreation cancels the update. Candidate 32fa97a2085f passed Original and Glass foreground switching with 12 samples each, unchanged PID, and one matching launcher entry after backgrounding. Background icon update can discard the old task on this launcher, so the subsequent launch starts the inbox. No system settings or permissions were changed.

The Glass phoenix icon is a separately generated transparent colored-glass asset. Its edge cleanup used built-in image_gen, preserved the colored phoenix/envelope, and was checked against navy, white, magenta and teal. Only Glass bitmap resources changed. The adaptive launcher tile is a static material-style resource; it cannot live-blur the system wallpaper.

Debug builds can collect fixed UI class/stage/timing metadata under FP-Motion. Honor logcat output returned only unreadable headers, so a bounded asynchronous private cache/ui-motion.log sink records the same non-PII timing events for run-as inspection. No body, account, address, URL or credential is logged. Candidate 32fa97a2085f's three reader cycles measured 14/692 janky frames (2.02%), p95 19 ms, p99 61 ms, and include a 351 ms slow frame. This does not establish that reader jank is solved. Timing shows repeated 12-33 ms Glass HTML presentation parsing on the UI thread; further optimization and physical remeasurement are pending.

### HTML thread optimization and final validation boundary

MessageHtmlRenderer now dispatches non-empty Glass HTML presentation parsing onto Dispatchers.Default and delivers only the latest result on the View lifecycle scope. Empty clearing and Original rendering remain synchronous. Four regressions cover worker dispatcher use, late non-cancellable old work, clearing/Original bypass, and cancellation. Candidate 182a4999b170 built successfully; the targeted tests and changed-module Detekt passed. Full follow-up quality (app lint, root and component Detekt, Kotlin/Kotlin Gradle Spotless) subsequently passed. App lint reports zero errors with inherited warnings.

The physical device then reported Asleep and keyguard showing; two installs of 182a4999b170 returned INSTALL_FAILED_ABORTED: User rejected permissions. No installer-update rollback was attempted because this evidence points to locked-device confirmation. The user has been asked to unlock. The prior 32fa97a2085f candidate remains the last hash-verified physical installation and contains the cleaned icon, background launcher update, native header and control tile fixes. Final performance APK is available but cannot be described as device-verified until installation and the same reader frame loop are rerun.
