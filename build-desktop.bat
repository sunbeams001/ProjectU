@echo off
REM ProjectU - Desktop 构建脚本 (Windows)

echo 🚀 开始构建 ProjectU Desktop 版本...

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

