# FirePhenix Git 与上游升级约定

2026-10-16 按项目所有者明确要求记录。

## 版本来源

- 当前产品源码以 Thunderbird 正式版标签 `THUNDERBIRD_24_0` 为基线，提交 `73a7b7fffcaed6db51c5b0aaa90e4ee012549563`。
- GitHub fork 创建时的 `main` 为上游开发线快照 `304074faf48c64bf8dfef35fd58554dc500f044a`。它不是本项目应自动采用的产品基线。
- 首次上传保留本机正式版基线历史与原远端历史，提交树采用经过检查的本机 FirePhenix 文件。远端原有开发线只作为历史父提交保留，不把其文件修改合入产品。

## 应用版本

- 首个测试版本为 `0.1.0-alpha.1`，内部 `versionCode` 为 `34`；调试构建按既有规则追加 `-SNAPSHOT`。
- 本轮明确采用 `0.1.0-alpha.2`，内部 `versionCode` 为 `35`，计划按预发布处理；签名、OAuth 和附件核验完成前不公开为可用发行版。
- FirePhenix 产品版本与 Thunderbird 上游版本分别记录，不能以升级上游为理由自动覆盖产品版本。
- 后续安装包的 `versionCode` 必须递增；APK Release 与 Git 标签的创建另行进行，不由源码上传自动触发。

## 日常提交

- `origin` 指向 `https://github.com/PurpleCoderBlue/FirePhenix-Android.git`，主分支为 `main`。
- 官方仓库仅作为 `upstream` 来源，禁止日常推送；不要从它执行 `pull`，也不要直接合并它的 `main`、Beta、Daily。
- 不使用 GitHub 的 Sync fork 更新产品主分支，不配置自动跟随官方默认分支的同步任务。
- 不强推、不重写已发布历史；保留有意义的文件删除。采用 Conventional Commits，提交前审阅暂存内容。
- APK、构建输出、真机截图、邮件、密钥、签名私钥及本机配置不进入源码 Git。凭据若已泄漏，忽略文件不能代替撤销与轮换。

## 升级条件和流程

只有新的官方正式版发布后才考虑升级；预发布版、Beta、Daily 和开发线提交均不满足条件。

1. 核实官方 Release 的正式版身份及其对应标签／提交。读取版本信息或下载对象不等同于合入代码。
2. 新建独立升级分支，在该正式版源码上重新应用 FirePhenix 的产品差异并逐项审阅，不依赖一次默认分支自动合并。
3. 核对 API、迁移、OAuth、许可证、依赖及 FirePhenix 功能，完成构建、相关测试和真机回归。
4. 记录版本差异、失败和未验证项；验证通过后再将升级结果合入 FirePhenix `main`。

首次上传为保留远端历史而建立的历史连接，不代表接受过那条开发线的代码，也不能代替日后正式版升级审阅。
