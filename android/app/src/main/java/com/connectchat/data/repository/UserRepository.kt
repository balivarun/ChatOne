package com.connectchat.data.repository

import com.connectchat.data.api.ApiService
import com.connectchat.data.api.model.UpdateProfileRequest
import com.connectchat.data.api.model.User
import okhttp3.MultipartBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getMe(): Result<User> = runCatching {
        val response = apiService.getMe()
        response.data ?: throw Exception(response.message ?: "Failed to get user")
    }

    suspend fun updateProfile(displayName: String?, bio: String?): Result<User> = runCatching {
        val response = apiService.updateProfile(UpdateProfileRequest(displayName, bio))
        response.data ?: throw Exception(response.message ?: "Failed to update profile")
    }

    suspend fun updateAvatar(file: MultipartBody.Part): Result<User> = runCatching {
        val response = apiService.updateAvatar(file)
        response.data ?: throw Exception(response.message ?: "Failed to update avatar")
    }

    suspend fun searchUsers(query: String, page: Int = 0, size: Int = 20): Result<List<User>> = runCatching {
        val response = apiService.searchUsers(query, page, size)
        response.data?.content?.filterNotNull() ?: emptyList()
    }

    suspend fun getUser(id: String): Result<User> = runCatching {
        val response = apiService.getUser(id)
        response.data ?: throw Exception(response.message ?: "User not found")
    }

    suspend fun blockUser(id: String): Result<Unit> = runCatching {
        apiService.blockUser(id)
    }

    suspend fun unblockUser(id: String): Result<Unit> = runCatching {
        apiService.unblockUser(id)
    }

    suspend fun getBlockedUsers(): Result<List<User>> = runCatching {
        val response = apiService.getBlockedUsers()
        response.data ?: emptyList()
    }
}
