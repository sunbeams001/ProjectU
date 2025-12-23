# ProjectU 签名配置检查脚本
# 用于验证签名配置是否正确设置

Write-Host "`n=== ProjectU 签名配置检查 ===`n" -ForegroundColor Cyan

# 检查 keystore.properties 文件
$keystorePropsFile = "keystore.properties"
if (Test-Path $keystorePropsFile) {
    Write-Host "✓ 找到 $keystorePropsFile 文件" -ForegroundColor Green
    
    # 读取配置
    $props = Get-Content $keystorePropsFile | Where-Object { $_ -match '=' -and $_ -notmatch '^#' }
    $hasStoreFile = ($props | Where-Object { $_ -match '^storeFile=' })
    $hasStorePass = ($props | Where-Object { $_ -match '^storePassword=' })
    $hasKeyAlias = ($props | Where-Object { $_ -match '^keyAlias=' })
    $hasKeyPass = ($props | Where-Object { $_ -match '^keyPassword=' })
    
    if ($hasStoreFile -and $hasStorePass -and $hasKeyAlias -and $hasKeyPass) {
        Write-Host "✓ 签名配置完整" -ForegroundColor Green
        
        # 检查 keystore 文件是否存在
        $storeFilePath = ($hasStoreFile -split '=')[1].Trim()
        if (Test-Path $storeFilePath) {
            Write-Host "✓ Keystore 文件存在: $storeFilePath" -ForegroundColor Green
            Write-Host "`n签名配置已正确设置！可以执行 Release 构建。" -ForegroundColor Green
            Write-Host "运行命令: .\gradlew.bat assembleRelease`n" -ForegroundColor Yellow
        } else {
            Write-Host "✗ Keystore 文件不存在: $storeFilePath" -ForegroundColor Red
            Write-Host "  请检查路径或生成 keystore 文件" -ForegroundColor Yellow
        }
    } else {
        Write-Host "⚠ 签名配置不完整，请检查以下字段:" -ForegroundColor Yellow
        if (-not $hasStoreFile) { Write-Host "  - storeFile" -ForegroundColor Red }
        if (-not $hasStorePass) { Write-Host "  - storePassword" -ForegroundColor Red }
        if (-not $hasKeyAlias) { Write-Host "  - keyAlias" -ForegroundColor Red }
        if (-not $hasKeyPass) { Write-Host "  - keyPassword" -ForegroundColor Red }
    }
} else {
    Write-Host "⚠ 未找到 $keystorePropsFile 文件" -ForegroundColor Yellow
    Write-Host "`n这是正常的！Release 构建将使用 Debug 签名。" -ForegroundColor Cyan
    Write-Host "`n如需配置正式签名，请执行以下步骤:" -ForegroundColor Cyan
    Write-Host "1. 复制配置模板:" -ForegroundColor White
    Write-Host "   copy keystore.properties.example keystore.properties" -ForegroundColor Gray
    Write-Host "`n2. 生成 keystore (如果还没有):" -ForegroundColor White
    Write-Host "   keytool -genkey -v -keystore release.keystore -alias projectu -keyalg RSA -keysize 2048 -validity 10000" -ForegroundColor Gray
    Write-Host "`n3. 编辑 keystore.properties 填入你的签名信息" -ForegroundColor White
    Write-Host "`n4. 查看详细指南:" -ForegroundColor White
    Write-Host "   docs\guides\签名配置指南.md`n" -ForegroundColor Gray
}

# 检查 .gitignore
Write-Host "`n--- 安全检查 ---" -ForegroundColor Cyan
$gitignoreContent = Get-Content .gitignore -ErrorAction SilentlyContinue
if ($gitignoreContent -match 'keystore.properties') {
    Write-Host "✓ keystore.properties 已在 .gitignore 中排除" -ForegroundColor Green
} else {
    Write-Host "⚠ keystore.properties 未在 .gitignore 中排除（建议添加）" -ForegroundColor Yellow
}

if ($gitignoreContent -match '\*.keystore' -or $gitignoreContent -match '\*.jks') {
    Write-Host "✓ keystore 文件已在 .gitignore 中排除" -ForegroundColor Green
} else {
    Write-Host "⚠ keystore 文件未在 .gitignore 中排除（建议添加）" -ForegroundColor Yellow
}

Write-Host "`n=== 检查完成 ===`n" -ForegroundColor Cyan
