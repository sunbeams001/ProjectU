package com.projectu.ui.screens.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.projectu.shared.di.pixivApiModule
import com.projectu.ui.screens.home.HomeScreen
import com.projectu.ui.screens.settings.BackupRestoreScreen
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules
import projectu.composeapp.generated.resources.*

/**
 * 登录屏幕
 * 用于输入 PHPSESSID 进行登录
 * 预留应用内登录功能的扩展空间
 */
class LoginScreen : Screen {
    
    @Composable
    override fun Content() {
        val viewModel: LoginViewModel = koinInject()
        val state by viewModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        
        // 监听登录成功
        LaunchedEffect(state.isLoading) {
            if (!state.isLoading && state.errorMessage == null && state.phpSessionId.isNotBlank()) {
                // 登录成功，导航到主页
                navigator.replaceAll(HomeScreen())
            }
        }
        
        LoginScreenContent(
            state = state,
            onIntent = viewModel::handleIntent
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoginScreenContent(
    state: LoginScreenState,
    onIntent: (LoginIntent) -> Unit
) {
    val navigator = LocalNavigator.currentOrThrow
    val keyboardController = LocalSoftwareKeyboardController.current
    var passwordVisible by remember { mutableStateOf(false) }
    var showWebViewLogin by remember { mutableStateOf(false) }
    var isManualInputExpanded by remember { mutableStateOf(false) }
    
    // 如果有错误或正在加载或已经输入了内容，自动展开手动输入框
    LaunchedEffect(state.errorMessage, state.isLoading, state.phpSessionId) {
        if (state.errorMessage != null || state.isLoading || state.phpSessionId.isNotEmpty()) {
            isManualInputExpanded = true
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.login_title)) },
                actions = {
                    IconButton(onClick = { onIntent(LoginIntent.ToggleHelpDialog(true)) }) {
                        Icon(Icons.AutoMirrored.Filled.Help, contentDescription = stringResource(Res.string.login_help))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            // 应用Logo/标题
            Text(
                text = "ProjectU",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = stringResource(Res.string.login_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 1. 推荐方式：WebView 登录卡片
            Card(
                onClick = { showWebViewLogin = true },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(Res.string.login_webview_button),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(Res.string.login_subtitle), // 这里复用一下 subtitle 作为描述
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = null
                    )
                }
            }
            
            // 2. 高级方式：手动输入 Token 卡片（可展开）
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { isManualInputExpanded = !isManualInputExpanded }
            ) {
                Column {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(Res.string.login_phpsessid_title),
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (!isManualInputExpanded) {
                                Text(
                                    text = stringResource(Res.string.login_phpsessid_description),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    AnimatedVisibility(
                        visible = isManualInputExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                             // 直接嵌入表单内容，去掉外层 Card 以避免嵌套 Card
                             OutlinedTextField(
                                value = state.phpSessionId,
                                onValueChange = { onIntent(LoginIntent.PhpSessionIdChanged(it)) },
                                label = { Text(stringResource(Res.string.login_phpsessid_label)) },
                                placeholder = { Text("12345678_xxxxxxxxxxxx") },
                                visualTransformation = if (passwordVisible) VisualTransformation.None 
                                    else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            imageVector = if (passwordVisible) Icons.Default.Visibility 
                                                else Icons.Default.VisibilityOff,
                                            contentDescription = null
                                        )
                                    }
                                },
                                singleLine = true,
                                enabled = !state.isLoading,
                                isError = state.errorMessage != null,
                                supportingText = state.errorMessage?.let { { Text(it) } },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { 
                                    keyboardController?.hide()
                                    onIntent(LoginIntent.LoginClicked) 
                                }),
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Button(
                                onClick = {
                                    keyboardController?.hide()
                                    onIntent(LoginIntent.LoginClicked)
                                },
                                enabled = !state.isLoading && state.phpSessionId.isNotBlank(),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (state.isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(stringResource(if (state.isLoading) Res.string.login_logging_in else Res.string.login_button))
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
            
            // 占位，把底部备份恢复顶下去（如果在更大屏幕上）
            // 但在 Column 中使用 weight 需要父级也是明确大小，这里暂用 Spacer
            Spacer(modifier = Modifier.height(32.dp))
            
            // 3. 底部辅助：从备份恢复
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                border = null, // 无边框，像一个按钮
                modifier = Modifier.fillMaxWidth().clickable { navigator.push(BackupRestoreScreen()) }
            ) {
                 Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Restore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(Res.string.login_restore_from_backup),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = stringResource(Res.string.login_restore_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
    
    // 帮助对话框
    if (state.showHelpDialog) {
        HelpDialog(
            onDismiss = { onIntent(LoginIntent.ToggleHelpDialog(false)) }
        )
    }
    
    // WebView登录对话框
    if (showWebViewLogin) {
        WebViewLoginDialog(
            onSuccess = { phpsessid ->
                showWebViewLogin = false
                onIntent(LoginIntent.PhpSessionIdChanged(phpsessid))
                onIntent(LoginIntent.LoginClicked)
                
                // 重新加载 Pixiv API 模块以使用新的凭据
                // 这样可以立即生效，无需重启应用
                try {
                    unloadKoinModules(pixivApiModule)
                    loadKoinModules(pixivApiModule)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            onDismiss = {
                showWebViewLogin = false
            }
        )
    }
}


/**
 * PHPSESSID 登录表单
 */
@Composable
private fun PhpSessionIdLoginForm(
    phpSessionId: String,
    isLoading: Boolean,
    errorMessage: String?,
    passwordVisible: Boolean,
    onPhpSessionIdChange: (String) -> Unit,
    onPasswordVisibilityToggle: () -> Unit,
    onLoginClick: () -> Unit,
    onErrorDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(Res.string.login_phpsessid_title),
                style = MaterialTheme.typography.titleMedium
            )
            
            Text(
                text = stringResource(Res.string.login_phpsessid_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            OutlinedTextField(
                value = phpSessionId,
                onValueChange = onPhpSessionIdChange,
                label = { Text(stringResource(Res.string.login_phpsessid_label)) },
                placeholder = { Text("12345678_xxxxxxxxxxxx") },
                visualTransformation = if (passwordVisible) VisualTransformation.None 
                    else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = onPasswordVisibilityToggle) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility 
                                else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) stringResource(Res.string.login_password_hide) else stringResource(Res.string.login_password_show)
                        )
                    }
                },
                singleLine = true,
                enabled = !isLoading,
                isError = errorMessage != null,
                supportingText = errorMessage?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onLoginClick() }),
                modifier = Modifier.fillMaxWidth()
            )
            
            Button(
                onClick = onLoginClick,
                enabled = !isLoading && phpSessionId.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(stringResource(if (isLoading) Res.string.login_logging_in else Res.string.login_button))
            }
        }
    }
}

/**
 * 应用内登录表单（预留）
 */
@Composable
private fun AppLoginForm() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(Res.string.login_in_app),
                style = MaterialTheme.typography.titleMedium
            )
            
            Text(
                text = stringResource(Res.string.login_in_development),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 登录方式切换按钮（预留）
 */
@Composable
private fun SegmentedButton(
    currentMode: LoginMode,
    onModeChange: (LoginMode) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = currentMode == LoginMode.PHPSESSID,
            onClick = { onModeChange(LoginMode.PHPSESSID) },
            label = { Text(stringResource(Res.string.login_phpsessid_login)) },
            modifier = Modifier.weight(1f)
        )
        
        FilterChip(
            selected = currentMode == LoginMode.APP_LOGIN,
            onClick = { onModeChange(LoginMode.APP_LOGIN) },
            label = { Text(stringResource(Res.string.login_in_app)) },
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * 帮助对话框
 */
@Composable
private fun HelpDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.login_help)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(Res.string.login_help_step1),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(Res.string.login_help_step2),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(Res.string.login_help_step3),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(Res.string.login_help_step4),
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = stringResource(Res.string.login_help_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.common_ok))
            }
        }
    )
}
