package dev.akinom.isod

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen

class SkibidiToilet(private val isVisible: Boolean = true) : Screen {

    @Composable
    override fun Content() {
        val infiniteTransition = rememberInfiniteTransition()
        val angle by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(3000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        )

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isVisible) {
                VideoPlayer(
                    modifier = Modifier,
                    resourceName = "https://akinom.dev/papa.mp4"
                )
            }
            Text(
                text = "WESOŁEGO JAJKA RZYCZY WRS",
                color = Color.Yellow,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.graphicsLayer {
                    rotationZ = angle
                    rotationY = angle/20
                }
            )
        }
    }
}
