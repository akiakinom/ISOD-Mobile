package dev.akinom.isod.news

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import dev.akinom.isod.domain.NewsType
import org.jetbrains.compose.resources.stringResource
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class NewsDetailScreenModel(id: String) : ScreenModel, KoinComponent {
    private val repo: NewsRepository by inject()
    val item = repo.getNewsItem(id)
}

data class NewsDetailScreen(val id: String) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel { NewsDetailScreenModel(id) }
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
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
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
                                if (data.type != NewsType.OTHER) {
                                    Surface(
                                        color = typeColor.copy(alpha = 0.1f),
                                        contentColor = typeColor,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Icon(data.type.toIcon(), null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = data.type.toLabel(),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }
                                }

                                if (data.label.isNotEmpty()) {
                                    val (tagContainer, tagContent) = getTagColors(data.label)
                                    if (data.type != NewsType.OTHER) Spacer(Modifier.width(12.dp))
                                    Surface(
                                        color = tagContainer,
                                        contentColor = tagContent,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = data.label,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            if (data.type != NewsType.OTHER || data.label.isNotEmpty()) {
                                Spacer(Modifier.height(20.dp))
                            }

                            Row(verticalAlignment = Alignment.Top) {
                                Text(
                                    text = data.title,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Black,
                                    lineHeight = 36.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(Modifier.height(24.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                if (data.author.isNotEmpty()) {
                                    MetaItem(Icons.Default.Person, data.author)
                                }
                                MetaItem(Icons.Default.Schedule, data.date?.formatFriendly() ?: "")
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
        val entities = remember {
            mapOf(
                "&aacute;" to "á", "&Aacute;" to "Á",
                "&aogonek;" to "ą", "&Aogonek;" to "Ą",
                "&cacute;" to "ć", "&Cacute;" to "Ć",
                "&eogonek;" to "ę", "&Eogonek;" to "Ę",
                "&lstroke;" to "ł", "&Lstroke;" to "Ł",
                "&nacute;" to "ń", "&Nacute;" to "Ń",
                "&oacute;" to "ó", "&Oacute;" to "Ó",
                "&sacute;" to "ś", "&Sacute;" to "Ś",
                "&zacute;" to "ź", "&Zacute;" to "Ź",
                "&zdot;" to "ż", "&Zdot;" to "Ż",
                "&quot;" to "\"", "&amp;" to "&", "&lt;" to "<", "&gt;" to ">",
                "&nbsp;" to " ", "&ndash;" to "–", "&mdash;" to "—"
            )
        }

        val decoded = remember(html) {
            var result = html
                .replace(Regex("<br\\s*/?>"), "\n")
                .replace(Regex("<p.*?>"), "")
                .replace("</p>", "\n\n")
                .replace(Regex("<.*?>"), "")
                .trim()
            
            entities.forEach { (entity, char) ->
                result = result.replace(entity, char)
            }
            result
        }

        Text(
            text = decoded,
            style = MaterialTheme.typography.bodyLarge,
            lineHeight = 28.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = modifier
        )
    }
}
