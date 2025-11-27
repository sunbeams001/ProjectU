# RankingMode 双维度分类使用指南

> 📅 创建日期: 2025-11-12  
> 🎯 目的: 说明如何使用 RankingMode 的双维度分类系统

---

## 📊 双维度分类体系

`RankingMode` 具有两个独立的分类维度：

### 维度1: 内容分类 (RankingCategory)
- **GENERAL** (一般) - 普通内容
- **R18** (R-18) - 成人内容

### 维度2: AI生成类型 (RankingAiType)
- **NON_AI** (普通) - 人工创作内容
- **AI** (AI生成) - AI生成内容

---

## 🎨 排行榜模式列表

### 一般向 - 非AI (8个)
| 枚举值 | API值 | 显示名称 | 备注 |
|--------|-------|----------|------|
| DAILY | daily | 今日 | |
| WEEKLY | weekly | 本周 | |
| MONTHLY | monthly | 本月 | |
| ROOKIE | rookie | 新人 | |
| ORIGINAL | original | 原创 | |
| MALE | male | 男性向 | |
| FEMALE | female | 女性向 | |
| **WEEKLY_ORIGINAL** | **weekly_original** | **本周原创** | 🆕 小说专属 |

### 一般向 - AI生成 (2个)
| 枚举值 | API值 | 显示名称 | 备注 |
|--------|-------|----------|------|
| DAILY_AI | daily_ai | AI生成 | |
| **WEEKLY_AI** | **weekly_ai** | **本周AI** | 🆕 小说专属 |

### R-18 - 非AI (4个)
| 枚举值 | API值 | 显示名称 |
|--------|-------|----------|
| DAILY_R18 | daily_r18 | 今日R-18 |
| WEEKLY_R18 | weekly_r18 | 本周R-18 |
| MALE_R18 | male_r18 | 男性向R-18 |
| FEMALE_R18 | female_r18 | 女性向R-18 |

### R-18 - AI生成 (2个)
| 枚举值 | API值 | 显示名称 | 备注 |
|--------|-------|----------|------|
| DAILY_R18_AI | daily_r18_ai | AI生成R-18 | |
| **WEEKLY_R18_AI** | **weekly_r18_ai** | **本周R-18 AI** | 🆕 小说专属 |

### R-18G (1个)
| 枚举值 | API值 | 显示名称 |
|--------|-------|----------|
| R18G | r18g | R-18G |

**总计**: 18 个排行榜模式（其中 3 个为小说专属）

---

## 🔍 查询方法

### 单维度查询

#### 按内容分类查询
```kotlin
// 获取所有一般向排行榜（包括AI和非AI）
val generalModes = RankingMode.getGeneralModes()
// 返回: [DAILY, WEEKLY, MONTHLY, ROOKIE, ORIGINAL, MALE, FEMALE, DAILY_AI]

// 获取所有R-18排行榜（包括AI和非AI）
val r18Modes = RankingMode.getR18Modes()
// 返回: [DAILY_R18, WEEKLY_R18, MALE_R18, FEMALE_R18, DAILY_R18_AI]
```

#### 按AI类型查询
```kotlin
// 获取所有非AI排行榜（包括一般向和R-18）
val nonAiModes = RankingMode.getNonAiModes()
// 返回: [DAILY, WEEKLY, MONTHLY, ROOKIE, ORIGINAL, MALE, FEMALE, 
//        DAILY_R18, WEEKLY_R18, MALE_R18, FEMALE_R18]

// 获取所有AI生成排行榜（包括一般向和R-18）
val aiModes = RankingMode.getAiModes()
// 返回: [DAILY_AI, DAILY_R18_AI]
```

### 双维度查询（核心方法）

```kotlin
/**
 * 通用查询方法
 * @param category 内容分类（null = 全部）
 * @param aiType AI类型（null = 全部）
 */
fun getModes(
    category: RankingCategory? = null,
    aiType: RankingAiType? = null
): List<RankingMode>
```

#### 示例1: 精确查询
```kotlin
// 只要一般向的非AI排行榜
val modes = RankingMode.getModes(
    category = RankingCategory.GENERAL,
    aiType = RankingAiType.NON_AI
)
// 返回: [DAILY, WEEKLY, MONTHLY, ROOKIE, ORIGINAL, MALE, FEMALE]

// 只要R-18的AI排行榜
val modes = RankingMode.getModes(
    category = RankingCategory.R18,
    aiType = RankingAiType.AI
)
// 返回: [DAILY_R18_AI]
```

#### 示例2: 单维度限制
```kotlin
// 一般向，不限AI类型
val modes = RankingMode.getModes(category = RankingCategory.GENERAL)
// 返回: [DAILY, WEEKLY, MONTHLY, ROOKIE, ORIGINAL, MALE, FEMALE, DAILY_AI]

// 非AI，不限内容分类
val modes = RankingMode.getModes(aiType = RankingAiType.NON_AI)
// 返回: 所有非AI排行榜（一般向 + R-18）

// 获取所有排行榜
val modes = RankingMode.getModes()
// 返回: 全部13个排行榜
```

### 便捷方法

```kotlin
// 一般向非AI
RankingMode.getGeneralNonAiModes()

// 一般向AI
RankingMode.getGeneralAiModes()

// R-18非AI
RankingMode.getR18NonAiModes()

// R-18 AI
RankingMode.getR18AiModes()

// 所有模式
RankingMode.getAllModes()
```

---

## 💡 实际使用场景

### 场景1: 用户设置过滤

```kotlin
class RankingPreferences(
    val showR18: Boolean = false,
    val showAi: Boolean = true
)

fun getAvailableRankings(preferences: RankingPreferences): List<RankingMode> {
    val category = if (preferences.showR18) null else RankingCategory.GENERAL
    val aiType = if (preferences.showAi) null else RankingAiType.NON_AI
    
    return RankingMode.getModes(category, aiType)
}

// 示例
val safeMode = RankingPreferences(showR18 = false, showAi = false)
val modes = getAvailableRankings(safeMode)
// 返回: 只有一般向非AI的排行榜
```

### 场景2: UI分类展示

```kotlin
@Composable
fun RankingSelector() {
    var selectedCategory by remember { mutableStateOf<RankingCategory?>(null) }
    var selectedAiType by remember { mutableStateOf<RankingAiType?>(null) }
    
    // 根据选择动态更新列表
    val availableModes = RankingMode.getModes(selectedCategory, selectedAiType)
    
    Column {
        // 内容分类选择
        Row {
            FilterChip("全部", selectedCategory == null)
            FilterChip("一般", selectedCategory == RankingCategory.GENERAL)
            FilterChip("R-18", selectedCategory == RankingCategory.R18)
        }
        
        // AI类型选择
        Row {
            FilterChip("全部", selectedAiType == null)
            FilterChip("普通", selectedAiType == RankingAiType.NON_AI)
            FilterChip("AI生成", selectedAiType == RankingAiType.AI)
        }
        
        // 显示可用的排行榜
        LazyColumn {
            items(availableModes) { mode ->
                RankingItem(mode)
            }
        }
    }
}
```

### 场景3: 分组展示

```kotlin
@Composable
fun GroupedRankingList() {
    Column {
        // 一般向
        ExpandableSection("一般向排行榜") {
            // 非AI
            SubSection("普通") {
                RankingMode.getGeneralNonAiModes().forEach { mode ->
                    RankingItem(mode)
                }
            }
            // AI
            SubSection("AI生成") {
                RankingMode.getGeneralAiModes().forEach { mode ->
                    RankingItem(mode)
                }
            }
        }
        
        // R-18（需要权限）
        if (hasR18Permission) {
            ExpandableSection("R-18排行榜") {
                SubSection("普通") {
                    RankingMode.getR18NonAiModes().forEach { mode ->
                        RankingItem(mode)
                    }
                }
                SubSection("AI生成") {
                    RankingMode.getR18AiModes().forEach { mode ->
                        RankingItem(mode)
                    }
                }
            }
        }
    }
}
```

### 场景4: 快捷入口

```kotlin
object QuickAccess {
    // 最热门的排行榜
    fun getPopularRankings() = listOf(
        RankingMode.DAILY,
        RankingMode.WEEKLY,
        RankingMode.DAILY_AI
    )
    
    // 适合新手的排行榜
    fun getBeginnerFriendlyRankings() = listOf(
        RankingMode.DAILY,
        RankingMode.WEEKLY,
        RankingMode.ROOKIE
    )
    
    // 今日系列
    fun getTodayRankings() = RankingMode.entries.filter { 
        it.displayName.contains("今日")
    }
}
```

---

## 📋 查询矩阵

所有可能的查询组合：

| category | aiType | 结果数量 | 说明 |
|----------|--------|---------|------|
| GENERAL | NON_AI | 7 | 一般向非AI |
| GENERAL | AI | 1 | 一般向AI |
| GENERAL | null | 8 | 所有一般向 |
| R18 | NON_AI | 4 | R-18非AI |
| R18 | AI | 1 | R-18 AI |
| R18 | null | 5 | 所有R-18 |
| null | NON_AI | 11 | 所有非AI |
| null | AI | 2 | 所有AI |
| null | null | 13 | 全部 |

---

## 📝 注意事项

1. **类型安全**: 使用枚举避免字符串错误
2. **灵活查询**: 两个维度可以独立或组合使用
3. **null 语义**: null 表示"不限制该维度"
4. **不可变性**: 所有查询方法返回新列表，不影响原始数据
5. **性能**: 使用 `filter` 操作，在排行榜数量较少时性能足够

---

## 🔗 相关文件

- `RankingMode.kt` - 枚举定义和查询方法
- `RankingContent.kt` - 内容类型枚举
- `RankingApi.kt` - API 调用实现

---

## 📊 统计信息

- **总排行榜数**: 13
- **维度数**: 2
- **分类组合**: 4 (一般×非AI, 一般×AI, R-18×非AI, R-18×AI)
- **查询方法**: 10+
- **支持场景**: 用户过滤、分组展示、快捷访问等
