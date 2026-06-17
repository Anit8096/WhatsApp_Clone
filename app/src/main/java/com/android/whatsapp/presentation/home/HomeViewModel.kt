package com.android.whatsapp.presentation.home

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.whatsapp.model.dataclass.Chat
import com.android.whatsapp.model.repository.ChatRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repo    : ChatRepository,
    private val auth    : FirebaseAuth,
    private val database: FirebaseDatabase
) : ViewModel() {

    // ── Chat list ─────────────────────────────────────────────
    val chats: StateFlow<List<Chat>> = repo.observeChats()
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    // ── Online presence map  uid → isOnline ───────────────────
    private val _onlineMap = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val onlineMap: StateFlow<Map<String, Boolean>> = _onlineMap.asStateFlow()

    // Tracks active Firebase listeners so we can remove them
    private val presenceListeners = mutableMapOf<String, Pair<DatabaseReference, ValueEventListener>>()

    init {
        // When the chat list changes, add/remove presence listeners for each peer
        viewModelScope.launch {
            chats.collect { chatList ->
                val newPeerIds = chatList.map { it.peerId }.toSet()

                // Remove listeners for peers no longer in the list
                val toRemove = presenceListeners.keys - newPeerIds
                toRemove.forEach { peerId ->
                    presenceListeners[peerId]?.let { (ref, listener) ->
                        ref.removeEventListener(listener)
                    }
                    presenceListeners.remove(peerId)
                    _onlineMap.value -= peerId
                }

                // Add listeners for new peers
                val toAdd = newPeerIds - presenceListeners.keys
                toAdd.forEach { peerId ->
                    if (peerId.isBlank()) return@forEach
                    val ref = database.reference.child("users").child(peerId).child("isOnline")
                    val listener = object : ValueEventListener {
                        override fun onDataChange(snap: DataSnapshot) {
                            val online = snap.getValue(Boolean::class.java) ?: false
                            _onlineMap.value += (peerId to online)
                        }
                        override fun onCancelled(e: DatabaseError) {}
                    }
                    ref.addValueEventListener(listener)
                    presenceListeners[peerId] = ref to listener
                }
            }
        }
    }

    // Clean up all Firebase listeners when ViewModel is destroyed
    @SuppressLint("EmptySuperCall")
    override fun onCleared() {
        super.onCleared()
        presenceListeners.values.forEach { (ref, listener) ->
            ref.removeEventListener(listener)
        }
        presenceListeners.clear()
    }

    // ── Actions ───────────────────────────────────────────────
    fun deleteChat(chatId: String) {
        viewModelScope.launch { repo.deleteChat(chatId) }
    }
}