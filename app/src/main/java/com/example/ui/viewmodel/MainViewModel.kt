package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.*
import com.example.data.remote.GeminiService
import com.example.data.repository.StoryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val geminiService = GeminiService()
    val repository = StoryRepository(db, geminiService)

    val profileState: StateFlow<ProfileEntity?> = repository.activeProfileFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allProfilesState: StateFlow<List<ProfileEntity>> = repository.allProfilesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeAgentsState: StateFlow<List<AgentEntity>> = repository.activeAgentsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAgentsState: StateFlow<List<AgentEntity>> = repository.allAgentsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val creditLogsState: StateFlow<List<CreditLogEntity>> = repository.creditLogsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedAgent = MutableStateFlow<AgentEntity?>(null)
    val selectedAgent: StateFlow<AgentEntity?> = _selectedAgent.asStateFlow()

    private val _activeSession = MutableStateFlow<UserAgentSessionEntity?>(null)
    val activeSession: StateFlow<UserAgentSessionEntity?> = _activeSession.asStateFlow()

    private val _selectedTab = MutableStateFlow(0) // 0: Companions, 1: Chat, 2: Memory, 3: Admin
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _isSendingMessage = MutableStateFlow(false)
    val isSendingMessage: StateFlow<Boolean> = _isSendingMessage.asStateFlow()

    private val _showTopUpDialog = MutableStateFlow(false)
    val showTopUpDialog: StateFlow<Boolean> = _showTopUpDialog.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isSummarizing = MutableStateFlow(false)
    val isSummarizing: StateFlow<Boolean> = _isSummarizing.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val chatMessagesState: StateFlow<List<ChatMessageEntity>> = _activeSession
        .flatMapLatest { session ->
            if (session != null) repository.getMessagesForSessionFlow(session.id)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentSessionFlowState: StateFlow<UserAgentSessionEntity?> = _activeSession
        .flatMapLatest { session ->
            if (session != null) repository.getSessionByIdFlow(session.id)
            else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        // Auto select first active agent when available if none selected
        viewModelScope.launch {
            activeAgentsState.collect { agents ->
                if (_selectedAgent.value == null && agents.isNotEmpty()) {
                    selectAgent(agents.first())
                }
            }
        }
    }

    fun setTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
    }

    fun openTopUpDialog() {
        _showTopUpDialog.value = true
    }

    fun dismissTopUpDialog() {
        _showTopUpDialog.value = false
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun selectAgent(agent: AgentEntity) {
        _selectedAgent.value = agent
        viewModelScope.launch {
            val session = repository.getOrCreateSession(repository.defaultUserId, agent.id)
            _activeSession.value = session
        }
    }

    fun selectAgentAndNavigateToChat(agent: AgentEntity) {
        selectAgent(agent)
        _selectedTab.value = 1 // Switch to Chat
    }

    fun sendMessage(userText: String) {
        val currentAgent = _selectedAgent.value ?: return
        if (userText.isBlank()) return

        val profile = profileState.value
        if (profile != null && profile.creditsRemaining <= 0) {
            _showTopUpDialog.value = true
            return
        }

        viewModelScope.launch {
            _isSendingMessage.value = true
            val result = repository.sendMessage(repository.defaultUserId, currentAgent.id, userText)
            _isSendingMessage.value = false

            if (result.isFailure) {
                val ex = result.exceptionOrNull()
                if (ex?.message == "OUT_OF_CREDITS") {
                    _showTopUpDialog.value = true
                } else {
                    _errorMessage.value = ex?.localizedMessage ?: "Failed to send message."
                }
            }
        }
    }

    fun claimDailyTopUp() {
        viewModelScope.launch {
            repository.topUpCredits(repository.defaultUserId, 15, "Daily Energy Claim (+15)")
            _showTopUpDialog.value = false
        }
    }

    fun purchaseCredits(amount: Int) {
        viewModelScope.launch {
            repository.topUpCredits(repository.defaultUserId, amount, "Energy Top-Up Pack (+$amount)")
            _showTopUpDialog.value = false
        }
    }

    fun adminAdjustCredits(userId: String, changeAmount: Int, reason: String) {
        viewModelScope.launch {
            repository.adminAdjustCredits(userId, changeAmount, reason)
        }
    }

    fun adminSaveAgent(agent: AgentEntity) {
        viewModelScope.launch {
            repository.adminSaveAgent(agent)
            if (_selectedAgent.value?.id == agent.id) {
                _selectedAgent.value = agent
            }
        }
    }

    fun adminToggleAgent(agentId: String, isActive: Boolean) {
        viewModelScope.launch {
            repository.adminToggleAgent(agentId, isActive)
        }
    }

    fun adminDeleteAgent(agentId: String) {
        viewModelScope.launch {
            repository.adminDeleteAgent(agentId)
            if (_selectedAgent.value?.id == agentId) {
                val remaining = activeAgentsState.value.filter { it.id != agentId }
                if (remaining.isNotEmpty()) {
                    selectAgent(remaining.first())
                }
            }
        }
    }

    fun adminCreateProfile(email: String, phone: String, initialCredits: Int) {
        viewModelScope.launch {
            repository.adminCreateProfile(email, phone, initialCredits)
        }
    }

    fun clearChatHistory() {
        val session = _activeSession.value ?: return
        viewModelScope.launch {
            repository.clearChatHistory(session.id)
        }
    }

    fun triggerManualSummarize() {
        val session = _activeSession.value ?: return
        viewModelScope.launch {
            _isSummarizing.value = true
            repository.triggerManualSummarize(session.id)
            _isSummarizing.value = false
        }
    }
}
