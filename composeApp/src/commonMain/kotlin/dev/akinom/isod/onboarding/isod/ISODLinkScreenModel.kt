package dev.akinom.isod.onboarding.isod

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import dev.akinom.isod.auth.CredentialsStorage
import dev.akinom.isod.auth.IsodAuthRepository
import dev.akinom.isod.auth.IsodAuthResult
import dev.akinom.isod.auth.IsodUserInfo
import dev.akinom.isod.auth.createSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class IsodLinkState {
    object Idle    : IsodLinkState()
    object Loading : IsodLinkState()
    data class Success(val user: IsodUserInfo) : IsodLinkState()
    data class Error(val message: String)      : IsodLinkState()
}

class IsodLinkScreenModel(
    private val storage: CredentialsStorage = CredentialsStorage(createSettings()),
    private val auth: IsodAuthRepository = IsodAuthRepository(),
) : ScreenModel {

    private val _state = MutableStateFlow<IsodLinkState>(IsodLinkState.Idle)
    val state = _state.asStateFlow()

    fun linkAccount(username: String, apiKey: String) {
        if (_state.value is IsodLinkState.Loading) return

        screenModelScope.launch {
            _state.value = IsodLinkState.Loading

            when (val result = auth.validateCredentials(username, apiKey)) {
                is IsodAuthResult.Success -> {
                    storage.saveIsodCredentials(username, apiKey)
                    _state.value = IsodLinkState.Success(result.user)
                }
                is IsodAuthResult.Error -> {
                    _state.value = IsodLinkState.Error(result.message)
                }
            }
        }
    }

    fun resetError() {
        if (_state.value is IsodLinkState.Error) {
            _state.value = IsodLinkState.Idle
        }
    }
}