package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConnectionStatus
import com.example.ui.theme.*

@Composable
fun StatusBadge(
    status: ConnectionStatus,
    modifier: Modifier = Modifier
) {
    val (color, text) = when (status) {
        ConnectionStatus.CONNECTED -> Pair(StatusConnected, "CONNECTED")
        ConnectionStatus.CONNECTING -> Pair(StatusConnecting, "CONNECTING...")
        ConnectionStatus.SEARCHING -> Pair(StatusTransferring, "SEARCHING...")
        ConnectionStatus.RECONNECTING -> Pair(StatusConnecting, "RECONNECTING...")
        ConnectionStatus.ERROR -> Pair(StatusDisconnected, "ERROR")
        ConnectionStatus.DISCONNECTED -> Pair(TextTertiary, "STANDBY")
    }

    // Pulse animation for active states
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(
                    if (status == ConnectionStatus.CONNECTING || status == ConnectionStatus.SEARCHING)
                        color.copy(alpha = alpha)
                    else color
                )
        )
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}
