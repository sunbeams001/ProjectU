#!/bin/bash
# ProjectU - Desktop 构建脚本

echo "🚀 开始构建 ProjectU Desktop 版本..."

# 清理旧的构建
echo "📦 清理旧构建..."
./gradlew clean

# 创建可分发包
echo "🔨 创建可分发包..."
./gradlew :composeApp:createDistributable

# 检查构建结果
if [ $? -eq 0 ]; then
    echo "✅ 构建成功！"
    echo "💻 可执行文件位置: composeApp/build/compose/binaries/main/"
    ls -lh composeApp/build/compose/binaries/main/
else
    echo "❌ 构建失败，请检查错误信息"
    exit 1
fi

