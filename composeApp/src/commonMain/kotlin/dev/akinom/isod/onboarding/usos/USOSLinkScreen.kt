package dev.akinom.isod.onboarding.usos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.akinom.isod.home.HomeScreen

class UsosLinkScreen : Screen {

    @Composable
    override fun Content() {
        val navigator   = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { UsosLinkScreenModel() }
        val state       by screenModel.state.collectAsState()
        var visible     by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) { visible = true }

        LaunchedEffect(state) {
            if (state is UsosLinkState.Success) {
                navigator.replaceAll(HomeScreen())
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center,
        ) {

            when (val s = state) {
                is UsosLinkState.Authorizing -> {
                    UsosWebView(
                        url = s.authorizeUrl,
                        onCallbackReceived = { verifier ->
                            screenModel.handleCallback(verifier)
                        },
                    )
                }

                else -> {
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 }),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "USOS",
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = "Link your account",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            Spacer(Modifier.height(24.dp))

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(10.dp),
                                    )
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(24.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                Text(
                                    text = "To access sports, languages and other USOS data, you need to authorize this app with USOS.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )

                                AnimatedVisibility(visible = s is UsosLinkState.Error) {
                                    Text(
                                        text = "⚠ ${(s as? UsosLinkState.Error)?.message ?: ""}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }

                            Spacer(Modifier.height(24.dp))

                            Button(
                                onClick = { screenModel.startAuth() },
                                enabled = s !is UsosLinkState.LoadingToken
                                        && s !is UsosLinkState.LoadingAccess,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(6.dp),
                            ) {
                                if (s is UsosLinkState.LoadingToken || s is UsosLinkState.LoadingAccess) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp,
                                    )
                                } else {
                                    Text(
                                        text = "Authorize with USOS →",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp,
                                    )
                                }
                            }

                            TextButton(
                                onClick = { navigator.replaceAll(HomeScreen()) },
                                enabled = s !is UsosLinkState.LoadingToken
                                        && s !is UsosLinkState.LoadingAccess,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = "Skip for now",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            Text(
                                text = "You'll be redirected to the USOS login page.\nNo passwords are stored by this app.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp,
                            )

                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}