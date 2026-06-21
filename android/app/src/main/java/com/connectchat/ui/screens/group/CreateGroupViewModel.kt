package com.connectchat.ui.screens.group

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.connectchat.data.api.model.User
import com.connectchat.data.repository.GroupRepository
import com.connectchat.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateGroupViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    var groupName by mutableStateOf("")
    var groupDescription by mutableStateOf("")
    var searchQuery by mutableStateOf("")
    var searchResults by mutableStateOf<List<User>>(emptyList())
    var selectedMembers by mutableStateOf<List<User>>(emptyList())
    var isCreating by mutableStateOf(false)
    var isSearching by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    private val searchFlow = MutableStateFlow("")

    init {
        observeSearch()
    }

    @OptIn(FlowPreview::class)
    private fun observeSearch() {
        viewModelScope.launch {
            searchFlow
                .debounce(300)
                .distinctUntilChanged()
                .collect { query ->
                    if (query.isBlank()) {
                        searchResults = emptyList()
                        return@collect
                    }
                    isSearching = true
                    userRepository.searchUsers(query).onSuccess { results ->
                        searchResults = results.filter { user ->
                            selectedMembers.none { it.id == user.id }
                        }
                    }
                    isSearching = false
                }
        }
    }

    fun updateSearch(query: String) {
        searchQuery = query
        viewModelScope.launch { searchFlow.emit(query) }
    }

    fun addMember(user: User) {
        if (selectedMembers.none { it.id == user.id }) {
            selectedMembers = selectedMembers + user
            searchResults = searchResults.filter { it.id != user.id }
        }
    }

    fun removeMember(user: User) {
        selectedMembers = selectedMembers.filter { it.id != user.id }
    }

    fun createGroup(onSuccess: (String) -> Unit) {
        if (groupName.isBlank()) {
            errorMessage = "Group name is required"
            return
        }
        viewModelScope.launch {
            isCreating = true
            groupRepository.createGroup(
                name = groupName.trim(),
                description = groupDescription.trim().ifBlank { null },
                memberIds = selectedMembers.map { it.id }
            ).onSuccess { group ->
                onSuccess(group.id)
            }.onFailure {
                errorMessage = it.message ?: "Failed to create group"
            }
            isCreating = false
        }
    }

    fun clearError() { errorMessage = null }
}
