@echo off
REM ProjectU - Desktop Windows 构建脚本 (仅 Windows x64)

echo 🚀 开始构建 ProjectU Desktop Windows 版本...

REM 清理旧的构建
echo 📦 清理旧构建...
call gradlew.bat clean

REM 创建 Windows 可分发包
echo 🔨 创建 Windows 可分发包...
call gradlew.bat :composeApp:packageDistributionForCurrentOS

if %ERRORLEVEL% == 0 (
    echo ✅ 构建成功！
    echo 💻 Windows 可执行文件位置: composeApp\build\compose\binaries\main\app\
    dir composeApp\build\compose\binaries\main\app\
) else (
    echo ❌ 构建失败，请检查错误信息
    exit /b 1
)

pause
