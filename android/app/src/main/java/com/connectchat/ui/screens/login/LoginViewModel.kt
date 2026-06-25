package com.connectchat.ui.screens.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.connectchat.data.repository.AuthRepository
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class LoginUiState {
    object Idle : LoginUiState()
    data class Loading(val message: String = "Signing in...") : LoginUiState()
    object Success : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    var uiState by mutableStateOf<LoginUiState>(LoginUiState.Idle)
        private set

    init {
        // Ping the server immediately so it wakes up before the user finishes Google Sign-In
        viewModelScope.launch {
            runCatching { authRepository.warmUpServer() }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            val maxAttempts = 3
            for (attempt in 1..maxAttempts) {
                val statusMsg = when (attempt) {
                    1 -> "Signing in..."
                    2 -> "Server is starting up, retrying... (${attempt}/$maxAttempts)"
                    else -> "Almost there, retrying... (${attempt}/$maxAttempts)"
                }
                uiState = LoginUiState.Loading(statusMsg)

                val result = authRepository.signInWithGoogle(idToken)
                if (result.isSuccess) {
                    uploadFcmToken()
                    uiState = LoginUiState.Success
                    return@launch
                }

                if (attempt == maxAttempts) {
                    uiState = LoginUiState.Error(
                        "Login failed after $maxAttempts attempts. " +
                        "The server may still be starting — please try again in a moment."
                    )
                }
            }
        }
    }

    private fun uploadFcmToken() {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            if (token != null) {
                viewModelScope.launch {
                    authRepository.uploadFcmToken(token)
                }
            }
        }
    }

    fun clearError() {
        if (uiState is LoginUiState.Error) {
            uiState = LoginUiState.Idle
        }
    }
}
