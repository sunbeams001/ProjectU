package com.projectu.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.projectu.shared.domain.model.FollowStatus
import com.projectu.shared.domain.model.User
import com.projectu.shared.domain.repository.UserRepository
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * 关注状态指示器组件（带内置关注逻辑）
 * 
 * 视觉效果：
 * - 未关注（NOT_FOLLOWING）：PersonAdd 图标（灰色）
 * - 公开关注（PUBLIC）：PersonRemove 图标（蓝色）
 * - 悄悄关注（PRIVATE）：PersonRemove 图标 + VisibilityOff 图标（蓝色，略显透明）
 * 
 * 交互逻辑：
 * - 短按：切换关注状态（未关注→公开关注→未关注）
 * - 长按：切换私密关注（未关注→悄悄关注，公开关注→悄悄关注，悄悄关注→未关注）
 * 
 * @param user 用户对象（需要包含完整的关注信息）
 * @param size 组件大小
 * @param onStatusChanged 状态变化回调（可选，用于更新UI）
 * @param modifier 修饰符
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FollowIndicator(
    user: User,
    size: Dp = 24.dp,
    onStatusChanged: ((FollowStatus) -> Unit)? = null,
    modifier: Modifier = Modifier,
    repository: UserRepository = koinInject()
) {
    var followStatus by remember { mutableStateOf(user.followStatus) }
    var isProcessing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // 监听外部状态变化，同步内部状态
    LaunchedEffect(user.followStatus) {
        followStatus = user.followStatus
    }
    
    // 颜色定义
    val followedColor = Color(0xFF2196F3) // Material Blue
    val privateFollowedColor = followedColor.copy(alpha = 0.7f)
    val notFollowedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .combinedClickable(
                enabled = !isProcessing,
                onClick = {
                    if (isProcessing) return@combinedClickable
                    
                    scope.launch {
                        isProcessing = true
                        try {
                            when (followStatus) {
                                FollowStatus.NOT_FOLLOWING -> {
                                    // 未关注 → 公开关注
                                    println("📌 FollowIndicator: 添加公开关注 - 用户ID: ${user.id}")
                                    repository.followUser(user.id.toLong(), restrict = "public")
                                        .onSuccess {
                                            followStatus = FollowStatus.PUBLIC
                                            onStatusChanged?.invoke(FollowStatus.PUBLIC)
                                            println("✅ 公开关注成功")
                                        }.onFailure { e ->
                                            println("❌ 关注失败: ${e.message}")
                                        }
                                }
                                FollowStatus.PUBLIC, FollowStatus.PRIVATE -> {
                                    // 已关注 → 取消关注
                                    println("📌 FollowIndicator: 取消关注 - 用户ID: ${user.id}")
                                    repository.unfollowUser(user.id.toLong())
                                        .onSuccess {
                                            followStatus = FollowStatus.NOT_FOLLOWING
                                            onStatusChanged?.invoke(FollowStatus.NOT_FOLLOWING)
                                            println("✅ 已取消关注")
                                        }.onFailure { e ->
                                            println("❌ 取消关注失败: ${e.message}")
                                        }
                                }
                            }
                        } finally {
                            isProcessing = false
                        }
                    }
                },
                onLongClick = {
                    if (isProcessing) return@combinedClickable
                    
                    scope.launch {
                        isProcessing = true
                        try {
                            when (followStatus) {
                                FollowStatus.NOT_FOLLOWING -> {
                                    // 未关注 → 悄悄关注
                                    println("📌 FollowIndicator: 添加悄悄关注 - 用户ID: ${user.id}")
                                    repository.followUser(user.id.toLong(), restrict = "private")
                                        .onSuccess {
                                            followStatus = FollowStatus.PRIVATE
                                            onStatusChanged?.invoke(FollowStatus.PRIVATE)
                                            println("✅ 悄悄关注成功")
                                        }.onFailure { e ->
                                            println("❌ 悄悄关注失败: ${e.message}")
                                        }
                                }
                                FollowStatus.PUBLIC -> {
                                    // 公开关注 → 悄悄关注
                                    println("📌 FollowIndicator: 转换为悄悄关注 - 用户ID: ${user.id}")
                                    repository.followUser(user.id.toLong(), restrict = "private")
                                        .onSuccess {
                                            followStatus = FollowStatus.PRIVATE
                                            onStatusChanged?.invoke(FollowStatus.PRIVATE)
                                            println("✅ 已转换为悄悄关注")
                                        }.onFailure { e ->
                                            println("❌ 转换失败: ${e.message}")
                                        }
                                }
                                FollowStatus.PRIVATE -> {
                                    // 悄悄关注 → 取消关注
                                    println("📌 FollowIndicator: 取消悄悄关注 - 用户ID: ${user.id}")
                                    repository.unfollowUser(user.id.toLong())
                                        .onSuccess {
                                            followStatus = FollowStatus.NOT_FOLLOWING
                                            onStatusChanged?.invoke(FollowStatus.NOT_FOLLOWING)
                                            println("✅ 已取消关注")
                                        }.onFailure { e ->
                                            println("❌ 取消关注失败: ${e.message}")
                                        }
                                }
                            }
                        } finally {
                            isProcessing = false
                        }
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        when (followStatus) {
            FollowStatus.NOT_FOLLOWING -> {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = "关注",
                    tint = notFollowedColor,
                    modifier = Modifier.size(size * 0.7f)
                )
            }
            FollowStatus.PUBLIC -> {
                Icon(
                    imageVector = Icons.Default.PersonRemove,
                    contentDescription = "取消关注",
                    tint = followedColor,
                    modifier = Modifier.size(size * 0.7f)
                )
            }
            FollowStatus.PRIVATE -> {
                // 悄悄关注：PersonRemove 图标 + VisibilityOff 覆盖层
                Icon(
                    imageVector = Icons.Default.PersonRemove,
                    contentDescription = "取消悄悄关注",
                    tint = privateFollowedColor,
                    modifier = Modifier.size(size * 0.7f)
                )
                // TODO: 可以考虑添加小的 VisibilityOff 图标在右下角作为标记
            }
        }
    }
}
