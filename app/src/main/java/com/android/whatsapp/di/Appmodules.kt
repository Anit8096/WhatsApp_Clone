package com.android.whatsapp.di

import com.android.whatsapp.model.repository.*
import com.android.whatsapp.presentation.auth.AuthViewModel
import com.android.whatsapp.presentation.chat.ConversationViewModel
import com.android.whatsapp.presentation.home.HomeViewModel
import com.android.whatsapp.presentation.newchat.NewChatViewModel
import com.android.whatsapp.presentation.profile.ProfileViewModel
import com.android.whatsapp.presentation.settings.SettingsViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val firebaseModule = module {
    single { FirebaseAuth.getInstance() }
    single { FirebaseDatabase.getInstance() }
    single { FirebaseStorage.getInstance() }
}

val authModule = module {
    single<AuthRepository> { FirebaseAuthRepository(get(), get()) }
    // AuthViewModel now needs UserRepository to check existing profile on login
    viewModel { AuthViewModel(get(), get()) }
}

val userModule = module {
    single<UserRepository> { FirebaseUserRepository(get(), get()) }
    single { PresenceRepository(get(), get()) }
}

val chatModule = module {
    single<ChatRepository> { FirebaseChatRepository(get(), get(), get()) }
    viewModelOf(::HomeViewModel)
    viewModelOf(::NewChatViewModel)
    viewModel { params -> ConversationViewModel(params.get(), get()) }
}

val profileModule = module {
    // ProfileViewModel now needs ChatRepository to propagate name changes
    viewModel { ProfileViewModel(get(), get(), get()) }
    viewModelOf(::SettingsViewModel)
}

val appModules = listOf(firebaseModule, authModule, userModule, chatModule, profileModule)