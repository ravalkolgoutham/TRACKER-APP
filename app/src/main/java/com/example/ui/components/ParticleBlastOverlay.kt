package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.model.BlastEvent
import com.example.model.BlastType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private enum class ParticleShape {
    DIAMOND,
    STAR,
    SPARK_CROSS,
    GLOW_ORB,
    SHARD
}

private data class Particle(
    val initialXRatio: Float,
    val initialYRatio: Float,
    val angle: Float,
    val speed: Float,
    val size: Float,
    val shape: ParticleShape,
    val color: Color,
    val rotationSpeed: Float,
    val initialRotation: Float,
    val drag: Float,
    val gravityFactor: Float
)

@Composable
fun ParticleBlastOverlay(
    blastEvent: BlastEvent?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (blastEvent == null) return

    val animProgress = remember(blastEvent.id) { Animatable(0f) }
    var particles by remember(blastEvent.id) { mutableStateOf<List<Particle>>(emptyList()) }

    // Generate balanced random blast particles
    LaunchedEffect(blastEvent.id) {
        val count = 100
        val rnd = Random(blastEvent.id)
        val palette = listOf(
            Color(0xFFFFD700), // Brilliant Gold
            Color(0xFFF59E0B), // Vibrant Amber
            Color(0xFF6366F1), // Electric Indigo
            Color(0xFF38BDF8), // Sky Cyan
            Color(0xFF10B981), // Emerald Green
            Color(0xFFF43F5E), // Coral Rose
            Color(0xFFA855F7), // Neon Violet
            Color(0xFFFFFFFF)  // Sparkle White
        )

        val shapes = listOf(
            ParticleShape.DIAMOND,
            ParticleShape.STAR,
            ParticleShape.SPARK_CROSS,
            ParticleShape.GLOW_ORB,
            ParticleShape.SHARD
        )

        particles = (0 until count).map {
            // Radial distribution with upward-explosion bias
            val angle = rnd.nextFloat() * 2f * PI.toFloat()
            val speed = rnd.nextFloat() * 1400f + 500f
            val size = rnd.nextFloat() * 12f + 6f
            val color = palette[rnd.nextInt(palette.size)]
            val shape = shapes[rnd.nextInt(shapes.size)]
            val rotationSpeed = (rnd.nextFloat() * 1200f - 600f)
            val initialRotation = rnd.nextFloat() * 360f
            val drag = rnd.nextFloat() * 0.35f + 0.15f
            val gravity = rnd.nextFloat() * 0.6f + 0.7f

            Particle(
                initialXRatio = 0.5f + (rnd.nextFloat() * 0.08f - 0.04f),
                initialYRatio = 0.36f + (rnd.nextFloat() * 0.06f - 0.03f),
                angle = angle,
                speed = speed,
                size = size,
                shape = shape,
                color = color,
                rotationSpeed = rotationSpeed,
                initialRotation = initialRotation,
                drag = drag,
                gravityFactor = gravity
            )
        }

        // Animate blast from 0f to 1f over 2.6 seconds
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 2600,
                easing = LinearEasing
            )
        )
        onDismiss()
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .zIndex(200f)
            .background(Color.Black.copy(alpha = (0.75f * (1f - animProgress.value * 0.3f)).coerceIn(0f, 0.75f)))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onDismiss()
            }
            .testTag("particle_blast_overlay")
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val progress = animProgress.value
        val timeSec = progress * 2.6f

        // 1. Particle Blast Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = widthPx * 0.5f
            val centerY = heightPx * 0.36f

            // Shockwave Rings radiating outward
            if (progress < 0.7f) {
                val ringProgress = (progress / 0.7f).coerceIn(0f, 1f)
                val ringRadius1 = ringProgress * (widthPx * 0.65f)
                val ringAlpha1 = ((1f - ringProgress) * 0.85f).coerceIn(0f, 1f)

                drawCircle(
                    color = Color(0xFFFFD700).copy(alpha = ringAlpha1),
                    radius = ringRadius1,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = (4.dp.toPx() * (1f - ringProgress)).coerceAtLeast(1f))
                )

                if (ringProgress > 0.15f) {
                    val ringProgress2 = ((ringProgress - 0.15f) / 0.85f).coerceIn(0f, 1f)
                    val ringRadius2 = ringProgress2 * (widthPx * 0.5f)
                    val ringAlpha2 = ((1f - ringProgress2) * 0.6f).coerceIn(0f, 1f)

                    drawCircle(
                        color = Color(0xFF6366F1).copy(alpha = ringAlpha2),
                        radius = ringRadius2,
                        center = Offset(centerX, centerY),
                        style = Stroke(width = (2.5.dp.toPx() * (1f - ringProgress2)).coerceAtLeast(1f))
                    )
                }
            }

            // Central Core Light Flash
            if (progress < 0.25f) {
                val flashAlpha = ((1f - progress / 0.25f) * 0.9f).coerceIn(0f, 1f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFD700).copy(alpha = flashAlpha),
                            Color(0x886366F1).copy(alpha = flashAlpha * 0.6f),
                            Color.Transparent
                        ),
                        center = Offset(centerX, centerY),
                        radius = 180.dp.toPx()
                    ),
                    radius = 180.dp.toPx(),
                    center = Offset(centerX, centerY)
                )
            }

            // Draw individual particles
            for (p in particles) {
                val pTime = timeSec
                val originX = widthPx * p.initialXRatio
                val originY = heightPx * p.initialYRatio

                // Physics: Velocity with air resistance deceleration + Gravity
                val vx = cos(p.angle) * p.speed
                val vy = sin(p.angle) * p.speed
                val dragMultiplier = 1f / (1f + p.drag * pTime)

                val x = originX + vx * pTime * dragMultiplier
                val gravityAcc = 1100f * p.gravityFactor
                val y = originY + (vy * pTime * dragMultiplier) + (0.5f * gravityAcc * pTime * pTime)

                // Alpha & Scale Lifecycle
                val scale = when {
                    progress < 0.08f -> (progress / 0.08f).coerceIn(0f, 1f)
                    progress > 0.8f -> (1f - (progress - 0.8f) / 0.2f).coerceIn(0f, 1f)
                    else -> 1f
                }

                val alpha = when {
                    progress < 0.05f -> (progress / 0.05f).coerceIn(0f, 1f)
                    progress > 0.6f -> {
                        val decay = (1f - (progress - 0.6f) / 0.4f).coerceIn(0f, 1f)
                        decay * decay
                    }
                    else -> 1f
                }

                if (alpha <= 0.01f || scale <= 0.01f) continue

                val currentRotation = p.initialRotation + p.rotationSpeed * pTime
                val particleColor = p.color.copy(alpha = (p.color.alpha * alpha).coerceIn(0f, 1f))
                val currentSize = p.size * scale

                rotate(degrees = currentRotation, pivot = Offset(x, y)) {
                    drawParticleShape(
                        shape = p.shape,
                        color = particleColor,
                        center = Offset(x, y),
                        sizePx = currentSize
                    )
                }
            }
        }

        // 2. Celebratory Milestone Banner Card
        AnimatedVisibility(
            visible = progress > 0.06f && progress < 0.95f,
            enter = scaleIn(
                initialScale = 0.65f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) + fadeIn(animationSpec = tween(200)),
            exit = scaleOut(
                targetScale = 0.85f,
                animationSpec = tween(250, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(250)),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp)
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 440.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .border(
                        1.5.dp, 
                        Brush.linearGradient(listOf(Color(0xFFFFD700), Color(0xFF6366F1), Color(0xFF38BDF8))), 
                        RoundedCornerShape(28.dp)
                    )
                    .testTag("blast_celebration_card"),
                color = Color(0xFF0F172A),
                shape = RoundedCornerShape(28.dp),
                tonalElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Top Icon Badge with Glow
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    when (blastEvent.type) {
                                        BlastType.STREAK_GOAL -> listOf(Color(0xFFF59E0B), Color(0xFFEF4444))
                                        BlastType.LEVEL_UP -> listOf(Color(0xFFFFD700), Color(0xFFF59E0B))
                                        BlastType.GOAL_COMPLETED -> listOf(Color(0xFF10B981), Color(0xFF06B6D4))
                                        BlastType.MILESTONE -> listOf(Color(0xFF6366F1), Color(0xFFA855F7))
                                    }
                                )
                            )
                            .padding(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B)),
                            contentAlignment = Alignment.Center
                        ) {
                            val icon = when (blastEvent.type) {
                                BlastType.STREAK_GOAL -> Icons.Default.LocalFireDepartment
                                BlastType.LEVEL_UP -> Icons.Default.EmojiEvents
                                BlastType.GOAL_COMPLETED -> Icons.Default.CheckCircle
                                BlastType.MILESTONE -> Icons.Default.Stars
                            }
                            val iconTint = when (blastEvent.type) {
                                BlastType.STREAK_GOAL -> Color(0xFFF59E0B)
                                BlastType.LEVEL_UP -> Color(0xFFFFD700)
                                BlastType.GOAL_COMPLETED -> Color(0xFF10B981)
                                BlastType.MILESTONE -> Color(0xFF818CF8)
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Pill Badge
                    if (blastEvent.badgeText.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF6366F1).copy(alpha = 0.2f))
                                .border(1.dp, Color(0xFF6366F1).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = blastEvent.badgeText,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF818CF8),
                                letterSpacing = 0.5.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Main Title
                    Text(
                        text = blastEvent.title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = (-0.5).sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Subtitle / Description
                    Text(
                        text = blastEvent.subtitle,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFFCBD5E1),
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Dismiss Prompt Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF1E293B))
                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
                            .clickable { onDismiss() }
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Tap anywhere to continue",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawParticleShape(
    shape: ParticleShape,
    color: Color,
    center: Offset,
    sizePx: Float
) {
    val half = sizePx / 2f
    when (shape) {
        ParticleShape.GLOW_ORB -> {
            drawCircle(
                color = color.copy(alpha = (color.alpha * 0.4f).coerceIn(0f, 1f)),
                radius = sizePx * 0.8f,
                center = center
            )
            drawCircle(
                color = color,
                radius = half,
                center = center
            )
        }

        ParticleShape.DIAMOND -> {
            val path = Path().apply {
                moveTo(center.x, center.y - half * 1.3f)
                lineTo(center.x + half, center.y)
                lineTo(center.x, center.y + half * 1.3f)
                lineTo(center.x - half, center.y)
                close()
            }
            drawPath(path = path, color = color)
        }

        ParticleShape.STAR -> {
            val path = Path().apply {
                val outer = half * 1.4f
                val inner = half * 0.35f
                moveTo(center.x, center.y - outer)
                lineTo(center.x + inner, center.y - inner)
                lineTo(center.x + outer, center.y)
                lineTo(center.x + inner, center.y + inner)
                lineTo(center.x, center.y + outer)
                lineTo(center.x - inner, center.y + inner)
                lineTo(center.x - outer, center.y)
                lineTo(center.x - inner, center.y - inner)
                close()
            }
            drawPath(path = path, color = color)
        }

        ParticleShape.SPARK_CROSS -> {
            val strokeW = (sizePx * 0.22f).coerceAtLeast(1.5f)
            // Vertical line
            drawLine(
                color = color,
                start = Offset(center.x, center.y - half * 1.5f),
                end = Offset(center.x, center.y + half * 1.5f),
                strokeWidth = strokeW
            )
            // Horizontal line
            drawLine(
                color = color,
                start = Offset(center.x - half * 1.5f, center.y),
                end = Offset(center.x + half * 1.5f, center.y),
                strokeWidth = strokeW
            )
            // Center sparkle core
            drawCircle(color = Color.White.copy(alpha = color.alpha), radius = strokeW, center = center)
        }

        ParticleShape.SHARD -> {
            drawRoundRect(
                color = color,
                topLeft = Offset(center.x - half * 0.5f, center.y - half * 1.4f),
                size = Size(half, sizePx * 1.4f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
            )
        }
    }
}
