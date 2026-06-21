package com.connectchat.ui.screens.group

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.connectchat.data.api.model.Group
import com.connectchat.data.api.model.GroupRole
import com.connectchat.data.api.model.User
import com.connectchat.data.preferences.UserPreferences
import com.connectchat.data.repository.GroupRepository
import com.connectchat.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupInfoViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val userRepository: UserRepository,
    private val userPreferences: UserPreferences,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val groupId: String = checkNotNull(savedStateHandle["groupId"])

    var group by mutableStateOf<Group?>(null)
    var members by mutableStateOf<List<User>>(emptyList())
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var currentUserId by mutableStateOf("")
    var isAdmin by mutableStateOf(false)

    init {
        viewModelScope.launch {
            currentUserId = userPreferences.userId.first() ?: ""
            loadGroup()
        }
    }

    fun loadGroup() {
        viewModelScope.launch {
            isLoading = true
            groupRepository.getGroup(groupId).onSuccess { g ->
                group = g
                isAdmin = g.role == GroupRole.ADMIN
            }.onFailure {
                errorMessage = it.message
            }
            isLoading = false
        }
    }

    fun removeMember(userId: String) {
        viewModelScope.launch {
            groupRepository.removeGroupMember(groupId, userId).onSuccess {
                loadGroup()
            }.onFailure {
                errorMessage = it.message ?: "Failed to remove member"
            }
        }
    }

    fun promoteToAdmin(userId: String) {
        viewModelScope.launch {
            groupRepository.promoteToAdmin(groupId, userId).onSuccess {
                loadGroup()
            }.onFailure {
                errorMessage = it.message ?: "Failed to promote member"
            }
        }
    }

    fun deleteGroup(onSuccess: () -> Unit) {
        viewModelScope.launch {
            groupRepository.deleteGroup(groupId).onSuccess {
                onSuccess()
            }.onFailure {
                errorMessage = it.message ?: "Failed to delete group"
            }
        }
    }

    fun leaveGroup(onSuccess: () -> Unit) {
        viewModelScope.launch {
            groupRepository.removeGroupMember(groupId, currentUserId).onSuccess {
                onSuccess()
            }.onFailure {
                errorMessage = it.message ?: "Failed to leave group"
            }
        }
    }

    fun clearError() { errorMessage = null }
}
