package com.projectu.ui.screens.apitest

/**
 * API 测试页面状态
 */
data class ApiTestState(
    val selectedModule: ApiModule = ApiModule.ILLUST,
    val selectedMethod: ApiMethod? = null,
    val parameterValues: Map<String, String> = emptyMap(),
    val testResult: TestResult = TestResult.Idle,
    val testHistory: List<TestHistoryItem> = emptyList(),
    val isLoginValid: Boolean = false
)

/**
 * API 测试页面意图
 */
sealed interface ApiTestIntent {
    data class SelectModule(val module: ApiModule) : ApiTestIntent
    data class SelectMethod(val method: ApiMethod) : ApiTestIntent
    data class UpdateParameter(val name: String, val value: String) : ApiTestIntent
    data object ExecuteTest : ApiTestIntent
    data object ClearResult : ApiTestIntent
    data class LoadHistoryItem(val item: TestHistoryItem) : ApiTestIntent
}

/**
 * 测试历史记录
 */
data class TestHistoryItem(
    val method: ApiMethod,
    val parameters: Map<String, String>,
    val result: TestResult,
    val timestamp: Long = System.currentTimeMillis()
) {
    val displayName: String
        get() = "[${method.module.displayName}] ${method.displayName}"
}
