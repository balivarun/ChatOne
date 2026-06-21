package com.connectchat.ui.screens.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.connectchat.data.api.model.User
import com.connectchat.data.preferences.UserPreferences
import com.connectchat.data.repository.AuthRepository
import com.connectchat.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    val theme: StateFlow<String> = userPreferences.theme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")

    var blockedUsers by mutableStateOf<List<User>>(emptyList())
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    init {
        loadBlockedUsers()
    }

    fun setTheme(theme: String) {
        viewModelScope.launch { userPreferences.setTheme(theme) }
    }

    fun loadBlockedUsers() {
        viewModelScope.launch {
            isLoading = true
            userRepository.getBlockedUsers().onSuccess { users ->
                blockedUsers = users
            }.onFailure {
                errorMessage = it.message
            }
            isLoading = false
        }
    }

    fun unblockUser(userId: String) {
        viewModelScope.launch {
            userRepository.unblockUser(userId).onSuccess {
                blockedUsers = blockedUsers.filter { it.id != userId }
            }.onFailure {
                errorMessage = it.message ?: "Failed to unblock user"
            }
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onLoggedOut()
        }
    }

    fun clearError() { errorMessage = null }
}
