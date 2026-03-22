package dev.akinom.isod.onboarding.usos

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import dev.akinom.isod.Secrets
import dev.akinom.isod.auth.CredentialsStorage
import dev.akinom.isod.auth.UsosAuthRepository
import dev.akinom.isod.auth.UsosAuthResult
import dev.akinom.isod.auth.UsosRequestToken
import dev.akinom.isod.auth.createSettings
import dev.akinom.isod.Res
import dev.akinom.isod.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

private const val CONSUMER_KEY    = Secrets.USOS_CONSUMER_KEY
private const val CONSUMER_SECRET = Secrets.USOS_CONSUMER_SECRET

sealed class USOSLinkState {
    data object Idle          : USOSLinkState()
    data object LoadingToken  : USOSLinkState()
    data class Authorizing(
        val authorizeUrl: String,
        val requestToken: UsosRequestToken,
    ) : USOSLinkState()
    data object LoadingAccess : USOSLinkState()
    data object Success       : USOSLinkState()
    data class Error(val message: String) : USOSLinkState()
}

class USOSLinkScreenModel(
    private val storage: CredentialsStorage = CredentialsStorage(createSettings()),
    private val auth: UsosAuthRepository = UsosAuthRepository(CONSUMER_KEY, CONSUMER_SECRET),
) : ScreenModel {

    private val _state = MutableStateFlow<USOSLinkState>(USOSLinkState.Idle)
    val state = _state.asStateFlow()

    fun startAuth() {
        if (_state.value is USOSLinkState.LoadingToken) return
        screenModelScope.launch {
            _state.value = USOSLinkState.LoadingToken
            val scopes = "studies"
            when (val result = auth.getRequestToken(scopes)) {
                is UsosAuthResult.RequestTokenSuccess -> {
                    _state.value = USOSLinkState.Authorizing(
                        authorizeUrl = result.requestToken.authorizeUrl,
                        requestToken = result.requestToken,
                    )
                }
                is UsosAuthResult.Error -> {
                    val message = if (result.isNetworkError) getString(Res.string.error_network) else getString(Res.string.error_server)
                    _state.value = USOSLinkState.Error(message)
                }
                else -> {}
            }
        }
    }

    fun handleCallback(verifier: String) {
        val current = _state.value as? USOSLinkState.Authorizing ?: return
        screenModelScope.launch {
            _state.value = USOSLinkState.LoadingAccess
            when (val result = auth.getAccessToken(
                requestToken       = current.requestToken.token,
                requestTokenSecret = current.requestToken.tokenSecret,
                verifier           = verifier,
            )) {
                is UsosAuthResult.AccessTokenSuccess -> {
                    storage.saveUsosTokens(result.accessToken.token, result.accessToken.tokenSecret)
                    _state.value = USOSLinkState.Success
                }
                is UsosAuthResult.Error -> {
                    val message = if (result.isNetworkError) getString(Res.string.error_network) else getString(Res.string.error_server)
                    _state.value = USOSLinkState.Error(message)
                }
                else -> {}
            }
        }
    }

    fun failAuth(message: String) {
        _state.value = USOSLinkState.Error(message)
    }

    fun cancelAuth() {
        _state.value = USOSLinkState.Idle
    }

    fun resetError() {
        if (_state.value is USOSLinkState.Error) _state.value = USOSLinkState.Idle
    }
}
