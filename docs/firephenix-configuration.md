# FirePhenix／火凤凰：配置与验收说明

本说明对应本地构建。首轮功能记录见 [实施记录](engineering/technical-designs/0005-firephenix-implementation.md)，液态玻璃资源复用、真机回归及最新交付见 [玻璃改造记录](engineering/technical-designs/0006-firephenix-liquid-glass.md)。

## 进入设置

- 外观：设置 → 常规设置 → 显示 → 外观。应用现仅保留液态玻璃，不再提供原版切换。
- 液态玻璃底层背景：外观页向下滚动，可选择墨蓝（默认）、暖灰、凤凰拼色或自选图片。图片居中裁切，用于统一液态玻璃背景。导入图片需不超过 32MiB／一亿像素，保存副本的长边限制为 2560 像素；取消或失败保留原选择。图片保存在 App 私有 noBackup 目录，不进入普通设置导出，换设备后需重新选择。
- 语言：设置 → 常规设置 → 显示 → 语言。玻璃外观下为限宽、可上下滚动的居中弹窗，沿用原来的语言选择和保存逻辑。
- 翻译、总结与发件人名单：设置 → 常规设置 → 互动 → 翻译与总结；玻璃阅读页的翻译按钮直接执行翻译，再点恢复原文；总结与翻译相邻，更多菜单保留双语、服务、语言、重新生成等选项。下载完整邮件与显示远程图片同在发件人栏右侧。
- 别名：设置 → 对应账户 → 发送邮件 → 管理身份，进入“发件别名”页。首次创建和导入账户后的入口也已接入。

## 配置在线服务

翻译和总结分别保存配置。默认服务列表内点选服务，长按已有项编辑或删除，列表末项添加服务；对应的发件人名单入口位于各自分组下方。另一动作已有大模型配置时，编辑器可以选择从中复制地址、提供方、模型、可选参数和密钥；复制后分别保存，后续修改不会联动。复制的密钥不会回填到可见输入框。更换复制后的地址或提供方时，必须重新填写对应密钥。保存只做本地校验，不向服务发送邮件，也不证明密钥、模型或服务可用。

|            类型            |                         基础地址／地址                          |               协议与填写要求                |
|--------------------------|----------------------------------------------------------|--------------------------------------|
| 千问                       | https://maas.qianwenaiapi.com/compatible-mode/v1         | 用户指定地址；Chat Completions，填写密钥、实际可用的模型 |
| OpenAI                   | https://api.openai.com/v1                                | Chat Completions，填写密钥与模型             |
| DeepSeek                 | https://api.deepseek.com/v1                              | Chat Completions，填写密钥与模型             |
| 自定义兼容服务                  | 用户自行填写 HTTPS 基础地址                                        | 标准 Bearer 认证的 Chat Completions       |
| Anthropic／Claude         | https://api.anthropic.com/v1                             | 官方 Messages 协议，填写密钥与模型               |
| Microsoft Translator     | https://api.cognitive.microsofttranslator.com            | Translator v3；填写订阅密钥，区域按资源填写         |
| Google Cloud Translation | https://translation.googleapis.com/language/translate/v2 | Cloud Translation v2；填写 API key      |

AI 类型按千问、OpenAI、DeepSeek、自定义、Anthropic 排列。机器翻译在另一类型中选择，总结仅选择 AI 服务。

兼容服务填写基础地址，客户端追加 `/chat/completions`；Anthropic 追加 `/messages`；Microsoft 追加 `/translate`。Google 填写表中的完整 v2 地址。不要把密钥写进 URL。

“可选参数”填写 JSON 对象，例如：

```json
{"temperature":0.2,"max_tokens":4096}
```

可选参数也可以留空，按空对象保存；名称留空时使用提供方名称。必填项或参数格式错误会在保存按钮附近显示并滚动到错误提示，保存期间显示状态。不会静默丢弃配置。

这是格式示例；字段是否被接受取决于你选择的模型。模型、消息、流式开关、系统指令和工具调用等受保护字段不能在这里覆盖。客户端不开启模型工具执行，也不在失败后换用另一家服务。

密钥留空仅在提供方和 API 地址未变时保留原密钥，或使用本次明确复制的匹配配置；更换目标后必须重新填写。整个服务配置（地址、模型、可选参数和密钥）及处理缓存保存在 App 私有 no-backup 目录，使用 Android Keystore 的 AES-256-GCM 加密，不进入普通设置导出。密钥生成和加解密在设备端完成，不在源码中内置密钥。不要在聊天、源码或截图中提供真实密钥。

## 阅读、翻译与总结

- 翻译支持原文、译文、双语对照、重新处理和取消。译文修改 HTML 副本中的文本节点，复用原有邮件渲染与附件解析。
- 翻译处理完整正文；总结默认提取最新内容，排除可识别的引用历史。需要时可开启最近引用层作为总结参考。
- 翻译和总结的手动语言、服务、执行状态和结果分别保存；手动语言默认均为 `zh-Hans`。自动处理使用该动作的默认服务和默认输出语言。
- 识别使用 Android 系统语言识别接口；设备不支持或置信度不足时显示未知。在线翻译协议也使用自动源语言识别。
- 不上传附件或图片。总结中的引用识别覆盖常见 blockquote、Gmail 和 Outlook 分隔结构；非标准邮件排版仍需结合原文核对。
- 原邮件始终可读，翻译／总结不会写回服务器原文。
- 翻译按钮直接执行翻译，再点恢复原文；总结与翻译相邻，更多菜单保留双语、服务、语言、重新生成和取消。总结完成后，同一个顶部按钮切换“显示总结／隐藏总结”；处理中分别显示“翻译中／总结中”，点击可取消，不另占状态行。下载完整邮件与显示远程图片同在发件人栏右侧，仅在邮件不完整或存在被阻止的图片时显示。

未配置、认证失败、余额／计费要求、限流、超时、网络错误、输出异常和不完整输出都有对应状态。服务生成内容的准确性需要结合原文核对；已完成的千问固定文本调用和仍未验收的整封邮件／其他服务见文末诊断记录。

## 发件人名单

旧规则全部停用，四个手动名单从空白开始，不迁移发件人、主题、文件夹或域名条件；已保存的服务和加密密钥保留。

- 自动翻译名单、自动总结名单：完整 From 地址精确匹配，使用对应默认服务。仅在打开完整邮件时触发；垃圾邮件和超过自动长度上限的邮件仍不自动处理。
- 自动下载图片名单：允许该发件人的远程图片。
- 禁止下载图片名单：不加载远程图片，并隐藏加载图片按钮；优先于允许项。

设置 → 常规设置 → 互动 → 发件人名单，可切换和管理四个名单。邮件详情中任何参与者的菜单也提供四个复选项，菜单显示的是当前邮件的实际 From 地址；不是被点击的收件人或 Reply-To。多个 From 时禁用加入操作。

内置 15 个品牌的地址／域名目录默认允许图片，Anthropic／Claude 默认禁止。精确的手动选择覆盖内置默认项；自动翻译和总结绝不由内置目录触发。未知发件人需手动加载；没有读取 Outlook 联系人权限。来源和匹配项见 [内置发件人目录](firephenix-official-senders.md)。目录匹配不代表邮件身份认证。

## 长邮件、取消与缓存

自动上限为 30,000 个 Unicode 码点，按正文文本计算。手动动作超过上限时确认一次，然后连续处理选定范围，内部最多每段 4,000 个码点、每批 100 个文本项；不会逐段再问，也不会把前半部分标成成功。

总结分段后继续合并，限制最大合并轮数并检测无法收敛的输出。处理中状态由对应按钮显示。中途失败、取消或输出不完整时，不保存半成品。

缓存按账户、文件夹／邮件、正文、服务配置版本、模型、语言和相关选项隔离。切换阅读模式不重新请求；重复进入或并发处理共享请求和进度。保存服务配置会更新版本，使旧结果失效。服务更改后，旧任务不能把新密钥用于旧地址。

可在服务设置清理加密结果缓存。删除账户时取消相关处理，清理该账户缓存、专属规则及别名偏好；全局服务、四个发件人名单及其他账户的数据保留。真实服务的请求限额、分段效果及计费尚未验证。

## 账户显示昵称与文件夹名称

抽屉顶部账户旁的编辑图标可打开账户常规设置，修改“账户显示昵称”并保存。此名称用于本机账户列表；原有自定义名称保留。默认邮箱名称按精确已知域名或标准收信主机显示 Outlook／Gmail，未识别服务保留邮箱地址。各账户可分别修改，实际邮件发件身份继续使用原设置。

收件箱、发件箱、草稿、已发送、已删除、归档和垃圾邮件按 App 当前语言显示；服务器文件夹名称和路径不被重命名。普通自建文件夹保留用户名称。

## Outlook 别名

本版使用已授权的手动方案，填写微软账户中真实存在的别名。App 不创建微软别名，也不把保存地址当作已验证能够发信；没有增加 Graph 权限或更换邮件后端。

每个账户的默认发件地址与收藏相互独立，收藏项优先展示。默认值只影响 App，不改变微软主别名。主登录身份不能在此页删除。

发件别名页只有一个地址输入下拉框。候选仅从当前账户本机已加载邮件提取：已发送文件夹取发件地址，其他文件夹取收件人与抄送地址。
候选不证明地址属于你，也不会自动注册成发件身份；只有明确选择并点击添加后才保存。不会额外搜索服务器或增加邮件权限。
默认、收藏和移除操作位于每个已配置别名的管理菜单。

选择顺序：

1. 本次手动选择；
2. 草稿／重发的原 From；
3. 回复邮件命中的收件别名；
4. 与收件人通信的唯一已发送历史；
5. 账户默认值。

存在歧义时必须选择后继续编辑；失效的显式选择或原 From 不会默默改成另一个地址。历史只采用仍在身份列表中的地址。只查询本机已加载的已发送历史，不再提供服务器查询按钮。撰写页显示实际 From，仍沿用原有 MIME、签名和发送路径。

本轮没有代发邮件。请用至少两个真实已有别名，自行检查收件端 From、Sender、Reply-To 和回复路由。服务器拒绝仍由原发送错误流程展示，不会自动改发件地址重发。

## 外观与尺寸

应用仅保留液态玻璃外观，仍可选择跟随系统、浅色和深色。外观页只有玻璃模糊与背景模糊两种滑块；移除内容背景模糊和不透明度设置。玻璃模糊范围 0–32，默认 16：0 清透，32 强磨砂。预览和数值输入均保留，正文文字和图片保持清晰。

背景模糊默认 12，滑块初始显示 0–128，可输入 0–512 的整数扩展范围。拖动时数值与滑块直接跟手，释放后只保存最终值；不按历史数值排队重放。玻璃模糊与背景模糊独立保存；背景、主题色及明暗选择保留。

玻璃主题色默认从原始背景图的主色提取，再生成浅色／深色的前景和容器配色；不从已经模糊、加遮罩的图像取色。关闭自动取色后点击“自定义”，在颜色轮盘弹窗中选择；点击“应用”保存，取消保留原值。手动颜色会保留，可随时重新开启自动取色。

玻璃原生页面及弹窗增加轻微位移和淡入淡出；App 关闭动画或系统任一相关动画缩放为零时停用。没有修改系统动画设置。

Compose 复用 Backdrop 2.0.0 的背景采样、折射、色散、高光及控件示例；旧 View 页面复用同源着色器，通过 GPU 显示列表桥接。邮件 WebView 和独立渲染表面不参与二次绘制，采样时只保留其背景填充，邮件正文继续由原有安全渲染链路显示。Android 13/API 33 起启用运行时光学效果；较旧系统使用可读替代呈现。模糊关闭时仍保留支持设备上的折射和边缘高光。

邮件自动分栏以实际窗口宽度至少 840dp、可用配置高度至少 480dp 为条件；紧凑窗口单栏。已有用户明确保存的分栏偏好会保留，需在常规设置的显示／布局中选择自动分栏。新服务编辑页限制宽度并支持滚动、键盘避让；服务设置在可用宽度至少 720dp 且高度至少 600dp 时按翻译／总结两栏排列，手机和窄窗口上下排列。

验证记录只代表实际测试的设备、窗口和页面，详见交付记录，不把它等同于全设备穷举验证。

## 仍需你完成的外部步骤

- 已用本机保存的千问配置完成一次固定短文本翻译和一次固定短文本总结，均为真实 HTTP 200 推理；不代表邮件长度、全部服务或限额均已验收。后续邮件速度测试单独记录。
- 用真实已有别名自行完成发信头部和回复路由检查。
- 荣耀真机目前未授予 App 通知权限。需要你授权，并配合真实来信、锁屏、省电与断网恢复验证；本轮未改这些系统设置。
- 已连接真机是荣耀 CHG-W60 平板；荣耀 500 Pro 的机型专项验证仍需连接该设备。
- FCM／荣耀 Push 仅提供未来信号处理 API 边界，未装配 SDK、注册令牌或部署网关；现有邮件同步与通知继续复用上游能力。

## Remote image lists and request scheduling (updated 2026-10-16)

- Settings > General settings > Interaction > Sender lists manages four exact From-address lists. All previous rules are disabled during upgrade; manual lists start empty. Sender lists use the existing AES-GCM encrypted configuration and are excluded from ordinary export/backup.
- Manual block beats manual allow. Exact manual choices override the built-in image defaults; Anthropic/Claude defaults to blocked, other documented brands default to automatic images. Unknown addresses require a manual tap. Multiple From addresses never automatically allow images.
- Outlook contacts are not read. No OAuth scope or contact permission was added. Saving a list makes no network requests; only explicitly added AI senders qualify for automatic processing when a complete, non-spam message is opened.
- New accounts default to automatic complete body download up to 5 MiB, increased from 128 KiB. Existing explicitly stored account values remain unchanged; adjust the account fetching setting for an existing account. This limit does not authorize remote image or attachment downloads and does not change automatic AI processing limits.
- Translation uses two bounded workers and one shared two-request semaphore across messages and actions. Original batch order is retained, repeated items within a batch share a result, cancellation propagates to child workers, and failed partial output is never cached. Existing full-result cache and in-flight request deduplication remain in effect. No service/model fallback or automatic paid retry was added.
- Source paragraphs already in each batch provide batch context. Additional subject/cross-batch context was not integrated: automatic approval rejected that added sensitive-data transfer. No extra context-summary request is made. Actual short Qwen fixture requests completed in about 4.1 seconds for translation and 2.8 seconds for summarization; email-length latency remains a separate check. The deterministic local queue regression models 2,000 ms serial work completing in 1,000 ms at concurrency two.
- Glass Activity, Fragment and popup enter/exit resources share 360 ms and the same easing, with app/system disabled-animation settings respected. Only the unified Glass appearance remains; page motion no longer uses full-page alpha compositing.

Sources: [Microsoft contact permissions](https://learn.microsoft.com/en-us/graph/api/user-list-contacts?view=graph-rest-1.0); [Read Frog request queue](https://github.com/mengxi-ream/read-frog/blob/f2e72180008812358b501e105b6a5ca66ff53b4e/src/utils/request/request-queue.ts), [batch queue](https://github.com/mengxi-ream/read-frog/blob/f2e72180008812358b501e105b6a5ca66ff53b4e/src/utils/request/batch-queue.ts), [translation cache](https://github.com/mengxi-ream/read-frog/blob/f2e72180008812358b501e105b6a5ca66ff53b4e/src/utils/host/translate/in-memory-translation-cache.ts). Architectural methods were reimplemented in Kotlin; GPL source was not copied into this fork.

## 桌面图标与诊断

桌面与应用内统一使用清理过透明边缘的液态玻璃凤凰图标。只有一个固定 MainActivity 启动入口，不再通过启用／禁用两个启动别名切换图标，避免荣耀桌面移除当前任务。图标是静态资源，不会实时折射系统桌面。

Debug 版的 `FP-Motion` 仅记录固定页面名、阶段、耗时和慢帧指标；私有 `cache/ui-motion.log` 异步写入且限制大小，不含邮件正文、地址、URL 或密钥。

## 千问连接与速度诊断（2026-10-15）

- 保留用户指定的 `https://maas.qianwenaiapi.com/compatible-mode/v1` 和已保存模型，不换服务或读取设备密钥到电脑。
- 千问 Chat Completions 使用 SSE；严格检查完成标记、输出条数和 JSON，推理过程不作为译文或缓存。传输失败不自动重试付费请求。
- 官方确认的 Qwen3.5–3.8 混合模型系列，在未填写 `enable_thinking` 时默认关闭思考以减少普通翻译和总结的等待；明确填写 `{"enable_thinking":true}` 可保留思考。未知或仅推理模型不注入此参数。支持范围见实际协议实现中的模型匹配表达式。
- “检测连接（不生成内容）”只 GET 模型元数据：通过不代表推理可用。“测试模型（固定短文本）”会真实调用当前配置，翻译使用约 1000 字符的公开固定示例，总结使用公开短例，可能消耗已有额度；不会读取当前邮件。
- Debug 安全诊断只记录 provider、动作、HTTP 状态、开始回答／推理和完成／错误阶段，不记录提示词、正文、输出、地址或密钥。完整服务配置继续使用设备端 Keystore AES-256-GCM。
- 已复现旧版真实邮件请求收到 HTTP 200 后等待至 180 秒总超时，说明该次并非 DNS／鉴权失败。关闭默认思考是针对该等待的修正，后续公开长例及用户邮件的实际结果以交付检查记录为准。

依据：[千问思考模式](https://platform.qianwenai.com/docs/developer-guides/text-generation/thinking)、本项目协议回归及 `artifacts/firephenix-liquid-glass/checks/ai-fixed-text-device.log`。

2026-10-16 诊断补充：设备已保存的千问配置对 1116 字符公开固定文本真实翻译为 HTTP 200，首内容约 0.464 秒、完整约 3.072 秒；不代表整封邮件验收。三次代理模型测试授权均已消耗。动画和回复弹层修正、验证边界见 [诊断记录](engineering/technical-designs/0007-firephenix-diagnosis.md)。

### 官方发件人目录

官方目录仅随应用版本更新，不提供联网刷新、下载或独立更新缓存。
目录只控制远程图片默认策略，不触发自动翻译、总结，也不显示官方身份标志。
完整名单、来源与维护方法见 [内置发件人目录](firephenix-official-senders.md)。

## 2026-10-16 阅读与界面补充

- 邮件切换使用上下方向，阅读正文优先。只有从顶部开始下拉或从底部开始上拉的新手势才交给邮件分页；左右滑动不再切换邮件。
- 原生与 Compose 页面复用 360ms 的方向与时长策略；Activity 关闭整窗过渡，固定壁纸单独绘制，内容层执行动画。
- 隐藏原生页面、正文和独立弹窗的滚动条；不禁用正文或列表滚动。Android 10 起通过公开窗口检查 API 覆盖独立窗口，玻璃弹窗绘制也会注册布局观察。
- 中文原文在本地识别完成前不闪现翻译入口，识别后隐藏翻译及跳过自动翻译，总结入口保留。使用现有 Android 本地分类器，并为较短中文、夹杂产品名的中文增加文字脚本判断；不发起语言识别 API 请求。识别不确定时保留翻译，不把少量汉字或日语一概当作中文。
- 方法参考 Read Frog 的[本地检测与可选模型检测](https://github.com/mengxi-ream/read-frog/blob/main/src/utils/content/language.ts)、[有限文本取样](https://github.com/mengxi-ream/read-frog/blob/main/src/utils/content/page-language.ts)和[目标语言跳过](https://github.com/mengxi-ream/read-frog/blob/main/src/utils/host/translate/target-language-skip.ts)，未引入浏览器扩展运行时或新的依赖。
- 图片仅按四个名单及随应用发布的品牌目录判断，不再使用 Outlook 联系人放行。GitHub 同时收录 notifications@github.com 与官方文档披露的 noreply@github.com；手动禁止仍优先，不允许仿冒后缀或未知子域。

真机检查记录与未完成的验收必须分别说明。逐帧录制用于检查过渡是否存在和背景是否位移，录屏不能替代实际帧截止时间统计，也不能证明所有页面满帧。
