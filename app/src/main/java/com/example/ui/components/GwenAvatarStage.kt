package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AvatarMode
import com.example.data.model.GwenExpression
import com.example.ui.theme.GwenAuburn
import com.example.ui.theme.GwenCyan
import com.example.ui.theme.GwenDanger
import com.example.ui.theme.GwenElectricBlue
import com.example.ui.theme.GwenObsidian
import com.example.ui.theme.GwenSuccess
import com.example.ui.theme.GwenTextPrimary
import com.example.ui.theme.GwenViolet
import com.example.ui.theme.GwenWarning
import kotlin.math.sin

@Composable
fun GwenAvatarStage(
    avatarMode: AvatarMode,
    expression: GwenExpression,
    isSpeaking: Boolean,
    isListening: Boolean,
    rmsLevel: Float,
    lightweightMode: Boolean,
    size: Dp = 220.dp,
    modifier: Modifier = Modifier
) {
    val auraColor by animateColorAsState(
        targetValue = when (expression) {
            GwenExpression.HAPPY -> GwenCyan
            GwenExpression.THINKING -> GwenViolet
            GwenExpression.LISTENING -> GwenElectricBlue
            GwenExpression.SPEAKING -> GwenCyan
            GwenExpression.SERIOUS -> Color(0xFF5E60CE)
            GwenExpression.ANGRY -> GwenDanger
            GwenExpression.LAUGHING -> GwenSuccess
            GwenExpression.SURPRISED -> Color(0xFF72EFDD)
            GwenExpression.CONFUSED -> GwenWarning
            GwenExpression.WORRIED -> Color(0xFFF72585)
        },
        animationSpec = tween(260),
        label = "auraColor"
    )

    val infinite = rememberInfiniteTransition(label = "gwenAvatarMotion")
    val orbitAngle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when {
                    lightweightMode -> 18000
                    expression == GwenExpression.THINKING -> 4200
                    else -> 9500
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbitAngle"
    )

    val floatOffset by infinite.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatOffset"
    )

    val lipPulse by infinite.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(190, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "lipPulse"
    )

    val drawableRes = when (avatarMode) {
        AvatarMode.UPPER_BODY -> R.drawable.img_gwen_upper_body
        AvatarMode.FULL_BODY -> R.drawable.img_gwen_full_body
        AvatarMode.HOLOGRAM -> R.drawable.img_gwen_hologram
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .offset(y = if (lightweightMode) 0.dp else floatOffset.dp),
            contentAlignment = Alignment.Center
        ) {
            // Outer Holographic Energy Rings
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = this.size.minDimension / 2f
                val pulseBoost = if (isSpeaking) lipPulse * 10f else if (isListening) rmsLevel * 16f else 0f

                if (!lightweightMode) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                auraColor.copy(alpha = 0.28f),
                                GwenViolet.copy(alpha = 0.12f),
                                Color.Transparent
                            )
                        ),
                        radius = radius
                    )
                }

                rotate(orbitAngle) {
                    drawCircle(
                        color = auraColor.copy(alpha = 0.62f),
                        radius = (radius - 10f + pulseBoost * 0.4f).coerceAtMost(radius - 4f),
                        style = Stroke(
                            width = 2.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(28f, 16f), 0f)
                        )
                    )
                }

                rotate(-orbitAngle * 0.65f) {
                    drawCircle(
                        color = GwenAuburn.copy(alpha = 0.35f),
                        radius = radius - 20f,
                        style = Stroke(
                            width = 1.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 22f), 0f)
                        )
                    )
                }
            }

            // Gwen Character Art Frame
            val imageShape = if (avatarMode == AvatarMode.UPPER_BODY) CircleShape else RoundedCornerShape(28.dp)
            Box(
                modifier = Modifier
                    .size(size - 32.dp)
                    .clip(imageShape)
                    .border(2.dp, auraColor.copy(alpha = 0.78f), imageShape)
                    .background(GwenObsidian)
            ) {
                Image(
                    painter = painterResource(id = drawableRes),
                    contentDescription = stringResource(R.string.cd_gwen_avatar),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Holographic Scanline & Voice Lip-Sync Overlay
                if (avatarMode == AvatarMode.HOLOGRAM || isSpeaking || isListening) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        if (avatarMode == AvatarMode.HOLOGRAM && !lightweightMode) {
                            var y = 0f
                            while (y < this.size.height) {
                                drawLine(
                                    color = GwenCyan.copy(alpha = 0.12f),
                                    start = Offset(0f, y),
                                    end = Offset(this.size.width, y),
                                    strokeWidth = 1f
                                )
                                y += 9f
                            }
                        }

                        if (isSpeaking || isListening) {
                            val barCount = 7
                            val baseWidth = this.size.width * 0.34f
                            val startX = (this.size.width - baseWidth) / 2f
                            val step = baseWidth / barCount
                            val baseY = this.size.height * 0.86f
                            for (i in 0 until barCount) {
                                val amp = if (isSpeaking) {
                                    (0.35f + 0.65f * kotlin.math.abs(sin(lipPulse * 3.14f + i * 0.8f))) * 22f
                                } else {
                                    (0.3f + rmsLevel * 0.7f) * 20f
                                }
                                drawLine(
                                    color = auraColor.copy(alpha = 0.9f),
                                    start = Offset(startX + i * step + step / 2f, baseY - amp / 2f),
                                    end = Offset(startX + i * step + step / 2f, baseY + amp / 2f),
                                    strokeWidth = 4.5f
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Expression & Mode Status Tag
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(GwenObsidian.copy(alpha = 0.9f))
                .border(1.dp, auraColor.copy(alpha = 0.5f), RoundedCornerShape(50))
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(auraColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "${expression.label} • ${avatarMode.label}",
                style = MaterialTheme.typography.labelMedium,
                color = GwenTextPrimary
            )
        }
    }
}
