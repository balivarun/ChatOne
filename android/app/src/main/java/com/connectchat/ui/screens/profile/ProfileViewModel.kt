package com.connectchat.ui.screens.profile

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.connectchat.data.api.model.User
import com.connectchat.data.preferences.UserPreferences
import com.connectchat.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val userPreferences: UserPreferences,
    @ApplicationContext private val context: Context
) : ViewModel() {

    var user by mutableStateOf<User?>(null)
    var displayName by mutableStateOf("")
    var bio by mutableStateOf("")
    var isSaving by mutableStateOf(false)
    var isLoading by mutableStateOf(false)
    var successMessage by mutableStateOf<String?>(null)
    var errorMessage by mutableStateOf<String?>(null)

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            isLoading = true
            userRepository.getMe().onSuccess { u ->
                user = u
                displayName = u.displayName
                bio = u.bio ?: ""
                userPreferences.saveUser(u)
            }.onFailure {
                errorMessage = it.message ?: "Failed to load profile"
            }
            isLoading = false
        }
    }

    fun saveProfile() {
        viewModelScope.launch {
            isSaving = true
            userRepository.updateProfile(
                displayName = displayName.trim().ifBlank { null },
                bio = bio.trim().ifBlank { null }
            ).onSuccess { u ->
                user = u
                userPreferences.saveUser(u)
                successMessage = "Profile updated"
            }.onFailure {
                errorMessage = it.message ?: "Failed to save profile"
            }
            isSaving = false
        }
    }

    fun updateAvatar(uri: Uri) {
        viewModelScope.launch {
            isSaving = true
            runCatching {
                val inputStream = context.contentResolver.openInputStream(uri)
                val file = File(context.cacheDir, "avatar_temp.jpg")
                FileOutputStream(file).use { out -> inputStream?.copyTo(out) }
                val requestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("file", file.name, requestBody)
                userRepository.updateAvatar(part)
            }.onSuccess { result ->
                result.onSuccess { u ->
                    user = u
                    userPreferences.saveUser(u)
                    successMessage = "Avatar updated"
                }.onFailure {
                    errorMessage = it.message ?: "Failed to update avatar"
                }
            }.onFailure {
                errorMessage = it.message ?: "Failed to process image"
            }
            isSaving = false
        }
    }

    fun clearMessages() {
        successMessage = null
        errorMessage = null
    }
}
