package dev.akinom.isod.onboarding.isod

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import dev.akinom.isod.auth.CredentialsStorage
import dev.akinom.isod.auth.IsodAuthRepository
import dev.akinom.isod.auth.IsodAuthResult
import dev.akinom.isod.auth.IsodUserInfo
import dev.akinom.isod.auth.createSettings
import dev.akinom.isod.Res
import dev.akinom.isod.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

sealed class ISODLinkState {
    data object Idle    : ISODLinkState()
    data object Loading : ISODLinkState()
    data class Success(val user: IsodUserInfo) : ISODLinkState()
    data class Error(val message: String)      : ISODLinkState()
}

class ISODLinkScreenModel : ScreenModel, KoinComponent {

    private val storage: CredentialsStorage by inject()
    private val auth: IsodAuthRepository by inject()

    private val _state = MutableStateFlow<ISODLinkState>(ISODLinkState.Idle)
    val state = _state.asStateFlow()

    fun link(username: String, apiKey: String) {
        if (_state.value is ISODLinkState.Loading) return

        screenModelScope.launch {
            _state.value = ISODLinkState.Loading

            when (val result = auth.validateCredentials(username, apiKey)) {
                is IsodAuthResult.Success -> {
                    storage.saveIsodCredentials(username, apiKey)
                    _state.value = ISODLinkState.Success(result.user)
                }
                is IsodAuthResult.Error -> {
                    val message = when {
                        result.isNetworkError -> getString(Res.string.error_network)
                        result.message.contains("Invalid") -> getString(Res.string.error_invalid_credentials)
                        else -> getString(Res.string.error_server)
                    }
                    _state.value = ISODLinkState.Error(message)
                }
            }
        }
    }

    fun resetError() {
        if (_state.value is ISODLinkState.Error) {
            _state.value = ISODLinkState.Idle
        }
    }
}
