package com.example.android_dev.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.android_dev.domain.TaskCategory
import kotlin.math.sin

// 单个活力环的数据：进度 0~1、颜色、图例文案。
data class ActivityRing(
    val progress: Float,
    val color: Color,
    val label: String,
    val value: String
)

// 分类时间分布的一段：分类 + 占比（0~1）。
data class CategorySlice(
    val category: TaskCategory,
    val fraction: Float
)

// 三重活力环卡片功能（对标 Apple Fitness 同心环）：
// 把「完成率 / 专注 / 连续」三个维度叠成三圈同心环，配动画填充 + 右侧图例，
// 底部再加一条分类时间分布堆叠条。所有数据来自现有 UiState，不改引擎。
@Composable
fun ActivityRingsCard(
    dateLine: String,
    headline: String,
    rings: List<ActivityRing>,
    distribution: List<CategorySlice>,
    categoryTint: (TaskCategory) -> Color,
    flowIntensity: Float = 0.4f
) {
    val flowColor = MaterialTheme.colorScheme.primary
    ElevatedCard(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box {
            // 环境流动水纹：卡片背景的持续流动条带（对标 Passage of Water / Kaspersky 的有机流动感）。
            FlowField(
                color = flowColor,
                intensity = flowIntensity,
                modifier = Modifier.matchParentSize()
            )
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 左：同心三环。
                ConcentricRings(
                    rings = rings,
                    modifier = Modifier.size(116.dp)
                )
                androidx.compose.foundation.layout.Spacer(Modifier.size(18.dp))
                // 右：日期 + 标题 + 三行图例。
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        dateLine,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        headline,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    androidx.compose.foundation.layout.Spacer(Modifier.size(2.dp))
                    rings.forEach { ring -> RingLegend(ring) }
                }
            }

            // 底部分类时间分布堆叠条。
            if (distribution.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "今日时间分布",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    DistributionBar(distribution = distribution, categoryTint = categoryTint)
                    // 图例：取占比最高的前 4 类。
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        distribution.sortedByDescending { it.fraction }.take(4).forEach { slice ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(categoryTint(slice.category))
                                )
                                Text(
                                    slice.category.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            }
        }
    }
}

// 单条图例：色点 + 名称 + 数值。
@Composable
private fun RingLegend(ring: ActivityRing) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(ring.color)
        )
        Text(
            ring.label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            ring.value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// 同心环绘制功能：从外到内每圈一个维度，先画淡轨道再画进度弧（12 点方向起，顺时针），带填充动画。
@Composable
private fun ConcentricRings(rings: List<ActivityRing>, modifier: Modifier = Modifier) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    // 每个环的动画进度。
    val animated = rings.map { ring ->
        animateFloatAsState(
            targetValue = ring.progress.coerceIn(0f, 1f),
            animationSpec = tween(900),
            label = "ring-${ring.label}"
        )
    }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(116.dp)) {
            val stroke = 12.dp.toPx()
            val gap = 5.dp.toPx()
            rings.forEachIndexed { index, ring ->
                // 外圈半径最大，向内逐圈缩小。
                val inset = stroke / 2 + index * (stroke + gap)
                val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
                val topLeft = Offset(inset, inset)
                if (arcSize.minDimension <= 0f) return@forEachIndexed
                drawArc(
                    color = trackColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                drawArc(
                    color = ring.color,
                    startAngle = -90f,
                    sweepAngle = 360f * animated[index].value,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }
    }
}

// 分类分布条绘制功能：一条圆角横条，按各分类占比依次填充对应颜色。
@Composable
private fun DistributionBar(
    distribution: List<CategorySlice>,
    categoryTint: (TaskCategory) -> Color
) {
    val total = distribution.sumOf { it.fraction.toDouble() }.toFloat().coerceAtLeast(0.0001f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        distribution.forEach { slice ->
            val weight = (slice.fraction / total).coerceIn(0.0001f, 1f)
            Box(
                modifier = Modifier
                    .weight(weight)
                    .height(10.dp)
                    .background(categoryTint(slice.category))
            )
        }
    }
}

// 环境流动水纹功能（对标 Google「Passage of Water」/ Kaspersky 实时图的有机流动感）：
// 画若干条横向正弦波带，用无限循环动画让相位持续平移，形成缓慢流动的水纹。
// intensity(0~1) 决定条带数量与流速 —— 任务越多，背景越「活跃」。整体低透明度，不干扰前景读字。
@Composable
private fun FlowField(
    color: Color,
    intensity: Float,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "flow")
    // 相位 0~2π 循环平移；intensity 越高，一个周期越短（流得越快）。
    val durationMillis = (9000 - (intensity.coerceIn(0f, 1f) * 4000)).toInt()
    val travel by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "flowTravel"
    )
    // 条带数量随强度在 3~6 之间。
    val streams = (3 + (intensity.coerceIn(0f, 1f) * 3)).toInt().coerceIn(3, 6)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas
        val amplitude = h * 0.10f
        val waveLength = w * 0.72f
        val shift = travel * waveLength
        val step = (waveLength / 22f).coerceAtLeast(8f)
        for (i in 0 until streams) {
            // 每条带的基准高度均匀分布，相位各自错开，营造层叠流动。
            val baseY = h * (0.18f + 0.64f * (i / (streams - 1f).coerceAtLeast(1f)))
            val streamPhase = i * 0.9f
            val alpha = 0.05f + 0.05f * (1f - i / streams.toFloat())
            val path = Path().apply {
                var x = -waveLength
                var firstPoint = true
                while (x <= w + waveLength) {
                    val normalized = ((x + shift) / waveLength) * 2f * Math.PI.toFloat()
                    val y = baseY +
                        sin(normalized + streamPhase) * amplitude +
                        sin(normalized * 2f + streamPhase * 1.4f) * (amplitude * 0.35f)
                    if (firstPoint) {
                        moveTo(x, y)
                        firstPoint = false
                    } else {
                        lineTo(x, y)
                    }
                    x += step
                }
            }
            drawPath(
                path = path,
                color = color.copy(alpha = alpha),
                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}
