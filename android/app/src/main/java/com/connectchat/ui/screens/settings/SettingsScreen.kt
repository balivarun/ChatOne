package com.connectchat.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.connectchat.ui.components.UserAvatar
import com.connectchat.ui.theme.Accent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val currentTheme by viewModel.theme.collectAsState()

    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    val themeOptions = listOf("light", "dark", "system")
    val themeLabels = listOf("Light", "Dark", "System")

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Accent)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item {
                Text(
                    text = "APPEARANCE",
                    style = MaterialTheme.typography.labelMedium,
                    color = Accent,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text("Theme", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        themeOptions.forEachIndexed { index, option ->
                            SegmentedButton(
                                selected = currentTheme == option,
                                onClick = { viewModel.setTheme(option) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = themeOptions.size),
                                label = { Text(themeLabels[index]) }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Divider()
            }

            item {
                Text(
                    text = "BLOCKED USERS",
                    style = MaterialTheme.typography.labelMedium,
                    color = Accent,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            if (viewModel.blockedUsers.isEmpty()) {
                item {
                    Text(
                        text = "No blocked users",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            } else {
                items(viewModel.blockedUsers, key = { it.id }) { user ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserAvatar(
                            avatarUrl = user.avatarUrl,
                            displayName = user.displayName,
                            size = 40.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(user.displayName, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        OutlinedButton(onClick = { viewModel.unblockUser(user.id) }) {
                            Text("Unblock")
                        }
                    }
                }
            }

            item {
                Divider(modifier = Modifier.padding(top = 16.dp))
                Button(
                    onClick = { viewModel.logout(onLogout) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign Out")
                }
            }
        }
    }
}
