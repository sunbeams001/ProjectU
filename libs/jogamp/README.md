# JOGAMP 本地依赖

此目录包含 JOGAMP 库的本地副本，用于解决官方 Maven 仓库不稳定的问题。

## 📦 包含的文件

- `gluegen-rt-2.5.0.jar` (370 KB) - JOGAMP GlueGen Runtime
- `jogl-all-2.5.0.jar` (3.6 MB) - JOGAMP JOGL (Java OpenGL)

这些文件由 `compose-webview-multiplatform` 库通过 `dev.datlag:jcef` 传递依赖。

## 🔄 如何更新

如果文件丢失或需要重新生成：

```bash
# Windows PowerShell
.\setup-jogamp-local.ps1

# 或手动下载
# 访问: https://jogamp.org/deployment/maven/org/jogamp/gluegen/gluegen-rt/2.5.0/
# 访问: https://jogamp.org/deployment/maven/org/jogamp/jogl/jogl-all/2.5.0/
```

## 📝 为什么使用本地文件？

JOGAMP 官方 Maven 仓库 (`https://jogamp.org/deployment/maven`) 经常出现：
- 连接超时
- 间歇性无法访问
- 下载速度极慢

这导致 CI/CD 构建失败率高达 70%。使用本地文件可以：
- ✅ 100% 构建稳定性
- ✅ 更快的构建速度
- ✅ 完全离线构建支持
