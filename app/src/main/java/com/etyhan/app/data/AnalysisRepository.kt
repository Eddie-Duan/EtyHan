package com.etyhan.app.data

import com.etyhan.app.ai.AnalysisParser
import com.etyhan.app.ai.LlmClient
import com.etyhan.app.ai.LlmConfig
import com.etyhan.app.ai.LlmException
import com.etyhan.app.ai.ParseResult
import com.etyhan.app.ai.PromptBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

data class StoredAnalysis(
    val id: Long,
    val query: String,
    val analysis: Analysis,
    val provider: String,
    val model: String,
    val tokensIn: Int,
    val tokensOut: Int,
    val elapsedMs: Long,
    val favorite: Boolean,
    val rawJson: String = "",
    val createdAt: Long,
    val updatedAt: Long,
)

sealed interface AnalyzeOutcome {
    data class Success(val id: Long, val fromCache: Boolean) : AnalyzeOutcome
    data class Failure(
        val message: String,
        val hint: String? = null,
        val raw: String? = null,
    ) : AnalyzeOutcome
}

class AnalysisRepository(
    private val dao: AnalysisDao,
    private val settings: SettingsRepository,
    private val secrets: SecretStore,
    private val client: LlmClient,
    private val json: Json,
) {

    fun observeRecent(limit: Int = 20): Flow<List<StoredAnalysis>> =
        dao.observeRecent(limit).mapList()

    fun observeAll(): Flow<List<StoredAnalysis>> = dao.observeAll().mapList()

    fun observeFavorites(): Flow<List<StoredAnalysis>> = dao.observeFavorites().mapList()

    fun search(q: String): Flow<List<StoredAnalysis>> = dao.search(q).mapList()

    suspend fun find(id: Long): StoredAnalysis? = dao.findById(id)?.toStored()

    suspend fun find(query: String): StoredAnalysis? = dao.findByNorm(norm(query))?.toStored()

    suspend fun setFavorite(id: Long, favorite: Boolean) = dao.setFavorite(id, favorite)

    suspend fun delete(id: Long) = dao.delete(id)

    suspend fun clearAll() = dao.clear()

    suspend fun count(): Int = dao.count()

    suspend fun config(): Pair<AppSettings, LlmConfig> {
        val s = settings.flow.first()
        val key = secrets.get(s.providerId)
        return s to LlmConfig(
            baseUrl = s.baseUrl,
            apiKey = key,
            model = s.model,
            jsonMode = s.jsonMode,
            maxTokens = s.maxTokens,
            temperature = s.temperature,
        )
    }

    fun promptPreview(settings: AppSettings): String =
        PromptBuilder.system(settings.extraInstruction) + "\n\n---\n\n" +
            PromptBuilder.user("학생들은 학교에 갑니다.", settings.depth, settings.showRomanization)

    /**
     * 分析主流程。force=false 且命中缓存时直接返回缓存 id（同一句话不重复烧 token）。
     */
    suspend fun analyze(query: String, force: Boolean): AnalyzeOutcome {
        val q = query.trim()
        if (q.isEmpty()) return AnalyzeOutcome.Failure("请输入韩语单词或句子")
        val normalized = norm(q)

        if (!force) {
            dao.findByNorm(normalized)?.let { return AnalyzeOutcome.Success(it.id, fromCache = true) }
        }

        val (s, cfg) = config()
        if (cfg.baseUrl.isBlank() || cfg.model.isBlank()) {
            return AnalyzeOutcome.Failure("还没有配置模型服务", hint = "去「设置」选一个服务并填入 API Key")
        }
        if (s.providerId != "ollama" && s.providerId != "custom" && cfg.apiKey.isBlank()) {
            return AnalyzeOutcome.Failure("缺少 API Key", hint = "在「设置」里填入 ${Providers.byId(s.providerId).name} 的 Key")
        }

        val started = System.currentTimeMillis()
        val completion = try {
            client.complete(
                cfg = cfg,
                system = PromptBuilder.system(s.extraInstruction),
                user = PromptBuilder.user(q, s.depth, s.showRomanization),
            )
        } catch (e: LlmException) {
            return AnalyzeOutcome.Failure(e.message, e.hint, e.raw)
        } catch (e: Exception) {
            return AnalyzeOutcome.Failure(e.message ?: e.javaClass.simpleName)
        }

        val parsed = when (val r = AnalysisParser.parse(completion.content)) {
            is ParseResult.Ok -> r.analysis
            is ParseResult.Fail -> return AnalyzeOutcome.Failure(
                message = r.message,
                hint = "模型没按 JSON 输出。可直接重试；反复出现就换模型或降低“详细度”",
                raw = completion.content,
            )
        }

        val elapsed = System.currentTimeMillis() - started
        val now = System.currentTimeMillis()
        val previous = dao.findByNorm(normalized)
        val entity = AnalysisEntity(
            id = previous?.id ?: 0,
            query = q,
            queryNorm = normalized,
            payload = json.encodeToString(Analysis.serializer(), parsed),
            provider = s.providerId,
            model = completion.model.ifBlank { s.model },
            tokensIn = completion.promptTokens,
            tokensOut = completion.completionTokens,
            elapsedMs = elapsed,
            favorite = previous?.favorite ?: false,
            hits = (previous?.hits ?: 0) + 1,
            createdAt = previous?.createdAt ?: now,
            updatedAt = now,
        )
        dao.insert(entity)
        val savedId = dao.findByNorm(normalized)?.id ?: previous?.id ?: 0
        return AnalyzeOutcome.Success(savedId, fromCache = false)
    }

    suspend fun testConnection(): String {
        val (_, cfg) = config()
        if (cfg.baseUrl.isBlank()) throw LlmException("先填 Base URL")
        return client.ping(cfg)
    }

    fun norm(q: String): String = q.trim().replace(WS, " ").lowercase()

    private fun Flow<List<AnalysisEntity>>.mapList(): Flow<List<StoredAnalysis>> =
        map { list -> list.mapNotNull { it.toStored() } }

    private fun AnalysisEntity.toStored(): StoredAnalysis? {
        val analysis = runCatching {
            json.decodeFromString(Analysis.serializer(), payload)
        }.getOrNull() ?: return null
        return StoredAnalysis(
            id = id,
            query = query,
            analysis = analysis,
            provider = provider,
            model = model,
            tokensIn = tokensIn,
            tokensOut = tokensOut,
            elapsedMs = elapsedMs,
            favorite = favorite,
            rawJson = payload,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }

    private companion object {
        val WS = Regex("\\s+")
    }
}
