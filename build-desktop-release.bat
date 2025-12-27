@echo off
REM ProjectU - Desktop Release 构建脚本 (生成 MSI 安装包)

echo 🚀 开始构建 ProjectU Desktop Release 版本...

REM 清理旧的构建
echo 📦 清理旧构建...
call gradlew.bat clean

REM 创建 Windows MSI 安装包
echo 🔨 创建 Windows MSI 安装包...
call gradlew.bat :composeApp:packageMsi

if %ERRORLEVEL% == 0 (
    echo ✅ 构建成功！
    echo 📦 MSI 安装包位置: composeApp\build\compose\binaries\main\msi\
    dir composeApp\build\compose\binaries\main\msi\
) else (
    echo ❌ 构建失败，请检查错误信息
    exit /b 1
)

pause
