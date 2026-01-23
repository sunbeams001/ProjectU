plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
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
    
    // 将 project 属性存储为任务输入，避免 Configuration Cache 问题
    val projectDir = layout.projectDirectory.asFile
    val failOnErrorProperty = providers.gradleProperty("failOnHardcodedChinese")
    
    doLast {
        // 是否在发现问题时使构建失败（默认只警告）
        val failOnError = failOnErrorProperty.orNull?.toBoolean() ?: false
        
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
            "个字符",           // HTML解析模式，匹配Pixiv页面内容
            "入り"             // 日文：Pixiv API 固定标签格式（如 "500users入り"）
        )
        
        // 要扫描的源码目录
        val sourceDirectories = listOf(
            File(projectDir, "androidApp/src/main/kotlin"),
            File(projectDir, "androidApp/src/main/java"),
            File(projectDir, "composeApp/src/commonMain/kotlin"),
            File(projectDir, "composeApp/src/androidMain/kotlin"),
            File(projectDir, "composeApp/src/desktopMain/kotlin"),
            File(projectDir, "shared/src/commonMain/kotlin"),
            File(projectDir, "shared/src/androidMain/kotlin"),
            File(projectDir, "shared/src/desktopMain/kotlin")
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
                var inSuppressedBlock = 0  // 追踪抑制块的嵌套层级
                
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
                    
                    // 检查 @Suppress 注解（函数、类或行级别）
                    if (line.contains("@Suppress") && line.contains("HardcodedChinese")) {
                        suppressNextLine = true
                        continue
                    }
                    
                    // 如果前一行有 @Suppress 注解，检查这一行是否是函数/类声明
                    if (suppressNextLine) {
                        // 检查是否是函数、类、对象或属性声明
                        val isFunctionOrClass = line.trimStart().let { trimmed ->
                            trimmed.startsWith("fun ") || 
                            trimmed.startsWith("internal fun ") ||
                            trimmed.startsWith("private fun ") ||
                            trimmed.startsWith("public fun ") ||
                            trimmed.startsWith("class ") ||
                            trimmed.startsWith("object ") ||
                            trimmed.startsWith("enum class ") ||
                            trimmed.startsWith("data class ") ||
                            trimmed.startsWith("sealed class ") ||
                            trimmed.startsWith("interface ") ||
                            trimmed.startsWith("val ") ||
                            trimmed.startsWith("var ")
                        }
                        
                        if (isFunctionOrClass && line.contains("{")) {
                            // 进入被抑制的代码块
                            inSuppressedBlock = 1
                        }
                        suppressNextLine = false
                        continue
                    }
                    
                    // 追踪代码块的嵌套
                    if (inSuppressedBlock > 0) {
                        inSuppressedBlock += line.count { it == '{' }
                        inSuppressedBlock -= line.count { it == '}' }
                        continue  // 跳过被抑制块中的所有行
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

/**
 * 检查多语言资源文件的完整性和一致性
 * 
 * 这个任务会检查：
 * 1. 不同语言的资源文件是否缺少某些字符串（相比默认语言）
 * 2. 不同语言的资源文件中，字符串的顺序是否与默认语言一致
 * 
 * 检查范围：
 * - composeApp/src/commonMain/composeResources/ (Compose Multiplatform 资源)
 * - androidApp/src/main/res/ (Android 原生资源)
 * 
 * 使用方式:
 *   ./gradlew checkResourceStrings                                    # 默认只警告，不失败
 *   ./gradlew checkResourceStrings -PfailOnResourceIssues=true       # 发现问题时失败
 */
tasks.register("checkResourceStrings") {
    group = "verification"
    description = "Check resource string files for missing strings and order consistency"
    
    val projectDir = layout.projectDirectory.asFile
    val failOnErrorProperty = providers.gradleProperty("failOnResourceIssues")
    
    doLast {
        val failOnError = failOnErrorProperty.orNull?.toBoolean() ?: false
        
        // 要检查的资源文件目录列表
        val resourceDirs = listOf(
            File(projectDir, "composeApp/src/commonMain/composeResources") to "Compose Multiplatform",
            File(projectDir, "androidApp/src/main/res") to "Android Native"
        )
        
        // 用于收集所有问题
        data class DirectoryReport(
            val dirPath: String,
            val dirType: String,
            val totalIssues: Int,
            val missingCount: Int,
            val extraCount: Int,
            val orderCount: Int
        )
        
        val allReports = mutableListOf<DirectoryReport>()
        
        println("")
        println("================================================================================")
        println("多语言资源检查报告 (Resource Strings Check Report)")
        println("================================================================================")
        println("检查模式: ${if (failOnError) "严格模式（发现问题则失败）" else "警告模式（仅报告）"}")
        println("================================================================================")
        
        // 解析XML文件，提取字符串名称和顺序
        fun parseStringsXml(file: File): List<String> {
            if (!file.exists()) return emptyList()
            
            val stringNames = mutableListOf<String>()
            val content = file.readText()
            
            // 使用正则表达式提取 <string name="..."> 标签
            val pattern = Regex("""<string\s+name="([^"]+)"""")
            pattern.findAll(content).forEach { match ->
                stringNames.add(match.groupValues[1])
            }
            
            return stringNames
        }
        
        // 循环检查每个资源目录
        for ((resourcesDir, dirType) in resourceDirs) {
            if (!resourcesDir.exists()) {
                println("")
                println("⏭️  跳过 [$dirType]: 目录不存在")
                println("   ${resourcesDir.absolutePath}")
                continue
            }
            
            // 默认语言文件（英文）
            val defaultStringsFile = File(resourcesDir, "values/strings.xml")
            
            if (!defaultStringsFile.exists()) {
                println("")
                println("⏭️  跳过 [$dirType]: 未找到默认语言文件")
                println("   ${resourcesDir.absolutePath}")
                continue
            }
            
            // 查找所有语言的资源目录
            val languageDirs = resourcesDir.listFiles()?.filter { 
                it.isDirectory && it.name.startsWith("values")
            }?.sortedBy { it.name } ?: emptyList()
            
            if (languageDirs.isEmpty()) {
                println("")
                println("⏭️  跳过 [$dirType]: 未找到任何语言资源目录")
                continue
            }
            
            println("")
            println("📂 检查: $dirType")
            println("   路径: ${resourcesDir.relativeTo(projectDir).path}")
            println("   默认语言: values (English)")
            println("   检查语言数: ${languageDirs.size}")
            
            // 解析默认语言文件
            val defaultStrings = parseStringsXml(defaultStringsFile)
            
            if (defaultStrings.isEmpty()) {
                println("   ⚠️  默认语言文件中没有找到任何字符串定义")
                continue
            }
            
            println("   默认语言字符串数量: ${defaultStrings.size}")
            
            // 用于收集问题
            data class LanguageIssue(
                val languageName: String,
                val missingStrings: List<String>,
                val extraStrings: List<String>,
                val orderDifferences: List<Pair<String, Int>> // 字符串名 -> 在默认语言中的位置
            )
            
            val issues = mutableListOf<LanguageIssue>()
            
            // 检查每个语言文件
            for (langDir in languageDirs) {
                if (langDir.name == "values") continue // 跳过默认语言
                
                // 跳过非语言目录（如 values-night, values-v27 等主题/版本目录）
                if (langDir.name.contains("-night") || 
                    langDir.name.matches(Regex("values-v\\d+"))) {
                    continue
                }
                
                val langFile = File(langDir, "strings.xml")
                val langName = langDir.name
                
                if (!langFile.exists()) {
                    println("   ⚠️  [$langName] 未找到 strings.xml 文件")
                    continue
                }
                
                val langStrings = parseStringsXml(langFile)
                
                // 检查缺失的字符串
                val missingStrings = defaultStrings.filter { it !in langStrings }
                
                // 检查多余的字符串（在翻译文件中有，但默认语言中没有）
                val extraStrings = langStrings.filter { it !in defaultStrings }
                
                // 检查顺序一致性
                val orderDifferences = mutableListOf<Pair<String, Int>>()
                val commonStrings = defaultStrings.filter { it in langStrings }
                
                var langIndex = 0
                for ((defaultIndex, stringName) in commonStrings.withIndex()) {
                    // 在当前语言中查找该字符串的位置
                    val actualIndex = langStrings.indexOf(stringName)
                    
                    // 如果顺序不一致（相对位置发生变化）
                    if (actualIndex != -1 && actualIndex < langIndex) {
                        // 字符串出现在了预期位置之前
                        orderDifferences.add(stringName to defaultIndex)
                    }
                    
                    if (actualIndex != -1) {
                        langIndex = actualIndex + 1
                    }
                }
                
                // 更精确的顺序检查：检查相邻字符串的顺序
                val orderIssues = mutableListOf<Pair<String, Int>>()
                for (i in 0 until commonStrings.size - 1) {
                    val current = commonStrings[i]
                    val next = commonStrings[i + 1]
                    
                    val currentIndexInLang = langStrings.indexOf(current)
                    val nextIndexInLang = langStrings.indexOf(next)
                    
                    // 如果在目标语言中，next出现在current之前，说明顺序不对
                    if (currentIndexInLang != -1 && nextIndexInLang != -1 && 
                    nextIndexInLang < currentIndexInLang) {
                    val defaultIndex = defaultStrings.indexOf(next)
                    orderIssues.add(next to defaultIndex)
                }
            }
            
            if (missingStrings.isNotEmpty() || extraStrings.isNotEmpty() || orderIssues.isNotEmpty()) {
                issues.add(LanguageIssue(langName, missingStrings, extraStrings, orderIssues))
            }
        }
        
        // 统计当前目录的问题
        val totalMissing = issues.sumOf { it.missingStrings.size }
        val totalExtra = issues.sumOf { it.extraStrings.size }
        val totalOrder = issues.sumOf { it.orderDifferences.size }
        val totalIssuesCount = totalMissing + totalExtra + totalOrder
        
        // 保存报告
        allReports.add(DirectoryReport(
            resourcesDir.relativeTo(projectDir).path,
            dirType,
            totalIssuesCount,
            totalMissing,
            totalExtra,
            totalOrder
        ))
        
        // 输出当前目录的问题
        if (issues.isEmpty()) {
            println("   ✅ 所有语言的资源文件都完整且顺序一致！")
        } else {
            println("")
            println("   发现以下问题:")
            println("")
            
            for (issue in issues) {
                val hasIssues = issue.missingStrings.isNotEmpty() || 
                               issue.extraStrings.isNotEmpty() || 
                               issue.orderDifferences.isNotEmpty()
                
                if (!hasIssues) continue
                
                println("   📁 [${issue.languageName}]")
                
                // 缺失的字符串
                if (issue.missingStrings.isNotEmpty()) {
                    println("      ❌ 缺失 ${issue.missingStrings.size} 个字符串:")
                    issue.missingStrings.take(10).forEach { stringName ->
                        println("         - $stringName")
                    }
                    if (issue.missingStrings.size > 10) {
                        println("         ... 还有 ${issue.missingStrings.size - 10} 个")
                    }
                }
                
                // 多余的字符串
                if (issue.extraStrings.isNotEmpty()) {
                    println("      ⚠️  多余 ${issue.extraStrings.size} 个字符串（默认语言中不存在）:")
                    issue.extraStrings.take(5).forEach { stringName ->
                        println("         - $stringName")
                    }
                    if (issue.extraStrings.size > 5) {
                        println("         ... 还有 ${issue.extraStrings.size - 5} 个")
                    }
                }
                
                // 顺序不一致
                if (issue.orderDifferences.isNotEmpty()) {
                    println("      🔄 有 ${issue.orderDifferences.size} 个字符串的顺序与默认语言不一致:")
                    issue.orderDifferences.take(5).forEach { (stringName, defaultPos) ->
                        println("         - $stringName (默认位置: ${defaultPos + 1})")
                    }
                    if (issue.orderDifferences.size > 5) {
                        println("         ... 还有 ${issue.orderDifferences.size - 5} 个")
                    }
                }
                
                println("")
            }
        }
    }
    
    // 输出总体报告
    println("")
    println("================================================================================")
    println("📊 总体统计:")
    println("================================================================================")
    
    if (allReports.isEmpty()) {
        println("⚠️  未找到任何可检查的资源目录")
        return@doLast
    }
    
    val grandTotalMissing = allReports.sumOf { it.missingCount }
    val grandTotalExtra = allReports.sumOf { it.extraCount }
    val grandTotalOrder = allReports.sumOf { it.orderCount }
    val grandTotal = allReports.sumOf { it.totalIssues }
    
    for (report in allReports) {
        val status = if (report.totalIssues == 0) "✅" else "⚠️"
        println("$status [${report.dirType}] ${report.dirPath}")
        if (report.totalIssues > 0) {
            println("   缺失: ${report.missingCount}, 多余: ${report.extraCount}, 顺序: ${report.orderCount}")
        }
    }
    
    println("")
    println("总计:")
    println("   缺失字符串总数: $grandTotalMissing")
    println("   多余字符串总数: $grandTotalExtra")
    println("   顺序不一致总数: $grandTotalOrder")
    println("================================================================================")
    
    if (grandTotal == 0) {
        println("✅ 所有资源目录的文件都完整且顺序一致！")
        println("================================================================================")
        return@doLast
    }
    
    println("💡 修复建议:")
    println("   1. 缺失字符串: 在对应语言文件中添加翻译")
    println("   2. 多余字符串: 检查是否为误添加，或在默认语言中补充")
    println("   3. 顺序不一致: 调整字符串顺序，保持与 values/strings.xml 一致")
    println("")
    println("   资源文件位置:")
    println("     - composeApp/src/commonMain/composeResources/")
    println("     - androidApp/src/main/res/")
    println("   启用严格模式: ./gradlew checkResourceStrings -PfailOnResourceIssues=true")
    println("================================================================================")
    
    if (failOnError) {
        throw GradleException(
            "发现资源文件问题: $grandTotalMissing 个缺失, $grandTotalExtra 个多余, $grandTotalOrder 个顺序不一致"
        )
    } else {
        println("")
        println("⚠️  警告: 发现资源文件问题，建议修复以确保多语言支持的完整性")
    }
    }
}

/**
 * 自动重排序资源文件，使其与默认语言（英文）的顺序一致
 * 
 * 此任务会：
 * 1. 解析默认语言文件（values/strings.xml）的结构（包括注释、空行、字符串）
 * 2. 读取其他语言文件的所有字符串翻译
 * 3. 按照默认语言文件的顺序和结构，重新生成其他语言文件
 * 4. 保留所有注释分组，保持相同的格式
 * 
 * 特性：
 * - ✅ 保留注释结构和分组
 * - ✅ 保留空行和格式
 * - ✅ 自动备份原文件
 * - ✅ 检测多余和缺失的字符串
 * - ✅ 生成详细的操作报告
 * 
 * 使用方式:
 *   ./gradlew reorderResourceStrings                                           # 默认只生成预览，不实际修改
 *   ./gradlew reorderResourceStrings -PapplyReorder=true                      # 实际应用重排序
 *   ./gradlew reorderResourceStrings -PapplyReorder=true -PtargetLang=zh-rCN # 仅处理指定语言
 * 
 * 安全性：
 * - 默认模式只生成预览，不会修改文件
 * - 自动创建 .backup 备份文件
 * - 可以使用 -PtargetLang 参数只处理特定语言
 */
tasks.register("reorderResourceStrings") {
    group = "maintenance"
    description = "Reorder resource strings to match the default language order"
    
    val projectDir = layout.projectDirectory.asFile
    val applyReorderProperty = providers.gradleProperty("applyReorder")
    val targetLangProperty = providers.gradleProperty("targetLang")
    
    doLast {
        val applyReorder = applyReorderProperty.orNull?.toBoolean() ?: false
        val targetLang = targetLangProperty.orNull
        
        // 资源文件目录
        val resourceDirs = listOf(
            File(projectDir, "composeApp/src/commonMain/composeResources") to "Compose Multiplatform",
            File(projectDir, "androidApp/src/main/res") to "Android Native"
        )
        
        println("")
        println("================================================================================")
        println("资源文件重排序工具 (Resource Strings Reorder Tool)")
        println("================================================================================")
        println("运行模式: ${if (applyReorder) "✍️ 应用模式（将修改文件）" else "👁️ 预览模式（不会修改文件）"}")
        if (targetLang != null) {
            println("目标语言: $targetLang")
        } else {
            println("目标语言: 全部")
        }
        println("================================================================================")
        
        // 解析XML文件，提取完整结构
        // 返回格式: List of Map with keys: type, content, name(for strings), value(for strings)
        fun parseResourceFile(file: File): List<Map<String, String>> {
            if (!file.exists()) return emptyList()
            
            val items = mutableListOf<Map<String, String>>()
            val lines = file.readLines()
            var consecutiveEmptyLines = 0
            
            for (line in lines) {
                val trimmed = line.trim()
                
                // 跳过 XML 声明和 <resources> 标签
                if (trimmed.startsWith("<?xml") || 
                    trimmed == "<resources>" || 
                    trimmed == "</resources>") {
                    continue
                }
                
                // 空行
                if (trimmed.isEmpty()) {
                    consecutiveEmptyLines++
                    continue
                } else {
                    if (consecutiveEmptyLines > 0) {
                        items.add(mapOf(
                            "type" to "empty",
                            "count" to consecutiveEmptyLines.toString()
                        ))
                        consecutiveEmptyLines = 0
                    }
                }
                
                // 注释
                if (trimmed.startsWith("<!--") && trimmed.endsWith("-->")) {
                    val commentText = trimmed.removePrefix("<!--").removeSuffix("-->").trim()
                    items.add(mapOf(
                        "type" to "comment",
                        "content" to commentText
                    ))
                    continue
                }
                
                // 字符串
                val stringPattern = Regex("""<string\s+name="([^"]+)">(.*?)</string>""")
                val match = stringPattern.find(trimmed)
                if (match != null) {
                    val name = match.groupValues[1]
                    val value = match.groupValues[2]
                    items.add(mapOf(
                        "type" to "string",
                        "name" to name,
                        "value" to value
                    ))
                }
            }
            
            return items
        }
        
        // 提取所有字符串到 Map
        fun extractStrings(items: List<Map<String, String>>): Map<String, String> {
            return items.filter { it["type"] == "string" }
                .associate { it["name"]!! to it["value"]!! }
        }
        
        // 生成XML文件内容
        fun generateXml(template: List<Map<String, String>>, translations: Map<String, String>): String {
            val sb = StringBuilder()
            sb.appendLine("""<?xml version="1.0" encoding="utf-8"?>""")
            sb.appendLine("<resources>")
            
            for (item in template) {
                when (item["type"]) {
                    "comment" -> {
                        sb.appendLine("    <!-- ${item["content"]} -->")
                    }
                    "empty" -> {
                        val count = item["count"]?.toIntOrNull() ?: 1
                        repeat(count) {
                            sb.appendLine()
                        }
                    }
                    "string" -> {
                        val name = item["name"]!!
                        val translatedValue = translations[name]
                        if (translatedValue != null) {
                            sb.appendLine("""    <string name="$name">$translatedValue</string>""")
                        } else {
                            // 如果翻译文件中没有这个字符串，使用默认值
                            sb.appendLine("""    <string name="$name">${item["value"]}</string>""")
                        }
                    }
                }
            }
            
            sb.appendLine("</resources>")
            return sb.toString()
        }
        
        var totalProcessed = 0
        var totalSkipped = 0
        
        for ((resourcesDir, dirType) in resourceDirs) {
            if (!resourcesDir.exists()) {
                println("")
                println("⏭️  跳过 [$dirType]: 目录不存在")
                continue
            }
            
            val defaultStringsFile = File(resourcesDir, "values/strings.xml")
            if (!defaultStringsFile.exists()) {
                println("")
                println("⏭️  跳过 [$dirType]: 未找到默认语言文件")
                continue
            }
            
            println("")
            println("📂 处理: $dirType")
            println("   路径: ${resourcesDir.relativeTo(projectDir).path}")
            
            // 解析默认语言文件的结构（作为模板）
            val template = parseResourceFile(defaultStringsFile)
            val templateStrings = extractStrings(template)
            
            println("   默认语言字符串数量: ${templateStrings.size}")
            
            // 查找所有语言目录
            val languageDirs = resourcesDir.listFiles()?.filter { 
                it.isDirectory && it.name.startsWith("values") && it.name != "values"
            }?.filter {
                // 跳过主题和版本目录
                !it.name.contains("-night") && !it.name.matches(Regex("values-v\\d+"))
            }?.sortedBy { it.name } ?: emptyList()
            
            // 如果指定了目标语言，只处理该语言
            val dirsToProcess = if (targetLang != null) {
                languageDirs.filter { it.name == "values-$targetLang" }
            } else {
                languageDirs
            }
            
            if (dirsToProcess.isEmpty()) {
                if (targetLang != null) {
                    println("   ⚠️  未找到目标语言: values-$targetLang")
                } else {
                    println("   ⚠️  未找到任何语言目录")
                }
                continue
            }
            
            for (langDir in dirsToProcess) {
                val langFile = File(langDir, "strings.xml")
                if (!langFile.exists()) {
                    println("   ⏭️  [${langDir.name}] 未找到 strings.xml")
                    totalSkipped++
                    continue
                }
                
                // 读取当前翻译
                val currentItems = parseResourceFile(langFile)
                val currentTranslations = extractStrings(currentItems)
                
                // 检查差异
                val missing = templateStrings.keys - currentTranslations.keys
                val extra = currentTranslations.keys - templateStrings.keys
                
                println("")
                println("   📝 [${langDir.name}]")
                println("      当前字符串数: ${currentTranslations.size}")
                println("      缺失: ${missing.size}, 多余: ${extra.size}")
                
                if (missing.isNotEmpty()) {
                    println("      ⚠️  缺失的字符串:")
                    missing.take(5).forEach { key ->
                        println("         - $key")
                    }
                    if (missing.size > 5) {
                        println("         ... 还有 ${missing.size - 5} 个")
                    }
                }
                
                if (extra.isNotEmpty()) {
                    println("      ℹ️  多余的字符串（将被移除）:")
                    extra.take(5).forEach { key ->
                        println("         - $key")
                    }
                    if (extra.size > 5) {
                        println("         ... 还有 ${extra.size - 5} 个")
                    }
                }
                
                // 生成新的XML内容
                val newContent = generateXml(template, currentTranslations)
                
                if (applyReorder) {
                    // 创建备份到 build/backups 目录
                    val backupDir = File(projectDir, "build/backups/resource-strings")
                    backupDir.mkdirs()
                    val timestamp = java.time.LocalDateTime.now().format(
                        java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                    )
                    val backupFile = File(backupDir, "${langDir.name}_${langFile.nameWithoutExtension}_${timestamp}.xml")
                    langFile.copyTo(backupFile, overwrite = true)
                    println("      ✅ 已创建备份: ${backupFile.relativeTo(projectDir).path}")
                    
                    // 写入新内容
                    langFile.writeText(newContent)
                    println("      ✅ 已重排序并保存")
                    totalProcessed++
                } else {
                    println("      👁️  预览模式：未修改文件")
                    totalSkipped++
                }
            }
        }
        
        println("")
        println("================================================================================")
        println("📊 操作总结:")
        println("   已处理: $totalProcessed 个文件")
        println("   已跳过: $totalSkipped 个文件")
        println("================================================================================")
        
        if (!applyReorder && totalSkipped > 0) {
            println("")
            println("💡 提示:")
            println("   当前是预览模式，没有修改任何文件")
            println("   如需实际应用重排序，请运行:")
            println("   ./gradlew reorderResourceStrings -PapplyReorder=true")
            println("")
            println("   如需只处理特定语言，请运行:")
            println("   ./gradlew reorderResourceStrings -PapplyReorder=true -PtargetLang=zh-rCN")
        } else if (applyReorder && totalProcessed > 0) {
            println("")
            println("✅ 重排序已完成！")
            println("   原文件已备份为 .backup")
            println("   建议运行 checkResourceStrings 验证结果")
            println("")
            println("   如需恢复，可以从备份文件还原:")
            println("   find . -name 'strings.xml.backup' -exec sh -c 'cp \"\$0\" \"\${0%.backup}\"' {} \\;")
        }
        
        println("================================================================================")
    }
}
