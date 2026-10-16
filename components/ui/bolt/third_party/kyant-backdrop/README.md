# 液态玻璃复用来源

本轮按照用户提供的 [Liquid Glass Design 资源库](https://liquidglassdesign.com/resources) 查阅组件和示例，不自行制作玻璃材质贴图。

- [iOS and iPadOS 27 Figma Kit 介绍](https://liquidglassdesign.com/resources/ios-and-ipados-27-figma-kit)：已查看组件总览图及介绍，Figma 原文件请求返回 403，未导入该文件，也未将 SF Symbols 或 Apple 图片打包到 Android。
- [明暗按钮参考](https://liquidglassdesign.com/gallery/liquid-glass-button-ios-26-light-dark)：已查看实际参考图，用于对照胶囊外形、边缘高光、通透度及明暗表现。
- Android 运行时复用 [Kyant0/AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass) 的 Backdrop 光学渲染和示例交互。版本固定为 `io.github.kyant0:backdrop:2.0.0`、`shapes:1.2.0`，来源为 Maven Central。
- 当前项目 Compose 为 1.11.1。Backdrop 2.0.0 的公开依赖为 Compose 1.11.0；2.0.1 使用 1.12.0。选择 2.0.0，保持当前框架版本，避免为了玻璃效果升级原版 UI 的依赖。
- 许可证为 Apache-2.0，保留 [LICENSE](LICENSE)；实际来源校验值及复制文件列表见 [provenance.json](provenance.json)。APK 同时打包许可证与署名文件。

复用内容包括现成 lens 折射、色散、vibrancy、高光、阴影，以及示例中的 InteractiveHighlight、拖动动画、滑块和开关。适配涉及包路径、项目格式、Compose 帧时钟及输入／无障碍衔接。旧 View 层复用 2.0.0 发布源码中的相同 shader 数学，仅提供本项目窗口与控件的绘制桥接。

材质只在液态玻璃外观启用；原版入口保留原来的 Material／Bolt 绘制。实际编译、设备、视觉和回归结果见后续交付记录，集成源码本身不代表效果或性能已验收。
