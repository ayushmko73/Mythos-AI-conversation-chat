package com.example.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles WHERE id = :id LIMIT 1")
    fun getProfileByIdFlow(id: String): Flow<ProfileEntity?>

    @Query("SELECT * FROM profiles WHERE id = :id LIMIT 1")
    suspend fun getProfileById(id: String): ProfileEntity?

    @Query("SELECT * FROM profiles ORDER BY lastLogin DESC")
    fun getAllProfilesFlow(): Flow<List<ProfileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity)

    @Query("UPDATE profiles SET creditsRemaining = creditsRemaining + :delta WHERE id = :id")
    suspend fun updateCredits(id: String, delta: Int)

    @Query("UPDATE profiles SET creditsRemaining = :newCredits WHERE id = :id")
    suspend fun setCredits(id: String, newCredits: Int)
}

@Dao
interface AgentDao {
    @Query("SELECT * FROM agents ORDER BY title ASC")
    fun getAllAgentsFlow(): Flow<List<AgentEntity>>

    @Query("SELECT * FROM agents WHERE isActive = 1 ORDER BY title ASC")
    fun getActiveAgentsFlow(): Flow<List<AgentEntity>>

    @Query("SELECT * FROM agents WHERE id = :id LIMIT 1")
    suspend fun getAgentById(id: String): AgentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAgent(agent: AgentEntity)

    @Query("UPDATE agents SET isActive = :isActive WHERE id = :id")
    suspend fun toggleAgentActive(id: String, isActive: Boolean)

    @Query("DELETE FROM agents WHERE id = :id")
    suspend fun deleteAgent(id: String)
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM user_agent_sessions WHERE userId = :userId AND agentId = :agentId LIMIT 1")
    suspend fun getSession(userId: String, agentId: String): UserAgentSessionEntity?

    @Query("SELECT * FROM user_agent_sessions WHERE id = :sessionId LIMIT 1")
    fun getSessionByIdFlow(sessionId: String): Flow<UserAgentSessionEntity?>

    @Query("SELECT * FROM user_agent_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: String): UserAgentSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: UserAgentSessionEntity)

    @Query("UPDATE user_agent_sessions SET messagesExchangedCount = messagesExchangedCount + 1, updatedAt = :updatedAt WHERE id = :sessionId")
    suspend fun incrementMessageCount(sessionId: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE user_agent_sessions SET narrativeSummary = :summary, updatedAt = :updatedAt WHERE id = :sessionId")
    suspend fun updateSummary(sessionId: String, summary: String, updatedAt: Long = System.currentTimeMillis())
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSessionFlow(sessionId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getMessagesForSession(sessionId: String): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesForSession(sessionId: String)
}

@Dao
interface CreditLogDao {
    @Query("SELECT * FROM credit_logs ORDER BY timestamp DESC")
    fun getAllLogsFlow(): Flow<List<CreditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: CreditLogEntity)
}
