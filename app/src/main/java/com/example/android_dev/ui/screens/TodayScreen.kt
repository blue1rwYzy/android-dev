@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.android_dev.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.android_dev.domain.CognitiveLoadLevel
import com.example.android_dev.domain.CognitiveSnapshot
import com.example.android_dev.domain.ScheduleSlot
import com.example.android_dev.domain.SmartTask
import com.example.android_dev.domain.TaskRecommendation
import com.example.android_dev.domain.UserCognitiveSignal
import com.example.android_dev.engine.SmartTaskEngine
import com.example.android_dev.domain.TaskCategory
import com.example.android_dev.ui.components.ActivityRing
import com.example.android_dev.ui.components.ActivityRingsCard
import com.example.android_dev.ui.components.CategorySlice
import com.example.android_dev.ui.components.CognitiveControls
import com.example.android_dev.ui.components.CognitiveStatusPanel
import com.example.android_dev.ui.components.CompletionCheckbox
import com.example.android_dev.ui.components.CountdownBubble
import com.example.android_dev.ui.components.SwipeableTaskRow
import com.example.android_dev.ui.components.TaskBubbleCloud
import com.example.android_dev.ui.components.MinimalFocusPanel
import com.example.android_dev.ui.components.SchedulePanel
import com.example.android_dev.ui.components.StatusBadge
import com.example.android_dev.ui.components.percent
import com.example.android_dev.ui.components.tint
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import java.time.temporal.ChronoUnit
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Surface
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Surface
import com.example.android_dev.domain.Countdown

// 今日页功能：以「列表为中心」的首页布局（对标 Todoist / TickTick / Things）——
// 顶部进度头 + 当前最该做的一件 → 快速捕捉 → 按「逾期/今天/稍后」分组的任务清单 → 可折叠的「状态与排程」抽屉。
@Composable
fun TodayScreen(
    tasks: List<SmartTask>,
    signal: UserCognitiveSignal,
    snapshot: CognitiveSnapshot,
    schedule: List<ScheduleSlot>,
    nextTask: SmartTask?,
    nextRecommendation: TaskRecommendation?,
    onSignalChange: (UserCognitiveSignal) -> Unit,
    onQuickAddTask: (String, String) -> Unit,
    onToggleTask: (SmartTask) -> Unit,
    onDeleteTask: (SmartTask) -> Unit = {},
    onEditTask: (SmartTask) -> Unit = {},
    countdowns: List<Countdown> = emptyList()  // 新增
) {
    val today = remember { LocalDate.now() }
    val activeTasks = tasks.filterNot { it.isCompleted }
    val completedCount = tasks.size - activeTasks.size

    // 三组分桶：逾期（截止日早于今天）、今天（截止今天或无截止日）、稍后（截止日在今天之后）。
    val overdue = activeTasks.filter { it.dueDate != null && it.dueDate.isBefore(today) }
    val todayTasks = activeTasks.filter { it.dueDate == null || it.dueDate.isEqual(today) }
    val later = activeTasks.filter { it.dueDate != null && it.dueDate.isAfter(today) }

    // 自适应触发：开启自适应模式后，负荷高/压力大/任务多时把「状态与排程」抽屉默认换成极简专注视图。
    val shouldSimplify = signal.adaptiveMode && (
        snapshot.level >= CognitiveLoadLevel.HIGH ||
            signal.stress > 0.7f ||
            activeTasks.size >= 7
        )
    var forceFullSchedule by rememberSaveable { mutableStateOf(false) }
    val simplified = shouldSimplify && !forceFullSchedule

    val todayCountdowns = remember(countdowns) {
        countdowns
            .filter { !it.targetDate.isBefore(LocalDate.now()) }
            .sortedBy { ChronoUnit.DAYS.between(LocalDate.now(), it.targetDate) }
            .take(5)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 顶部进度头：日期 + 完成进度环 + 当前最该做的一件。
        item(key = "header") {
            TodayHeaderCard(
                today = today,
                total = tasks.size,
                completed = completedCount,
                snapshot = snapshot,
                nextTask = nextTask,
                nextRecommendation = nextRecommendation,
                signal = signal,
                onToggleNext = { nextTask?.let(onToggleTask) },
                tasks = tasks
            )
        }

        if (todayCountdowns.isNotEmpty()) {
            item(key = "countdowns") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    todayCountdowns.forEach { countdown ->
                        CountdownBubble(countdown = countdown, today = today)
                    }
                }
            }
        }

        // 快速捕捉：单行输入 + 加号；输入后展开备注。
        item(key = "quick") {
            QuickCapturePanel(onQuickAddTask = onQuickAddTask)
        }

        // 逾期分组：气泡云视图（拖动气泡到顶部完成、拖出边界删除、轻点展开）。
        if (overdue.isNotEmpty()) {
            sectionHeader(key = "h-overdue", title = "逾期", count = overdue.size, tint = { MaterialTheme.colorScheme.error })
            item(key = "overdue-bubbles") {
                TaskBubbleCloud(
                    tasks = overdue,
                    onComplete = onToggleTask,
                    onDelete = onDeleteTask,
                    onEdit = onEditTask
                )
            }
        }

        // 今天分组：气泡云视图。
        sectionHeader(key = "h-today", title = "今天", count = todayTasks.size, tint = { MaterialTheme.colorScheme.primary })
        item(key = "today-bubbles") {
            TaskBubbleCloud(
                tasks = todayTasks,
                onComplete = onToggleTask,
                onDelete = onDeleteTask,
                onEdit = onEditTask
            )
        }

        // 稍后分组：气泡云视图。
        if (later.isNotEmpty()) {
            sectionHeader(key = "h-later", title = "稍后", count = later.size, tint = { MaterialTheme.colorScheme.onSurfaceVariant })
            item(key = "later-bubbles") {
                TaskBubbleCloud(
                    tasks = later,
                    onComplete = onToggleTask,
                    onDelete = onDeleteTask,
                    onEdit = onEditTask
                )
            }
        }

        // 「状态与排程」区：认知负荷 / 状态调节 / 智能排程，始终展示（不再折叠）。
        item(key = "advanced") {
            AdvancedSection(
                levelLabel = snapshot.level.label,
                overall = snapshot.overall,
                tint = snapshot.level.tint()
            ) {
                CognitiveStatusPanel(snapshot = snapshot, signal = signal)
                CognitiveControls(signal = signal, onSignalChange = onSignalChange)
                if (simplified) {
                    MinimalFocusPanel(
                        tasks = tasks,
                        signal = signal,
                        onToggleTask = onToggleTask,
                        onShowFullSchedule = { forceFullSchedule = true }
                    )
                } else {
                    if (shouldSimplify && forceFullSchedule) {
                        TextButton(
                            onClick = { forceFullSchedule = false },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("回到专注模式") }
                    }
                    SchedulePanel(schedule = schedule)
                }
            }
        }
    }
}

// 顶部进度头功能：三重活力环可视化（完成率 / 专注 / 习惯连续）+ 分类时间分布条 + 「现在做」高亮条。
@Composable
private fun TodayHeaderCard(
    today: LocalDate,
    total: Int,
    completed: Int,
    snapshot: CognitiveSnapshot,
    nextTask: SmartTask?,
    nextRecommendation: TaskRecommendation?,
    signal: UserCognitiveSignal,
    onToggleNext: () -> Unit,
    tasks: List<SmartTask>
) {
    val ratio = if (total == 0) 0f else completed.toFloat() / total
    val weekday = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.CHINA)

    // 三环数据（全部来自现有数据，不改引擎）：
    // 1) 完成环：已完成 / 总数。
    // 2) 专注环：专注力得分 = 1 - 当前认知负荷（负荷越低越能专注），转成百分制评分。
    // 3) 连续环：最长习惯连续天数 / 7 天目标。
    val focusScore = (1f - snapshot.overall).coerceIn(0f, 1f)
    val maxStreak = tasks.filter { it.isHabit }.maxOfOrNull { it.streak } ?: 0
    val streakGoal = 7
    val rings = listOf(
        ActivityRing(
            progress = ratio,
            color = MaterialTheme.colorScheme.primary,
            label = "完成",
            value = "$completed/$total"
        ),
        ActivityRing(
            progress = focusScore,
            color = MaterialTheme.colorScheme.tertiary,
            label = "专注",
            value = "${(focusScore * 100).toInt()} 分"
        ),
        ActivityRing(
            progress = (maxStreak.toFloat() / streakGoal).coerceIn(0f, 1f),
            color = MaterialTheme.colorScheme.secondary,
            label = "连续",
            value = "$maxStreak 天"
        )
    )

    // 分类时间分布：按未完成任务的预估时长在各分类间的占比。
    val distribution = remember(tasks) {
        tasks.filterNot { it.isCompleted }
            .groupBy { it.category }
            .map { (cat, list) -> CategorySlice(cat, list.sumOf { it.estimatedMinutes }.toFloat()) }
            .filter { it.fraction > 0f }
    }

    val dateLine = "${today.monthValue} 月 ${today.dayOfMonth} 日 · $weekday"
    val headline = if (total == 0) "今天还没有任务" else "今日活力 ${percent(ratio)}"

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ActivityRingsCard(
            dateLine = dateLine,
            headline = headline,
            rings = rings,
            distribution = distribution,
            categoryTint = { it.tint() },
            // 流动强度随未完成任务量提升：任务越多，背景水纹越活跃。
            flowIntensity = (tasks.count { !it.isCompleted } / 10f).coerceIn(0.2f, 1f)
        )

        // 「现在做」：把下一步推荐压缩成一行高亮条，带呼吸光晕；右滑直接完成，点左侧圆圈也可完成。
        nextTask?.let { task ->
                val prediction = remember(task, signal) { SmartTaskEngine.predictTime(task, signal) }
                // 呼吸光晕：背景透明度在 0.55~1.0 间缓慢循环，吸引注意而不刺眼。
                val breathe = rememberInfiniteTransition(label = "nowBreathe")
                val glow by breathe.animateFloat(
                    initialValue = 0.55f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1800, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "glow"
                )
                val container = MaterialTheme.colorScheme.primaryContainer
                SwipeableTaskRow(onComplete = onToggleNext) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(container.copy(alpha = glow))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                            .clickable { onToggleNext() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "完成",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "现在做",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            task.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    StatusBadge("${prediction.minutes} 分", MaterialTheme.colorScheme.primary)
                }
                }
            }
        }
}

// 分组标题功能：静态小标题，左侧色点 + 标题 + 数量徽章（不再折叠，无展开箭头）。
private fun androidx.compose.foundation.lazy.LazyListScope.sectionHeader(
    key: String,
    title: String,
    count: Int,
    tint: @Composable () -> androidx.compose.ui.graphics.Color
) {
    item(key = key) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(tint())
            )
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                "$count",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// 任务行功能：轻量列表行——左侧优先级色条 + 勾选框 + 标题 + 分类/截止/耗时元信息。
@Composable
private fun TodayTaskRow(
    task: SmartTask,
    signal: UserCognitiveSignal,
    today: LocalDate,
    onToggle: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val prediction = remember(task, signal) { SmartTaskEngine.predictTime(task, signal) }
    // 滑动手势包裹：右滑完成、左滑删除（露一角信号）。
    SwipeableTaskRow(onComplete = onToggle, onDelete = onDelete) {
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(44.dp)
                        .background(task.priority.tint())
                )
                Spacer(modifier = Modifier.width(10.dp))
                // Todoist 式动画勾选框：完成时填充主色 + 弹性回弹 + 对勾描边。
                CompletionCheckbox(
                    checked = task.isCompleted,
                    tint = task.priority.tint(),
                    onToggle = onToggle
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        task.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                        color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = taskMeta(task, today, prediction.minutes),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (task.isOverdue) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                StatusBadge(text = task.priority.label, color = task.priority.tint())
                Spacer(modifier = Modifier.width(10.dp))
            }
        }
    }
}

// 元信息拼接功能：分类 · 约X分 · 截止/逾期 · 子任务进度。
private fun taskMeta(task: SmartTask, today: LocalDate, minutes: Int): String = buildString {
    append(task.category.label)
    append(" · 约 $minutes 分")
    task.dueDate?.let {
        val label = when {
            it.isBefore(today) -> "逾期 $it"
            it.isEqual(today) -> "今天截止"
            else -> "截止 $it"
        }
        append(" · $label")
    }
    if (task.subtasks.isNotEmpty()) {
        append(" · 子任务 ${task.subtasks.count { it.done }}/${task.subtasks.size}")
    }
}

// 空态提示功能：分组为空时给一句轻提示。
@Composable
private fun EmptyHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp)
    )
}

// 高级抽屉功能：折叠收纳认知负荷、状态调节、智能排程/专注视图等特色功能，默认折叠不打扰清单。
// 状态与排程区功能：始终展示（不折叠）的卡片，顶部标题 + 负荷徽章，下方直接铺开内容。
@Composable
private fun AdvancedSection(
    levelLabel: String,
    overall: Float,
    tint: androidx.compose.ui.graphics.Color,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("状态与排程", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        "认知负荷 · 状态调节 · 智能排程",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusBadge("$levelLabel ${percent(overall)}", tint)
            }
            Column(
                modifier = Modifier.padding(top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                content()
            }
        }
    }
}

// 快速捕捉功能：收集标题和备注，由 ViewModel 创建智能任务。
// 紧凑设计：默认只显示单行输入 + 加号按钮；一旦输入内容才展开备注框与说明，平时不占额外高度。
@Composable
private fun QuickCapturePanel(onQuickAddTask: (String, String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    val expanded = text.isNotBlank()
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("快速捕捉") },
                    placeholder = { Text("记一件待办…") },
                    singleLine = true
                )
                FilledIconButton(
                    enabled = text.isNotBlank(),
                    onClick = {
                        onQuickAddTask(text, note)
                        text = ""
                        note = ""
                    }
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "加入")
                }
            }
            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // 实时解析预览：用现有 DueDateParser 解析日期、SmartTaskEngine.classify 解析分类，输入即浮出 chip。
                    val parsedDate = remember(text, note) {
                        com.example.android_dev.ai.DueDateParser.parse("$text $note")
                    }
                    val parsedCategory = remember(text, note) {
                        SmartTaskEngine.classify(text, note)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ParseChip("#${parsedCategory.label}", parsedCategory.tint())
                        parsedDate?.let {
                            val label = when {
                                it.isEqual(LocalDate.now()) -> "今天截止"
                                else -> "截止 ${it.monthValue}/${it.dayOfMonth}"
                            }
                            ParseChip(label, MaterialTheme.colorScheme.primary)
                        }
                    }
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("补充信息（可选）") },
                        maxLines = 2
                    )
                }
            }
        }
    }
}

// 解析结果芯片功能：快速捕捉时浮出的彩色小标签，提示已识别的分类/日期。
@Composable
private fun ParseChip(text: String, tint: androidx.compose.ui.graphics.Color) {
    Surface(shape = RoundedCornerShape(8.dp), color = tint.copy(alpha = 0.12f)) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = tint
        )
    }
}
