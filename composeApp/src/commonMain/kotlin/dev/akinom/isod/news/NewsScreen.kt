package dev.akinom.isod.news

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.akinom.isod.auth.currentSemester
import dev.akinom.isod.domain.NewsHeader
import dev.akinom.isod.domain.NewsType
import dev.akinom.isod.domain.parseSubject
import dev.akinom.isod.HomeScreenModel
import dev.akinom.isod.Res
import dev.akinom.isod.*
import org.jetbrains.compose.resources.stringResource

class NewsScreen(val semester: String = currentSemester()) : Screen {

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel { HomeScreenModel(semester) }
        val news by screenModel.news.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        
        var selectedFilters by remember { mutableStateOf(setOf("All")) }
        
        val newsWithParsed = remember(news) {
            news.map { it to parseSubject(it.subject) }
        }

        val filters = remember(newsWithParsed) {
            val availableTypes = newsWithParsed.map { it.first.type }.distinct()
            val typeFilters = NewsType.entries
                .filter { it in availableTypes && it != NewsType.UNKNOWN }
                .map { it.name }
            
            val availableTags = newsWithParsed.mapNotNull { it.second.tag }.distinct()
            val tagFilters = availableTags.filter { it == "Dziekanat" || it == "WRS" }
            
            listOf("All") + typeFilters + tagFilters
        }

        val onFilterToggle: (String) -> Unit = { filter ->
            if (filter == "All") {
                selectedFilters = setOf("All")
            } else {
                val newFilters = selectedFilters.toMutableSet()
                if ("All" in newFilters) newFilters.remove("All")
                
                if (filter in newFilters) {
                    newFilters.remove(filter)
                } else {
                    newFilters.add(filter)
                }
                
                if (newFilters.isEmpty()) newFilters.add("All")
                selectedFilters = newFilters
            }
        }

        LaunchedEffect(filters) {
            val validFilters = selectedFilters.filter { it in filters || it == "All" }.toSet()
            if (validFilters.isEmpty()) {
                selectedFilters = setOf("All")
            } else if (validFilters != selectedFilters) {
                selectedFilters = validFilters
            }
        }

        val filteredNews = remember(newsWithParsed, selectedFilters) {
            val result = if ("All" in selectedFilters) {
                newsWithParsed.map { it.first }
            } else {
                newsWithParsed.filter { (item, parsed) ->
                    val typeName = item.type.name
                    typeName in selectedFilters || (parsed.tag != null && parsed.tag in selectedFilters)
                }.map { it.first }
            }
            result.sortedByDescending { it.parseDateToSortable() }
        }

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(stringResource(Res.string.news_feed_title), fontWeight = FontWeight.Bold) },
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                // Filter Chips Flow Grid
                if (filters.size > 1) {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        filters.forEach { filter ->
                            val isSelected = filter in selectedFilters
                            val label = when {
                                filter == "All" -> stringResource(Res.string.filter_all)
                                filter in NewsType.entries.map { it.name } -> {
                                    val type = NewsType.valueOf(filter)
                                    type.toStringRes()?.let { stringResource(it) } ?: filter
                                }
                                else -> filter
                            }
                            
                            FilterChip(
                                selected = isSelected,
                                onClick = { onFilterToggle(filter) },
                                label = { Text(label) },
                                leadingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Filled.Done,
                                            contentDescription = stringResource(Res.string.selected),
                                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                                        )
                                    }
                                } else null,
                                shape = MaterialTheme.shapes.medium
                            )
                        }
                    }
                }

                if (filteredNews.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.NotificationsNone,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                            Spacer(Modifier.height(16.dp))
                            val emptyText = if (news.isEmpty()) stringResource(Res.string.no_news_yet) else stringResource(Res.string.no_matching_news)
                            @Suppress("DEPRECATION")
                            Text(emptyText, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(filteredNews) { item ->
                            NewsCard(item) {
                                navigator.push(NewsDetailScreen(item.hash))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NewsCard(item: NewsHeader, onClick: () -> Unit) {
    val typeColor = item.type.toColor()
    val parsed = remember(item.subject) { parseSubject(item.subject) }
    
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp, 
            if (parsed.isGradeUpdate) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) 
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = typeColor.copy(alpha = 0.1f),
                        contentColor = typeColor,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        val typeText = item.type.toStringRes()?.let { stringResource(it) } 
                            ?: item.type.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
                            
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(item.toIcon(null), null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = typeText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    if (parsed.tag != null) {
                        val tagVal = parsed.tag!!
                        val (tagContainer, tagContent) = getTagColors(tagVal)
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = tagContainer,
                            contentColor = tagContent,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                if (tagVal.uppercase() == "WRS" || tagVal.uppercase() == "DZIEKANAT") {
                                    Icon(item.toIcon(tagVal), null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                }
                                Text(
                                    text = tagVal,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                
                if (item.noAttachments > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Attachment,
                            null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(4.dp))
                        @Suppress("DEPRECATION")
                        Text(
                            item.noAttachments.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = parsed.getDisplaySubject(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (parsed.isGradeUpdate) FontWeight.ExtraBold else FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 22.sp,
                color = if (parsed.isGradeUpdate) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                @Suppress("DEPRECATION")
                Text(
                    text = item.modifiedBy,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                @Suppress("DEPRECATION")
                Text(
                    text = item.modifiedDate.split(" ")[0],
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}
