#!/bin/bash
# ProjectU 签名配置检查脚本
# 用于验证签名配置是否正确设置

echo ""
echo -e "\033[36m=== ProjectU 签名配置检查 ===\033[0m"
echo ""

# 检查 keystore.properties 文件
KEYSTORE_PROPS="keystore.properties"
if [ -f "$KEYSTORE_PROPS" ]; then
    echo -e "\033[32m✓ 找到 $KEYSTORE_PROPS 文件\033[0m"
    
    # 检查必需的配置项
    HAS_STORE_FILE=$(grep -c "^storeFile=" "$KEYSTORE_PROPS" || echo "0")
    HAS_STORE_PASS=$(grep -c "^storePassword=" "$KEYSTORE_PROPS" || echo "0")
    HAS_KEY_ALIAS=$(grep -c "^keyAlias=" "$KEYSTORE_PROPS" || echo "0")
    HAS_KEY_PASS=$(grep -c "^keyPassword=" "$KEYSTORE_PROPS" || echo "0")
    
    if [ "$HAS_STORE_FILE" -gt 0 ] && [ "$HAS_STORE_PASS" -gt 0 ] && [ "$HAS_KEY_ALIAS" -gt 0 ] && [ "$HAS_KEY_PASS" -gt 0 ]; then
        echo -e "\033[32m✓ 签名配置完整\033[0m"
        
        # 检查 keystore 文件是否存在（去除前后空白）
        STORE_FILE=$(grep "^storeFile=" "$KEYSTORE_PROPS" | cut -d'=' -f2 | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
        if [ -f "$STORE_FILE" ]; then
            echo -e "\033[32m✓ Keystore 文件存在: $STORE_FILE\033[0m"
            echo ""
            echo -e "\033[32m签名配置已正确设置！可以执行 Release 构建。\033[0m"
            echo -e "\033[33m运行命令: ./gradlew :androidApp:assembleRelease\033[0m"
            echo ""
        else
            echo -e "\033[31m✗ Keystore 文件不存在: $STORE_FILE\033[0m"
            echo -e "\033[33m  请检查路径或生成 keystore 文件\033[0m"
        fi
    else
        echo -e "\033[33m⚠ 签名配置不完整，请检查以下字段:\033[0m"
        [ "$HAS_STORE_FILE" -eq 0 ] && echo -e "\033[31m  - storeFile\033[0m"
        [ "$HAS_STORE_PASS" -eq 0 ] && echo -e "\033[31m  - storePassword\033[0m"
        [ "$HAS_KEY_ALIAS" -eq 0 ] && echo -e "\033[31m  - keyAlias\033[0m"
        [ "$HAS_KEY_PASS" -eq 0 ] && echo -e "\033[31m  - keyPassword\033[0m"
    fi
else
    echo -e "\033[33m⚠ 未找到 $KEYSTORE_PROPS 文件\033[0m"
    echo ""
    echo -e "\033[36m这是正常的！Release 构建将使用 Debug 签名。\033[0m"
    echo ""
    echo -e "\033[36m如需配置正式签名，请执行以下步骤:\033[0m"
    echo -e "\033[37m1. 复制配置模板:\033[0m"
    echo -e "\033[90m   cp keystore.properties.example keystore.properties\033[0m"
    echo ""
    echo -e "\033[37m2. 生成 keystore (如果还没有):\033[0m"
    echo -e "\033[90m   keytool -genkey -v -keystore release.keystore -alias projectu -keyalg RSA -keysize 2048 -validity 10000\033[0m"
    echo ""
    echo -e "\033[37m3. 编辑 keystore.properties 填入你的签名信息\033[0m"
    echo ""
    echo -e "\033[37m4. 查看详细指南:\033[0m"
    echo -e "\033[90m   docs/guides/签名配置指南.md\033[0m"
    echo ""
fi

# 检查 .gitignore
echo ""
echo -e "\033[36m--- 安全检查 ---\033[0m"
if grep -q "keystore.properties" .gitignore 2>/dev/null; then
    echo -e "\033[32m✓ keystore.properties 已在 .gitignore 中排除\033[0m"
else
    echo -e "\033[33m⚠ keystore.properties 未在 .gitignore 中排除（建议添加）\033[0m"
fi

if grep -qE '\*.keystore|\*.jks' .gitignore 2>/dev/null; then
    echo -e "\033[32m✓ keystore 文件已在 .gitignore 中排除\033[0m"
else
    echo -e "\033[33m⚠ keystore 文件未在 .gitignore 中排除（建议添加）\033[0m"
fi

echo ""
echo -e "\033[36m=== 检查完成 ===\033[0m"
echo ""
