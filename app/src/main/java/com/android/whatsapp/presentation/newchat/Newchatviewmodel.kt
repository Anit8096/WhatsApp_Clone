package com.android.whatsapp.presentation.newchat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.whatsapp.model.dataclass.User
import com.android.whatsapp.model.repository.AuthRepository
import com.android.whatsapp.model.repository.ChatRepository
import com.android.whatsapp.model.repository.UserRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class NewChatState(
    val query        : String       = "",
    val users        : List<User>   = emptyList(),
    val isLoading    : Boolean      = false,
    val createdChatId: String?      = null,
    val selectedUser : User?        = null,
    val error        : String?      = null
)

@OptIn(FlowPreview::class)
class NewChatViewModel(
    private val authRepo: AuthRepository,
    private val userRepo: UserRepository,
    private val chatRepo: ChatRepository
) : ViewModel() {

    private val _state  = MutableStateFlow(NewChatState())
    val state: StateFlow<NewChatState> = _state.asStateFlow()

    private val queryFlow = MutableStateFlow("")
    private val currentUid = authRepo.currentUser?.uid ?: ""

    init {
        viewModelScope.launch {
            queryFlow
                .debounce(300)
                .distinctUntilChanged()
                .collect { q ->
                    if (q.isBlank()) {
                        _state.value = _state.value.copy(users = emptyList(), isLoading = false, error = null)
                    } else {
                        _state.value = _state.value.copy(isLoading = true, error = null)
                        runCatching { userRepo.searchUsers(q, currentUid) }
                            .onSuccess { results ->
                                _state.value = _state.value.copy(users = results, isLoading = false)
                            }
                            .onFailure { e ->
                                _state.value = _state.value.copy(
                                    isLoading = false,
                                    error     = e.message ?: "Search failed"
                                )
                            }
                    }
                }
        }
    }

    fun search(query: String) {
        _state.value  = _state.value.copy(query = query)
        queryFlow.value = query
    }

    fun startChat(user: User) {
        if (_state.value.isLoading) return     // prevent double-tap
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, selectedUser = user, error = null)
            chatRepo.createOrGetChat(user.uid)
                .onSuccess { chatId ->
                    _state.value = _state.value.copy(createdChatId = chatId, isLoading = false)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error     = e.message ?: "Could not create chat"
                    )
                }
        }
    }

    fun resetCreated() { _state.value = _state.value.copy(createdChatId = null) }
    fun clearError()   { _state.value = _state.value.copy(error = null) }
}