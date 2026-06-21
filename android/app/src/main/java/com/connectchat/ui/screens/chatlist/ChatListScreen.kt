package com.connectchat.ui.screens.chatlist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import com.connectchat.ui.components.ConversationItem
import com.connectchat.ui.components.GroupItem
import com.connectchat.ui.theme.Accent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    onConversationClick: (String) -> Unit,
    onCreateGroup: () -> Unit,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onNewChatClick: () -> Unit,
    viewModel: ChatListViewModel = hiltViewModel()
) {
    val conversations by viewModel.conversations.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ConnectChat", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Accent),
                actions = {
                    IconButton(onClick = onNewChatClick) {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
                    }
                    IconButton(onClick = onCreateGroup) {
                        Icon(Icons.Default.GroupAdd, contentDescription = "New Group", tint = Color.White)
                    }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Profile") },
                            leadingIcon = { Icon(Icons.Default.AccountCircle, null) },
                            onClick = { showMenu = false; onProfileClick() }
                        )
                        DropdownMenuItem(
                            text = { Text("Settings") },
                            leadingIcon = { Icon(Icons.Default.Settings, null) },
                            onClick = { showMenu = false; onSettingsClick() }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewChatClick,
                containerColor = Accent
            ) {
                Icon(Icons.Default.Edit, contentDescription = "New Chat", tint = Color.White)
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            TabRow(selectedTabIndex = selectedTab, containerColor = Accent) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Chats", color = Color.White) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Groups", color = Color.White) }
                )
            }

            if (viewModel.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Accent)
                }
            } else {
                when (selectedTab) {
                    0 -> {
                        if (conversations.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    "No conversations yet\nTap the pencil icon to start chatting",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(conversations, key = { it.id }) { conversation ->
                                    ConversationItem(
                                        conversation = conversation,
                                        currentUserId = viewModel.currentUserId,
                                        onClick = { onConversationClick(conversation.id) },
                                        onArchive = { viewModel.archiveConversation(conversation.id) },
                                        onPin = { viewModel.pinConversation(conversation.id) },
                                        onMute = { viewModel.muteConversation(conversation.id) }
                                    )
                                }
                            }
                        }
                    }
                    1 -> {
                        if (viewModel.groups.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    "No groups yet\nTap the group icon to create one",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(viewModel.groups, key = { it.id }) { group ->
                                    GroupItem(
                                        group = group,
                                        onClick = { onConversationClick(group.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
