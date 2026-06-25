@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.android_dev.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.android_dev.domain.SmartTask
import com.example.android_dev.domain.Subtask
import com.example.android_dev.domain.UserCognitiveSignal
import com.example.android_dev.engine.SmartTaskEngine

// 任务卡片功能：展示标题、优先级（高/中/低）、截止日期、标签、子任务进度与操作按钮。
@Composable
fun TaskCard(
    task: SmartTask,
    signal: UserCognitiveSignal,
    onToggleTask: () -> Unit,
    onDeleteTask: () -> Unit,
    onEditTask: (() -> Unit)? = null,
    onUpdateTask: ((SmartTask) -> Unit)? = null,
    onFocusTask: (() -> Unit)? = null
) {
    val prediction = SmartTaskEngine.predictTime(task, signal)
    // 展开态：点卡片主体在下方展开子任务清单与操作按钮。
    var expanded by remember { mutableStateOf(false) }
    // 微信风格：紧凑卡片，圆角更小、留白更省、信息一行可读。已完成标题降透明度并加删除线感。
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(start = 12.dp, top = 10.dp, end = 12.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 左侧勾选：缩小占位，仅完成态着主色，贴近微信的简洁。
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggleTask() },
                modifier = Modifier.size(22.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 标题行：标题 + 右侧轻量优先级圆点（已完成则不强调）。
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        task.title,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = if (task.isCompleted)
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!task.isCompleted) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(task.priority.tint())
                        )
                    }
                }

                if (task.description.isNotBlank()) {
                    Text(
                        task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 单行元信息：分类 · 截止/逾期 · 预估耗时 · 习惯连续，用「·」分隔代替多枚芯片。
                MetaLine(task = task, prediction = prediction)

                // 子任务细进度条：仅在有子任务时出现，高度更细。
                if (task.subtasks.isNotEmpty()) {
                    LinearProgressIndicator(
                        progress = { task.subtaskProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        color = task.priority.tint(),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }

        // 展开区：子任务清单 + 操作按钮，收起时卡片保持紧凑。
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (onUpdateTask != null && (task.subtasks.isNotEmpty() || !task.isHabit)) {
                    SubtaskList(
                        subtasks = task.subtasks,
                        onSubtasksChange = { updated ->
                            onUpdateTask(task.copy(subtasks = updated))
                        }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "创建于 ${formatCreatedAt(task.createdAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // 专注入口：未完成任务可一键进入番茄钟专注模式。
                        if (onFocusTask != null && !task.isCompleted) {
                            TextButton(onClick = onFocusTask) { Text("🍅 专注") }
                        }
                        onEditTask?.let {
                            TextButton(onClick = it) { Text("编辑") }
                        }
                        TextButton(onClick = onDeleteTask) { Text("删除") }
                    }
                }
            }
        }
    }
}

// 单行元信息功能：把分类、截止、预估耗时、标签和习惯连续天数压缩成一行「·」分隔文本，节省高度。
@Composable
private fun MetaLine(task: SmartTask, prediction: com.example.android_dev.domain.TimePrediction) {
    val parts = buildList {
        add(task.category.label)
        task.dueDate?.let { add(if (task.isOverdue) "逾期$it" else "截止$it") }
        add("${prediction.minutes}分")
        if (task.isHabit) add("连续${task.streak}天")
        task.tags.firstOrNull()?.let { add("#$it") }
    }
    val overdue = task.isOverdue && !task.isCompleted
    Text(
        text = parts.joinToString("  ·  "),
        style = MaterialTheme.typography.labelSmall,
        color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

// 创建时间格式化功能：把毫秒时间戳转成本地「yyyy-MM-dd HH:mm」文本。
private fun formatCreatedAt(epochMillis: Long): String {
    val dt = java.time.Instant.ofEpochMilli(epochMillis)
        .atZone(java.time.ZoneId.systemDefault())
    return "%04d-%02d-%02d %02d:%02d".format(
        dt.year, dt.monthValue, dt.dayOfMonth, dt.hour, dt.minute
    )
}
