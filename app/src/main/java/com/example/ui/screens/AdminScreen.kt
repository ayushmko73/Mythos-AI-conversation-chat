package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.AgentEntity
import com.example.data.db.CreditLogEntity
import com.example.data.db.ProfileEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    profiles: List<ProfileEntity>,
    agents: List<AgentEntity>,
    creditLogs: List<CreditLogEntity>,
    onAdjustCredits: (userId: String, changeAmount: Int, reason: String) -> Unit,
    onSaveAgent: (AgentEntity) -> Unit,
    onToggleAgent: (agentId: String, isActive: Boolean) -> Unit,
    onDeleteAgent: (agentId: String) -> Unit,
    onCreateProfile: (email: String, phone: String, initialCredits: Int) -> Unit
) {
    var adminSubTab by remember { mutableIntStateOf(0) } // 0: User Management, 1: Agent CMS, 2: Credit Logs
    var searchQuery by remember { mutableStateOf("") }

    // Dialog States for Agent Editing
    var showAgentDialog by remember { mutableStateOf(false) }
    var editingAgent by remember { mutableStateOf<AgentEntity?>(null) }

    // Form fields for agent
    var agentTitle by remember { mutableStateOf("") }
    var agentDesc by remember { mutableStateOf("") }
    var agentPrompt by remember { mutableStateOf("") }
    var agentImg by remember { mutableStateOf("img_aria_1786182368013") }
    var agentRespLimit by remember { mutableStateOf("1000") }
    var agentUserLimit by remember { mutableStateOf("500") }
    var agentIsActive by remember { mutableStateOf(true) }

    // Dialog state for adding user profile
    var showAddUserDialog by remember { mutableStateOf(false) }
    var newUserEmail by remember { mutableStateOf("") }
    var newUserPhone by remember { mutableStateOf("") }
    var newUserCredits by remember { mutableStateOf("20") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Admin Header Banner
        Surface(
            color = AdminCardBg,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        tint = PrimaryViolet,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "System Admin Dashboard",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Manage user energy profiles, credit adjustments, and AI companion CMS personas.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Navigation Tabs within Admin Panel
                TabRow(
                    selectedTabIndex = adminSubTab,
                    containerColor = DarkSurfaceVariant,
                    contentColor = PrimaryViolet,
                    divider = { HorizontalDivider(color = DarkSurfaceHighlight) }
                ) {
                    Tab(
                        selected = adminSubTab == 0,
                        onClick = { adminSubTab = 0 },
                        text = { Text("Users & Credits", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("admin_tab_users")
                    )
                    Tab(
                        selected = adminSubTab == 1,
                        onClick = { adminSubTab = 1 },
                        text = { Text("Agent CMS (${agents.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("admin_tab_agents")
                    )
                    Tab(
                        selected = adminSubTab == 2,
                        onClick = { adminSubTab = 2 },
                        text = { Text("Credit Audit Logs", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("admin_tab_logs")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Sub tab contents
        when (adminSubTab) {
            0 -> {
                // USER MANAGEMENT TABLE
                val filteredProfiles = remember(profiles, searchQuery) {
                    if (searchQuery.isBlank()) profiles
                    else profiles.filter {
                        it.email.contains(searchQuery, ignoreCase = true) ||
                                it.phone.contains(searchQuery, ignoreCase = true) ||
                                it.id.contains(searchQuery, ignoreCase = true)
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search by email or phone...", color = TextMuted) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("admin_user_search"),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = DarkSurface,
                                    unfocusedContainerColor = DarkSurface,
                                    focusedBorderColor = PrimaryViolet,
                                    unfocusedBorderColor = DarkSurfaceHighlight,
                                    focusedTextColor = TextPrimary
                                )
                            )

                            Button(
                                onClick = { showAddUserDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("admin_add_user_btn")
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add User", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    items(filteredProfiles, key = { it.id }) { user ->
                        UserProfileCard(
                            user = user,
                            onAdjust = { delta ->
                                onAdjustCredits(
                                    user.id,
                                    delta,
                                    if (delta > 0) "Admin Increase +$delta" else "Admin Decrease $delta"
                                )
                            }
                        )
                    }
                }
            }

            1 -> {
                // AGENT CREATOR & CMS PANEL
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Button(
                            onClick = {
                                editingAgent = null
                                agentTitle = ""
                                agentDesc = ""
                                agentPrompt = ""
                                agentImg = "img_aria_1786182368013"
                                agentRespLimit = "1000"
                                agentUserLimit = "500"
                                agentIsActive = true
                                showAgentDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("admin_create_agent_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Create New AI Storyteller Persona", fontWeight = FontWeight.Bold)
                        }
                    }

                    items(agents, key = { it.id }) { agent ->
                        AdminAgentCard(
                            agent = agent,
                            onEdit = {
                                editingAgent = agent
                                agentTitle = agent.title
                                agentDesc = agent.description
                                agentPrompt = agent.systemPrompt
                                agentImg = agent.dpImageUrl
                                agentRespLimit = agent.responseLimit.toString()
                                agentUserLimit = agent.userTextLimit.toString()
                                agentIsActive = agent.isActive
                                showAgentDialog = true
                            },
                            onToggle = { isActive -> onToggleAgent(agent.id, isActive) },
                            onDelete = { onDeleteAgent(agent.id) }
                        )
                    }
                }
            }

            2 -> {
                // CREDIT AUDIT LOGS
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(creditLogs, key = { it.id }) { log ->
                        CreditLogCard(log = log)
                    }
                }
            }
        }
    }

    // Modal Dialog: Create / Edit Agent
    if (showAgentDialog) {
        AlertDialog(
            onDismissRequest = { showAgentDialog = false },
            title = {
                Text(
                    if (editingAgent == null) "Create AI Persona" else "Edit AI Persona",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = agentTitle,
                        onValueChange = { agentTitle = it },
                        label = { Text("Title / Name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_agent_title_input"),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = agentDesc,
                        onValueChange = { agentDesc = it },
                        label = { Text("Short Description") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_agent_desc_input"),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = agentPrompt,
                        onValueChange = { agentPrompt = it },
                        label = { Text("System Prompt (Persona Behavior)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .testTag("admin_agent_prompt_input"),
                        shape = RoundedCornerShape(10.dp),
                        maxLines = 5
                    )
                    OutlinedTextField(
                        value = agentImg,
                        onValueChange = { agentImg = it },
                        label = { Text("Drawable Image Asset Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = agentRespLimit,
                            onValueChange = { agentRespLimit = it },
                            label = { Text("Max Token Limit") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = agentUserLimit,
                            onValueChange = { agentUserLimit = it },
                            label = { Text("User Char Limit") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Active Persona Status", color = TextPrimary, fontSize = 13.sp)
                        Switch(
                            checked = agentIsActive,
                            onCheckedChange = { agentIsActive = it }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (agentTitle.isNotBlank() && agentPrompt.isNotBlank()) {
                            val newAgent = AgentEntity(
                                id = editingAgent?.id ?: "agent_${UUID.randomUUID().toString().take(8)}",
                                title = agentTitle,
                                description = agentDesc,
                                dpImageUrl = agentImg,
                                systemPrompt = agentPrompt,
                                responseLimit = agentRespLimit.toIntOrNull() ?: 1000,
                                userTextLimit = agentUserLimit.toIntOrNull() ?: 500,
                                isActive = agentIsActive
                            )
                            onSaveAgent(newAgent)
                            showAgentDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet),
                    modifier = Modifier.testTag("admin_save_agent_confirm_btn")
                ) {
                    Text("Save Persona", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAgentDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }

    // Modal Dialog: Create User
    if (showAddUserDialog) {
        AlertDialog(
            onDismissRequest = { showAddUserDialog = false },
            title = { Text("Register User Account", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newUserEmail,
                        onValueChange = { newUserEmail = it },
                        label = { Text("Email Address") },
                        modifier = Modifier.fillMaxWidth().testTag("admin_new_user_email"),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = newUserPhone,
                        onValueChange = { newUserPhone = it },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth().testTag("admin_new_user_phone"),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = newUserCredits,
                        onValueChange = { newUserCredits = it },
                        label = { Text("Initial Energy Credits") },
                        modifier = Modifier.fillMaxWidth().testTag("admin_new_user_credits"),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newUserEmail.isNotBlank()) {
                            onCreateProfile(
                                newUserEmail,
                                newUserPhone,
                                newUserCredits.toIntOrNull() ?: 20
                            )
                            showAddUserDialog = false
                            newUserEmail = ""
                            newUserPhone = ""
                            newUserCredits = "20"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet)
                ) {
                    Text("Register User", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddUserDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }
}

@Composable
private fun UserProfileCard(
    user: ProfileEntity,
    onAdjust: (Int) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DarkSurfaceHighlight, RoundedCornerShape(16.dp))
            .testTag("user_row_${user.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = user.email.ifBlank { "User ID: ${user.id}" },
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "Phone: ${user.phone.ifBlank { "N/A" }} • Joined ${dateFormat.format(Date(user.createdAt))}",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 10.sp)
                    )
                }

                // Credits Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = EnergyDarkBg,
                    modifier = Modifier.border(1.dp, EnergyGold.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = EnergyGold, modifier = Modifier.size(16.dp))
                        Text(
                            text = "${user.creditsRemaining} Energy",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = EnergyGoldGlow,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = DarkSurfaceHighlight)
            Spacer(modifier = Modifier.height(10.dp))

            // Inline Credit Adjustment Quick Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quick Adjust Credits:",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 11.sp)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = { onAdjust(-10) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B1E1E)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("admin_decrease_credits_${user.id}")
                    ) {
                        Text("-10", color = Color(0xFFFF5252), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { onAdjust(-5) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B1E1E)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("-5", color = Color(0xFFFF5252), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { onAdjust(5) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D3320)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("admin_increase_credits_${user.id}")
                    ) {
                        Text("+5", color = Color(0xFF4CAF50), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { onAdjust(25) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D3320)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("+25", color = Color(0xFF4CAF50), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminAgentCard(
    agent: AgentEntity,
    onEdit: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DarkSurfaceHighlight, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = null,
                        tint = PrimaryViolet
                    )
                    Text(
                        text = agent.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = agent.isActive,
                        onCheckedChange = onToggle,
                        modifier = Modifier.testTag("admin_agent_switch_${agent.id}")
                    )
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = SecondaryCyan)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF5252))
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = agent.description,
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp),
                maxLines = 2
            )
        }
    }
}

@Composable
private fun CreditLogCard(log: CreditLogEntity) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault()) }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ReceiptLong,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = log.reason,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Text(
                        text = "User: ${log.profileId.take(16)}... • ${dateFormat.format(Date(log.timestamp))}",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 10.sp)
                    )
                }
            }

            Text(
                text = if (log.changeAmount > 0) "+${log.changeAmount}" else "${log.changeAmount}",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (log.changeAmount > 0) Color(0xFF4CAF50) else Color(0xFFFF5252)
                )
            )
        }
    }
}
