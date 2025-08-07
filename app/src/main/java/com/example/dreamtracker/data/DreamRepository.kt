package com.example.dreamtracker.data

import android.content.Context
import com.example.dreamtracker.network.ChatCompletionRequest
import com.example.dreamtracker.network.ChatMessage
import com.example.dreamtracker.network.OpenRouterService
import com.example.dreamtracker.secrets.OpenRouterKeyProvider
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

    data class SymbolMeaning(
        val symbol: String,
        val meaning: String
    )

    suspend fun observeDreams() = dao.observeAll()

    suspend fun getDream(id: Long) = dao.getById(id)

    suspend fun saveDream(
        audioFilePath: String?,
        transcriptText: String
    ): Long {
        val symbols = extractSymbols(transcriptText)
        val ruleBased = buildRuleBasedAnalysis(symbols)
        val llm = runLlmAnalysis(transcriptText, symbols)
        val combinedAnalysis = buildString {
            append("Правила: \n")
            append(ruleBased)
            append("\n\nНейросеть: \n")
            append(llm)
        }
        val dream = Dream(
            audioFilePath = audioFilePath,
            transcriptText = transcriptText,
            symbolsMatched = symbols.joinToString(","),
            analysisText = combinedAnalysis
        )
        return dao.upsert(dream)
    }

    private suspend fun extractSymbols(text: String): List<String> = withContext(Dispatchers.IO) {
        val list = loadSymbols()
        val lowered = text.lowercase()
        list.filter { lowered.contains(it.symbol.lowercase()) }.map { it.symbol }
    }

    private suspend fun buildRuleBasedAnalysis(symbols: List<String>): String = withContext(Dispatchers.Default) {
        if (symbols.isEmpty()) return@withContext "Ярких символов не найдено."
        val all = loadSymbols().associateBy { it.symbol.lowercase() }
        symbols.joinToString(separator = "\n") { s ->
            val m = all[s.lowercase()]?.meaning ?: ""
            "• $s: $m"
        }
    }

    private suspend fun runLlmAnalysis(text: String, symbols: List<String>): String = withContext(Dispatchers.IO) {
        val key = OpenRouterKeyProvider.readKey(context) ?: return@withContext "Ключ OpenRouter не найден. Добавьте openrouter_key.txt."
        val service = OpenRouterService.create(key)
        val systemPrompt = """
            Ты — внимательный интерпретатор сновидений. Анализируй текст сна кратко и бережно.
            Учитывай список символов, их возможные значения и эмоциональный тон.
            Дай 3-5 наблюдений и 1-2 мягких рекомендации для самопомощи.
        """.trimIndent()
        val userPrompt = buildString {
            appendLine("Текст сна:")
            appendLine(text)
            appendLine()
            appendLine("Символы:")
            appendLine(symbols.joinToString(", ").ifEmpty { "(нет)" })
        }
        val req = ChatCompletionRequest(
            model = OpenRouterService.DEFAULT_MODEL,
            messages = listOf(
                ChatMessage(role = "system", content = systemPrompt),
                ChatMessage(role = "user", content = userPrompt)
            )
        )
        val resp = service.createCompletion(req)
        resp.choices.firstOrNull()?.message?.content ?: "Не удалось получить ответ нейросети."
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