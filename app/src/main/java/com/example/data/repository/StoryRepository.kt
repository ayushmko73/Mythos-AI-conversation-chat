package com.example.data.repository

import com.example.data.db.*
import com.example.data.remote.GeminiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

class StoryRepository(
    private val db: AppDatabase,
    private val geminiService: GeminiService
) {
    val defaultUserId = "user_default"

    val activeProfileFlow: Flow<ProfileEntity?> = db.profileDao().getProfileByIdFlow(defaultUserId)
    val allProfilesFlow: Flow<List<ProfileEntity>> = db.profileDao().getAllProfilesFlow()
    val activeAgentsFlow: Flow<List<AgentEntity>> = db.agentDao().getActiveAgentsFlow()
    val allAgentsFlow: Flow<List<AgentEntity>> = db.agentDao().getAllAgentsFlow()
    val creditLogsFlow: Flow<List<CreditLogEntity>> = db.creditLogDao().getAllLogsFlow()

    fun getMessagesForSessionFlow(sessionId: String): Flow<List<ChatMessageEntity>> {
        return db.chatMessageDao().getMessagesForSessionFlow(sessionId)
    }

    fun getSessionByIdFlow(sessionId: String): Flow<UserAgentSessionEntity?> {
        return db.sessionDao().getSessionByIdFlow(sessionId)
    }

    suspend fun getOrCreateSession(userId: String = defaultUserId, agentId: String): UserAgentSessionEntity {
        return withContext(Dispatchers.IO) {
            val existing = db.sessionDao().getSession(userId, agentId)
            if (existing != null) {
                existing
            } else {
                val newSession = UserAgentSessionEntity(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    agentId = agentId,
                    messagesExchangedCount = 0,
                    narrativeSummary = "",
                    updatedAt = System.currentTimeMillis()
                )
                db.sessionDao().insertSession(newSession)
                newSession
            }
        }
    }

    suspend fun sendMessage(
        userId: String = defaultUserId,
        agentId: String,
        userText: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val profile = db.profileDao().getProfileById(userId)
            ?: return@withContext Result.failure(Exception("User profile not found."))

        if (profile.creditsRemaining <= 0) {
            return@withContext Result.failure(Exception("OUT_OF_CREDITS"))
        }

        val agent = db.agentDao().getAgentById(agentId)
            ?: return@withContext Result.failure(Exception("Agent persona not found."))

        // 1. Deduct 1 credit
        db.profileDao().updateCredits(userId, -1)
        db.creditLogDao().insertLog(
            CreditLogEntity(
                id = UUID.randomUUID().toString(),
                profileId = userId,
                changeAmount = -1,
                reason = "Story Message with ${agent.title.take(20)}"
            )
        )

        // 2. Get/Create Session
        val session = getOrCreateSession(userId, agentId)

        // 3. Insert User Message
        val userMsg = ChatMessageEntity(
            id = UUID.randomUUID().toString(),
            sessionId = session.id,
            sender = "user",
            text = userText,
            timestamp = System.currentTimeMillis()
        )
        db.chatMessageDao().insertMessage(userMsg)

        // 4. Update session message counter
        db.sessionDao().incrementMessageCount(session.id)
        val updatedSession = db.sessionDao().getSessionById(session.id) ?: session

        // 5. Query existing message history for Gemini context
        val history = db.chatMessageDao().getMessagesForSession(session.id)

        // 6. Generate AI Persona Response
        val agentResponseText = geminiService.generateStoryResponse(
            systemPrompt = agent.systemPrompt,
            narrativeSummary = updatedSession.narrativeSummary,
            history = history.dropLast(1), // history before user's latest message, or all history
            userMessageText = userText
        )

        val agentMsg = ChatMessageEntity(
            id = UUID.randomUUID().toString(),
            sessionId = session.id,
            sender = "agent",
            text = agentResponseText,
            timestamp = System.currentTimeMillis()
        )
        db.chatMessageDao().insertMessage(agentMsg)

        // 7. Contextual Memory / Summarization Layer check
        // Every 12 messages exchanged (or if message count is a multiple of 12)
        if (updatedSession.messagesExchangedCount > 0 && updatedSession.messagesExchangedCount % 12 == 0) {
            val allHistory = db.chatMessageDao().getMessagesForSession(session.id)
            val newSummary = geminiService.generateContextualSummary(allHistory, updatedSession.narrativeSummary)
            db.sessionDao().updateSummary(session.id, newSummary)
        }

        Result.success(Unit)
    }

    suspend fun topUpCredits(userId: String = defaultUserId, amount: Int, reason: String) {
        withContext(Dispatchers.IO) {
            db.profileDao().updateCredits(userId, amount)
            db.creditLogDao().insertLog(
                CreditLogEntity(
                    id = UUID.randomUUID().toString(),
                    profileId = userId,
                    changeAmount = amount,
                    reason = reason
                )
            )
        }
    }

    suspend fun adminAdjustCredits(targetUserId: String, changeAmount: Int, reason: String) {
        withContext(Dispatchers.IO) {
            db.profileDao().updateCredits(targetUserId, changeAmount)
            db.creditLogDao().insertLog(
                CreditLogEntity(
                    id = UUID.randomUUID().toString(),
                    profileId = targetUserId,
                    changeAmount = changeAmount,
                    reason = "Admin Adjustment ($reason)"
                )
            )
        }
    }

    suspend fun adminSaveAgent(agent: AgentEntity) {
        withContext(Dispatchers.IO) {
            db.agentDao().insertAgent(agent)
        }
    }

    suspend fun adminToggleAgent(agentId: String, isActive: Boolean) {
        withContext(Dispatchers.IO) {
            db.agentDao().toggleAgentActive(agentId, isActive)
        }
    }

    suspend fun adminDeleteAgent(agentId: String) {
        withContext(Dispatchers.IO) {
            db.agentDao().deleteAgent(agentId)
        }
    }

    suspend fun adminCreateProfile(email: String, phone: String, initialCredits: Int) {
        withContext(Dispatchers.IO) {
            val newId = "user_${UUID.randomUUID().toString().take(8)}"
            db.profileDao().insertProfile(
                ProfileEntity(
                    id = newId,
                    email = email,
                    phone = phone,
                    createdAt = System.currentTimeMillis(),
                    lastLogin = System.currentTimeMillis(),
                    creditsRemaining = initialCredits,
                    totalSpent = 0.0
                )
            )
            db.creditLogDao().insertLog(
                CreditLogEntity(
                    id = UUID.randomUUID().toString(),
                    profileId = newId,
                    changeAmount = initialCredits,
                    reason = "Admin Created Account Grant"
                )
            )
        }
    }

    suspend fun clearChatHistory(sessionId: String) {
        withContext(Dispatchers.IO) {
            db.chatMessageDao().deleteMessagesForSession(sessionId)
            val session = db.sessionDao().getSessionById(sessionId)
            if (session != null) {
                db.sessionDao().updateSummary(sessionId, "")
            }
        }
    }

    suspend fun triggerManualSummarize(sessionId: String) {
        withContext(Dispatchers.IO) {
            val session = db.sessionDao().getSessionById(sessionId) ?: return@withContext
            val history = db.chatMessageDao().getMessagesForSession(sessionId)
            val newSummary = geminiService.generateContextualSummary(history, session.narrativeSummary)
            db.sessionDao().updateSummary(sessionId, newSummary)
        }
    }
}
