package com.connectchat.ui.screens.usersearch

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.connectchat.data.api.model.User
import com.connectchat.data.repository.ConversationRepository
import com.connectchat.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserSearchViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val conversationRepository: ConversationRepository
) : ViewModel() {

    var searchQuery by mutableStateOf("")
    var searchResults by mutableStateOf<List<User>>(emptyList())
    var isSearching by mutableStateOf(false)
    var isCreatingConversation by mutableStateOf(false)
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
                        searchResults = results
                    }.onFailure {
                        errorMessage = it.message ?: "Search failed"
                    }
                    isSearching = false
                }
        }
    }

    fun updateSearch(query: String) {
        searchQuery = query
        viewModelScope.launch { searchFlow.emit(query) }
    }

    fun selectUser(userId: String, onConversationCreated: (String) -> Unit) {
        viewModelScope.launch {
            isCreatingConversation = true
            conversationRepository.createConversation(userId).onSuccess { conversation ->
                onConversationCreated(conversation.id)
            }.onFailure {
                errorMessage = it.message ?: "Failed to create conversation"
            }
            isCreatingConversation = false
        }
    }

    fun clearError() { errorMessage = null }
}
