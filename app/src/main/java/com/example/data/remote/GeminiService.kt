package com.example.data.remote

import com.example.BuildConfig
import com.example.data.db.ChatMessageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun generateStoryResponse(
        systemPrompt: String,
        narrativeSummary: String,
        history: List<ChatMessageEntity>,
        userMessageText: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "*(System Alert: Gemini API Key is not set in Secrets. Please add your GEMINI_API_KEY in the AI Studio Secrets panel. Meanwhile, the persona smiles warmly.)*\n\n\"Welcome traveler! I am in offline narrative preview mode. Configure your API key to unlock real-time Gemini story intelligence!\""
        }

        try {
            val requestJson = JSONObject()

            // System Instruction (Persona + Narrative Memory Context)
            val fullSystemText = StringBuilder().apply {
                append(systemPrompt.trim())
                if (narrativeSummary.isNotBlank()) {
                    append("\n\n--- NARRATIVE MEMORY & CONTEXT ---\n")
                    append("Key Plot Points & Emotional State:\n")
                    append(narrativeSummary)
                    append("\n--- END MEMORY ---\n")
                }
                append("\nMaintain long-term story continuity and respond immersively in character.")
            }.toString()

            val systemPart = JSONObject().put("text", fullSystemText)
            val systemInstructionObj = JSONObject().put("parts", JSONArray().put(systemPart))
            requestJson.put("systemInstruction", systemInstructionObj)

            // Conversation Contents Array
            val contentsArray = JSONArray()

            // Include last 10 messages for immediate dialogue flow
            val recentHistory = history.takeLast(10)
            for (msg in recentHistory) {
                val contentObj = JSONObject()
                val role = if (msg.sender.lowercase() == "user") "user" else "model"
                contentObj.put("role", role)
                val partsArray = JSONArray().put(JSONObject().put("text", msg.text))
                contentObj.put("parts", partsArray)
                contentsArray.put(contentObj)
            }

            // Append current user message
            val currentMsgObj = JSONObject()
            currentMsgObj.put("role", "user")
            currentMsgObj.put("parts", JSONArray().put(JSONObject().put("text", userMessageText)))
            contentsArray.put(currentMsgObj)

            requestJson.put("contents", contentsArray)

            // Generation config
            val genConfig = JSONObject()
                .put("temperature", 0.85)
                .put("topP", 0.95)
            requestJson.put("generationConfig", genConfig)

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBodyString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext "*(Narrative Pause: Unable to reach Gemini API. Error code: ${response.code})*"
            }

            val responseJson = JSONObject(responseBodyString)
            val candidates = responseJson.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val candidate = candidates.getJSONObject(0)
                val content = candidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    val text = parts.getJSONObject(0).optString("text", "")
                    if (text.isNotBlank()) {
                        return@withContext text
                    }
                }
            }

            "*(The persona pauses thoughtfully, lost in reflection...)*"
        } catch (e: Exception) {
            e.printStackTrace()
            "*(Narrative Error: ${e.localizedMessage ?: "Failed to generate story response."})*"
        }
    }

    suspend fun generateContextualSummary(
        history: List<ChatMessageEntity>,
        existingSummary: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext if (existingSummary.isNotBlank()) existingSummary else "• Early relationship established between User and Persona.\n• Adventuring together in the active story realm.\n• 20 energy tokens granted upon journey start."
        }

        try {
            val conversationText = StringBuilder()
            history.takeLast(16).forEach {
                conversationText.append("${if (it.sender == "user") "User" else "Persona"}: ${it.text}\n")
            }

            val prompt = """
                You are a story continuity and memory tracking AI.
                Analyze the following recent conversation between the User and Persona:
                
                --- CONVERSATION HISTORY ---
                $conversationText
                --- END HISTORY ---
                
                Existing Summary Context:
                ${existingSummary.ifBlank { "None" }}
                
                Task: Update and summarize the key plot developments, emotional connection, user preferences, and important narrative milestones into 3-4 bullet points. Keep it concise, focused, and present-tense.
            """.trimIndent()

            val requestJson = JSONObject()
            val contentObj = JSONObject()
                .put("role", "user")
                .put("parts", JSONArray().put(JSONObject().put("text", prompt)))
            requestJson.put("contents", JSONArray().put(contentObj))

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBodyString = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val responseJson = JSONObject(responseBodyString)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val text = candidates.getJSONObject(0)
                        .optJSONObject("content")
                        ?.optJSONArray("parts")
                        ?.getJSONObject(0)
                        ?.optString("text")
                    if (!text.isNullOrBlank()) {
                        return@withContext text.trim()
                    }
                }
            }
            existingSummary.ifBlank { "• Story unfolding with active persona exchanges.\n• Deepening dialogue and companion rapport." }
        } catch (e: Exception) {
            e.printStackTrace()
            existingSummary.ifBlank { "• Ongoing interactive narrative thread." }
        }
    }
}
