package com.projectu.ui.screens.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.projectu.ui.screens.home.HomeScreen
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
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
    val keyboardController = LocalSoftwareKeyboardController.current
    var passwordVisible by remember { mutableStateOf(false) }
    
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
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 登录方式切换（预留）
            if (false) {  // 暂时隐藏，等应用内登录实现后开启
                SegmentedButton(
                    currentMode = state.loginMode,
                    onModeChange = { onIntent(LoginIntent.SwitchLoginMode(it)) }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            when (state.loginMode) {
                LoginMode.PHPSESSID -> {
                    PhpSessionIdLoginForm(
                        phpSessionId = state.phpSessionId,
                        isLoading = state.isLoading,
                        errorMessage = state.errorMessage,
                        passwordVisible = passwordVisible,
                        onPhpSessionIdChange = { onIntent(LoginIntent.PhpSessionIdChanged(it)) },
                        onPasswordVisibilityToggle = { passwordVisible = !passwordVisible },
                        onLoginClick = {
                            keyboardController?.hide()
                            onIntent(LoginIntent.LoginClicked)
                        },
                        onErrorDismiss = { onIntent(LoginIntent.ClearError) }
                    )
                }
                
                LoginMode.APP_LOGIN -> {
                    // 预留：应用内登录表单
                    AppLoginForm()
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
