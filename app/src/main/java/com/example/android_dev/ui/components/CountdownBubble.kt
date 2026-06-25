package com.example.android_dev.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.android_dev.domain.Countdown
import java.time.LocalDate
import java.time.temporal.ChronoUnit

// 倒计时气泡进度环功能（对标 radial countdown chip）：
// 外圈弧表示「从创建日到目标日」已流逝的时间进度（越接近目标越满），圆心显示剩余天数，
// 颜色随紧迫度由绿→橙→红渐变；轻触有放大回弹动画。数据来自现有 Countdown 模型。
@Composable
fun CountdownBubble(
    countdown: Countdown,
    today: LocalDate = LocalDate.now(),
    onClick: () -> Unit = {}
) {
    val daysLeft = ChronoUnit.DAYS.between(today, countdown.targetDate).toInt()
    val createdDate = remember(countdown.createdAt) {
        java.time.Instant.ofEpochMilli(countdown.createdAt)
            .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
    }
    val totalDays = ChronoUnit.DAYS.between(createdDate, countdown.targetDate).toInt().coerceAtLeast(1)
    val elapsed = ChronoUnit.DAYS.between(createdDate, today).toInt().coerceIn(0, totalDays)
    // 进度：已流逝 / 总跨度。越接近目标越满。
    val progress = (elapsed.toFloat() / totalDays).coerceIn(0f, 1f)

    // 紧迫度配色：剩余天数越少越偏红。
    val calm = Color(0xFF2F7D57)   // 绿：从容
    val mid = Color(0xFFC06A24)    // 橙：临近
    val hot = Color(0xFFB13E4B)    // 红：紧迫
    val urgency = when {
        daysLeft <= 1 -> 1f
        daysLeft >= 14 -> 0f
        else -> 1f - (daysLeft - 1) / 13f
    }
    val ringColor = if (urgency < 0.5f) lerp(calm, mid, urgency * 2f) else lerp(mid, hot, (urgency - 0.5f) * 2f)

    val animatedProgress by animateFloatAsState(progress, tween(700), label = "cdProgress")
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 1.08f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "cdScale"
    )

    Column(
        modifier = Modifier
            .scale(scale)
            .clickable(interactionSource = interaction, indication = null) { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(64.dp)) {
            val track = MaterialTheme.colorScheme.surfaceVariant
            Canvas(modifier = Modifier.size(64.dp)) {
                val stroke = 6.dp.toPx()
                val inset = stroke / 2
                val arcSize = Size(size.width - stroke, size.height - stroke)
                val topLeft = Offset(inset, inset)
                drawArc(track, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                drawArc(
                    ringColor, -90f, 360f * animatedProgress, false, topLeft, arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (daysLeft <= 0) "今天" else "$daysLeft",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ringColor
                )
                if (daysLeft > 0) {
                    Text("天", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Text(
            countdown.title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}
