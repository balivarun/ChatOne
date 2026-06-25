package com.connectchat.data.repository

import com.connectchat.data.api.ApiService
import com.connectchat.data.api.model.FcmTokenRequest
import com.connectchat.data.api.model.MobileAuthRequest
import com.connectchat.data.api.model.RefreshTokenRequest
import com.connectchat.data.api.model.User
import com.connectchat.data.preferences.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val apiService: ApiService,
    private val userPreferences: UserPreferences
) {
    suspend fun warmUpServer(): Result<Unit> = runCatching {
        apiService.healthCheck()
        Unit
    }

    suspend fun signInWithGoogle(idToken: String): Result<User> = runCatching {
        val response = apiService.mobileGoogleSignIn(MobileAuthRequest(idToken))
        val authData = response.data ?: throw Exception(response.message ?: "Sign in failed")
        userPreferences.saveTokens(authData.accessToken, authData.refreshToken)
        userPreferences.saveUser(authData.user)
        authData.user
    }

    suspend fun refreshToken(): Result<Unit> = runCatching {
        val refresh = userPreferences.refreshToken.first() ?: throw Exception("No refresh token")
        val response = apiService.refreshToken(RefreshTokenRequest(refresh))
        val data = response.data ?: throw Exception("Refresh failed")
        userPreferences.saveTokens(data.accessToken, data.refreshToken)
    }

    suspend fun uploadFcmToken(token: String): Result<Unit> = runCatching {
        apiService.updateFcmToken(FcmTokenRequest(token))
    }

    suspend fun logout() {
        runCatching { apiService.logout() }
        userPreferences.clearAll()
    }

    fun isLoggedIn(): Flow<Boolean> = userPreferences.accessToken.map { !it.isNullOrBlank() }

    suspend fun getCurrentUser(): Result<User> = runCatching {
        val response = apiService.getMe()
        response.data ?: throw Exception(response.message ?: "Failed to get user")
    }
}
