package com.projectu.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.projectu.ui.components.SimpleAdaptiveLayout
import com.projectu.ui.screens.settings.SettingsScreen
import com.projectu.ui.util.WindowSize

/**
 * 主屏幕 - 包含底部导航栏的容器
 * 使用自适应布局支持手机和平板
 */
class HomeScreen : Screen {
    
    @Composable
    override fun Content() {
        SimpleAdaptiveLayout(
            phoneContent = { windowSize -> HomeScreenPhone(windowSize) },
            tabletContent = { windowSize -> HomeScreenTablet(windowSize) }
        )
    }
}

/**
 * 手机布局 - 底部导航栏
 */
@Composable
private fun HomeScreenPhone(windowSize: WindowSize) {
    TabNavigator(HomeTab) {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    TabNavigationItem(HomeTab)
                    TabNavigationItem(DiscoveryTab)
                    TabNavigationItem(RankingTab)
                    TabNavigationItem(ProfileTab)
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                it.current.Content()
            }
        }
    }
}

/**
 * 平板/桌面布局 - 侧边导航栏
 */
@Composable
private fun HomeScreenTablet(windowSize: WindowSize) {
    TabNavigator(HomeTab) {
        Row(modifier = Modifier.fillMaxSize()) {
            // 侧边导航栏
            NavigationRail(
                modifier = Modifier.fillMaxHeight()
            ) {
                Spacer(Modifier.height(16.dp))
                NavigationRailItem(
                    selected = it.current == HomeTab,
                    onClick = { it.current = HomeTab },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("首页") }
                )
                NavigationRailItem(
                    selected = it.current == DiscoveryTab,
                    onClick = { it.current = DiscoveryTab },
                    icon = { Icon(Icons.Default.Search, contentDescription = null) },
                    label = { Text("发现") }
                )
                NavigationRailItem(
                    selected = it.current == RankingTab,
                    onClick = { it.current = RankingTab },
                    icon = { Icon(Icons.Default.Star, contentDescription = null) },
                    label = { Text("排行") }
                )
                NavigationRailItem(
                    selected = it.current == ProfileTab,
                    onClick = { it.current = ProfileTab },
                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                    label = { Text("我的") }
                )
            }
            
            // 内容区域
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                it.current.Content()
            }
        }
    }
}

@Composable
private fun RowScope.TabNavigationItem(tab: Tab) {
    val tabNavigator = cafe.adriel.voyager.navigator.tab.LocalTabNavigator.current
    val title = when (tab) {
        HomeTab -> "首页"
        DiscoveryTab -> "发现"
        RankingTab -> "排行"
        ProfileTab -> "我的"
        else -> ""
    }
    val icon = when (tab) {
        HomeTab -> Icons.Default.Home
        DiscoveryTab -> Icons.Default.Search
        RankingTab -> Icons.Default.Star
        ProfileTab -> Icons.Default.Person
        else -> Icons.Default.Home
    }
    
    NavigationBarItem(
        selected = tabNavigator.current == tab,
        onClick = { tabNavigator.current = tab },
        icon = { Icon(icon, contentDescription = title) },
        label = { Text(title) }
    )
}

// 主页标签
object HomeTab : Tab {
    override val options: TabOptions
        @Composable
        get() {
            val icon = rememberVectorPainter(Icons.Default.Home)
            return remember {
                TabOptions(
                    index = 0u,
                    title = "首页",
                    icon = icon
                )
            }
        }
    
    @Composable
    override fun Content() {
        HomeTabContent()
    }
}

@Composable
fun HomeTabContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "首页",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "项目框架搭建完成",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// 发现标签
object DiscoveryTab : Tab {
    override val options: TabOptions
        @Composable
        get() {
            val icon = rememberVectorPainter(Icons.Default.Search)
            return remember {
                TabOptions(
                    index = 1u,
                    title = "发现",
                    icon = icon
                )
            }
        }
    
    @Composable
    override fun Content() {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "发现",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }
    }
}

// 排行榜标签
object RankingTab : Tab {
    override val options: TabOptions
        @Composable
        get() {
            val icon = rememberVectorPainter(Icons.Default.Star)
            return remember {
                TabOptions(
                    index = 2u,
                    title = "排行",
                    icon = icon
                )
            }
        }
    
    @Composable
    override fun Content() {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "排行榜",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }
    }
}

// 个人资料标签
object ProfileTab : Tab {
    override val options: TabOptions
        @Composable
        get() {
            val icon = rememberVectorPainter(Icons.Default.Person)
            return remember {
                TabOptions(
                    index = 3u,
                    title = "我的",
                    icon = icon
                )
            }
        }
    
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "我的",
                    style = MaterialTheme.typography.headlineMedium
                )
                
                // 设置按钮
                Button(
                    onClick = { navigator.push(SettingsScreen()) },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("设置")
                }
            }
        }
    }
}
