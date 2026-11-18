package com.etyhan.app.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

data class LlmConfig(
    val baseUrl: String,
    val apiKey: String,
    val model: String,
    val jsonMode: Boolean = true,
    val maxTokens: Int = 1200,
    val temperature: Float = 0.3f,
)

data class Completion(
    val content: String,
    val promptTokens: Int,
    val completionTokens: Int,
    val finishReason: String,
    val model: String,
    val latencyMs: Long,
)

class LlmException(
    override val message: String,
    val status: Int? = null,
    val raw: String? = null,
    val hint: String? = null,
) : Exception(message)

/** 所有请求都走 OpenAI 兼容的 /chat/completions，一套代码接 DeepSeek、GLM、千问、Kimi、Ollama… */
class LlmClient(private val json: Json) {

    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(240, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    suspend fun complete(cfg: LlmConfig, system: String, user: String): Completion = withContext(Dispatchers.IO) {
        if (cfg.baseUrl.isBlank()) throw LlmException("Base URL 为空", hint = "到设置里选一个服务，或填自定义地址")
        if (cfg.model.isBlank()) throw LlmException("模型名为空", hint = "到设置里填模型名，例如 deepseek-chat")
        val started = System.currentTimeMillis()

        var attempt = 0
        while (true) {
            // 第一次按设置带 response_format；兼容实现不认（400/422）时去掉再试一次。
            val withJsonMode = cfg.jsonMode && attempt == 0
            val (code, body) = post(cfg, system, user, withJsonMode, cfg.maxTokens)
            if (code in 200..299) return@withContext parseCompletion(body, System.currentTimeMillis() - started)
            if (withJsonMode && (code == 400 || code == 422)) {
                attempt++
                continue
            }
            throw httpError(code, body)
        }
        @Suppress("UNREACHABLE_CODE")
        throw LlmException("请求失败")
    }

    suspend fun ping(cfg: LlmConfig): String = withContext(Dispatchers.IO) {
        val (code, body) = post(cfg, "你是一个连接测试助手。", "只回复两个字母：OK", false, 24)
        if (code !in 200..299) throw httpError(code, body)
        val content = parseCompletion(body, 0).content
        if (content.isBlank()) throw LlmException("连接成功，但模型返回空内容")
        "连接成功 · ${cfg.model} · 回复：${content.trim().take(40)}"
    }

    private fun post(
        cfg: LlmConfig,
        system: String,
        user: String,
        jsonMode: Boolean,
        maxTokens: Int,
    ): Pair<Int, String> {
        val payload = buildJsonObject {
            put("model", cfg.model.trim())
            put("messages", buildJsonArray {
                add(buildJsonObject {
                    put("role", "system")
                    put("content", system)
                })
                add(buildJsonObject {
                    put("role", "user")
                    put("content", user)
                })
            })
            put("temperature", cfg.temperature)
            put("max_tokens", maxTokens)
            put("stream", false)
            if (jsonMode) putJsonObject("response_format") { put("type", "json_object") }
        }

        val builder = Request.Builder()
            .url(endpoint(cfg.baseUrl))
            .post(payload.toString().toRequestBody(JSON_MEDIA))
            .header("Accept", "application/json")
        if (cfg.apiKey.isNotBlank()) builder.header("Authorization", "Bearer ${cfg.apiKey.trim()}")

        try {
            http.newCall(builder.build()).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                return resp.code to text
            }
        } catch (e: IOException) {
            throw LlmException(
                message = "网络请求失败：${e.message ?: e.javaClass.simpleName}",
                hint = "检查网络/代理，以及 Base URL 是否能访问",
            )
        }
    }

    private fun endpoint(baseUrl: String): String {
        val base = baseUrl.trim().trimEnd('/')
        return if (base.endsWith("/chat/completions")) base else "$base/chat/completions"
    }

    private fun parseCompletion(body: String, latency: Long): Completion {
        val root = runCatching { json.parseToJsonElement(body).jsonObject }
            .getOrElse { throw LlmException("响应不是 JSON", raw = body.take(600)) }

        val choice = root["choices"]?.jsonArray?.firstOrNull()?.jsonObject
            ?: throw LlmException("响应里没有 choices", raw = body.take(600))

        val message = choice["message"]?.jsonObject
        val content = message?.get("content")?.jsonPrimitive?.text().orEmpty()
        if (content.isBlank()) {
            val reasoning = message?.get("reasoning_content")?.jsonPrimitive?.text()
            if (!reasoning.isNullOrBlank()) {
                throw LlmException(
                    message = "模型只输出了思考内容，没有正文",
                    hint = "换用非推理模型（如 deepseek-chat），或降低“详细度”",
                    raw = body.take(600),
                )
            }
            throw LlmException("模型返回空内容", raw = body.take(600))
        }

        val usage = root["usage"]?.jsonObject
        return Completion(
            content = content,
            promptTokens = usage?.get("prompt_tokens")?.jsonPrimitive?.text()?.toIntOrNull() ?: 0,
            completionTokens = usage?.get("completion_tokens")?.jsonPrimitive?.text()?.toIntOrNull() ?: 0,
            finishReason = choice["finish_reason"]?.jsonPrimitive?.text().orEmpty(),
            model = root["model"]?.jsonPrimitive?.text().orEmpty(),
            latencyMs = latency,
        )
    }

    private fun httpError(code: Int, body: String): LlmException {
        val detail = runCatching {
            json.parseToJsonElement(body).jsonObject["error"]?.jsonObject
                ?.get("message")?.jsonPrimitive?.text()
        }.getOrNull().orEmpty()

        val (friendly, hint) = when (code) {
            401, 403 -> "鉴权失败（$code）" to "API Key 不正确，或该 Key 没有这个模型的权限"
            402 -> "余额不足（402）" to "去服务商控制台充值后重试"
            404 -> "接口或模型不存在（404）" to "检查 Base URL 是否带 /v1，以及模型名是否拼对"
            413 -> "请求过长（413）" to "把输入截短一些"
            429 -> "请求太频繁或额度用尽（429）" to "等一会儿再试，或换一个服务商"
            in 500..599 -> "服务端错误（$code）" to "服务商临时故障，稍后重试"
            else -> "请求失败（$code）" to null
        }
        return LlmException(
            message = if (detail.isBlank()) friendly else "$friendly：${detail.take(200)}",
            status = code,
            raw = body.take(800),
            hint = hint,
        )
    }

    private companion object {
        val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    }
}

private fun JsonPrimitive.text(): String? = if (this is JsonNull) null else content
