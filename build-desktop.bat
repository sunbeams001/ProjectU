@echo off
REM ProjectU - Desktop 构建脚本 (Windows)
REM 注意：此脚本使用 createDistributable 会包含所有平台资源，体积较大
REM 如需仅构建 Windows 版本，请使用 build-desktop-windows.bat
REM 如需生成发布安装包，请使用 build-desktop-release.bat

echo 🚀 开始构建 ProjectU Desktop 版本...
echo ⚠️  注意：此构建会包含所有平台的资源（体积较大）
echo 💡 提示：如需仅构建 Windows 版本，请使用 build-desktop-windows.bat
echo.

REM 清理旧的构建
echo 📦 清理旧构建...
call gradlew.bat clean

REM 创建可分发包
echo 🔨 创建可分发包...
call gradlew.bat :composeApp:createDistributable

if %ERRORLEVEL% == 0 (
    echo ✅ 构建成功！
    echo 💻 可执行文件位置: composeApp\build\compose\binaries\main\
    dir composeApp\build\compose\binaries\main\
) else (
    echo ❌ 构建失败，请检查错误信息
    exit /b 1
)

