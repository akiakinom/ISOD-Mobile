package dev.akinom.isod

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@Composable
actual fun VideoPlayer(modifier: Modifier, resourceName: String) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ALL
        }
    }

    LaunchedEffect(resourceName) {
        val mediaUri = if (resourceName.startsWith("http")) {
            resourceName
        } else {
            val nameWithoutExtension = resourceName.substringBeforeLast(".")
            
            val rawResourceId = context.resources.getIdentifier(
                nameWithoutExtension,
                "raw",
                context.packageName
            )
            val drawableResourceId = context.resources.getIdentifier(
                nameWithoutExtension,
                "drawable",
                context.packageName
            )
            
            val finalResourceId = if (rawResourceId != 0) rawResourceId else drawableResourceId
            
            if (finalResourceId != 0) {
                "android.resource://${context.packageName}/$finalResourceId"
            } else {
                // Fallback for Compose Multiplatform resources located in assets
                "asset:///compose-resources/drawable/$resourceName"
            }
        }
        
        val mediaItem = MediaItem.fromUri(mediaUri)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    exoPlayer.pause()
                }
                Lifecycle.Event.ON_RESUME -> {
                    exoPlayer.play()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = {
            PlayerView(it).apply {
                player = exoPlayer
                useController = false
                resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            }
        },
        modifier = modifier
    )
}
