package com.example.android_dev.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.android_dev.domain.SmartTask
import kotlinx.coroutines.delay

// 专注模式弹层功能：全屏圆环倒计时（番茄钟）。复用任务的预估时长作为专注时长，
// 计时归零或用户手动「完成专注」后，回调 onComplete 让上层把任务标记完成（写入 completionHistory / 成就），
// 并以「种下一颗成就种子 🌱」的仪式感反馈收尾。
@Composable
fun FocusModeDialog(
    task: SmartTask,
    onComplete: () -> Unit,
    onDismiss: () -> Unit
) {
    // 专注总时长：取任务预估时长，限制在 5~120 分钟之间，避免极端值。
    val totalSeconds = remember(task.id) { (task.estimatedMinutes.coerceIn(5, 120)) * 60 }
    var remainingSeconds by remember(task.id) { mutableStateOf(totalSeconds) }
    var running by remember(task.id) { mutableStateOf(true) }
    var finished by remember(task.id) { mutableStateOf(false) }

    // 每秒递减；暂停或已结束时不再计时；归零即自动进入「已结束」庆祝态。
    LaunchedEffect(running, finished) {
        while (running && !finished && remainingSeconds > 0) {
            delay(1000)
            remainingSeconds -= 1
        }
        if (remainingSeconds <= 0 && !finished) finished = true
    }

    val progress = if (totalSeconds == 0) 1f else 1f - remainingSeconds.toFloat() / totalSeconds
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(600), label = "ringProgress")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (finished) {
                    FocusFinished(task = task, onComplete = onComplete, onDismiss = onDismiss)
                } else {
                    Text(
                        if (task.isHabit) "习惯专注" else "专注进行中",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        task.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(36.dp))

                    // 圆环 + 中心倒计时文本。
                    Box(contentAlignment = Alignment.Center) {
                        FocusRing(
                            progress = animatedProgress,
                            ringColor = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(240.dp)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                formatClock(remainingSeconds),
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "共 ${totalSeconds / 60} 分钟",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(40.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { running = !running }) {
                            Text(if (running) "暂停" else "继续")
                        }
                        Button(onClick = { finished = true }) {
                            Text("完成专注")
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = onDismiss) { Text("放弃本次") }
                }
            }
        }
    }
}

// 专注完成态功能：展示「种下成就种子」的庆祝文案，确认后把任务标记完成。
@Composable
private fun FocusFinished(task: SmartTask, onComplete: () -> Unit, onDismiss: () -> Unit) {
    Text("🌱", style = MaterialTheme.typography.displayLarge)
    Spacer(Modifier.height(16.dp))
    Text(
        "专注完成，种下一颗成就种子",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(Modifier.height(8.dp))
    Text(
        "「${task.title}」的这段专注已记入你的成就。",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(32.dp))
    Button(
        onClick = {
            // 仅在任务尚未完成时标记完成，避免重复打卡。
            if (!task.isCompleted) onComplete()
            onDismiss()
        },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Text(if (task.isCompleted) "完成" else "收下种子并标记完成")
    }
}

// 圆环进度功能：用 Canvas 画一圈轨道 + 一段进度弧（从 12 点方向顺时针）。
@Composable
private fun FocusRing(
    progress: Float,
    ringColor: androidx.compose.ui.graphics.Color,
    trackColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.drawBehind {
            val strokeWidth = 18.dp.toPx()
            val inset = strokeWidth / 2
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(inset, inset)
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
    )
}

// 秒数格式化功能：把剩余秒数转成 mm:ss。
private fun formatClock(totalSeconds: Int): String {
    val s = totalSeconds.coerceAtLeast(0)
    return "%02d:%02d".format(s / 60, s % 60)
}
