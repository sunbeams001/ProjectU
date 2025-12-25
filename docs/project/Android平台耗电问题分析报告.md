# Android平台耗电问题分析报告

> 📅 **创建日期**: 2025-12-24  
> 🎯 **目标**: 系统性分析ProjectU在Android平台的耗电问题并提供优化方向  
> 📊 **当前状态**: 问题定位阶段 - 待逐项测试验证

---

## 📋 目录

1. [问题概述](#问题概述)
2. [耗电问题分类](#耗电问题分类)
3. [详细问题分析](#详细问题分析)
4. [测试验证方案](#测试验证方案)
5. [优化优先级建议](#优化优先级建议)

---

## 问题概述

### 跨平台应用耗电特点

**Compose Multiplatform应用在Android平台耗电量偏高是常见现象**，主要原因：

1. **额外的抽象层开销** - Kotlin Multiplatform在JVM之上运行，增加运行时开销
2. **重组（Recomposition）机制** - Compose的声明式UI会频繁触发重组
3. **跨平台通用性优化不足** - 为兼容多平台，缺少某些平台特定优化
4. **协程密集使用** - 大量协程并发运行消耗CPU资源

### 本项目特殊性

ProjectU是一个功能完整的Pixiv客户端，具有以下特点会导致额外耗电：

- **图片密集型应用** - 大量高分辨率图片加载和缓存
- **动画播放** - Ugoira动图的逐帧播放
- **无限滚动** - 瀑布流列表持续加载更多内容
- **实时状态同步** - 收藏、关注等状态的全局同步
- **视频编码** - Ugoira到MP4/GIF的转换

---

## 耗电问题分类

### 风险等级评估

| 问题分类 | 风险等级 | 预估耗电占比 | 定位难度 | 优化优先级 |
|---------|---------|------------|---------|-----------|
| Ugoira动图播放器 | 🔴 极高 | 30-40% | 低 | **P0** |
| 图片加载与缓存 | 🟠 高 | 20-25% | 中 | **P0** |
| 无限滚动监听 | 🟠 高 | 15-20% | 中 | **P1** |
| 下载系统（视频编码） | 🟠 高 | 10-15% | 低 | **P1** |
| 全局状态系统 | 🟡 中 | 5-10% | 高 | **P2** |
| 多层导航缓存 | 🟡 中 | 5-8% | 高 | **P2** |
| HTTP请求开销 | 🟢 中低 | 3-5% | 低 | **P3** |

---

## 详细问题分析

### 问题1: Ugoira动图播放器 🔴 极高风险

#### 问题描述

**文件位置**: `composeApp/src/commonMain/kotlin/com/projectu/ui/components/UgoiraPlayer.kt`

**核心问题代码**:
```kotlin
LaunchedEffect(frameBitmaps, isPlaying, playbackSpeed) {
    if (frameBitmaps.isNotEmpty() && isPlaying) {
        while (isPlaying) {  // ⚠️ 无限循环
            for (index in currentFrameIndex until metadata.frames.size) {
                if (!isPlaying) return@LaunchedEffect
                currentFrameIndex = index
                val adjustedDelay = (metadata.frames[index].delay / playbackSpeed).toLong()
                delay(adjustedDelay)  // 通常60-100ms
            }
            currentFrameIndex = 0
        }
    }
}
```

#### 具体问题点

1. **持续的while(true)循环**
   - 只要动图在播放就会一直循环
   - 即使用户滚动离开视口也可能继续运行
   - 没有生命周期感知机制

2. **高频率的UI重组**
   - 每60-100ms触发一次状态更新（`currentFrameIndex`变化）
   - 相当于10-16 FPS的刷新率
   - 每次更新都触发Compose重组

3. **内存占用问题**
   - `frameBitmaps: List<ImageBitmap>` 将所有帧保存在内存中
   - 一个典型Ugoira有30-60帧，每帧约500KB-2MB
   - 单个动图可能占用30-120MB内存

4. **缺少性能优化**
   - 没有使用硬件加速解码
   - 没有帧缓冲机制
   - 没有后台播放控制

#### 耗电机制

- **CPU**: 持续运行协程调度，状态更新计算
- **GPU**: 每次重组都需要重新渲染整个Image组件
- **内存**: 大量Bitmap导致GC频繁运行，GC本身耗电且导致卡顿

#### 测试方法

1. **基准测试**
   ```
   测试场景: 打开一个Ugoira作品详情页，让动图播放5分钟
   监控指标:
   - CPU使用率（Android Studio Profiler）
   - 内存占用（Memory Profiler）
   - 电量消耗（Battery Historian）
   - 帧率（GPU渲染分析）
   ```

2. **对比测试**
   ```
   A组: 播放动图5分钟
   B组: 暂停动图5分钟
   对比: 电量消耗差异
   ```

3. **代码插桩**
   ```kotlin
   // 在LaunchedEffect中添加日志
   Log.d("UgoiraPlayer", "Frame update: $currentFrameIndex, " +
       "Time: ${System.currentTimeMillis()}, " +
       "Memory: ${Runtime.getRuntime().totalMemory() / 1024 / 1024}MB")
   ```

#### 优化方向

1. **添加生命周期感知**
   ```kotlin
   val lifecycleOwner = LocalLifecycleOwner.current
   DisposableEffect(lifecycleOwner) {
       val observer = LifecycleEventObserver { _, event ->
           if (event == Lifecycle.Event.ON_PAUSE) {
               isPlaying = false
           }
       }
       lifecycleOwner.lifecycle.addObserver(observer)
       onDispose {
           lifecycleOwner.lifecycle.removeObserver(observer)
       }
   }
   ```

2. **视口可见性检测**
   - 使用LazyColumn的`key`和可见性检测
   - 离开屏幕时自动暂停

3. **降低刷新率**
   - 限制最大帧率到30 FPS
   - 使用帧跳过策略

4. **考虑使用ExoPlayer**
   - 将Ugoira转换为视频格式
   - 利用MediaCodec硬件加速
   - 更好的内存管理

---

### 问题2: 图片加载与缓存 🟠 高风险

#### 问题描述

**文件位置**: 
- `composeApp/src/androidMain/kotlin/com/projectu/ui/util/ImageLoaderFactory.android.kt`
- `composeApp/src/commonMain/kotlin/com/projectu/ui/util/ImageLoaderFactory.kt`

#### 具体问题点

1. **过大的磁盘缓存**
   ```kotlin
   // ImageLoaderFactory.android.kt
   private val DEFAULT_CACHE_SIZE = CacheSize.DEFAULT.sizeInBytes  // 512MB
   
   val diskCache = DiskCache.Builder()
       .directory(context.cacheDir.resolve("image_cache").toOkioPath())
       .maxSizeBytes(maxCacheSizeBytes)  // 512MB
       .build()
   ```
   - 512MB缓存意味着大量磁盘I/O
   - 每次启动需要扫描缓存目录
   - 缓存淘汰算法运行消耗CPU

2. **网络请求配置** ✅ **已优化**
   ```kotlin
   install(HttpTimeout) {
       requestTimeoutMillis = 15000    // 15秒超时（已优化）
       connectTimeoutMillis = 10000    // 10秒连接超时（已优化）
   }
   ```
   - ✅ 已缩短超时时间到15秒
   - ✅ 已切换到OkHttp引擎（更好的连接池和HTTP/2支持）
   - 提升了网络性能和资源利用效率

3. **图片Key生成开销**
   ```kotlin
   // ImageLoaderFactory.kt
   class PixivImageKeyer : Keyer<String> {
       override fun key(data: String, options: Options): String {
           // 每次都要解析URL提取ID
           val illustIdRegex = Regex("""(\d+)_p(\d+)""")
           val match = illustIdRegex.find(data)
           // ... 字符串操作
       }
   }
   ```
   - 每张图片加载都执行正则表达式匹配
   - 字符串拼接和操作

4. **缺少内存缓存策略**
   - Coil默认的内存缓存可能不够激进
   - 没有针对缩略图的预加载优化

#### 耗电机制

- **网络**: 移动网络基带持续工作，4G/5G模组耗电量大
- **磁盘I/O**: Flash存储读写耗电
- **CPU**: 图片解码（JPEG/PNG）、缩放、裁剪
- **内存**: 大量Bitmap缓存触发GC

#### 测试方法

1. **网络耗电测试**
   ```
   测试场景: 在排行榜页面快速滚动，加载100张图片
   监控指标:
   - 网络流量（Settings -> 网络使用情况）
   - 网络唤醒次数（Battery Historian）
   - 基带功耗（需要root或专业工具）
   ```

2. **缓存性能测试**
   ```
   测试场景A: 首次加载100张图片（冷启动）
   测试场景B: 二次加载相同图片（热启动）
   对比: CPU使用率、磁盘I/O、内存占用
   ```

3. **Coil调试日志**
   ```kotlin
   // 临时启用Coil日志
   ImageLoader.Builder(context)
       .logger(DebugLogger())
       .build()
   ```

#### 优化方向 ✅ **部分已完成**

1. **优化网络请求** ✅ **已完成**
   - ✅ 缩短超时时间到15秒
   - ✅ 切换到OkHttp引擎
   - ✅ OkHttp自动启用连接池和HTTP/2

2. **减小磁盘缓存**
   - 降低到256MB或128MB
   - 使用更激进的LRU策略

3. **图片预处理**
   - 服务端返回多种尺寸
   - 客户端只下载需要的尺寸
   - 使用WebP格式减少50%体积

4. **懒加载优化**
   - 增加prefetch距离
   - 使用占位符减少重组
   - 实现渐进式加载

---

### 问题3: 无限滚动监听 🟠 高风险

#### 问题描述

**涉及文件**: 几乎所有Screen文件（20+个）
- `RankingScreen.kt`
- `UserScreen.kt`
- `SearchResultScreen.kt`
- `DiscoveryIllustsScreen.kt`
- 等等...

#### 具体问题点

1. **过度使用snapshotFlow**
   ```kotlin
   // 在20+个Screen中都有类似代码
   LaunchedEffect(listState) {
       snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
           .collect { lastVisibleIndex ->
               if (lastVisibleIndex != null && lastVisibleIndex >= artworks.size - 10) {
                   onLoadMore()  // 触发网络请求
               }
           }
   }
   ```
   - 每次滚动都触发Flow emit
   - 在快速滚动时会高频触发（每帧都可能触发）
   - 每个Screen至少1-3个这样的监听器

2. **derivedStateOf过度使用**
   ```kotlin
   // RankingScreen.kt
   val targetScrollIndex by remember(mode.value) {
       derivedStateOf { scrollIndices[mode.value] }
   }
   ```
   - 在多层嵌套的Pager中，每次切换都重新计算
   - Map查找操作在重组时频繁执行

3. **没有防抖机制**
   - 快速滚动时会连续触发多次loadMore
   - 没有请求去重逻辑
   - 可能同时发起多个相同请求

4. **协程作用域管理不当**
   ```kotlin
   val coroutineScope = rememberCoroutineScope()
   // 可能创建大量协程但没有及时取消
   ```

#### 耗电机制

- **CPU**: 持续监听滚动事件，Flow收集和分发
- **网络**: 频繁的API请求加载更多数据
- **内存**: 列表持续增长，旧数据没有及时释放

#### 测试方法

1. **滚动性能测试**
   ```
   测试场景: 在排行榜快速滚动到底部
   监控指标:
   - CPU使用率变化（Profiler）
   - 网络请求频率（Network Inspector）
   - 协程数量（Coroutine Debugger）
   - 帧率（GPU渲染）
   ```

2. **内存泄漏检测**
   ```
   测试场景: 反复进入退出同一列表页面10次
   监控指标:
   - 内存增长趋势（Memory Profiler）
   - LeakCanary报告
   ```

3. **协程监控**
   ```kotlin
   // 添加全局协程调试
   System.setProperty("kotlinx.coroutines.debug", "on")
   
   // 在关键位置打印协程信息
   Log.d("Coroutine", "Active coroutines: ${
       Thread.getAllStackTraces().keys.filter { 
           it.name.contains("coroutine") 
       }.size
   }")
   ```

#### 优化方向

1. **添加防抖/节流**
   ```kotlin
   LaunchedEffect(listState) {
       snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
           .debounce(300)  // 300ms防抖
           .distinctUntilChanged()
           .collect { lastVisibleIndex ->
               if (lastVisibleIndex != null && lastVisibleIndex >= artworks.size - 10) {
                   onLoadMore()
               }
           }
   }
   ```

2. **优化触发阈值**
   - 将触发距离从10改为20
   - 减少触发频率

3. **请求去重**
   ```kotlin
   private var isLoadingMore = false
   
   fun loadMore() {
       if (isLoadingMore) return
       isLoadingMore = true
       // ... 加载逻辑
       isLoadingMore = false
   }
   ```

4. **列表数据分页管理**
   - 使用Paging 3库
   - 实现虚拟滚动
   - 旧数据自动释放

---

### 问题4: 下载系统与视频编码 🟠 高风险

#### 问题描述

**文件位置**:
- `shared/src/commonMain/kotlin/com/projectu/shared/data/manager/DownloadManager.kt`
- `shared/src/androidMain/kotlin/com/projectu/shared/data/util/UgoiraMp4Converter.android.kt`
- `shared/src/desktopMain/kotlin/com/projectu/shared/data/util/UgoiraMp4Converter.desktop.kt`

#### 具体问题点

1. **缺少WorkManager**
   - 下载任务直接在前台协程中执行
   - 没有使用Android推荐的WorkManager
   - 应用退到后台时可能中断
   - 没有电量优化策略

2. **视频编码参数过高**
   ```kotlin
   // UgoiraMp4Converter.android.kt:71
   val bitrate = (encoderWidth * encoderHeight * frameRate * 0.25)
                 .toInt().coerceIn(15_000_000, 50_000_000)
   // 最高50Mbps的码率
   ```
   - 极高的码率导致编码时间长
   - CPU/GPU满负荷运行

3. **无限循环等待编码**
   ```kotlin
   // UgoiraMp4Converter.android.kt:242
   while (true) {
       val bufferInfo = MediaCodec.BufferInfo()
       val outputBufferId = codec.dequeueOutputBuffer(bufferInfo, 10000)
       // ... 编码逻辑
       if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
           break
       }
   }
   ```
   - 持续轮询编码器状态
   - 可能长时间占用CPU

4. **大文件I/O**
   - 直接写入大文件到磁盘
   - 没有缓冲写入优化
   - MediaMuxer同步写入

#### 耗电机制

- **CPU**: 视频编码计算密集型任务
- **GPU**: 可能使用硬件编码器（取决于设备）
- **内存**: 帧缓冲区占用
- **磁盘**: 大文件连续写入
- **散热**: 高负载导致温度升高，触发散热机制

#### 测试方法

1. **编码性能测试**
   ```
   测试场景: 下载并转换一个30秒的Ugoira到MP4
   监控指标:
   - CPU使用率（应该接近100%）
   - 温度变化（使用CPU-Z）
   - 编码时间
   - 文件大小
   - 电量消耗（Battery Historian）
   ```

2. **不同码率对比**
   ```
   测试组:
   A: 50Mbps（当前）
   B: 15Mbps（中等）
   C: 5Mbps（低）
   对比: 编码时间、文件大小、视频质量、耗电量
   ```

3. **MediaCodec性能分析**
   ```kotlin
   val startTime = System.currentTimeMillis()
   val startBattery = getBatteryLevel()
   
   // 编码过程
   
   val endTime = System.currentTimeMillis()
   val endBattery = getBatteryLevel()
   Log.d("Encoding", "Time: ${endTime - startTime}ms, " +
       "Battery: ${startBattery - endBattery}%")
   ```

#### 优化方向

1. **使用WorkManager**
   ```kotlin
   class UgoiraDownloadWorker(
       context: Context,
       params: WorkerParameters
   ) : CoroutineWorker(context, params) {
       override suspend fun doWork(): Result {
           // 下载和编码逻辑
       }
   }
   
   // 提交任务
   val constraints = Constraints.Builder()
       .setRequiredNetworkType(NetworkType.CONNECTED)
       .setRequiresBatteryNotLow(true)  // 电量充足时执行
       .build()
   ```

2. **降低编码码率**
   ```kotlin
   // 改为更合理的码率
   val bitrate = (encoderWidth * encoderHeight * frameRate * 0.1)
                 .toInt().coerceIn(2_000_000, 10_000_000)
   // 最高10Mbps，对于动图足够
   ```

3. **添加编码预设**
   - 质量优先: 15Mbps
   - 平衡: 8Mbps
   - 省电优先: 3Mbps

4. **异步非阻塞编码**
   - 使用MediaCodec的异步模式
   - 避免轮询等待

---

### 问题5: 全局状态缓存系统 🟡 中等风险

#### 问题描述

**文件位置**:
- `shared/src/commonMain/kotlin/com/projectu/shared/data/cache/StateCacheManager.kt`
- `shared/src/commonMain/kotlin/com/projectu/shared/data/repository/StateCacheRepositoryInMemory.kt`

#### 具体问题点

1. **全局Flow订阅**
   ```kotlin
   // 在每个ViewModel中
   viewModelScope.launch {
       stateCacheManager.stateChangeEvents.collect { event ->
           when (event) {
               is StateCacheEvent.ArtworkBookmarkChanged -> {
                   updateArtworkInList(event.artworkId, event.status, event.bookmarkId)
               }
               // ... 更多事件类型
           }
       }
   }
   ```
   - 每个活动的ViewModel都在监听
   - 事件广播到所有订阅者
   - 大量不必要的事件处理

2. **Map操作开销**
   ```kotlin
   private val cache = MutableStateFlow<Map<String, StateCacheEntry>>(emptyMap())
   
   // 每次更新都要复制整个Map
   cache.value = cache.value + (key to entry)
   ```
   - Map复制操作消耗CPU和内存
   - Flow emit触发所有订阅者重组

3. **缓存策略不完善**
   - 缓存无上限增长
   - 没有LRU淘汰机制
   - 旧数据永不过期

#### 耗电机制

- **CPU**: Flow事件分发、Map操作
- **内存**: 无限增长的缓存
- **GC**: 频繁的Map复制触发GC

#### 测试方法

1. **订阅者数量统计**
   ```kotlin
   // 添加监控代码
   private var subscriberCount = 0
   
   init {
       viewModelScope.launch {
           subscriberCount++
           Log.d("StateCache", "Subscriber count: $subscriberCount")
           // ...
       }
   }
   ```

2. **事件频率监控**
   ```kotlin
   private val eventCount = AtomicInteger(0)
   
   fun emitEvent(event: StateCacheEvent) {
       eventCount.incrementAndGet()
       Log.d("StateCache", "Events emitted: ${eventCount.get()}")
       // ...
   }
   ```

3. **内存增长测试**
   ```
   测试场景: 浏览100个作品，收藏50个
   监控: 缓存Map大小、内存占用
   ```

#### 优化方向

1. **限制缓存大小**
   ```kotlin
   private val maxCacheSize = 1000
   
   fun updateCache(key: String, entry: StateCacheEntry) {
       if (cache.value.size >= maxCacheSize) {
           // 移除最旧的条目
           val oldestKey = cache.value.keys.first()
           cache.value = cache.value - oldestKey
       }
       cache.value = cache.value + (key to entry)
   }
   ```

2. **使用Channel替代Flow**
   - Channel只发送给一个订阅者
   - 减少不必要的广播

3. **按需订阅**
   - 只在页面可见时订阅
   - 离开页面时取消订阅

4. **批量更新**
   - 收集多个状态变更
   - 一次性emit

---

### 问题6: 多层导航缓存 🟡 中等风险

#### 问题描述

**涉及文件**:
- `RankingScreen.kt`
- `UserScreen.kt`
- `SearchResultScreen.kt`

#### 具体问题点

1. **过度缓存**
   ```kotlin
   // RankingScreen支持6种内容类型 × 每种5-15个模式
   val listStates: MutableStateMap<String, Any>  // 30-90个状态
   val scrollIndices: MutableMap<String, Int>     // 30-90个位置
   val modeDataCache: Map<String, ModeData>       // 30-90份数据
   ```

2. **HorizontalPager嵌套**
   - 第一层：内容类型Pager
   - 第二层：模式Pager
   - 每层都创建新的LazyStaggeredGridState

3. **状态持久化到对象**
   ```kotlin
   // Tab对象中保存滚动位置
   object RankingTab : Tab {
       private val scrollIndices = mutableStateMapOf<String, Int>()
       // ...
   }
   ```

#### 耗电机制

- **内存**: 大量状态对象和数据缓存
- **GC**: 内存压力触发频繁GC
- **CPU**: 状态查找和同步操作

#### 测试方法

1. **内存快照分析**
   ```
   步骤:
   1. 打开排行榜页面
   2. 切换所有模式和内容类型
   3. 使用Memory Profiler创建heap dump
   4. 分析LazyStaggeredGridState实例数量
   ```

2. **对比测试**
   ```
   A组: 保留所有状态缓存（当前）
   B组: 只缓存当前页面状态
   对比: 内存占用、流畅度
   ```

#### 优化方向

1. **懒加载状态**
   - 只为可见页面创建状态
   - 离开时释放状态

2. **限制缓存数量**
   - 只保留最近访问的5个模式状态
   - 使用LRU淘汰策略

3. **序列化状态**
   - 将滚动位置保存到DataStore
   - 内存中不保存完整状态对象

---

### 问题7: HTTP请求开销 🟢 中低风险

#### 问题描述

**文件位置**: `shared/src/commonMain/kotlin/com/projectu/shared/data/remote/api/PixivApiClient.kt`

#### 具体问题点

1. **冗余请求头**
   ```kotlin
   header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)...")
   header("Accept", "*/*")
   header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8,...")
   header("Priority", "u=1, i")
   header("Sec-Ch-Ua", "\"Chromium\";v=\"142\",...")
   header("Sec-Ch-Ua-Mobile", "?0")
   header("Sec-Ch-Ua-Platform", "\"Windows\"")
   header("Sec-Fetch-Dest", "empty")
   header("Sec-Fetch-Mode", "cors")
   header("Sec-Fetch-Site", "same-origin")
   // 每个请求都添加12个Header
   ```

2. **CIO引擎选择**
   - 使用Ktor CIO引擎
   - OkHttp引擎在Android上通常有更好的优化

3. **没有连接池优化**
   - 可能没有有效复用TCP连接
   - 每次请求都建立新连接

#### 耗电机制

- **网络**: 额外的Header增加数据传输量
- **CPU**: Header构建和序列化

#### 测试方法

1. **网络流量统计**
   ```
   工具: Charles Proxy / Wireshark
   对比: Header占用的字节数 vs 实际数据大小
   ```

2. **引擎性能对比**
   ```
   测试: 发送100个相同请求
   A组: CIO引擎
   B组: OkHttp引擎
   对比: 耗时、CPU使用率
   ```

#### 优化方向

1. **精简Header**
   - 只保留必需的Header
   - User-Agent、Referer、Cookie

2. **切换到OkHttp引擎**
   ```kotlin
   val httpClient = HttpClient(OkHttp) {
       engine {
           config {
               retryOnConnectionFailure(true)
               connectTimeout(10, TimeUnit.SECONDS)
           }
       }
   }
   ```

3. **启用HTTP/2**
   - 多路复用减少连接数
   - 头部压缩减少传输量

---

## 测试验证方案

### 工具清单

#### Android官方工具

1. **Android Studio Profiler**
   - CPU Profiler: 实时CPU使用率、方法追踪
   - Memory Profiler: 内存分配、GC事件、Heap dump
   - Network Inspector: 网络请求时间线、数据量
   - Energy Profiler: 电量使用情况

2. **Battery Historian**
   ```bash
   # 安装
   adb bugreport > bugreport.zip
   
   # 分析
   docker run -p 9999:9999 gcr.io/android-battery-historian/stable:3.0 \
       --port 9999
   # 上传bugreport.zip到 http://localhost:9999
   ```

3. **GPU渲染分析**
   ```
   设置 -> 开发者选项 -> GPU呈现模式分析 -> 在屏幕上显示为条形图
   ```

#### 第三方工具

1. **LeakCanary**
   ```kotlin
   dependencies {
       debugImplementation("com.squareup.leakcanary:leakcanary-android:2.12")
   }
   ```

2. **Chucker** (网络调试)
   ```kotlin
   dependencies {
       debugImplementation("com.github.chuckerteam.chucker:library:4.0.0")
   }
   ```

3. **CPU-Z** (温度监控)
   - Google Play安装
   - 实时监控CPU温度

### 测试流程模板

#### 标准耗电测试流程

```
前置条件:
1. 设备充满电（100%）
2. 关闭所有后台应用
3. 关闭自动亮度，设置屏幕亮度50%
4. 连接到稳定的WiFi网络
5. 清除应用数据，冷启动

测试步骤:
1. 记录初始电量和时间
2. 执行特定测试场景
3. 记录结束电量和时间
4. 计算耗电速率（%/小时）

监控指标:
- 电量消耗（%）
- CPU使用率（%）
- 内存占用（MB）
- 网络流量（MB）
- 磁盘I/O（MB）
- 温度（°C）

测试时长: 至少30分钟
重复次数: 3次取平均值
```

#### 具体测试场景

##### 场景1: Ugoira播放测试

```
测试步骤:
1. 打开一个Ugoira作品详情页
2. 播放动图
3. 保持屏幕常亮，持续播放30分钟
4. 记录数据

对比测试:
- 对照组: 打开普通插画详情页30分钟
- 实验组: 播放Ugoira 30分钟

预期结果:
- 实验组耗电应该显著高于对照组（预估3-5倍）
```

##### 场景2: 瀑布流滚动测试

```
测试步骤:
1. 打开排行榜页面
2. 以中速持续滚动（每3秒滚动一屏）
3. 滚动30分钟，加载至少300张图片
4. 记录数据

对比测试:
- 对照组: 静态浏览首屏30分钟
- 实验组: 持续滚动30分钟

预期结果:
- 实验组耗电应该显著高于对照组（预估2-3倍）
```

##### 场景3: 图片缓存测试

```
测试步骤:
1. 清除应用缓存
2. 打开排行榜，滚动加载100张图片
3. 退出并重新进入，再次滚动相同位置
4. 对比首次和二次加载的耗电

预期结果:
- 二次加载耗电应该明显降低（预估减少50-70%）
- 如果差异不大，说明缓存策略有问题
```

##### 场景4: 后台行为测试

```
测试步骤:
1. 打开应用，开始播放Ugoira
2. 按Home键退到后台
3. 保持后台10分钟
4. 查看是否仍在消耗电量

预期结果:
- 理想情况: 后台耗电接近0
- 问题情况: 后台持续耗电（说明协程未暂停）
```

### 性能基准参考

#### 正常耗电参考值（Android）

```
应用类型            | 耗电速率（%/小时） | 场景
-------------------|------------------|------------------------
静态阅读应用        | 3-5%             | 浏览文字内容
图片浏览应用        | 5-10%            | 浏览图片列表
视频播放应用        | 10-15%           | 播放在线视频（720p）
游戏应用           | 15-25%           | 3D游戏
```

#### ProjectU当前可能的耗电情况（待验证）

```
场景                | 预估耗电速率       | 风险等级
-------------------|------------------|----------
浏览作品列表        | 8-12%/小时        | 中等
快速滚动瀑布流      | 12-18%/小时       | 偏高
播放Ugoira动图     | 20-30%/小时       | 高
下载并转换视频      | 30-40%/小时       | 极高
```

---

## 优化优先级建议

### P0 - 立即优化（预期收益 > 50%）

#### 1. Ugoira播放器优化

**预期收益**: 减少30-40%耗电

**优化项目**:
- [ ] 添加生命周期感知（ON_PAUSE暂停）
- [ ] 添加视口可见性检测
- [ ] 降低最大帧率到30 FPS
- [ ] 优化内存管理（按需加载帧）

**验证方法**: 播放Ugoira 30分钟前后对比

---

#### 2. 图片缓存优化

**预期收益**: 减少15-20%耗电

**优化项目**:
- [ ] 降低磁盘缓存到256MB
- [ ] 缩短网络超时到10秒
- [ ] 优化图片Key生成（缓存结果）
- [ ] 启用更激进的内存缓存

**验证方法**: 快速滚动100张图片前后对比

---

### P1 - 近期优化（预期收益 20-30%）

#### 3. 滚动监听优化

**预期收益**: 减少10-15%耗电

**优化项目**:
- [ ] 添加300ms防抖
- [ ] 增加触发阈值到20
- [ ] 添加请求去重逻辑
- [ ] 优化协程取消机制

**验证方法**: 持续滚动30分钟前后对比

---

#### 4. 下载系统优化

**预期收益**: 减少编码时耗电50%

**优化项目**:
- [ ] 集成WorkManager
- [ ] 降低视频码率到10Mbps
- [ ] 添加编码质量预设
- [ ] 使用异步MediaCodec

**验证方法**: 转换5个Ugoira前后对比

---

### P2 - 中期优化（预期收益 10-15%）

#### 5. 全局状态系统优化

**优化项目**:
- [ ] 添加缓存大小限制（1000条）
- [ ] 实现LRU淘汰
- [ ] 按需订阅Flow
- [ ] 批量更新优化

---

#### 6. 导航缓存优化

**优化项目**:
- [ ] 懒加载页面状态
- [ ] 限制缓存模式数量（5个）
- [ ] 序列化滚动位置到DataStore

---

### P3 - 长期优化（预期收益 < 10%）

#### 7. HTTP请求优化 ✅ **已完成**

**优化项目**:
- ✅ 切换到OkHttp引擎（已完成 - 2025-12-24）
- ✅ 缩短超时时间到15秒（已完成 - 2025-12-24）
- ✅ OkHttp自动启用HTTP/2和连接池优化
- ⏸️ 精简请求Header（后续考虑）

**实施说明**：
- 已在Android平台使用OkHttp引擎替代CIO
- 请求超时从30秒缩短到15秒
- 连接超时从15秒缩短到10秒
- OkHttp提供更好的连接池管理和HTTP/2支持

---

## 附录

### A. 监控代码示例

#### A.1 电量监控工具类

```kotlin
class BatteryMonitor(private val context: Context) {
    
    fun getBatteryLevel(): Int {
        val batteryIntent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        
        return if (level >= 0 && scale > 0) {
            (level.toFloat() / scale.toFloat() * 100).toInt()
        } else {
            -1
        }
    }
    
    fun getBatteryTemperature(): Float {
        val batteryIntent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val temperature = batteryIntent?.getIntExtra(
            BatteryManager.EXTRA_TEMPERATURE, 
            -1
        ) ?: -1
        
        return temperature / 10.0f  // 转换为摄氏度
    }
    
    fun isCharging(): Boolean {
        val batteryIntent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val status = batteryIntent?.getIntExtra(
            BatteryManager.EXTRA_STATUS,
            -1
        ) ?: -1
        
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
               status == BatteryManager.BATTERY_STATUS_FULL
    }
}
```

#### A.2 性能监控工具类

```kotlin
object PerformanceMonitor {
    
    private val metrics = mutableMapOf<String, Long>()
    
    fun startTrack(tag: String) {
        metrics[tag] = System.currentTimeMillis()
    }
    
    fun endTrack(tag: String): Long {
        val start = metrics[tag] ?: return -1
        val duration = System.currentTimeMillis() - start
        Log.d("Performance", "[$tag] Duration: ${duration}ms")
        metrics.remove(tag)
        return duration
    }
    
    fun trackMemory(tag: String) {
        val runtime = Runtime.getRuntime()
        val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
        val maxMemory = runtime.maxMemory() / 1024 / 1024
        Log.d("Performance", "[$tag] Memory: ${usedMemory}MB / ${maxMemory}MB")
    }
    
    fun trackCoroutines(tag: String) {
        val activeThreads = Thread.getAllStackTraces().keys.filter {
            it.name.contains("coroutine")
        }.size
        Log.d("Performance", "[$tag] Active coroutine threads: $activeThreads")
    }
}
```

#### A.3 使用示例

```kotlin
// 在Ugoira播放器中添加监控
@Composable
fun UgoiraPlayer(/* ... */) {
    val batteryMonitor = remember { BatteryMonitor(context) }
    
    DisposableEffect(Unit) {
        val initialBattery = batteryMonitor.getBatteryLevel()
        val initialTemp = batteryMonitor.getBatteryTemperature()
        val startTime = System.currentTimeMillis()
        
        Log.d("UgoiraPlayer", "Start - Battery: $initialBattery%, Temp: $initialTemp°C")
        
        onDispose {
            val finalBattery = batteryMonitor.getBatteryLevel()
            val finalTemp = batteryMonitor.getBatteryTemperature()
            val duration = (System.currentTimeMillis() - startTime) / 1000 / 60
            val consumption = initialBattery - finalBattery
            
            Log.d("UgoiraPlayer", "End - Battery: $finalBattery%, Temp: $finalTemp°C")
            Log.d("UgoiraPlayer", "Consumption: $consumption% in ${duration}min " +
                "(${consumption.toFloat() / duration}%/min)")
        }
    }
    
    // ... 原有代码
}
```

### B. 测试记录模板

```markdown
## 测试记录

### 测试信息
- 日期: YYYY-MM-DD
- 设备: [设备型号]
- Android版本: [版本]
- 应用版本: [版本]
- 测试人员: [姓名]

### 测试场景
[描述测试场景]

### 测试数据

| 指标 | 测试前 | 测试后 | 变化 |
|-----|-------|-------|------|
| 电量 | X% | Y% | -Z% |
| CPU使用率 | X% | Y% | +Z% |
| 内存占用 | XMB | YMB | +ZMB |
| 温度 | X°C | Y°C | +Z°C |

### 测试结论
[结论描述]

### 优化建议
[建议内容]
```

---

## 总结

本报告系统性地分析了ProjectU在Android平台可能存在的7大类耗电问题，预估总体优化空间可达**50-70%的耗电降低**。

### 关键要点

1. **Ugoira播放器**是最主要的耗电源（30-40%），必须优先优化
2. **图片加载**是第二大耗电源（20-25%），优化空间大
3. **无限滚动**影响日常使用体验，需要添加防抖机制
4. 建议采用**分步测试、逐个验证**的方式进行优化

### 下一步行动

1. ✅ 搭建测试环境（Android Studio Profiler + Battery Historian）
2. ✅ 执行基准测试，建立性能基线
3. ✅ 按照P0 → P1 → P2优先级逐个优化
4. ✅ 每次优化后进行A/B对比测试
5. ✅ 记录测试数据，评估优化效果

### 预期成果

完成P0和P1优化后，预期可以达到：
- **正常浏览**: 6-8%/小时（当前8-12%）
- **快速滚动**: 8-12%/小时（当前12-18%）
- **播放Ugoira**: 10-15%/小时（当前20-30%）
- **视频转换**: 15-20%/小时（当前30-40%）

---

*文档版本: v1.0*  
*最后更新: 2025-12-24*
