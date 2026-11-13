package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.GwenCyan
import com.example.ui.theme.GwenDeepBlack
import com.example.ui.theme.GwenElectricBlue
import com.example.ui.theme.GwenGlassBorder
import com.example.ui.theme.GwenObsidian
import com.example.ui.theme.GwenSurface
import com.example.ui.theme.GwenSurfaceElevated
import com.example.ui.theme.GwenTextMuted
import com.example.ui.theme.GwenTextPrimary
import com.example.ui.theme.GwenTextSecondary
import com.example.ui.theme.GwenViolet
import kotlin.math.sin

@Composable
fun GwenCosmicBackground(
    lightweightMode: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                if (lightweightMode) {
                    Brush.verticalGradient(listOf(GwenDeepBlack, GwenObsidian))
                } else {
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF151232),
                            Color(0xFF0A0D1A),
                            GwenDeepBlack
                        ),
                        radius = 1250f
                    )
                }
            )
    ) {
        content()
    }
}

@Composable
fun GwenGlassCard(
    modifier: Modifier = Modifier,
    accentColor: Color = GwenCyan,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    val cardModifier = modifier
        .clip(shape)
        .then(
            if (onClick != null) {
                Modifier
                    .minimumInteractiveComponentSize()
                    .clickable(onClick = onClick)
            } else {
                Modifier
            }
        )
    Surface(
        modifier = cardModifier,
        shape = shape,
        color = GwenSurface.copy(alpha = 0.86f),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                listOf(
                    accentColor.copy(alpha = 0.45f),
                    GwenGlassBorder.copy(alpha = 0.55f),
                    GwenViolet.copy(alpha = 0.35f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun GwenScreenTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(GwenSurfaceElevated.copy(alpha = 0.8f))
                    .border(1.dp, GwenGlassBorder, CircleShape)
                    .testTag("top_bar_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                    tint = GwenCyan
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = GwenTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = GwenCyan.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        }
    }
}

@Composable
fun GwenStatusPill(
    label: String,
    color: Color,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.45f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(13.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
fun VoiceWaveformVisualizer(
    rmsLevel: Float,
    isActive: Boolean,
    isSpeaking: Boolean,
    lightweightMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "waveform")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 6.283f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (lightweightMode) 1400 else 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp)
    ) {
        val barCount = 26
        val spacing = size.width / barCount
        val barWidth = (spacing * 0.54f).coerceAtLeast(3f)
        val centerY = size.height / 2f
        val energy = when {
            isActive -> rmsLevel.coerceIn(0.22f, 1f)
            isSpeaking -> 0.65f
            else -> 0.12f
        }

        for (i in 0 until barCount) {
            val wave = if (isActive || isSpeaking) {
                (0.45f + 0.55f * kotlin.math.abs(sin(phase + i * 0.45f)))
            } else {
                0.28f + 0.2f * kotlin.math.abs(sin(i * 0.5f))
            }
            val barHeight = (size.height * energy * wave).coerceIn(4f, size.height)
            val x = i * spacing + (spacing - barWidth) / 2f
            val barColor = when {
                isSpeaking -> GwenCyan
                isActive -> GwenElectricBlue
                else -> GwenViolet.copy(alpha = 0.5f)
            }
            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, centerY - barHeight / 2f),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

data class GwenNavDestination(
    val key: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun GwenBottomNavBar(
    currentKey: String,
    onSelect: (String) -> Unit
) {
    val items = listOf(
        GwenNavDestination("dashboard", "Home", Icons.Default.Home),
        GwenNavDestination("chat", "AI Chat", Icons.Default.ChatBubble),
        GwenNavDestination("control", "Control", Icons.Default.Devices),
        GwenNavDestination("studio", "Studio", Icons.Default.AutoAwesome),
        GwenNavDestination("settings", "Settings", Icons.Default.Settings)
    )

    Surface(
        color = GwenObsidian.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, GwenGlassBorder.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val selected = currentKey == item.key
                val tint = if (selected) GwenCyan else GwenTextMuted
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .minimumInteractiveComponentSize()
                        .clickable { onSelect(item.key) }
                        .background(if (selected) GwenCyan.copy(alpha = 0.14f) else Color.Transparent)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("nav_${item.key}"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = tint,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) GwenCyan else GwenTextSecondary,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
