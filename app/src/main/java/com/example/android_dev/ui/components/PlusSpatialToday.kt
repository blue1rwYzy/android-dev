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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.android_dev.domain.CognitiveSnapshot
import com.example.android_dev.domain.ScheduleSlot
import com.example.android_dev.domain.SmartTask
import com.example.android_dev.domain.TaskRecommendation
import com.example.android_dev.domain.UserCognitiveSignal
import com.example.android_dev.engine.SmartTaskEngine
import java.time.LocalDate
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.launch

private class OrbitBubbleState(
    val task: SmartTask,
    val homeX: Float,
    val homeY: Float,
    val radiusPx: Float,
    val pos: Animatable<Offset, *>
)

@Composable
fun PlusSpatialTodayScreen(
    tasks: List<SmartTask>,
    signal: UserCognitiveSignal,
    snapshot: CognitiveSnapshot,
    schedule: List<ScheduleSlot>,
    nextRecommendation: TaskRecommendation?,
    onToggleTask: (SmartTask) -> Unit,
    onDeleteTask: (SmartTask) -> Unit,
    onEditTask: (SmartTask) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeTasks = tasks.filterNot { it.isCompleted }
    var focusTask by remember { mutableStateOf<SmartTask?>(null) }
    var expandedTask by remember { mutableStateOf<SmartTask?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF07110F))
    ) {
        SpatialBackground(load = snapshot.overall, taskCount = activeTasks.size)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 14.dp, end = 14.dp, top = 76.dp, bottom = 86.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SpatialCommandHeader(
                tasks = tasks,
                snapshot = snapshot,
                nextRecommendation = nextRecommendation,
                onFocus = { focusTask = it },
                onEdit = onEditTask
            )

            SpatialTaskOrbit(
                tasks = activeTasks,
                signal = signal,
                onTapTask = { expandedTask = it },
                onComplete = onToggleTask,
                onDelete = onDeleteTask,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            SpatialInsightDock(
                tasks = tasks,
                schedule = schedule,
                snapshot = snapshot
            )
        }
    }

    expandedTask?.let { task ->
        SpatialTaskSheet(
            task = task,
            signal = signal,
            onDismiss = { expandedTask = null },
            onFocus = {
                expandedTask = null
                focusTask = task
            },
            onEdit = {
                expandedTask = null
                onEditTask(task)
            },
            onComplete = {
                expandedTask = null
                onToggleTask(task)
            }
        )
    }

    focusTask?.let { task ->
        FocusModeDialog(
            task = task,
            todayCompleted = tasks.count { it.isCompleted },
            todayTotal = tasks.size,
            plusModeEnabled = true,
            onComplete = { onToggleTask(task) },
            onDismiss = { focusTask = null }
        )
    }
}

@Composable
private fun SpatialBackground(load: Float, taskCount: Int) {
    val pulse = rememberInfiniteTransition(label = "spacePulse")
    val drift by pulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Reverse),
        label = "spaceDrift"
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
            Brush.radialGradient(
                colors = listOf(Color(0xFF153C34), Color(0xFF07110F), Color(0xFF030706)),
                center = Offset(size.width * (0.35f + drift * 0.18f), size.height * 0.18f),
                radius = size.maxDimension * 0.86f
            )
        )
        val count = (26 + taskCount * 3).coerceAtMost(70)
        repeat(count) { index ->
            val phase = index * 1.618f + drift * 2.4f
            val x = ((sin(phase) * 0.5f + 0.5f) * size.width)
            val y = ((cos(phase * 1.27f) * 0.5f + 0.5f) * size.height)
            val r = 1.1f + (index % 5) * 0.45f + load * 1.4f
            drawCircle(Color.White.copy(alpha = 0.10f + (index % 4) * 0.035f), r, Offset(x, y))
        }
        drawCircle(
            color = Color(0xFF8FE0A8).copy(alpha = 0.10f),
            radius = size.minDimension * (0.28f + load * 0.16f),
            center = Offset(size.width * 0.50f, size.height * 0.50f),
            style = Stroke(width = 1.2.dp.toPx())
        )
        drawCircle(
            color = Color(0xFF9DC2F0).copy(alpha = 0.08f),
            radius = size.minDimension * 0.42f,
            center = Offset(size.width * 0.50f, size.height * 0.50f),
            style = Stroke(width = 0.8.dp.toPx())
        )
    }
}

@Composable
private fun SpatialCommandHeader(
    tasks: List<SmartTask>,
    snapshot: CognitiveSnapshot,
    nextRecommendation: TaskRecommendation?,
    onFocus: (SmartTask) -> Unit,
    onEdit: (SmartTask) -> Unit
) {
    val active = tasks.count { !it.isCompleted }
    val overdue = tasks.count { it.isOverdue }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color.White.copy(alpha = 0.10f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(SPACE_TITLE, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        SPACE_SUBTITLE,
                        color = Color.White.copy(alpha = 0.62f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Surface(shape = RoundedCornerShape(16.dp), color = snapshot.level.tint().copy(alpha = 0.22f)) {
                    Text(
                        "${percent(snapshot.overall)}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SpaceMetric(ACTIVE_LABEL, active.toString())
                SpaceMetric(OVERDUE_LABEL, overdue.toString())
                SpaceMetric(LOAD_LABEL, snapshot.level.label)
            }
            nextRecommendation?.task?.let { task ->
                Surface(shape = RoundedCornerShape(18.dp), color = Color(0xFF8FE0A8).copy(alpha = 0.16f)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(NEXT_ACTION_LABEL, color = Color(0xFFB8F5D0), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Text(task.title, color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        TextButton(onClick = { onEdit(task) }) { Text(EDIT_LABEL, color = Color.White.copy(alpha = 0.82f)) }
                        Button(onClick = { onFocus(task) }) { Text(FOCUS_LABEL) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpatialTaskOrbit(
    tasks: List<SmartTask>,
    signal: UserCognitiveSignal,
    onTapTask: (SmartTask) -> Unit,
    onComplete: (SmartTask) -> Unit,
    onDelete: (SmartTask) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var widthPx by remember { mutableStateOf(1f) }
    var heightPx by remember { mutableStateOf(1f) }
    val completeZoneHeight = with(density) { 72.dp.toPx() }
    val deleteZoneHeight = with(density) { 72.dp.toPx() }

    Box(
        modifier = modifier.onSizeChanged {
            widthPx = it.width.toFloat().coerceAtLeast(1f)
            heightPx = it.height.toFloat().coerceAtLeast(1f)
        }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawOval(
                color = Color.White.copy(alpha = 0.08f),
                topLeft = Offset(size.width * 0.08f, size.height * 0.18f),
                size = Size(size.width * 0.84f, size.height * 0.56f),
                style = Stroke(width = 1.dp.toPx())
            )
            drawOval(
                color = Color(0xFF8FE0A8).copy(alpha = 0.09f),
                topLeft = Offset(size.width * 0.17f, size.height * 0.27f),
                size = Size(size.width * 0.66f, size.height * 0.38f),
                style = Stroke(width = 1.dp.toPx())
            )
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 4.dp, end = 4.dp),
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF8FE0A8).copy(alpha = 0.16f)
        ) {
            Text(DROP_DONE_LABEL, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = Color(0xFFB8F5D0), fontWeight = FontWeight.Bold)
        }
        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 4.dp, bottom = 4.dp),
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFFB45347).copy(alpha = 0.18f)
        ) {
            Text(DROP_REMOVE_LABEL, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = Color(0xFFFFC4BC), fontWeight = FontWeight.Bold)
        }

        if (tasks.isEmpty()) {
            Text(
                EMPTY_SPACE_LABEL,
                modifier = Modifier.align(Alignment.Center),
                color = Color.White.copy(alpha = 0.66f),
                textAlign = TextAlign.Center
            )
            return@Box
        }

        if (widthPx < 180f || heightPx < 240f) {
            Text(
                SPACE_LOADING_LABEL,
                modifier = Modifier.align(Alignment.Center),
                color = Color.White.copy(alpha = 0.56f),
                textAlign = TextAlign.Center
            )
            return@Box
        }

        val bubbles = remember(tasks, widthPx, heightPx) {
            buildOrbitBubbles(tasks.take(18), widthPx, heightPx, density)
        }
        bubbles.forEachIndexed { index, bubble ->
            SpatialBubble(
                bubble = bubble,
                index = index,
                signal = signal,
                completeZoneHeight = completeZoneHeight,
                deleteZoneHeight = deleteZoneHeight,
                widthPx = widthPx,
                heightPx = heightPx,
                onTap = { onTapTask(bubble.task) },
                onComplete = { onComplete(bubble.task) },
                onDelete = { onDelete(bubble.task) },
                scope = scope
            )
        }
    }
}

@Composable
private fun SpatialBubble(
    bubble: OrbitBubbleState,
    index: Int,
    signal: UserCognitiveSignal,
    completeZoneHeight: Float,
    deleteZoneHeight: Float,
    widthPx: Float,
    heightPx: Float,
    onTap: () -> Unit,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
    scope: kotlinx.coroutines.CoroutineScope
) {
    val density = LocalDensity.current
    val drift = rememberInfiniteTransition(label = "bubbleDrift$index")
    val floatY by drift.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(1800 + index * 140, easing = LinearEasing), RepeatMode.Reverse),
        label = "floatY$index"
    )
    var dragging by remember { mutableStateOf(false) }
    val diameter = with(density) { (bubble.radiusPx * 2).toDp() }
    val confidence = remember(bubble.task, signal) { SmartTaskEngine.predictTime(bubble.task, signal).confidence }
    val tint = bubble.task.priority.tint()

    Box(
        modifier = Modifier
            .graphicsLayer {
                translationX = bubble.pos.value.x
                translationY = bubble.pos.value.y + if (dragging) 0f else floatY
                shadowElevation = if (dragging) 18f else 6f
                scaleX = if (dragging) 1.12f else 1f
                scaleY = if (dragging) 1.12f else 1f
            }
            .size(diameter)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(Color.White.copy(alpha = 0.30f), tint.copy(alpha = 0.88f), Color(0xFF07110F).copy(alpha = 0.80f))
                )
            )
            .pointerInput(bubble) {
                detectTapGestures(onTap = { onTap() })
            }
            .pointerInput(bubble) {
                detectDragGestures(
                    onDragStart = { dragging = true },
                    onDragEnd = {
                        dragging = false
                        val center = bubble.pos.value + Offset(bubble.radiusPx, bubble.radiusPx)
                        when {
                            center.y <= completeZoneHeight && center.x > widthPx * 0.50f -> onComplete()
                            center.y >= heightPx - deleteZoneHeight && center.x < widthPx * 0.50f -> onDelete()
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
                        scope.launch { bubble.pos.snapTo(bubble.pos.value + dragAmount) }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(7.dp)) {
            Text(
                bubble.task.title,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Text("${(confidence * 100).roundToInt()}%", color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun SpatialInsightDock(
    tasks: List<SmartTask>,
    schedule: List<ScheduleSlot>,
    snapshot: CognitiveSnapshot
) {
    val report = remember(tasks, snapshot) { simpleSpatialReport(tasks, snapshot) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color.White.copy(alpha = 0.10f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(SCAN_LABEL, color = Color.White, fontWeight = FontWeight.Bold)
                Text("${report.score}/100", color = Color(0xFFB8F5D0), fontWeight = FontWeight.Bold)
            }
            Text(report.line, color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.bodySmall)
            if (schedule.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    schedule.take(2).forEach { slot ->
                        Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp), color = Color.White.copy(alpha = 0.08f)) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("${slot.start}-${slot.end}", color = Color(0xFFB8F5D0), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                Text(slot.task.title, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpatialTaskSheet(
    task: SmartTask,
    signal: UserCognitiveSignal,
    onDismiss: () -> Unit,
    onFocus: () -> Unit,
    onEdit: () -> Unit,
    onComplete: () -> Unit
) {
    val prediction = remember(task, signal) { SmartTaskEngine.predictTime(task, signal) }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = Color(0xFF10201C)) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(task.title, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "${task.category.label} · ${prediction.minutes} min · ${task.priority.label}",
                    color = Color.White.copy(alpha = 0.68f),
                    style = MaterialTheme.typography.bodySmall
                )
                if (task.description.isNotBlank()) {
                    Text(task.description, color = Color.White.copy(alpha = 0.78f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onEdit) { Text(EDIT_LABEL, color = Color.White.copy(alpha = 0.86f)) }
                    TextButton(onClick = onComplete) { Text(COMPLETE_LABEL, color = Color(0xFFB8F5D0)) }
                    Button(onClick = onFocus) { Text(FOCUS_LABEL) }
                }
            }
        }
    }
}

@Composable
private fun SpaceMetric(label: String, value: String) {
    Surface(shape = RoundedCornerShape(14.dp), color = Color.White.copy(alpha = 0.10f)) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = Color.White, fontWeight = FontWeight.Bold)
            Text(label, color = Color.White.copy(alpha = 0.58f), style = MaterialTheme.typography.labelSmall)
        }
    }
}

private data class SpatialReport(val score: Int, val line: String)

private fun simpleSpatialReport(tasks: List<SmartTask>, snapshot: CognitiveSnapshot): SpatialReport {
    val active = tasks.filterNot { it.isCompleted }
    val overdue = active.count { it.isOverdue }
    val complex = active.count { it.complexity >= 4 }
    val todayDue = active.count { it.dueDate == LocalDate.now() }
    val score = (snapshot.overall * 45 + overdue * 13 + complex * 8 + todayDue * 7).roundToInt().coerceIn(0, 100)
    val line = when {
        overdue > 0 -> SPACE_REPORT_OVERDUE
        complex > 1 -> SPACE_REPORT_COMPLEX
        snapshot.overall >= 0.58f -> SPACE_REPORT_HIGH
        else -> SPACE_REPORT_STABLE
    }
    return SpatialReport(score, line)
}

private fun buildOrbitBubbles(
    tasks: List<SmartTask>,
    widthPx: Float,
    heightPx: Float,
    density: androidx.compose.ui.unit.Density
): List<OrbitBubbleState> {
    val minR = with(density) { 30.dp.toPx() }
    val maxR = with(density) { 55.dp.toPx() }
    val center = Offset(widthPx / 2f, heightPx / 2f)
    val radiusX = widthPx * 0.34f
    val radiusY = heightPx * 0.27f
    return tasks.mapIndexed { index, task ->
        val angle = index * (Math.PI * 2.0 / tasks.size.coerceAtLeast(1)) - Math.PI / 2.0
        val layer = 1f + (index % 3) * 0.12f
        val r = (minR + task.priority.weight / 3f * (maxR - minR) + task.complexity * 1.8f).coerceIn(minR, maxR)
        val x = clampBubbleCoordinate(
            value = center.x + cos(angle).toFloat() * radiusX * layer - r,
            min = 8f,
            max = widthPx - r * 2f - 8f
        )
        val y = clampBubbleCoordinate(
            value = center.y + sin(angle).toFloat() * radiusY * layer - r,
            min = 70f,
            max = heightPx - r * 2f - 70f
        )
        OrbitBubbleState(task, x, y, r, Animatable(Offset(x, y), Offset.VectorConverter))
    }
}

private fun clampBubbleCoordinate(value: Float, min: Float, max: Float): Float {
    if (max < min) return min
    return value.coerceIn(min, max)
}

private const val SPACE_TITLE = "\u6df1\u7a7a\u4efb\u52a1\u8231"
private const val SPACE_SUBTITLE = "\u7528\u6c14\u6ce1\u3001\u8f68\u9053\u548c\u6295\u653e\u52a8\u4f5c\u7ba1\u7406\u4eca\u5929\u3002"
private const val ACTIVE_LABEL = "\u6d3b\u8dc3"
private const val OVERDUE_LABEL = "\u903e\u671f"
private const val LOAD_LABEL = "\u8d1f\u8377"
private const val NEXT_ACTION_LABEL = "\u7126\u70b9\u4efb\u52a1"
private const val EDIT_LABEL = "\u7f16\u8f91"
private const val FOCUS_LABEL = "\u4e13\u6ce8"
private const val COMPLETE_LABEL = "\u5b8c\u6210"
private const val DROP_DONE_LABEL = "\u62d6\u5230\u8fd9\u91cc\u5b8c\u6210"
private const val DROP_REMOVE_LABEL = "\u62d6\u5230\u8fd9\u91cc\u79fb\u51fa"
private const val EMPTY_SPACE_LABEL = "\u4eca\u5929\u6ca1\u6709\u5f85\u529e\u6c14\u6ce1"
private const val SPACE_LOADING_LABEL = "\u6b63\u5728\u521d\u59cb\u5316\u4efb\u52a1\u7a7a\u95f4"
private const val SCAN_LABEL = "\u4efb\u52a1\u626b\u63cf"
private const val SPACE_REPORT_OVERDUE = "\u5148\u5904\u7406\u903e\u671f\u6c14\u6ce1\uff0c\u518d\u8fdb\u5165\u4e00\u6bb5\u6df1\u5ea6\u4e13\u6ce8\u3002"
private const val SPACE_REPORT_COMPLEX = "\u9ad8\u590d\u6742\u4efb\u52a1\u504f\u591a\uff0c\u5148\u62c6\u51fa\u4e00\u4e2a 15 \u5206\u949f\u542f\u52a8\u52a8\u4f5c\u3002"
private const val SPACE_REPORT_HIGH = "\u4eca\u65e5\u8d1f\u8377\u504f\u9ad8\uff0c\u5efa\u8bae\u53ea\u4fdd\u7559\u4e09\u4e2a\u5173\u952e\u884c\u52a8\u3002"
private const val SPACE_REPORT_STABLE = "\u4efb\u52a1\u8f68\u9053\u7a33\u5b9a\uff0c\u9002\u5408\u63a8\u8fdb\u4e00\u4e2a\u9ad8\u4ef7\u503c\u4efb\u52a1\u3002"
