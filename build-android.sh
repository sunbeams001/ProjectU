#!/bin/bash
# ProjectU - Android 构建脚本

echo "🚀 开始构建 ProjectU Android 版本..."

# 清理旧的构建
echo "📦 清理旧构建..."
./gradlew clean

# 构建 Debug APK
echo "🔨 构建 Debug APK..."
./gradlew :composeApp:assembleDebug

# 检查构建结果
if [ $? -eq 0 ]; then
    echo "✅ 构建成功！"
    echo "📱 APK 位置: composeApp/build/outputs/apk/debug/"
    ls -lh composeApp/build/outputs/apk/debug/*.apk
else
    echo "❌ 构建失败，请检查错误信息"
    exit 1
fi

