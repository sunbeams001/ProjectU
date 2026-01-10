package com.projectu.ui.screens.apitest

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.compose.koinInject

/**
 * API 测试页面
 */
class ApiTestScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel: ApiTestViewModel = koinInject()
        val state by viewModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Pixiv API 测试工具") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // 左侧: API 选择器
                ApiSelector(
                    modifier = Modifier
                        .weight(0.3f)
                        .fillMaxHeight(),
                    selectedModule = state.selectedModule,
                    selectedMethod = state.selectedMethod,
                    onModuleSelect = { viewModel.handleIntent(ApiTestIntent.SelectModule(it)) },
                    onMethodSelect = { viewModel.handleIntent(ApiTestIntent.SelectMethod(it)) }
                )
                
                // 右侧: 参数输入 + 结果展示
                Column(
                    modifier = Modifier
                        .weight(0.7f)
                        .fillMaxHeight()
                ) {
                    // 登录状态提示
                    if (!state.isLoginValid) {
                        LoginWarningCard()
                    }
                    
                    // 参数输入区
                    ParameterInputSection(
                        modifier = Modifier.weight(0.4f),
                        method = state.selectedMethod,
                        parameterValues = state.parameterValues,
                        onParameterChange = { name, value ->
                            viewModel.handleIntent(ApiTestIntent.UpdateParameter(name, value))
                        },
                        onExecute = { viewModel.handleIntent(ApiTestIntent.ExecuteTest) },
                        isLoading = state.testResult is TestResult.Loading
                    )
                    
                    HorizontalDivider()
                    
                    // 结果展示区
                    TestResultSection(
                        modifier = Modifier.weight(0.6f),
                        result = state.testResult,
                        onClear = { viewModel.handleIntent(ApiTestIntent.ClearResult) }
                    )
                }
            }
        }
    }
}

/**
 * API 选择器组件
 */
@Composable
fun ApiSelector(
    selectedModule: ApiModule,
    selectedMethod: ApiMethod?,
    onModuleSelect: (ApiModule) -> Unit,
    onMethodSelect: (ApiMethod) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(8.dp)
    ) {
        // API 模块区域 - 固定比例
        Column(
            modifier = Modifier
                .weight(0.35f)
                .fillMaxWidth()
        ) {
            Text(
                text = "API 模块",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(8.dp)
            )
            
            // 模块选择 - 使用滚动列
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(ApiModule.entries) { module ->
                    ModuleChip(
                        module = module,
                        isSelected = selectedModule == module,
                        onClick = { onModuleSelect(module) }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))
        
        // API 方法区域 - 固定比例
        Column(
            modifier = Modifier
                .weight(0.65f)
                .fillMaxWidth()
        ) {
            Text(
                text = "API 方法",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(8.dp)
            )
            
            // 方法列表
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val methods = ApiMethod.getMethodsByModule(selectedModule)
                items(methods) { method ->
                    MethodItem(
                        method = method,
                        isSelected = selectedMethod == method,
                        onClick = { onMethodSelect(method) }
                    )
                }
            }
        }
    }
}

@Composable
fun ModuleChip(
    module: ApiModule,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(module.displayName) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

@Composable
fun MethodItem(
    method: ApiMethod,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val priorityColor = when (method.priority) {
        0 -> MaterialTheme.colorScheme.error
        1 -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.tertiary
    }
    
    val priorityText = when (method.priority) {
        0 -> "P0"
        1 -> "P1"
        else -> "P2"
    }
    
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            CardDefaults.outlinedCardBorder()
        }
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // 优先级标签
            Surface(
                color = priorityColor,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.align(Alignment.Start)
            ) {
                Text(
                    text = priorityText,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onError
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = method.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            Text(
                text = method.methodName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 登录警告卡片
 */
@Composable
fun LoginWarningCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "未配置登录凭据",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = "请先在设置页面配置 PHPSESSID",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

/**
 * 参数输入区
 */
@Composable
fun ParameterInputSection(
    method: ApiMethod?,
    parameterValues: Map<String, String>,
    onParameterChange: (String, String) -> Unit,
    onExecute: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        if (method == null) {
            Text(
                text = "← 请选择要测试的 API 方法",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(32.dp)
            )
            return@Column
        }
        
        Text(
            text = "测试参数",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // 参数输入
        method.parameters.forEach { param ->
            ParameterInput(
                parameter = param,
                value = parameterValues[param.name] ?: param.defaultValue,
                onValueChange = { onParameterChange(param.name, it) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 执行按钮
        Button(
            onClick = onExecute,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("执行中...")
            } else {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("执行测试")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParameterInput(
    parameter: ApiParameter,
    value: String,
    onValueChange: (String) -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = parameter.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            if (parameter.required) {
                Text(
                    text = " *",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        if (parameter.options != null) {
            // 下拉选择
            var expanded by remember { mutableStateOf(false) }
            
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = value,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )
                
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    parameter.options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                onValueChange(option)
                                expanded = false
                            }
                        )
                    }
                }
            }
        } else {
            // 文本输入
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(parameter.defaultValue) },
                singleLine = true
            )
        }
    }
}

/**
 * 测试结果展示区
 */
@Composable
fun TestResultSection(
    result: TestResult,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "测试结果",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            if (result !is TestResult.Idle) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Clear, "清除结果")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Card(
            modifier = Modifier.fillMaxSize(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            when (result) {
                is TestResult.Idle -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "等待执行测试...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                is TestResult.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("正在调用 API...")
                        }
                    }
                }
                
                is TestResult.Success -> {
                    SuccessResultView(result)
                }
                
                is TestResult.Error -> {
                    ErrorResultView(result)
                }
            }
        }
    }
}

@Composable
fun SuccessResultView(result: TestResult.Success) {
    var selectedTab by remember { mutableStateOf(0) }
    
    Column(modifier = Modifier.fillMaxSize()) {
        SecondaryTabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("摘要") },
                icon = { Icon(Icons.Default.Info, null) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("原始 JSON") },
                icon = { Icon(Icons.Default.Code, null) }
            )
        }
        
        when (selectedTab) {
            0 -> {
                // 摘要视图
                SelectionContainer {
                    Text(
                        text = result.summary,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            1 -> {
                // JSON 视图
                SelectionContainer {
                    Text(
                        text = result.rawJson,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun ErrorResultView(result: TestResult.Error) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "测试失败",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        SelectionContainer {
            Column {
                Text(
                    text = result.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                
                if (result.stackTrace != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "堆栈跟踪:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = result.stackTrace,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
