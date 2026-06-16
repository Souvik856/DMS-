package com.example.ui.api

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- Models for Gemini API ---

@Serializable
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@Serializable
data class Content(
    val parts: List<Part>,
    val role: String? = null
)

@Serializable
data class Part(
    val text: String? = null
)

@Serializable
data class GenerationConfig(
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null,
    val maxOutputTokens: Int? = null
)

@Serializable
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

@Serializable
data class Candidate(
    val content: Content
)

// --- Retrofit & API Client setup ---

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        val json = Json { ignoreUnknownKeys = true }
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        retrofit.create(GeminiApiService::class.java)
    }
}

class GeminiManager {
    companion object {
        private const val TAG = "GeminiManager"
        private const val SYSTEM_PROMPT = """
            Your name is DMS Co-Producer AI, an elite professional sound design & production co-pilot for DEB MUSIC STUDIO.
            You help engineers and artists design high-fidelity soundscapes, write hit lyrics, and tune modular synthesizers.
            
            Whenever you suggest or generate an audio style, soundscape, or production patch, ALWAYS output a matching patch setup on a single line at the exact end of your response in this precise format:
            [PRESET: Reverb=X, Echo=Y, Pitch=Z, Speed=W, Gain=K]
            where X, Y, Z, W, and K are decimal numbers between 0.0 and 1.0 representing slider values.
            
            E.g., if suggesting a dreamy neon pad:
            "...This creates a lush, spaced sound.
            [PRESET: Reverb=0.85, Echo=0.60, Pitch=0.45, Speed=0.50, Gain=0.75]"
            
            Always tailor the sound configuration accurately to matches the aesthetic style described.
        """

        suspend fun askCoProducer(prompt: String, chatHistory: List<Content> = emptyList()): String = withContext(Dispatchers.IO) {
            val key = BuildConfig.GEMINI_API_KEY
            if (key.isBlank() || key == "GEMINI_API_KEY_DEFAULT_VALUE") {
                Log.w(TAG, "Gemini API key is empty or default")
                return@withContext "Error: Gemini API Key is missing. Please configure it in your AI Studio Secrets panel."
            }

            val fullHistory = chatHistory + Content(parts = listOf(Part(text = prompt)), role = "user")
            val request = GenerateContentRequest(
                contents = fullHistory,
                generationConfig = GenerationConfig(temperature = 0.7f),
                systemInstruction = Content(parts = listOf(Part(text = SYSTEM_PROMPT)))
            )

            try {
                val response = RetrofitClient.service.generateContent(key, request)
                val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                text ?: "I am online, but received empty synthesis output. Try phrasing your prompt differently."
            } catch (e: Exception) {
                Log.e(TAG, "Error generating content: ${e.message}", e)
                "Error communicating with AI Synthesis Core: ${e.localizedMessage ?: e.message}. Please verify internet access and your API key."
            }
        }
    }
}
