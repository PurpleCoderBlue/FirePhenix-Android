# FirePhenix 安全说明

FirePhenix 是 Thunderbird for Android 的独立衍生项目。上游的安全审计、CASA 评估及签名指纹不表示 FirePhenix 已获得相同评估或使用相同证书；历史信息保存在 [上游安全说明](SECURITY.thunderbird.md)。当前没有正式发行签名或已发布 APK Release。

## 报告安全问题

如本仓库启用了 [GitHub 私密漏洞报告](https://github.com/PurpleCoderBlue/FirePhenix-Android/security/advisories/new)，请优先使用。若入口不可用，可在 [Issues](https://github.com/PurpleCoderBlue/FirePhenix-Android/issues) 留下不含利用细节的联系请求，不要公开邮件、凭据、令牌、可用攻击代码或私人截图。

## 凭据与构建产物

- 不提交真实 API 密钥、OAuth Client Secret、访问令牌、签名私钥、服务账户文件或本机配置。
- API 服务配置与处理缓存由设备 Keystore AES-256-GCM 加密，保存在 App 私有 no-backup 目录，普通设置导出不包含它们。
- OAuth Client ID、应用包名及回调签名哈希是公开标识，不等同于密钥；自行构建须注册自己的应用与签名。
- `artifacts/`、本地日志、真机截图、邮件导出及构建输出不进入源码提交。`.gitignore` 不能移除已经进入 Git 历史的秘密；发现泄漏须先撤销／轮换相应凭据。
- 上游已经受管理的测试证书／假凭据属于测试夹具，不用于正式签名。

## 安装前核验

仅安装来源和签名可核验的 APK。未来发布时应一并提供 SHA-256 和正式签名证书信息；当前不能用 Thunderbird 官方证书指纹验证 FirePhenix。

```bash
apksigner verify --verbose --print-certs path/to/FirePhenix.apk
```

仓库源码检查、构建成功及调试版安装不等同于独立安全审计。
