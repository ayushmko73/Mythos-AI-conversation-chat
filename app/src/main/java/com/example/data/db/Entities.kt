package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: String,
    val email: String,
    val phone: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLogin: Long = System.currentTimeMillis(),
    val creditsRemaining: Int = 20,
    val totalSpent: Double = 0.0
)

@Entity(tableName = "agents")
data class AgentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val dpImageUrl: String,
    val systemPrompt: String,
    val responseLimit: Int = 1000,
    val userTextLimit: Int = 500,
    val isActive: Boolean = true
)

@Entity(tableName = "user_agent_sessions")
data class UserAgentSessionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val agentId: String,
    val messagesExchangedCount: Int = 0,
    val narrativeSummary: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val sender: String, // "user" or "agent"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val tokensUsed: Int = 0
)

@Entity(tableName = "credit_logs")
data class CreditLogEntity(
    @PrimaryKey val id: String,
    val profileId: String,
    val changeAmount: Int,
    val reason: String,
    val timestamp: Long = System.currentTimeMillis()
)
