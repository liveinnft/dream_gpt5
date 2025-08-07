package com.example.dreamtracker.network

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface OpenRouterService {
    @Headers("Content-Type: application/json")
    @POST("api/v1/chat/completions")
    suspend fun createCompletion(@Body request: ChatCompletionRequest): ChatCompletionResponse

    companion object {
        const val BASE_URL = "https://openrouter.ai/"
        const val DEFAULT_MODEL = "openai/gpt-4o-mini"

        fun create(apiKey: String): OpenRouterService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            val authInterceptor = Interceptor { chain ->
                val req = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("HTTP-Referer", "https://example.com")
                    .addHeader("X-Title", "DreamTracker")
                    .build()
                chain.proceed(req)
            }
            val client = OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(logging)
                .build()

            val moshi = Moshi.Builder().build()
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .client(client)
                .build()
            return retrofit.create(OpenRouterService::class.java)
        }
    }
}

data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>
)

data class ChatMessage(
    val role: String,
    val content: String
)

data class ChatCompletionResponse(
    val id: String?,
    val choices: List<Choice>
)

data class Choice(
    val index: Int,
    val message: ChatMessage,
    @Json(name = "finish_reason") val finishReason: String?
)