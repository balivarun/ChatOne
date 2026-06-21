package com.connectchat.ui.screens.usersearch

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.connectchat.ui.components.UserListItem
import com.connectchat.ui.theme.Accent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSearchScreen(
    onUserSelected: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: UserSearchViewModel = hiltViewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = viewModel.searchQuery,
                        onValueChange = { viewModel.updateSearch(it) },
                        placeholder = { Text("Search users...", color = Color.White.copy(alpha = 0.7f)) },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = Color.White)
                        },
                        trailingIcon = {
                            if (viewModel.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateSearch("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.White)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Accent)
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when {
                viewModel.isCreatingConversation -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Accent)
                    }
                }
                viewModel.isSearching -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Accent)
                    }
                }
                viewModel.searchQuery.isBlank() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Search for users to start a conversation",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                viewModel.searchResults.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No users found for \"${viewModel.searchQuery}\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(viewModel.searchResults, key = { it.id }) { user ->
                            UserListItem(
                                user = user,
                                onClick = {
                                    viewModel.selectUser(user.id) { conversationId ->
                                        onUserSelected(conversationId)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
