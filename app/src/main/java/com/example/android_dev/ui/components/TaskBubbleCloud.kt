package com.example.android_dev.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.android_dev.domain.SmartTask
import kotlinx.coroutines.launch

// 单个气泡的运行时状态：归位坐标(home) + 当前动画坐标(pos) + 半径(px)。
private class BubbleState(
    val task: SmartTask,
    var homeX: Float,
    var homeY: Float,
    val radiusPx: Float,
    val pos: Animatable<Offset, *>
)

// 任务气泡云功能（对标 packed bubble / force layout + 拖放交互）：
// 把「今天」任务渲染成一片可拖动的气泡。气泡大小由预估时长决定、颜色由优先级决定；
// 轻点气泡 → 展开详情卡（含完成/删除按钮）；拖动气泡到顶部「✓ 完成」区 → 完成；
// 拖出屏幕边界 → 删除；松手在其它位置则弹性回到原位。越界/触发时伴随轻触觉。
@Composable
fun TaskBubbleCloud(
    tasks: List<SmartTask>,
    onComplete: (SmartTask) -> Unit,
    onDelete: (SmartTask) -> Unit,
    onEdit: (SmartTask) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    var expandedTask by remember { mutableStateOf<SmartTask?>(null) }

    // 画布固定高度，气泡在其中布局。
    val canvasHeightDp = 320.dp
    val canvasHeightPx = with(density) { canvasHeightDp.toPx() }
    var canvasWidthPx by remember { mutableStateOf(1f) }
    // 完成区高度（顶部）。
    val dropZoneHeightPx = with(density) { 56.dp.toPx() }

    val primary = MaterialTheme.colorScheme.primary

    Column(modifier = modifier.fillMaxWidth()) {
        // 顶部完成投放区。
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            color = primary.copy(alpha = 0.08f),
            border = androidx.compose.foundation.BorderStroke(1.dp, primary.copy(alpha = 0.30f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    "拖到这里 ✓ 完成 · 拖出边缘 🗑 删除",
                    style = MaterialTheme.typography.labelMedium,
                    color = primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(canvasHeightDp)
                .onSizeChangedPx { canvasWidthPx = it }
        ) {
            if (canvasWidthPx <= 1f) return@Box

            // 为每个任务建立气泡状态：按网格初始布点，半径由预估时长映射到 26~46dp。
            val bubbles = remember(tasks, canvasWidthPx) {
                buildBubbles(tasks, canvasWidthPx, canvasHeightPx, dropZoneHeightPx, density)
            }

            bubbles.forEach { bubble ->
                BubbleItem(
                    bubble = bubble,
                    canvasWidthPx = canvasWidthPx,
                    canvasHeightPx = canvasHeightPx,
                    dropZoneHeightPx = dropZoneHeightPx,
                    onTap = { expandedTask = bubble.task },
                    onComplete = {
                        view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
                        onComplete(bubble.task)
                    },
                    onDelete = {
                        view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                        onDelete(bubble.task)
                    },
                    scope = scope
                )
            }

            if (tasks.isEmpty()) {
                Text(
                    "今天没有待办，享受片刻轻松 🎉",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // 展开详情卡：轻点气泡后浮出，提供完成/编辑/删除。
    expandedTask?.let { task ->
        BubbleDetailSheet(
            task = task,
            onDismiss = { expandedTask = null },
            onComplete = { onComplete(task); expandedTask = null },
            onDelete = { onDelete(task); expandedTask = null },
            onEdit = { onEdit(task); expandedTask = null }
        )
    }
}

@Composable
private fun BubbleItem(
    bubble: BubbleState,
    canvasWidthPx: Float,
    canvasHeightPx: Float,
    dropZoneHeightPx: Float,
    onTap: () -> Unit,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
    scope: kotlinx.coroutines.CoroutineScope
) {
    val density = LocalDensity.current
    // 漂浮微动：每个气泡用各自相位做轻微上下浮动，营造「活」的感觉。
    val floatTransition = rememberInfiniteTransition(label = "float")
    val floatY by floatTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(
                durationMillis = 2200 + (bubble.task.id.hashCode() % 800),
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatY"
    )
    var dragging by remember { mutableStateOf(false) }
    val diameterDp = with(density) { (bubble.radiusPx * 2).toDp() }

    Box(
        modifier = Modifier
            .graphicsLayer {
                translationX = bubble.pos.value.x
                translationY = bubble.pos.value.y + if (dragging) 0f else floatY
                if (dragging) {
                    scaleX = 1.12f; scaleY = 1.12f
                }
            }
            .size(diameterDp)
            .clip(CircleShape)
            .background(bubble.task.priority.tint().copy(alpha = if (dragging) 0.95f else 0.85f))
            .pointerInput(bubble) {
                detectTapGestures(onTap = { onTap() })
            }
            .pointerInput(bubble) {
                detectDragGestures(
                    onDragStart = { dragging = true },
                    onDragEnd = {
                        dragging = false
                        val center = bubble.pos.value
                        val cx = center.x + bubble.radiusPx
                        val cy = center.y + bubble.radiusPx
                        when {
                            // 拖到顶部完成区。
                            cy <= dropZoneHeightPx -> onComplete()
                            // 拖出左右或底部边界 → 删除。
                            cx < 0f || cx > canvasWidthPx || cy > canvasHeightPx -> onDelete()
                            // 否则弹性回到原位。
                            else -> scope.launch {
                                bubble.pos.animateTo(
                                    Offset(bubble.homeX, bubble.homeY),
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                                )
                            }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            bubble.pos.snapTo(bubble.pos.value + dragAmount)
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = bubble.task.title,
            modifier = Modifier.padding(6.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// 气泡详情卡：底部浮出的小卡，展示标题/元信息 + 完成/编辑/删除。
@Composable
private fun BubbleDetailSheet(
    task: SmartTask,
    onDismiss: () -> Unit,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(task.priority.tint())
                    )
                    Text(task.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Text(
                    "${task.category.label} · 优先级 ${task.priority.label} · 约 ${task.estimatedMinutes} 分" +
                        (task.dueDate?.let { " · 截止 $it" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (task.description.isNotBlank()) {
                    Text(task.description, style = MaterialTheme.typography.bodyMedium)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDelete) { Text("删除") }
                    TextButton(onClick = onEdit) { Text("编辑") }
                    TextButton(onClick = onComplete) { Text("✓ 完成") }
                }
            }
        }
    }
}

// 气泡布点功能：按行网格初始布局，半径由预估时长映射；超出可用宽度自动换行。
private fun buildBubbles(
    tasks: List<SmartTask>,
    widthPx: Float,
    heightPx: Float,
    topInsetPx: Float,
    density: androidx.compose.ui.unit.Density
): List<BubbleState> {
    val minR = with(density) { 26.dp.toPx() }
    val maxR = with(density) { 46.dp.toPx() }
    val gap = with(density) { 10.dp.toPx() }
    var cursorX = gap
    var rowY = topInsetPx + gap
    var rowMaxR = 0f
    return tasks.map { task ->
        val r = (minR + (task.estimatedMinutes.coerceIn(10, 120) / 120f) * (maxR - minR))
        if (cursorX + r * 2 + gap > widthPx) {
            cursorX = gap
            rowY += rowMaxR * 2 + gap
            rowMaxR = 0f
        }
        val x = cursorX
        val y = rowY.coerceAtMost(heightPx - r * 2 - gap)
        cursorX += r * 2 + gap
        rowMaxR = maxOf(rowMaxR, r)
        BubbleState(
            task = task,
            homeX = x,
            homeY = y,
            radiusPx = r,
            pos = Animatable(Offset(x, y), Offset.VectorConverter)
        )
    }
}

// 像素尺寸回调修饰符：只取宽度。
private fun Modifier.onSizeChangedPx(block: (Float) -> Unit): Modifier =
    this.onSizeChanged { block(it.width.toFloat().coerceAtLeast(1f)) }
