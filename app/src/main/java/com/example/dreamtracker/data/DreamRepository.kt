package com.example.dreamtracker.data

import android.content.Context
import com.example.dreamtracker.network.ChatCompletionRequest
import com.example.dreamtracker.network.ChatMessage
import com.example.dreamtracker.network.OpenRouterService
import com.example.dreamtracker.secrets.OpenRouterKeyProvider
import com.example.dreamtracker.settings.SettingsRepository
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.buffer
import okio.source

class DreamRepository(private val context: Context) {
    private val database by lazy { AppDatabase.get(context) }
    private val dao by lazy { database.dreamDao() }

    private val moshi: Moshi = Moshi.Builder().build()
    private val settings by lazy { SettingsRepository(context) }

    data class SymbolMeaning(
        val symbol: String,
        val meaning: String
    )

    data class LlmAnalysis(
        val summary: String,
        val insights: List<String>,
        val recommendations: List<String>,
        val tone: String,
        val confidence: Float
    )

    suspend fun observeDreams() = dao.observeAll()

    suspend fun getDream(id: Long) = dao.getById(id)

    suspend fun saveDream(
        audioFilePath: String?,
        transcriptText: String,
        title: String = "",
        moodScore: Int = 3,
        tags: String = ""
    ): Long {
        val clippedTranscript = transcriptText.take(4000)
        val symbols = extractSymbols(clippedTranscript)
        val llm = runLlmAnalysis(clippedTranscript, symbols, title, moodScore)
        val dream = Dream(
            audioFilePath = audioFilePath,
            transcriptText = clippedTranscript,
            title = title.take(80),
            moodScore = moodScore.coerceIn(1, 5),
            summary = llm?.summary?.take(400) ?: "",
            symbolsMatched = symbols.joinToString(",").take(300),
            insights = llm?.insights?.joinToString("\n") { it.take(200) }?.take(800) ?: "",
            recommendations = llm?.recommendations?.joinToString("\n") { it.take(200) }?.take(400) ?: "",
            tone = llm?.tone?.take(40) ?: "",
            confidence = llm?.confidence ?: 0f,
            tags = tags.take(120),
            analysisJson = serializeJson(llm)
        )
        return dao.upsert(dream)
    }

    suspend fun updateDream(updated: Dream) {
        dao.update(updated.copy(
            title = updated.title.take(80),
            transcriptText = updated.transcriptText.take(4000),
            tags = updated.tags.take(120)
        ))
    }

    suspend fun toggleFavorite(id: Long) {
        val d = dao.getById(id) ?: return
        dao.update(d.copy(isFavorite = !d.isFavorite))
    }

    suspend fun deleteDream(id: Long) {
        dao.deleteById(id)
    }

    private fun serializeJson(llm: LlmAnalysis?): String =
        if (llm == null) "" else moshi.adapter(LlmAnalysis::class.java).toJson(llm)

    private suspend fun extractSymbols(text: String): List<String> = withContext(Dispatchers.IO) {
        val list = loadSymbols()
        val lowered = text.lowercase()
        list.filter { lowered.contains(it.symbol.lowercase()) }.map { it.symbol }
    }

    private suspend fun runLlmAnalysis(
        text: String,
        symbols: List<String>,
        title: String,
        moodScore: Int
    ): LlmAnalysis? = withContext(Dispatchers.IO) {
        var key = OpenRouterKeyProvider.readKey(context)
        val usingDemo = key == null
        if (usingDemo && settings.isDemoLimitReached()) {
            return@withContext null
        }
        if (key == null) key = "demo"

        val service = OpenRouterService.create(key)
        val systemPrompt = """
            Ты — бережный психолог и интерпретатор сновидений. Возвращай ОТВЕТ СТРОГО в JSON по схеме:
            {
              "summary": "краткое резюме (<= 400 символов)",
              "insights": ["до 3 наблюдений, каждое <= 160"],
              "recommendations": ["1-2 мягкие рекомендации, каждая <= 160"],
              "tone": "одно слово о тоне (например: спокойный, тревожный)",
              "confidence": 0.0..1.0
            }
            Никакого текста вне JSON. Никаких комментариев. Только JSON.
        """.trimIndent()
        val userPrompt = buildString {
            appendLine("Название: ${'$'}title")
            appendLine("Настроение (1..5): ${'$'}moodScore")
            appendLine("Текст сна:")
            appendLine(text)
            appendLine()
            appendLine("Символы:")
            appendLine(symbols.joinToString(", ").ifEmpty { "(нет)" })
        }
        val modelId = settings.getModelOrDefault(OpenRouterService.DEFAULT_MODEL)
        val req = ChatCompletionRequest(
            model = modelId,
            messages = listOf(
                ChatMessage(role = "system", content = systemPrompt),
                ChatMessage(role = "user", content = userPrompt)
            )
        )
        val resp = service.createCompletion(req)
        val content = resp.choices.firstOrNull()?.message?.content ?: return@withContext null

        if (usingDemo) settings.incrementDemoUse()
        parseLlmJson(content)
    }

    private fun parseLlmJson(json: String): LlmAnalysis? {
        return try {
            val adapter: JsonAdapter<LlmAnalysis> = moshi.adapter(LlmAnalysis::class.java)
            adapter.fromJson(json)
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun loadSymbols(): List<SymbolMeaning> = withContext(Dispatchers.IO) {
        val type = Types.newParameterizedType(List::class.java, SymbolMeaning::class.java)
        val adapter: JsonAdapter<List<SymbolMeaning>> = moshi.adapter(type)
        val assetManager = context.assets
        val input = assetManager.open("dream_symbols.json")
        input.source().buffer().use { buf ->
            val json = buf.readUtf8()
            adapter.fromJson(json) ?: emptyList()
        }
    }
}