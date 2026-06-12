package com.example.android_dev.ai

// 火山引擎 ARK 配置功能：集中存放大模型访问地址、模型名与访问令牌。
//
// 使用说明：
//   1. 登录火山引擎 ARK 平台，创建端点并获取 API Key 和 Endpoint ID。
//   2. 把 API Key 填入 API_TOKEN，把 Endpoint ID 填入 MODEL。
//
// 留空时（API_TOKEN 为空字符串），应用会自动降级到本地启发式拆解（LocalAiTaskPlanner），
// 保证「AI 智能拆解」功能在无网络/无令牌时依旧可用。
object PaddleAiConfig {

    // 火山引擎 ARK 对话补全接口地址。
    const val BASE_URL = "https://ark.cn-beijing.volces.com/api/v3/chat/completions"

    // 端点 ID（Endpoint ID）。
    const val MODEL = "ep-20260612212041-sqprz"

    // 访问令牌（API Key）。
    const val API_TOKEN = "ark-eada6e4b-df8b-4e50-8ab0-39dc917484b4-73965"

    // 连接超时（秒）：连不上就快速失败。
    const val CONNECT_TIMEOUT_SECONDS = 15L

    // 读/写/整体超时（秒）：大模型生成较慢，给足时间，避免长任务被误判超时。
    const val CALL_TIMEOUT_SECONDS = 90L

    // 兼容旧引用的单次请求超时（秒）。
    const val TIMEOUT_SECONDS = CALL_TIMEOUT_SECONDS

    // 是否已配置令牌：用于决定走远程还是本地。
    val hasToken: Boolean
        get() = API_TOKEN.isNotBlank()
}
