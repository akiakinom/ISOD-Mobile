package dev.akinom.isod.news

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.akinom.isod.Res
import dev.akinom.isod.*
import dev.akinom.isod.data.repository.NewsRepository
import org.jetbrains.compose.resources.stringResource
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class NewsDetailScreenModel(private val hash: String) : ScreenModel, KoinComponent {
    private val repo: NewsRepository by inject()
    val item = repo.getNewsItem(hash)
}

class NewsDetailScreen(private val hash: String) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel { NewsDetailScreenModel(hash) }
        val item by screenModel.item.collectAsState(initial = null)
        val navigator = LocalNavigator.currentOrThrow

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        IconButton(
                            onClick = { navigator.replaceAll(MainScreen(MainTab.News)) },
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(
                                Res.string.back))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        ) { paddingValues ->
            if (item == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val data = item!!
                val typeColor = data.type.toColor()
                val typeRes = data.type.toStringRes()
                val parsed = remember(data.subject) { parseSubject(data.subject) }
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header Area
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(typeColor.copy(alpha = 0.05f))
                            .padding(horizontal = 24.dp)
                            .padding(top = 108.dp, bottom = 32.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = typeColor.copy(alpha = 0.1f),
                                    contentColor = typeColor,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = if (typeRes != null) stringResource(typeRes) else data.type.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }

                                if (parsed.tag != null) {
                                    val (tagContainer, tagContent) = getTagColors(parsed.tag)
                                    Spacer(Modifier.width(12.dp))
                                    Surface(
                                        color = tagContainer,
                                        contentColor = tagContent,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = parsed.tag,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(20.dp))

                            Row(verticalAlignment = Alignment.Top) {
                                if (parsed.isGradeUpdate) {
                                    Icon(
                                        Icons.Default.Star,
                                        null,
                                        modifier = Modifier.padding(top = 4.dp, end = 12.dp).size(28.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = parsed.getDisplaySubject(),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Black,
                                    lineHeight = 36.sp,
                                    color = if (parsed.isGradeUpdate) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(Modifier.height(24.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                MetaItem(Icons.Default.Person, data.modifiedBy)
                                MetaItem(Icons.Default.Schedule, data.modifiedDate.split(" ")[0])
                            }
                        }
                    }

                    // Content Area
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        HtmlText(
                            html = data.content,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (data.attachments.isNotEmpty()) {
                            Spacer(Modifier.height(32.dp))
                            Text(
                                stringResource(Res.string.attachments),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(16.dp))
                            
                            data.attachments.forEach { attachment ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    shape = MaterialTheme.shapes.medium,
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                    onClick = { /* TODO */ }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Attachment,
                                            null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(Modifier.width(16.dp))
                                        Column {
                                            Text(
                                                text = attachment.filename,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${attachment.size / 1024} KB",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        Spacer(Modifier.height(48.dp))
                    }
                }
            }
        }
    }

    @Composable
    private fun MetaItem(icon: ImageVector, text: String) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    @Composable
    private fun HtmlText(html: String, modifier: Modifier = Modifier) {
        val cleanText = remember(html) {
            html.replace(Regex("<br\\s*/?>"), "\n")
                .replace(Regex("<p>"), "")
                .replace(Regex("</p>"), "\n\n")
                .replace(Regex("&oacute;"), "ó")
                .replace(Regex("&ascii;"), " ")
                .replace(Regex("&nbsp;"), " ")
                .replace(Regex("&quot;"), "\"")
                .replace(Regex("&amp;"), "&")
                .replace(Regex("&lt;"), "<")
                .replace(Regex("&gt;"), ">")
                .replace(Regex("<[^>]*>"), "")
        }
        Text(
            text = cleanText.trim(),
            modifier = modifier,
            style = MaterialTheme.typography.bodyLarge,
            lineHeight = 26.sp
        )
    }
}
