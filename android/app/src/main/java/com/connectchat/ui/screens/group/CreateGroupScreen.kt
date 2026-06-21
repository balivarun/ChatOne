package com.connectchat.ui.screens.group

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateGroupScreen(
    onBack: () -> Unit,
    onGroupCreated: (String) -> Unit,
    viewModel: CreateGroupViewModel = hiltViewModel()
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
                title = { Text("New Group", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Accent),
                actions = {
                    if (viewModel.isCreating) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = { viewModel.createGroup(onGroupCreated) }) {
                            Icon(Icons.Default.Check, contentDescription = "Create", tint = Color.White)
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = viewModel.groupName,
                    onValueChange = { viewModel.groupName = it },
                    label = { Text("Group Name*") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = viewModel.groupDescription,
                    onValueChange = { viewModel.groupDescription = it },
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (viewModel.selectedMembers.isNotEmpty()) {
                item {
                    Text("Selected Members", style = MaterialTheme.typography.labelMedium, color = Accent)
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        viewModel.selectedMembers.forEach { user ->
                            FilterChip(
                                selected = true,
                                onClick = { viewModel.removeMember(user) },
                                label = { Text(user.displayName) },
                                trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Remove", Modifier.padding(2.dp)) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Accent.copy(alpha = 0.2f))
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            item {
                OutlinedTextField(
                    value = viewModel.searchQuery,
                    onValueChange = { viewModel.updateSearch(it) },
                    label = { Text("Add Participants") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (viewModel.isSearching) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Accent)
                    }
                }
            } else {
                items(viewModel.searchResults, key = { it.id }) { user ->
                    UserListItem(
                        user = user,
                        onClick = { viewModel.addMember(user) }
                    )
                }
            }
        }
    }
}
