package dev.akinom.isod

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.akinom.isod.auth.CredentialsStorage
import dev.akinom.isod.auth.createSettings
import dev.akinom.isod.auth.getAppVersion
import dev.akinom.isod.onboarding.isod.ISODLinkScreen
import dev.akinom.isod.onboarding.usos.USOSLinkScreen
import org.jetbrains.compose.resources.stringResource

class SettingsScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val storage = CredentialsStorage(createSettings())
        val hasUsos = storage.hasUsosTokens()
        val version = remember { getAppVersion() }
        val isBeta = remember(version) { version.startsWith("0.") }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(Res.string.settings_title), fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
                        }
                    }
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    SectionHeader(stringResource(Res.string.section_account))
                }

                item {
                    ListItem(
                        headlineContent = { Text(stringResource(Res.string.isod_user)) },
                        supportingContent = { Text(storage.getIsodUsername() ?: stringResource(Res.string.not_linked)) },
                        leadingContent = { Icon(Icons.Default.Person, null) }
                    )
                }

                item {
                    ListItem(
                        headlineContent = { Text(stringResource(Res.string.usos_account)) },
                        supportingContent = { Text(if (hasUsos) stringResource(Res.string.linked) else stringResource(Res.string.not_linked)) },
                        leadingContent = { Icon(Icons.Default.AccountBalance, null) },
                        trailingContent = {
                            if (hasUsos) {
                                TextButton(onClick = { 
                                    storage.clearUsosTokens()
                                    navigator.pop() // Refresh state
                                }) {
                                    Text(stringResource(Res.string.unlink), color = MaterialTheme.colorScheme.error)
                                }
                            } else {
                                Button(onClick = { navigator.push(USOSLinkScreen()) }) {
                                    Text(stringResource(Res.string.link))
                                }
                            }
                        }
                    )
                }

                item {
                    Spacer(Modifier.height(16.dp))
                    SectionHeader(stringResource(Res.string.section_about))
                }

                item {
                    AppInfoCard(version, isBeta)
                }

                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                }

                item {
                    Button(
                        onClick = {
                            storage.clearAll()
                            navigator.replaceAll(ISODLinkScreen())
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(Res.string.log_out), fontWeight = FontWeight.Bold)
                    }
                }
                
                item {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = stringResource(Res.string.version_format, version) + if (isBeta) stringResource(Res.string.beta_suffix) else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun AppInfoCard(version: String, isBeta: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.School,
                    null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(Modifier.height(16.dp))
            
            Text(
                text = stringResource(Res.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )

            Spacer(Modifier.height(16.dp))
            
            Text(
                text = stringResource(Res.string.app_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            
            Spacer(Modifier.height(20.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                InfoBadge(Icons.Default.Bolt, stringResource(Res.string.tech_stack_energy_drinks))
                Spacer(Modifier.width(8.dp))
                InfoBadge(Icons.Default.Favorite, stringResource(Res.string.tech_stack_open_source))
            }
        }
    }
}

@Composable
private fun InfoBadge(icon: ImageVector, text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = CircleShape,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(6.dp))
            Text(text, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
        }
    }
}
