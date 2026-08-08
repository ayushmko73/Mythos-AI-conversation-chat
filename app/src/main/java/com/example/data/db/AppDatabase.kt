package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

@Database(
    entities = [
        ProfileEntity::class,
        AgentEntity::class,
        UserAgentSessionEntity::class,
        ChatMessageEntity::class,
        CreditLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun agentDao(): AgentDao
    abstract fun sessionDao(): SessionDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun creditLogDao(): CreditLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "story_companion_db"
                )
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateDatabase(database)
                    }
                }
            }

            private suspend fun populateDatabase(db: AppDatabase) {
                val defaultUserId = "user_default"
                db.profileDao().insertProfile(
                    ProfileEntity(
                        id = defaultUserId,
                        email = "explorer@storycompanion.ai",
                        phone = "+1 (555) 019-2834",
                        createdAt = System.currentTimeMillis(),
                        lastLogin = System.currentTimeMillis(),
                        creditsRemaining = 20,
                        totalSpent = 0.0
                    )
                )

                db.creditLogDao().insertLog(
                    CreditLogEntity(
                        id = UUID.randomUUID().toString(),
                        profileId = defaultUserId,
                        changeAmount = 20,
                        reason = "Welcome Gift Credits"
                    )
                )

                db.profileDao().insertProfile(
                    ProfileEntity(
                        id = "user_demo_2",
                        email = "cyber.voyager@orbit.net",
                        phone = "+1 (555) 882-9102",
                        createdAt = System.currentTimeMillis() - 86400000L * 3,
                        lastLogin = System.currentTimeMillis() - 3600000L * 2,
                        creditsRemaining = 12,
                        totalSpent = 15.00
                    )
                )

                val defaultAgents = listOf(
                    AgentEntity(
                        id = "agent_aria",
                        title = "Aria - Cyberpunk Netrunner",
                        description = "A sharp, witty hacker from Neo-Tokyo 2099 who navigates dark markets, AI constructs, and digital heist operations.",
                        dpImageUrl = "img_aria_1786182368013",
                        systemPrompt = "You are Aria, an elite netrunner companion in a cyberpunk world. Speak with crisp, edgy hacker slang, tech jargon, and witty banter. Stay in character as an immersive interactive storyteller. Describe visual scenes vividly with neon aesthetic details.",
                        responseLimit = 1000,
                        userTextLimit = 500,
                        isActive = true
                    ),
                    AgentEntity(
                        id = "agent_eldrin",
                        title = "Eldrin - High Archmage",
                        description = "A centuries-old arcane scholar guarding forgotten spellbooks in the Spire of Aethelgard. Speaks with grand, mysterious wisdom.",
                        dpImageUrl = "img_eldrin_1786182380888",
                        systemPrompt = "You are Archmage Eldrin, a legendary fantasy scholar and spellcaster. Speak poetically, solemnly, and with deep magical lore. Guide the user through arcane mysteries, ancient prophecy, and epic fantasy choices.",
                        responseLimit = 1000,
                        userTextLimit = 500,
                        isActive = true
                    ),
                    AgentEntity(
                        id = "agent_maya",
                        title = "Maya - Cozy Daily Companion",
                        description = "A warm, comforting storyteller who offers gentle conversations, daily advice, creative prompts, and soothing hearthside tales.",
                        dpImageUrl = "img_maya_1786182395712",
                        systemPrompt = "You are Maya, a warm, supportive, and cozy companion and creative storyteller. Speak with empathy, gentle humor, and comforting warmth. Ask friendly questions and write delightful hearthside stories.",
                        responseLimit = 1000,
                        userTextLimit = 500,
                        isActive = true
                    ),
                    AgentEntity(
                        id = "agent_vance",
                        title = "Captain Vance - Sci-Fi Commander",
                        description = "Commander of the starship Meridian, exploring uncharted deep-space anomalies, planetary landings, and alien diplomacy.",
                        dpImageUrl = "img_vance_1786182408484",
                        systemPrompt = "You are Captain Vance of the Starship Meridian. Speak as a confident, strategic sci-fi military commander. Detail tactical decisions, bridge alerts, planetary exploration, and sci-fi narrative choices.",
                        responseLimit = 1000,
                        userTextLimit = 500,
                        isActive = true
                    )
                )

                defaultAgents.forEach { db.agentDao().insertAgent(it) }
            }
        }
    }
}
