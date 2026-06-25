package com.example.android_dev.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import kotlin.math.abs
import kotlinx.coroutines.launch

// 可滑动任务行功能（对标 Todoist / iOS Mail 的滑动手势）：
// 右滑 → 完成（绿色 ✓ 背景渐显）；左滑 → 删除（红色背景）。露出一角作为可发现性信号；
// 超过阈值松手触发回调并伴随轻触觉，未过阈值则弹回原位。content 为前景行内容。
@Composable
fun SwipeableTaskRow(
    onComplete: () -> Unit,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val offsetX = remember { Animatable(0f) }
    var rowWidthPx by remember { mutableStateOf(1f) }
    // 触发阈值：滑过行宽的 32% 即判定。
    val thresholdFraction = 0.32f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
    ) {
        // 背景层：根据滑动方向显示「完成」或「删除」提示。
        val dragging = offsetX.value
        val bgColor = when {
            dragging > 0f -> MaterialTheme.colorScheme.primary
            dragging < 0f -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.surface
        }
        val progress = (abs(dragging) / (rowWidthPx * thresholdFraction)).coerceIn(0f, 1f)
        Row(
            modifier = Modifier
                .matchParentSize()
                .background(bgColor.copy(alpha = 0.10f + 0.25f * progress))
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (dragging > 0f) {
                Text("✓ 完成", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            } else if (dragging < 0f && onDelete != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
                    Text("删除", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        // 前景层：随手势水平位移；松手按阈值判定动作或回弹。
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = offsetX.value }
                .pointerInput(onDelete == null) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val threshold = rowWidthPx * thresholdFraction
                            when {
                                offsetX.value >= threshold -> {
                                    view.performHapticFeedback(
                                        android.view.HapticFeedbackConstants.CONFIRM
                                    )
                                    scope.launch {
                                        offsetX.animateTo(rowWidthPx)
                                        onComplete()
                                        offsetX.snapTo(0f)
                                    }
                                }
                                offsetX.value <= -threshold && onDelete != null -> {
                                    view.performHapticFeedback(
                                        android.view.HapticFeedbackConstants.LONG_PRESS
                                    )
                                    scope.launch {
                                        offsetX.animateTo(-rowWidthPx)
                                        onDelete()
                                        offsetX.snapTo(0f)
                                    }
                                }
                                else -> scope.launch { offsetX.animateTo(0f) }
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            val next = offsetX.value + dragAmount
                            // 不允许左滑超界（无删除回调时禁止左滑）。
                            val clamped = if (onDelete == null) next.coerceAtLeast(0f) else next
                            scope.launch { offsetX.snapTo(clamped) }
                        }
                    )
                }
                .onSizeChanged { rowWidthPx = it.width.toFloat().coerceAtLeast(1f) }
        ) {
            content()
        }
    }
}

// 完成勾选框功能（对标 Todoist 完成动画）：点击切换；完成时圆环填充主色 + 弹性回弹缩放，
// 对勾用 Canvas 描边画入。点击伴随轻触觉。
@Composable
fun CompletionCheckbox(
    checked: Boolean,
    tint: Color,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    // 选中时弹性放大回弹（spring 过冲）。
    val scale by animateFloatAsState(
        targetValue = if (checked) 1f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "cbScale"
    )
    // 对勾描边进度 0→1。
    val checkProgress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(durationMillis = 320),
        label = "checkDraw"
    )
    val ringColor = if (checked) tint else MaterialTheme.colorScheme.outline
    Box(
        modifier = modifier
            .size(24.dp)
            .scale(scale)
            .clickable {
                view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
                onToggle()
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(24.dp)) {
            val stroke = 2.2.dp.toPx()
            val r = (size.minDimension - stroke) / 2f
            val c = Offset(size.width / 2f, size.height / 2f)
            // 圆环 / 完成时实心填充。
            if (checked) {
                drawCircle(color = tint, radius = r, center = c)
            } else {
                drawCircle(color = ringColor, radius = r, center = c, style = Stroke(width = stroke))
            }
            // 对勾：两段折线按 checkProgress 逐步画出。
            if (checkProgress > 0f) {
                val p1 = Offset(c.x - r * 0.45f, c.y + r * 0.02f)
                val p2 = Offset(c.x - r * 0.10f, c.y + r * 0.38f)
                val p3 = Offset(c.x + r * 0.50f, c.y - r * 0.35f)
                val seg1 = checkProgress.coerceIn(0f, 0.5f) / 0.5f
                val seg2 = (checkProgress - 0.5f).coerceIn(0f, 0.5f) / 0.5f
                val mid = Offset(p1.x + (p2.x - p1.x) * seg1, p1.y + (p2.y - p1.y) * seg1)
                drawLine(Color.White, p1, mid, strokeWidth = stroke, cap = StrokeCap.Round)
                if (seg2 > 0f) {
                    val end = Offset(p2.x + (p3.x - p2.x) * seg2, p2.y + (p3.y - p2.y) * seg2)
                    drawLine(Color.White, p2, end, strokeWidth = stroke, cap = StrokeCap.Round)
                }
            }
        }
    }
}
