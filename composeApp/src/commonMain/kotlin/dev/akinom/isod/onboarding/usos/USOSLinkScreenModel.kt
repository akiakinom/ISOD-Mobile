package dev.akinom.isod.onboarding.usos

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import dev.akinom.isod.Secrets
import dev.akinom.isod.auth.CredentialsStorage
import dev.akinom.isod.auth.UsosAuthRepository
import dev.akinom.isod.auth.UsosAuthResult
import dev.akinom.isod.auth.UsosRequestToken
import dev.akinom.isod.auth.createSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val CONSUMER_KEY    = Secrets.USOS_CONSUMER_KEY
private const val CONSUMER_SECRET = Secrets.USOS_CONSUMER_SECRET

sealed class UsosLinkState {
    object Idle          : UsosLinkState()
    object LoadingToken  : UsosLinkState()
    data class Authorizing(
        val authorizeUrl: String,
        val requestToken: UsosRequestToken,
    ) : UsosLinkState()
    object LoadingAccess : UsosLinkState()
    object Success       : UsosLinkState()
    data class Error(val message: String) : UsosLinkState()
}

class UsosLinkScreenModel(
    private val storage: CredentialsStorage = CredentialsStorage(createSettings()),
    private val auth: UsosAuthRepository = UsosAuthRepository(CONSUMER_KEY, CONSUMER_SECRET),
) : ScreenModel {

    private val _state = MutableStateFlow<UsosLinkState>(UsosLinkState.Idle)
    val state = _state.asStateFlow()

    fun startAuth() {
        if (_state.value is UsosLinkState.LoadingToken) return
        screenModelScope.launch {
            _state.value = UsosLinkState.LoadingToken
            when (val result = auth.getRequestToken()) {
                is UsosAuthResult.RequestTokenSuccess -> {
                    _state.value = UsosLinkState.Authorizing(
                        authorizeUrl = result.requestToken.authorizeUrl,
                        requestToken = result.requestToken,
                    )
                }
                is UsosAuthResult.Error -> _state.value = UsosLinkState.Error(result.message)
                else -> {}
            }
        }
    }

    fun handleCallback(verifier: String) {
        val current = _state.value as? UsosLinkState.Authorizing ?: return
        screenModelScope.launch {
            _state.value = UsosLinkState.LoadingAccess
            when (val result = auth.getAccessToken(
                requestToken       = current.requestToken.token,
                requestTokenSecret = current.requestToken.tokenSecret,
                verifier           = verifier,
            )) {
                is UsosAuthResult.AccessTokenSuccess -> {
                    storage.saveUsosTokens(result.accessToken.token, result.accessToken.tokenSecret)
                    _state.value = UsosLinkState.Success
                }
                is UsosAuthResult.Error -> _state.value = UsosLinkState.Error(result.message)
                else -> {}
            }
        }
    }

    fun resetError() {
        if (_state.value is UsosLinkState.Error) _state.value = UsosLinkState.Idle
    }
}