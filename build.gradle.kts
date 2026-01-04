plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.jetbrains.compose) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.ksp) apply false
}

// 应用版本管理任务
apply(from = "version.gradle.kts")

/**
 * 检查 Kotlin 源码中的硬编码中文字符串
 * 
 * 这个任务会扫描所有 Kotlin 源文件，检测字符串字面量中是否包含中文字符。
 * 硬编码的中文应该移到 composeResources/values/strings.xml 中进行国际化。
 * 
 * 使用方式:
 *   ./gradlew checkHardcodedChinese                                    # 默认只警告，不失败
 *   ./gradlew checkHardcodedChinese -PfailOnHardcodedChinese=true     # 发现问题时失败
 * 
 * 排除规则:
 *   - 注释中的中文（单行和多行注释）
 *   - 日志语句中的中文（println, print, Log.x 等）
 *   - 包含 @Suppress("HardcodedChinese") 注解的文件或行
 *   - 测试文件
 *   - 特定的技术字符串（如日语标签、语言名称常量等）
 */
tasks.register("checkHardcodedChinese") {
    group = "verification"
    description = "Check for hardcoded Chinese strings in Kotlin source files"
    
    doLast {
        // 是否在发现问题时使构建失败（默认只警告）
        val failOnError = project.findProperty("failOnHardcodedChinese")?.toString()?.toBoolean() ?: false
        
        // 中文字符的正则表达式（包括中文标点）
        val chinesePattern = Regex("[\\u4e00-\\u9fa5\\u3000-\\u303f\\uff00-\\uffef]")
        
        // 字符串字面量的正则表达式（匹配双引号字符串）
        val stringLiteralPattern = Regex(""""([^"\\]|\\.)*"""")
        
        // 日志函数模式
        val logPattern = Regex("""(println|print|Log\.[dievw]|logger\.|Napier\.)\s*\(""", RegexOption.IGNORE_CASE)
        
        // 要排除的特定字符串（技术性字符串，不需要国际化）
        val excludedStrings = setOf(
            "AI小説",           // 日语标签，用于判断AI生成内容
            "日本語",           // 语言名称常量
            "简体中文",         // 语言名称常量
            "繁體中文",         // 语言名称常量
            "件",              // HTML解析模式，匹配Pixiv页面内容
            "个字符"           // HTML解析模式，匹配Pixiv页面内容
        )
        
        // 要扫描的源码目录
        val sourceDirectories = listOf(
            file("composeApp/src/commonMain/kotlin"),
            file("composeApp/src/androidMain/kotlin"),
            file("composeApp/src/desktopMain/kotlin"),
            file("shared/src/commonMain/kotlin"),
            file("shared/src/androidMain/kotlin"),
            file("shared/src/desktopMain/kotlin")
        )
        
        // 要排除的路径模式
        val excludePatterns = listOf("test", "Test", "androidTest", "NovelContentParser.kt")
        
        // 使用 Map 存储违规信息: file path -> list of (lineNumber, line, chineseText)
        val violations = mutableMapOf<String, MutableList<Triple<Int, String, String>>>()
        var filesScanned = 0
        
        for (sourceDir in sourceDirectories) {
            if (!sourceDir.exists()) continue
            
            val ktFiles = sourceDir.walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .filter { f -> excludePatterns.none { p -> f.path.contains(p) } }
                .toList()
            
            for (ktFile in ktFiles) {
                filesScanned++
                val content = ktFile.readText()
                
                // 检查文件级别的抑制注解
                if (content.contains("@file:Suppress") && content.contains("HardcodedChinese")) {
                    continue
                }
                
                val lines = content.lines()
                var inMultiLineComment = false
                var suppressNextLine = false
                
                for (i in lines.indices) {
                    val line = lines[i]
                    val lineNumber = i + 1
                    
                    // 处理多行注释
                    if (line.contains("/*")) {
                        inMultiLineComment = true
                    }
                    if (line.contains("*/")) {
                        inMultiLineComment = false
                        continue
                    }
                    if (inMultiLineComment) {
                        continue
                    }
                    
                    // 跳过单行注释
                    val codeBeforeComment = if (line.contains("//")) {
                        line.substringBefore("//")
                    } else {
                        line
                    }
                    
                    // 检查行级别的抑制注解
                    if (line.contains("@Suppress") && line.contains("HardcodedChinese")) {
                        suppressNextLine = true
                        continue
                    }
                    
                    if (suppressNextLine) {
                        suppressNextLine = false
                        continue
                    }
                    
                    // 检查是否是日志语句
                    if (logPattern.containsMatchIn(line)) {
                        continue
                    }
                    
                    // 在代码部分查找字符串字面量
                    for (match in stringLiteralPattern.findAll(codeBeforeComment)) {
                        val stringContent = match.value
                        val chineseMatches = chinesePattern.findAll(stringContent).toList()
                        
                        if (chineseMatches.isNotEmpty()) {
                            val chineseText = chineseMatches.joinToString("") { it.value }
                            
                            // 检查是否在排除列表中
                            val isExcluded = excludedStrings.any { excluded ->
                                stringContent.contains(excluded)
                            }
                            
                            if (!isExcluded) {
                                val filePath = ktFile.absolutePath
                                violations.getOrPut(filePath) { mutableListOf() }
                                    .add(Triple(lineNumber, line.trim(), chineseText))
                            }
                        }
                    }
                }
            }
        }
        
        // 统计总违规数
        val totalViolations = violations.values.sumOf { it.size }
        
        // 输出报告
        println("")
        println("================================================================================")
        println("硬编码中文检查报告 (Hardcoded Chinese Check Report)")
        println("================================================================================")
        println("扫描文件数: $filesScanned")
        println("发现问题数: $totalViolations")
        println("检查模式: ${if (failOnError) "严格模式（发现问题则失败）" else "警告模式（仅报告）"}")
        println("================================================================================")
        
        if (violations.isNotEmpty()) {
            println("")
            println("发现以下硬编码中文字符串:")
            println("")
            
            for ((filePath, fileViolations) in violations) {
                val relativePath = File(filePath).relativeTo(projectDir).path
                println("📁 $relativePath")
                
                for ((lineNum, lineContent, chinese) in fileViolations) {
                    val displayLine = if (lineContent.length > 100) lineContent.take(100) + "..." else lineContent
                    println("   第 $lineNum 行: $displayLine")
                    println("   └─ 中文内容: \"$chinese\"")
                }
                println("")
            }
            
            println("=================================================================================")
            println("💡 修复建议:")
            println("   1. 将硬编码的中文字符串移到 composeResources/values/strings.xml")
            println("   2. 使用 stringResource(Res.string.xxx) 获取字符串")
            println("   3. 如果确实需要硬编码，可以添加 @Suppress(\"HardcodedChinese\") 注解")
            println("")
            println("   启用严格模式: ./gradlew checkHardcodedChinese -PfailOnHardcodedChinese=true")
            println("=================================================================================")
            
            if (failOnError) {
                throw GradleException("发现 $totalViolations 处硬编码中文字符串，请修复后重新编译")
            } else {
                println("")
                println("⚠️  警告: 发现 $totalViolations 处硬编码中文字符串，建议逐步迁移到国际化资源文件")
            }
        } else {
            println("")
            println("✅ 没有发现硬编码中文字符串，检查通过！")
            println("================================================================================")
        }
    }
}

// 让 check 任务依赖于 checkHardcodedChinese
tasks.matching { it.name == "check" }.configureEach {
    dependsOn("checkHardcodedChinese")
}

