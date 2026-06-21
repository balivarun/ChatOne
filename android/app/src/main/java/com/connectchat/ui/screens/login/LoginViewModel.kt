package com.connectchat.ui.screens.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.connectchat.data.repository.AuthRepository
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
                onSuccess = { uiState = LoginUiState.Success },
                onFailure = { uiState = LoginUiState.Error(it.message ?: "Sign in failed") }
            )
        }
    }

    fun clearError() {
        if (uiState is LoginUiState.Error) {
            uiState = LoginUiState.Idle
        }
    }
}
