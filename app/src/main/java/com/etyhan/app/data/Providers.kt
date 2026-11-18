package com.etyhan.app.data

/**
 * 模型服务预设（快捷方式）。
 *
 * 全部走 OpenAI 兼容的 `POST {baseUrl}/chat/completions`，所以换服务只是换 baseUrl + model。
 * 顺序按「对中文用户最省事」排：DeepSeek 默认，其次是国内可直连、有免费额度的。
 */
data class ProviderPreset(
    val id: String,
    val name: String,
    val initial: String,
    val baseUrl: String,
    val model: String,
    val models: List<String>,
    val keyUrl: String,
    val note: String,
    /** 是否默认带 response_format=json_object（不支持时客户端会自动重试） */
    val jsonMode: Boolean = true,
    /** 是否需要 API Key（本地 Ollama 不需要） */
    val needsKey: Boolean = true,
)

object Providers {

    val all: List<ProviderPreset> = listOf(
        ProviderPreset(
            id = "deepseek",
            name = "DeepSeek",
            initial = "DS",
            baseUrl = "https://api.deepseek.com/v1",
            model = "deepseek-chat",
            models = listOf("deepseek-chat", "deepseek-reasoner"),
            keyUrl = "https://platform.deepseek.com/api_keys",
            note = "推荐。便宜、韩语够用，JSON 稳定",
        ),
        ProviderPreset(
            id = "zhipu",
            name = "智谱 GLM",
            initial = "GLM",
            baseUrl = "https://open.bigmodel.cn/api/paas/v4",
            model = "glm-4-flash",
            models = listOf("glm-4-flash", "glm-4-air", "glm-4-plus"),
            keyUrl = "https://bigmodel.cn/usercenter/apikeys",
            note = "glm-4-flash 有免费额度，适合先试",
        ),
        ProviderPreset(
            id = "siliconflow",
            name = "硅基流动",
            initial = "SF",
            baseUrl = "https://api.siliconflow.cn/v1",
            model = "deepseek-ai/DeepSeek-V3",
            models = listOf(
                "deepseek-ai/DeepSeek-V3",
                "deepseek-ai/DeepSeek-R1",
                "Qwen/Qwen2.5-72B-Instruct",
                "Qwen/Qwen2.5-7B-Instruct",
            ),
            keyUrl = "https://cloud.siliconflow.cn/account/ak",
            note = "聚合多家开源模型，新用户有赠送额度",
        ),
        ProviderPreset(
            id = "dashscope",
            name = "阿里百炼",
            initial = "QW",
            baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1",
            model = "qwen-plus",
            models = listOf("qwen-plus", "qwen-max", "qwen-turbo", "qwen2.5-72b-instruct"),
            keyUrl = "https://bailian.console.aliyun.com/?tab=model#/api-key",
            note = "通义千问，韩语语法解释比较细",
        ),
        ProviderPreset(
            id = "moonshot",
            name = "Kimi",
            initial = "K",
            baseUrl = "https://api.moonshot.cn/v1",
            model = "moonshot-v1-8k",
            models = listOf("moonshot-v1-8k", "moonshot-v1-32k", "kimi-latest"),
            keyUrl = "https://platform.moonshot.cn/console/api-keys",
            note = "中文表达自然，长句分析稳",
        ),
        ProviderPreset(
            id = "openrouter",
            name = "OpenRouter",
            initial = "OR",
            baseUrl = "https://openrouter.ai/api/v1",
            model = "deepseek/deepseek-chat",
            models = listOf(
                "deepseek/deepseek-chat",
                "google/gemini-2.0-flash-001",
                "anthropic/claude-3.5-haiku",
            ),
            keyUrl = "https://openrouter.ai/keys",
            note = "一个 Key 用多家模型（需要外币卡）",
        ),
        ProviderPreset(
            id = "openai",
            name = "OpenAI",
            initial = "AI",
            baseUrl = "https://api.openai.com/v1",
            model = "gpt-4o-mini",
            models = listOf("gpt-4o-mini", "gpt-4o", "gpt-4.1-mini"),
            keyUrl = "https://platform.openai.com/api-keys",
            note = "需要能访问 api.openai.com",
        ),
        ProviderPreset(
            id = "gemini",
            name = "Gemini",
            initial = "G",
            baseUrl = "https://generativelanguage.googleapis.com/v1beta/openai",
            model = "gemini-2.0-flash",
            models = listOf("gemini-2.0-flash", "gemini-1.5-flash"),
            keyUrl = "https://aistudio.google.com/apikey",
            note = "Google 的 OpenAI 兼容端点，有免费额度",
        ),
        ProviderPreset(
            id = "ollama",
            name = "本地 Ollama",
            initial = "OL",
            baseUrl = "http://10.0.2.2:11434/v1",
            model = "qwen2.5:7b",
            models = listOf("qwen2.5:7b", "qwen2.5:14b", "gemma2:9b"),
            keyUrl = "",
            note = "完全离线。模拟器用 10.0.2.2，真机填电脑局域网 IP",
            jsonMode = false,
            needsKey = false,
        ),
        ProviderPreset(
            id = "custom",
            name = "自定义",
            initial = "＋",
            baseUrl = "",
            model = "",
            models = emptyList(),
            keyUrl = "",
            note = "任何 OpenAI 兼容服务，自己填地址和模型名",
        ),
    )

    fun byId(id: String): ProviderPreset = all.firstOrNull { it.id == id } ?: all.first()
}
