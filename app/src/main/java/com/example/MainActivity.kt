package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AppTopBar
import com.example.ui.components.CreditTopUpDialog
import com.example.ui.screens.AdminScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.CompanionsScreen
import com.example.ui.screens.MemoryScreen
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            StoryCompanionTheme {
                val profile by viewModel.profileState.collectAsStateWithLifecycle()
                val allProfiles by viewModel.allProfilesState.collectAsStateWithLifecycle()
                val activeAgents by viewModel.activeAgentsState.collectAsStateWithLifecycle()
                val allAgents by viewModel.allAgentsState.collectAsStateWithLifecycle()
                val creditLogs by viewModel.creditLogsState.collectAsStateWithLifecycle()
                val selectedAgent by viewModel.selectedAgent.collectAsStateWithLifecycle()
                val activeSession by viewModel.currentSessionFlowState.collectAsStateWithLifecycle()
                val messages by viewModel.chatMessagesState.collectAsStateWithLifecycle()
                val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
                val isSending by viewModel.isSendingMessage.collectAsStateWithLifecycle()
                val showTopUp by viewModel.showTopUpDialog.collectAsStateWithLifecycle()
                val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
                val isSummarizing by viewModel.isSummarizing.collectAsStateWithLifecycle()

                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(errorMessage) {
                    errorMessage?.let { msg ->
                        snackbarHostState.showSnackbar(msg)
                        viewModel.clearError()
                    }
                }

                Scaffold(
                    topBar = {
                        AppTopBar(
                            creditsRemaining = profile?.creditsRemaining ?: 0,
                            onEnergyClick = { viewModel.openTopUpDialog() },
                            selectedTab = selectedTab,
                            onTabSelected = { viewModel.setTab(it) }
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = DarkSurface,
                            contentColor = TextPrimary,
                            modifier = Modifier
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .testTag("main_bottom_nav")
                        ) {
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { viewModel.setTab(0) },
                                icon = { Icon(Icons.Default.SmartToy, contentDescription = "Companions") },
                                label = { Text("Companions", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = PrimaryViolet,
                                    selectedTextColor = PrimaryViolet,
                                    indicatorColor = PrimaryViolet.copy(alpha = 0.2f),
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag("nav_item_companions")
                            )

                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { viewModel.setTab(1) },
                                icon = { Icon(Icons.Default.Chat, contentDescription = "Active Chat") },
                                label = { Text("Story Chat", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = PrimaryViolet,
                                    selectedTextColor = PrimaryViolet,
                                    indicatorColor = PrimaryViolet.copy(alpha = 0.2f),
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag("nav_item_chat")
                            )

                            NavigationBarItem(
                                selected = selectedTab == 2,
                                onClick = { viewModel.setTab(2) },
                                icon = { Icon(Icons.Default.Psychology, contentDescription = "Story Memory") },
                                label = { Text("Memory", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = PrimaryViolet,
                                    selectedTextColor = PrimaryViolet,
                                    indicatorColor = PrimaryViolet.copy(alpha = 0.2f),
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag("nav_item_memory")
                            )

                            NavigationBarItem(
                                selected = selectedTab == 3,
                                onClick = { viewModel.setTab(3) },
                                icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin CMS") },
                                label = { Text("Admin CMS", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = PrimaryViolet,
                                    selectedTextColor = PrimaryViolet,
                                    indicatorColor = PrimaryViolet.copy(alpha = 0.2f),
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag("nav_item_admin")
                            )
                        }
                    },
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    containerColor = DarkBackground
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (selectedTab) {
                            0 -> CompanionsScreen(
                                agents = activeAgents,
                                selectedAgent = selectedAgent,
                                onSelectAgent = { viewModel.selectAgent(it) },
                                onStartChat = { viewModel.selectAgentAndNavigateToChat(it) }
                            )

                            1 -> ChatScreen(
                                agent = selectedAgent,
                                session = activeSession,
                                messages = messages,
                                creditsRemaining = profile?.creditsRemaining ?: 0,
                                isSending = isSending,
                                onSendMessage = { text -> viewModel.sendMessage(text) },
                                onOpenTopUp = { viewModel.openTopUpDialog() },
                                onClearHistory = { viewModel.clearChatHistory() },
                                onRefreshMemory = { viewModel.triggerManualSummarize() },
                                onViewMemory = { viewModel.setTab(2) }
                            )

                            2 -> MemoryScreen(
                                agent = selectedAgent,
                                session = activeSession,
                                isSummarizing = isSummarizing,
                                onTriggerSummarize = { viewModel.triggerManualSummarize() }
                            )

                            3 -> AdminScreen(
                                profiles = allProfiles,
                                agents = allAgents,
                                creditLogs = creditLogs,
                                onAdjustCredits = { userId, delta, reason ->
                                    viewModel.adminAdjustCredits(userId, delta, reason)
                                },
                                onSaveAgent = { agent -> viewModel.adminSaveAgent(agent) },
                                onToggleAgent = { agentId, isActive ->
                                    viewModel.adminToggleAgent(agentId, isActive)
                                },
                                onDeleteAgent = { agentId -> viewModel.adminDeleteAgent(agentId) },
                                onCreateProfile = { email, phone, initialCredits ->
                                    viewModel.adminCreateProfile(email, phone, initialCredits)
                                }
                            )
                        }

                        if (showTopUp) {
                            CreditTopUpDialog(
                                currentCredits = profile?.creditsRemaining ?: 0,
                                onDismiss = { viewModel.dismissTopUpDialog() },
                                onClaimDaily = { viewModel.claimDailyTopUp() },
                                onPurchasePack = { amount -> viewModel.purchaseCredits(amount) }
                            )
                        }
                    }
                }
            }
        }
    }
}
