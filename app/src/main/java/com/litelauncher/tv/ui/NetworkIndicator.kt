package com.litelauncher.tv.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.litelauncher.tv.data.NetworkKind
import com.litelauncher.tv.data.NetworkStatus

/** Wi-Fi fan with 1-3 lit arcs; a short label for Ethernet or no connection. */
@Composable
fun NetworkIndicator(status: NetworkStatus, modifier: Modifier = Modifier) {
    val dim = Color.White.copy(alpha = 0.3f)
    val label = when (status.kind) {
        NetworkKind.Offline -> "Offline"
        NetworkKind.Ethernet -> "Ethernet"
        NetworkKind.Other -> "Online"
        NetworkKind.Wifi -> null
    }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        if (status.kind == NetworkKind.Wifi || status.kind == NetworkKind.Offline) {
            val lit = if (status.kind == NetworkKind.Wifi) status.level else 0
            Canvas(Modifier.size(22.dp)) {
                val origin = Offset(size.width / 2f, size.height * 0.86f)
                val stroke = size.width * 0.11f
                val maxRadius = size.height * 0.74f
                for (arc in 1..3) {
                    val radius = maxRadius * arc / 3f
                    drawArc(
                        color = if (arc <= lit) Color.White else dim,
                        startAngle = 225f,
                        sweepAngle = 90f,
                        useCenter = false,
                        topLeft = Offset(origin.x - radius, origin.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                }
                drawCircle(
                    color = if (lit > 0) Color.White else dim,
                    radius = stroke * 0.75f,
                    center = origin,
                )
            }
        }
        if (label != null) {
            if (status.kind == NetworkKind.Offline) Spacer(Modifier.width(8.dp))
            Text(text = label, fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
        }
    }
}
