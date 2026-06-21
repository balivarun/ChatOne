package com.connectchat.ui.screens.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
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
import com.connectchat.ui.components.UserAvatar
import com.connectchat.ui.theme.Accent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.updateAvatar(it) }
    }

    LaunchedEffect(viewModel.successMessage) {
        viewModel.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Profile", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Accent),
                actions = {
                    if (viewModel.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = { viewModel.saveProfile() }) {
                            Icon(Icons.Default.Check, contentDescription = "Save", tint = Color.White)
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (viewModel.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Accent)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Box(contentAlignment = Alignment.BottomEnd) {
                UserAvatar(
                    avatarUrl = viewModel.user?.avatarUrl,
                    displayName = viewModel.displayName.ifBlank { viewModel.user?.displayName ?: "?" },
                    size = 100.dp,
                    modifier = Modifier.clickable { imagePickerLauncher.launch("image/*") }
                )
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Change Avatar",
                    tint = Color.White,
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { imagePickerLauncher.launch("image/*") }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tap to change photo",
                style = MaterialTheme.typography.bodySmall,
                color = Accent
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = viewModel.displayName,
                onValueChange = { viewModel.displayName = it },
                label = { Text("Display Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = viewModel.bio,
                onValueChange = { viewModel.bio = it },
                label = { Text("Bio") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 4,
                placeholder = { Text("Tell something about yourself...") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            viewModel.user?.email?.let { email ->
                Text(
                    text = "Email: $email",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}
