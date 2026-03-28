package dev.akinom.isod

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.*
import platform.AVKit.*
import platform.CoreGraphics.*
import platform.CoreMedia.CMTimeMake
import platform.Foundation.*
import platform.QuartzCore.*
import platform.UIKit.*

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun VideoPlayer(modifier: Modifier, resourceName: String) {
    val player = remember(resourceName) {
        if (resourceName.startsWith("http://") || resourceName.startsWith("https://")) {
            val url = NSURL.URLWithString(resourceName)
            if (url != null) AVPlayer(uRL = url) else AVPlayer()
        } else {
            val bundle = NSBundle.mainBundle
            val name = resourceName.substringBeforeLast(".")
            val extension = resourceName.substringAfterLast(".")
            
            var path = bundle.pathForResource(name, extension)
            if (path == null) {
                // Try Compose Multiplatform resource path
                path = bundle.pathForResource(name, extension, "compose-resources/drawable")
            }
            
            if (path == null) {
                AVPlayer()
            } else {
                val url = NSURL.fileURLWithPath(path)
                AVPlayer(uRL = url)
            }
        }
    }

    val playerLayer = remember(player) {
        AVPlayerLayer.playerLayerWithPlayer(player).apply {
            videoGravity = AVLayerVideoGravityResizeAspectFill
        }
    }

    val viewController = remember(playerLayer) {
        UIViewController().apply {
            view.layer.addSublayer(playerLayer)
        }
    }

    DisposableEffect(player) {
        player.actionAtItemEnd = AVPlayerActionAtItemEndNone
        
        val notificationObserver = NSNotificationCenter.defaultCenter.addObserverForName(
            name = AVPlayerItemDidPlayToEndTimeNotification,
            `object` = player.currentItem,
            queue = NSOperationQueue.mainQueue
        ) { _ ->
            player.seekToTime(CMTimeMake(0, 1))
            player.play()
        }
        
        player.play()
        
        onDispose {
            NSNotificationCenter.defaultCenter.removeObserver(notificationObserver)
            player.pause()
        }
    }

    UIKitView(
        factory = {
            viewController.view
        },
        modifier = modifier,
        update = { view ->
            CATransaction.begin()
            CATransaction.setDisableActions(true)
            playerLayer.frame = view.bounds
            CATransaction.commit()
        }
    )
}
