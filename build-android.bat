@echo off
REM ProjectU - Android 构建脚本 (Windows)

echo 🚀 开始构建 ProjectU Android 版本...

REM 清理旧的构建
echo 📦 清理旧构建...
call gradlew.bat clean

REM 构建 Debug APK
echo 🔨 构建 Debug APK...
call gradlew.bat :composeApp:assembleDebug

if %ERRORLEVEL% == 0 (
    echo ✅ 构建成功！
    echo 📱 APK 位置: composeApp\build\outputs\apk\debug\
    dir composeApp\build\outputs\apk\debug\*.apk
) else (
    echo ❌ 构建失败，请检查错误信息
    exit /b 1
)

