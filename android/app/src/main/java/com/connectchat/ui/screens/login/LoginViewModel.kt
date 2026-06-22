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
    object Loading : LoginUiState()
    object Success : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    var uiState by mutableStateOf<LoginUiState>(LoginUiState.Idle)
        private set

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            uiState = LoginUiState.Loading
            authRepository.signInWithGoogle(idToken).fold(
                onSuccess = {
                    uploadFcmToken()
                    uiState = LoginUiState.Success
                },
                onFailure = { uiState = LoginUiState.Error(it.message ?: "Sign in failed") }
            )
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
