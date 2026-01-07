/**
 * 版本管理任务
 * 
 * 用法（Linux/Mac）：
 *   ./gradlew setVersion -PnewVersion=1.0.2    # 设置新版本号（自动递增versionCode）
 *   ./gradlew bumpPatch                        # 递增修订版本号 (1.0.1 -> 1.0.2)
 *   ./gradlew bumpMinor                        # 递增次版本号 (1.0.1 -> 1.1.0)
 *   ./gradlew bumpMajor                        # 递增主版本号 (1.0.1 -> 2.0.0)
 *   ./gradlew showVersion                      # 显示当前版本信息
 * 
 * 用法（Windows PowerShell）：
 *   .\gradlew.bat setVersion "-PnewVersion=1.0.2"  # 设置新版本号（参数需要引号）
 *   .\gradlew.bat bumpPatch                        # 递增修订版本号 (1.0.1 -> 1.0.2)
 *   .\gradlew.bat bumpMinor                        # 递增次版本号 (1.0.1 -> 1.1.0)
 *   .\gradlew.bat bumpMajor                        # 递增主版本号 (1.0.1 -> 2.0.0)
 *   .\gradlew.bat showVersion                      # 显示当前版本信息
 */

// 定义版本文件路径
val buildGradleFile = file("composeApp/build.gradle.kts")

// 读取当前版本信息
data class VersionInfo(
    val versionCode: Int,
    val versionName: String
)

fun readCurrentVersion(): VersionInfo {
    val content = buildGradleFile.readText()
    
    val versionCodeRegex = Regex("""versionCode\s*=\s*(\d+)""")
    val versionNameRegex = Regex("""versionName\s*=\s*"([^"]+)"""")
    
    val versionCode = versionCodeRegex.find(content)?.groupValues?.get(1)?.toInt() ?: 1
    val versionName = versionNameRegex.find(content)?.groupValues?.get(1) ?: "1.0.0"
    
    return VersionInfo(versionCode, versionName)
}

// 更新版本号
fun updateVersion(newVersionName: String, newVersionCode: Int) {
    val content = buildGradleFile.readText()
    
    var updatedContent = content.replace(
        Regex("""versionCode\s*=\s*\d+"""),
        "versionCode = $newVersionCode"
    )
    
    updatedContent = updatedContent.replace(
        Regex("""versionName\s*=\s*"[^"]+""""),
        """versionName = "$newVersionName""""
    )
    
    // 同时更新 desktop packageVersion
    updatedContent = updatedContent.replace(
        Regex("""packageVersion\s*=\s*"[^"]+""""),
        """packageVersion = "$newVersionName""""
    )
    
    buildGradleFile.writeText(updatedContent)
    
    println("✓ 版本更新成功:")
    println("  versionCode: $newVersionCode")
    println("  versionName: $newVersionName")
}

// 解析版本号
fun parseVersion(version: String): Triple<Int, Int, Int> {
    val parts = version.split(".")
    if (parts.size != 3) {
        throw IllegalArgumentException("版本号格式错误，应为 major.minor.patch (例如: 1.0.1)")
    }
    return Triple(
        parts[0].toInt(),
        parts[1].toInt(),
        parts[2].toInt()
    )
}

// 格式化版本号
fun formatVersion(major: Int, minor: Int, patch: Int): String {
    return "$major.$minor.$patch"
}

// 任务1: 显示当前版本
tasks.register("showVersion") {
    group = "版本管理"
    description = "显示当前版本信息"
    
    // 配置缓存兼容性
    notCompatibleWithConfigurationCache("This task reads build file dynamically")
    
    doLast {
        val current = readCurrentVersion()
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("📦 当前版本信息")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("versionCode: ${current.versionCode}")
        println("versionName: ${current.versionName}")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }
}

// 任务2: 设置指定版本号
tasks.register("setVersion") {
    group = "版本管理"
    description = "设置指定版本号（自动递增versionCode）\n示例: ./gradlew setVersion -PnewVersion=1.0.2"
    
    // 配置缓存兼容性
    notCompatibleWithConfigurationCache("This task modifies build file dynamically")
    
    doLast {
        val newVersionName = project.findProperty("newVersion") as? String
            ?: throw IllegalArgumentException("请使用 -PnewVersion=x.y.z 指定新版本号")
        
        // 验证版本号格式
        parseVersion(newVersionName)
        
        val current = readCurrentVersion()
        val newVersionCode = current.versionCode + 1
        
        updateVersion(newVersionName, newVersionCode)
    }
}

// 任务3: 递增修订版本号 (patch)
tasks.register("bumpPatch") {
    group = "版本管理"
    description = "递增修订版本号 (例如: 1.0.1 -> 1.0.2)"
    
    // 配置缓存兼容性
    notCompatibleWithConfigurationCache("This task modifies build file dynamically")
    
    doLast {
        val current = readCurrentVersion()
        val (major, minor, patch) = parseVersion(current.versionName)
        
        val newVersionName = formatVersion(major, minor, patch + 1)
        val newVersionCode = current.versionCode + 1
        
        updateVersion(newVersionName, newVersionCode)
    }
}

// 任务4: 递增次版本号 (minor)
tasks.register("bumpMinor") {
    group = "版本管理"
    description = "递增次版本号 (例如: 1.0.1 -> 1.1.0)"
    
    // 配置缓存兼容性
    notCompatibleWithConfigurationCache("This task modifies build file dynamically")
    
    doLast {
        val current = readCurrentVersion()
        val (major, minor, _) = parseVersion(current.versionName)
        
        val newVersionName = formatVersion(major, minor + 1, 0)
        val newVersionCode = current.versionCode + 1
        
        updateVersion(newVersionName, newVersionCode)
    }
}

// 任务5: 递增主版本号 (major)
tasks.register("bumpMajor") {
    group = "版本管理"
    description = "递增主版本号 (例如: 1.0.1 -> 2.0.0)"
    
    // 配置缓存兼容性
    notCompatibleWithConfigurationCache("This task modifies build file dynamically")
    
    doLast {
        val current = readCurrentVersion()
        val (major, _, _) = parseVersion(current.versionName)
        
        val newVersionName = formatVersion(major + 1, 0, 0)
        val newVersionCode = current.versionCode + 1
        
        updateVersion(newVersionName, newVersionCode)
    }
}
