package dev.akinom.isod

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun VideoPlayer(modifier: Modifier, resourceName: String)
