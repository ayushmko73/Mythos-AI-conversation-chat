package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.AgentEntity
import com.example.ui.theme.*

@Composable
fun CompanionsScreen(
    agents: List<AgentEntity>,
    selectedAgent: AgentEntity?,
    onSelectAgent: (AgentEntity) -> Unit,
    onStartChat: (AgentEntity) -> Unit
) {
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Cyberpunk", "Fantasy", "Sci-Fi", "Cozy")

    val filteredAgents = remember(agents, selectedCategory) {
        if (selectedCategory == "All") agents
        else agents.filter {
            it.title.contains(selectedCategory, ignoreCase = true) ||
                    it.description.contains(selectedCategory, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            listOf(PrimaryViolet, SecondaryCyan)
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF211440),
                                    DarkSurface
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = SecondaryCyan
                            )
                            Text(
                                text = "Interactive AI Companions",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Select a persona to step into an immersive, evolving interactive story thread with contextual memory persistence.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        )
                    }
                }
            }
        }

        // Category Filter Chips
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryViolet,
                            selectedLabelColor = Color.White,
                            containerColor = DarkSurfaceVariant,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedCategory == category,
                            borderColor = DarkSurfaceHighlight,
                            selectedBorderColor = PrimaryViolet
                        ),
                        modifier = Modifier.testTag("filter_chip_$category")
                    )
                }
            }
        }

        // Agent Persona Cards
        items(filteredAgents, key = { it.id }) { agent ->
            AgentCard(
                agent = agent,
                isSelected = selectedAgent?.id == agent.id,
                onSelect = { onSelectAgent(agent) },
                onStartChat = { onStartChat(agent) }
            )
        }

        if (filteredAgents.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No companions match '$selectedCategory'. Try selecting 'All' or add a new persona in Admin Panel.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun AgentCard(
    agent: AgentEntity,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onStartChat: () -> Unit
) {
    val context = LocalContext.current
    val imageResId = remember(agent.dpImageUrl) {
        val name = agent.dpImageUrl.substringBeforeLast(".")
        context.resources.getIdentifier(name, "drawable", context.packageName)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) PrimaryViolet else DarkSurfaceHighlight,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable { onSelect() }
            .testTag("agent_card_${agent.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF1E172F) else DarkSurface
        ),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Avatar Image / Fallback
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(PrimaryViolet, SecondaryCyan)
                            )
                        )
                        .border(2.dp, PrimaryViolet, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageResId != 0) {
                        Image(
                            painter = painterResource(id = imageResId),
                            contentDescription = agent.title,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = agent.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 16.sp
                            )
                        )

                        // Active Indicator Tag
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (agent.isActive) Color(0xFF0D3320) else Color(0xFF3B1E1E)
                        ) {
                            Text(
                                text = if (agent.isActive) "Active" else "Disabled",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (agent.isActive) Color(0xFF4CAF50) else Color(0xFFFF5252),
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = agent.description,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        ),
                        maxLines = 3
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Limits & Prompt Summary Tags
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = { },
                    label = {
                        Text(
                            "Max ${agent.responseLimit} Tokens",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = SecondaryCyan,
                            modifier = Modifier.size(12.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(containerColor = DarkSurfaceVariant),
                    border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = DarkSurfaceHighlight)
                )

                AssistChip(
                    onClick = { },
                    label = {
                        Text(
                            "User Limit: ${agent.userTextLimit} chars",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(containerColor = DarkSurfaceVariant),
                    border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = DarkSurfaceHighlight)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onStartChat,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .testTag("start_chat_btn_${agent.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Chat,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isSelected) "Continue Interactive Chat" else "Start Story Thread",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}
