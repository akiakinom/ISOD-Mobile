package dev.akinom.isod.onboarding.usos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.akinom.isod.MainScreen
import dev.akinom.isod.Res
import dev.akinom.isod.*
import org.jetbrains.compose.resources.stringResource

class USOSLinkScreen : Screen {

    @Composable
    override fun Content() {
        val navigator   = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { USOSLinkScreenModel() }
        val state       by screenModel.state.collectAsState()
        var visible     by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) { visible = true }

        LaunchedEffect(state) {
            if (state is USOSLinkState.Success) {
                navigator.replaceAll(MainScreen())
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center,
        ) {

            when (val s = state) {
                is USOSLinkState.Authorizing -> {
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
                                .padding(horizontal = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )

                            Spacer(Modifier.height(16.dp))

                            Text(
                                text = stringResource(Res.string.usos_title),
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Text(
                                text = stringResource(Res.string.usos_link_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            Spacer(Modifier.height(40.dp))

                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.large,
                                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                ) {
                                    Text(
                                        text = stringResource(Res.string.why_link_usos),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Text(
                                        text = stringResource(Res.string.usos_link_reason),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 20.sp
                                    )

                                    AnimatedVisibility(visible = s is USOSLinkState.Error) {
                                        Text(
                                            text = "⚠ ${(s as? USOSLinkState.Error)?.message ?: ""}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error,
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(32.dp))

                            Button(
                                onClick = { screenModel.startAuth() },
                                enabled = s !is USOSLinkState.LoadingToken
                                        && s !is USOSLinkState.LoadingAccess,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = MaterialTheme.shapes.large,
                            ) {
                                if (s is USOSLinkState.LoadingToken || s is USOSLinkState.LoadingAccess) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 3.dp,
                                    )
                                } else {
                                    Text(
                                        text = stringResource(Res.string.authorize_usos_btn),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }

                            TextButton(
                                onClick = { navigator.replaceAll(MainScreen()) },
                                enabled = s !is USOSLinkState.LoadingToken
                                        && s !is USOSLinkState.LoadingAccess,
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            ) {
                                Text(
                                    text = stringResource(Res.string.skip_for_now),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            Spacer(Modifier.height(16.dp))

                            Text(
                                text = stringResource(Res.string.usos_login_hint),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}
