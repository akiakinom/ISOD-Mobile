package dev.akinom.isod.news

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import dev.akinom.isod.HomeScreenModel
import dev.akinom.isod.Res
import dev.akinom.isod.*
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource

class NewsScreen(val semester: String = currentSemester()) : Screen {

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel { HomeScreenModel(semester) }
        val news by screenModel.news.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        
        var isRefreshing by remember { mutableStateOf(false) }
        var selectedFilters by remember { mutableStateOf(setOf("All")) }
        
        val filters = remember(news) {
            val availableTypes = news.map { it.type }.distinct()
            val typeFilters = NewsType.entries
                .filter { it in availableTypes && it != NewsType.OTHER }
                .map { it.name }
            
            val availableTags = news.map { it.label }.filter { it.isNotEmpty() }.distinct()
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

        val (groupedNews, displayedCount) = remember(news, selectedFilters) {
            val filtered = if ("All" in selectedFilters) {
                news
            } else {
                news.filter { item ->
                    val typeName = item.type.name
                    typeName in selectedFilters || (item.label.isNotEmpty() && item.label in selectedFilters)
                }
            }
            
            val grouped = filtered.sortedByDescending { it.parseDateToSortable() }
                .groupBy { it.date?.date ?: LocalDate(1970, 1, 1) }
            
            grouped to filtered.size
        }

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(stringResource(Res.string.news_feed_title), fontWeight = FontWeight.Bold) },
                )
            }
        ) { paddingValues ->
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    scope.launch {
                        isRefreshing = true
                        screenModel.refresh()
                        isRefreshing = false
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (filters.size > 1) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filters) { filter ->
                                val isSelected = filter in selectedFilters
                                val label = when {
                                    filter == "All" -> stringResource(Res.string.filter_all)
                                    filter in NewsType.entries.map { it.name } -> {
                                        NewsType.valueOf(filter).toLabel()
                                    }
                                    else -> filter
                                }
                                
                                val chipIcon = when {
                                    filter == "All" -> Icons.Default.Tune
                                    filter in NewsType.entries.map { it.name } -> NewsType.valueOf(filter).toIcon()
                                    else -> Icons.Default.LocalOffer
                                }
                                
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onFilterToggle(filter) },
                                    label = { Text(label) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (isSelected) Icons.Default.Done else chipIcon,
                                            contentDescription = null,
                                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                                        )
                                    },
                                    shape = MaterialTheme.shapes.medium
                                )
                            }
                        }
                    }

                    if (groupedNews.isEmpty()) {
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
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            groupedNews.forEach { (date, items) ->
                                item(key = date) {
                                    NewsDateHeader(date)
                                }
                                items(items, key = { it.id }) { item ->
                                    NewsLogItem(item) {
                                        navigator.push(NewsDetailScreen(item.id))
                                    }
                                }
                            }
                            
                            if (displayedCount > 20) {
                                item {
                                    Text(
                                        text = "🥚",
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        style = MaterialTheme.typography.displayLarge,
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NewsDateHeader(date: LocalDate) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        HorizontalDivider(
            modifier = Modifier.width(32.dp - 4.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )
        Text(
            text = date.formatHeader(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.outline
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@Composable
private fun NewsLogItem(item: NewsHeader, onClick: () -> Unit) {
    val typeColor = item.type.toColor()
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Icon / Timeline indicator
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(typeColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.type.toIcon(),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = typeColor
                )
            }
        }
        
        Spacer(Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = item.date?.formatTimeOnly() ?: "--:--",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                
                if (item.label.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    val (tagContainer, tagContent) = getTagColors(item.label)
                    Surface(
                        color = tagContainer,
                        contentColor = tagContent,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(2.dp))

            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Text(
                text = item.author,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
