package com.example.android_dev.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.android_dev.domain.AchievementBadge
import com.example.android_dev.domain.CognitiveLoadLevel
import com.example.android_dev.domain.CognitiveLoadRecord
import com.example.android_dev.domain.CognitiveSnapshot
import com.example.android_dev.domain.CompletionTrend
import com.example.android_dev.domain.ExtractedTask
import com.example.android_dev.domain.HeatmapData
import com.example.android_dev.domain.SmartTask
import com.example.android_dev.domain.TaskPriority
import com.example.android_dev.domain.TaskRecommendation
import com.example.android_dev.domain.UserCognitiveSignal
import com.example.android_dev.domain.WeeklyReport
import com.example.android_dev.engine.SmartTaskEngine
import java.time.LocalDate
import kotlin.math.roundToInt

@Composable
fun DailyLoadCard(
    tasks: List<SmartTask>,
    snapshot: CognitiveSnapshot,
    modifier: Modifier = Modifier
) {
    val active = tasks.count { !it.isCompleted }
    val overdue = tasks.count { it.isOverdue }
    val tint = snapshot.level.tint()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = tint.copy(alpha = 0.10f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("今日行动驾驶舱", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        dailyLoadAdvice(snapshot.level),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusBadge("${snapshot.level.label} ${percent(snapshot.overall)}", tint)
            }
            LinearProgressIndicator(
                progress = { snapshot.overall.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(8.dp)),
                color = tint,
                trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                CompactStat("活跃任务", active.toString(), MaterialTheme.colorScheme.primary)
                CompactStat("逾期", overdue.toString(), MaterialTheme.colorScheme.error)
                CompactStat("视觉负荷", percent(snapshot.visualLoad), MaterialTheme.colorScheme.secondary)
                CompactStat("时间压力", percent(snapshot.temporalPressure), MaterialTheme.colorScheme.tertiary)
            }
        }
    }
}

@Composable
fun ActionRecommendationCard(
    recommendation: TaskRecommendation?,
    signal: UserCognitiveSignal,
    onFocus: (SmartTask) -> Unit,
    onEdit: (SmartTask) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("现在最值得做", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            if (recommendation == null) {
                Text("当前没有待处理任务，可以保留空档或做一次轻量复盘。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                return@Column
            }

            val task = recommendation.task
            val prediction = remember(task, signal) { SmartTaskEngine.predictTime(task, signal) }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(task.priority.tint())
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(task.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${prediction.minutes} 分钟 · 推荐分 ${recommendation.priority.totalScore}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusBadge(task.priority.label, task.priority.tint())
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                recommendation.priority.factors
                    .sortedByDescending { it.contribution }
                    .take(2)
                    .forEach { factor ->
                        StatusBadge("${factor.label} ${(factor.score * 100).roundToInt()}%", MaterialTheme.colorScheme.primary)
                    }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                OutlinedButton(onClick = { onEdit(task) }) { Text("编辑") }
                Spacer(Modifier.size(8.dp))
                Button(onClick = { onFocus(task) }) { Text("开始专注") }
            }
        }
    }
}

@Composable
fun TaskHealthReportCard(
    tasks: List<SmartTask>,
    snapshot: CognitiveSnapshot,
    modifier: Modifier = Modifier
) {
    val report = remember(tasks, snapshot) { buildTaskHealthReport(tasks, snapshot) }
    val tint = when {
        report.score >= 78 -> MaterialTheme.colorScheme.error
        report.score >= 55 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("今日任务体检", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(report.oneLine, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("${report.score}/100", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = tint)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                CompactStat("截止任务", "${report.todayDueCount}", MaterialTheme.colorScheme.tertiary)
                CompactStat("高复杂", "${report.highComplexityCount}", MaterialTheme.colorScheme.error)
                CompactStat("建议延后", "${report.deferCount}", MaterialTheme.colorScheme.outline)
            }

            Surface(shape = RoundedCornerShape(10.dp), color = tint.copy(alpha = 0.10f)) {
                Text(
                    report.strategy,
                    modifier = Modifier.padding(10.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = tint,
                    fontWeight = FontWeight.SemiBold
                )
            }
            report.suggestions.forEach { suggestion ->
                Text("· $suggestion", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedButton(onClick = { }, modifier = Modifier.fillMaxWidth()) {
                Text("生成可分享体检卡")
            }
        }
    }
}

@Composable
fun AiPlannerQuickActions(
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val actions = listOf(
        "帮我规划今天",
        "把这个目标拆成计划",
        "我现在压力很大，帮我收敛任务",
        "从这段文字提取任务"
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        actions.forEach { action ->
            AssistActionChip(text = action, onClick = { onPick(actionPrompt(action)) })
        }
    }
}

@Composable
fun AiPlanPreviewPanel(
    tasks: List<ExtractedTask>,
    onOpenPreview: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (tasks.isEmpty()) return
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("已识别出可执行计划", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            tasks.take(3).forEach { task ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(task.priority.tint()))
                    Text(task.title, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${task.estimatedMinutes} 分钟", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Button(onClick = onOpenPreview, modifier = Modifier.fillMaxWidth()) {
                Text("选择后加入计划")
            }
        }
    }
}

@Composable
fun FocusResultCard(
    task: SmartTask,
    focusedMinutes: Int,
    todayCompleted: Int = 0,
    todayTotal: Int = 0,
    modifier: Modifier = Modifier
) {
    val completion = if (todayTotal == 0) 0 else (todayCompleted * 100f / todayTotal).roundToInt()
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("智能 TodoLife", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text("专注成果卡", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(task.title, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ShareStat("专注", "${focusedMinutes}分", Modifier.weight(1f))
                ShareStat("完成率", "${completion}%", Modifier.weight(1f))
                ShareStat("连续", "${task.streak}天", Modifier.weight(1f))
            }
            Text(
                "把一个任务认真做完，比继续扩充清单更重要。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun WeeklyReviewCard(
    report: WeeklyReport,
    loadCurve: List<CognitiveLoadRecord>,
    heatmapData: HeatmapData,
    achievements: List<AchievementBadge>,
    modifier: Modifier = Modifier
) {
    val peakHour = loadCurve.maxByOrNull { it.overall }?.hour
    val unlocked = achievements.count { it.unlockedAt != null }
    val suggestion = weeklySuggestion(report, peakHour)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("周复盘摘要", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("用本周数据修正下一轮安排", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                StatusBadge(trendLabel(report.trend), trendColor(report.trend))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                CompactStat("完成", "${report.totalCompleted}/${report.totalTasks}", MaterialTheme.colorScheme.primary)
                CompactStat("完成率", "${(report.avgCompletionRate * 100).roundToInt()}%", MaterialTheme.colorScheme.secondary)
                CompactStat("习惯", "${report.totalHabitStreak}天", MaterialTheme.colorScheme.tertiary)
                CompactStat("徽章", "$unlocked", MaterialTheme.colorScheme.error)
            }
            Text(
                "最高压时段：${peakHour?.let { "%02d:00".format(it) } ?: "暂无记录"} · 近 ${heatmapData.dates.size} 天习惯记录已纳入复盘",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)) {
                Text(suggestion, modifier = Modifier.padding(10.dp), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }
            OutlinedButton(onClick = { }, modifier = Modifier.fillMaxWidth()) {
                Text("生成周报卡片")
            }
        }
    }
}

@Composable
fun TodoLifePlusCard(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("TodoLife Plus", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(PLUS_SUBTITLE, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = enabled, onCheckedChange = onEnabledChange)
            }
            val features = PLUS_FEATURES
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                features.forEach { feature -> AssistActionChip(text = feature, onClick = {}) }
            }
            Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.58f)) {
                Text(
                    if (enabled) PLUS_ENABLED_TEXT else PLUS_DISABLED_TEXT,
                    modifier = Modifier.padding(10.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private const val PLUS_SUBTITLE =
    "\u514d\u8d39\u9ad8\u7ea7\u6a21\u5f0f\uff1a\u628a\u4efb\u52a1\u7ba1\u7406\u5347\u7ea7\u4e3a\u8ba4\u77e5\u72b6\u6001\u6548\u7387\u6559\u7ec3\u3002"
private const val PLUS_ENABLED_TEXT =
    "Plus \u5df2\u5f00\u542f\uff0c\u6240\u6709\u9ad8\u7ea7\u4f53\u9a8c\u5747\u4e3a\u514d\u8d39\u672c\u5730\u529f\u80fd\u3002"
private const val PLUS_DISABLED_TEXT =
    "Plus \u5df2\u5173\u95ed\uff0c\u5e94\u7528\u4fdd\u7559\u57fa\u7840\u5f85\u529e\u3001\u770b\u677f\u3001\u65e5\u5386\u4e0e\u666e\u901a\u7edf\u8ba1\u3002"
private val PLUS_FEATURES = listOf(
    "AI \u89c4\u5212\u5e08",
    "\u4eca\u65e5\u4efb\u52a1\u4f53\u68c0",
    "\u884c\u52a8\u9a7e\u9a76\u8231",
    "\u5468\u590d\u76d8\u6458\u8981",
    "\u4e13\u6ce8\u6210\u679c\u5361",
    "\u5206\u4eab\u5361\u7247\u9884\u7559"
)

@Composable
private fun CompactStat(label: String, value: String, color: Color) {
    Surface(shape = RoundedCornerShape(10.dp), color = color.copy(alpha = 0.12f)) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.labelLarge, color = color, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ShareStat(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)) {
        Column(modifier = Modifier.padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AssistActionChip(text: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        onClick = onClick
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private data class TaskHealthReport(
    val score: Int,
    val todayDueCount: Int,
    val highComplexityCount: Int,
    val deferCount: Int,
    val strategy: String,
    val oneLine: String,
    val suggestions: List<String>
)

private fun buildTaskHealthReport(tasks: List<SmartTask>, snapshot: CognitiveSnapshot): TaskHealthReport {
    val today = LocalDate.now()
    val active = tasks.filterNot { it.isCompleted }
    val overdue = active.count { it.isOverdue }
    val todayDue = active.count { it.dueDate == today }
    val highComplexity = active.count { it.complexity >= 4 }
    val lowPriority = active.count { it.priority == TaskPriority.LOW }
    val avgMinutes = active.map { it.estimatedMinutes }.average().takeIf { !it.isNaN() } ?: 0.0
    val score = (
        snapshot.overall * 42 +
            overdue * 12 +
            todayDue * 6 +
            highComplexity * 7 +
            (avgMinutes / 45.0 * 8)
        ).roundToInt().coerceIn(0, 100)
    val strategy = when {
        overdue > 0 -> "推荐策略：先清理 1 个逾期任务，再进入 45 分钟深度专注。"
        highComplexity > 1 -> "推荐策略：把最高复杂任务拆成 15 分钟启动动作。"
        snapshot.level >= CognitiveLoadLevel.HIGH -> "推荐策略：先收敛任务池，只保留前三个关键行动。"
        else -> "推荐策略：安排一个高价值任务，并保留短休息窗口。"
    }
    val suggestions = buildList {
        if (overdue > 0) add("先处理逾期任务，避免压力继续累积")
        if (highComplexity > 0) add("高复杂任务先拆出 15 分钟启动动作")
        if (lowPriority > 0) add("隐藏或延后低优先级任务，减少选择噪音")
        if (isEmpty()) add("当前任务池较稳定，可以推进一个深度任务")
    }.take(3)
    val oneLine = if (score >= 70) "别扩充任务池，先完成关键闭环。" else "任务负荷可控，适合稳定推进。"
    return TaskHealthReport(score, todayDue, highComplexity, lowPriority, strategy, oneLine, suggestions)
}

private fun dailyLoadAdvice(level: CognitiveLoadLevel): String = when (level) {
    CognitiveLoadLevel.LOW -> "状态较轻，适合安排深度任务。"
    CognitiveLoadLevel.BALANCED -> "保持节奏，优先推进一个高价值任务。"
    CognitiveLoadLevel.HIGH -> "负荷偏高，建议收敛任务池。"
    CognitiveLoadLevel.OVERWHELMING -> "先暂停新增任务，只处理关键闭环。"
}

private fun actionPrompt(action: String): String = when (action) {
    "帮我规划今天" -> "请根据我的今日任务，按优先级和精力消耗规划一个可执行顺序。"
    "把这个目标拆成计划" -> "请把下面这个目标拆成 3 到 6 个可执行步骤，并标注预计时长。"
    "我现在压力很大，帮我收敛任务" -> "我现在压力很大，请帮我减少今天要处理的任务，只保留最关键的行动。"
    "从这段文字提取任务" -> "请从下面这段文字中提取可加入计划的任务，并给出优先级和预计时长。"
    else -> action
}

private fun trendLabel(trend: CompletionTrend): String = when (trend) {
    CompletionTrend.IMPROVING -> "上升"
    CompletionTrend.STABLE -> "平稳"
    CompletionTrend.DECLINING -> "下降"
}

@Composable
private fun trendColor(trend: CompletionTrend): Color = when (trend) {
    CompletionTrend.IMPROVING -> MaterialTheme.colorScheme.primary
    CompletionTrend.STABLE -> MaterialTheme.colorScheme.secondary
    CompletionTrend.DECLINING -> MaterialTheme.colorScheme.error
}

private fun weeklySuggestion(report: WeeklyReport, peakHour: Int?): String = when {
    report.avgCompletionRate < 0.45f -> "下周建议减少任务总量，优先保证每天 1 个关键闭环。"
    peakHour != null && peakHour in 13..16 -> "下周建议把高复杂任务前移到上午，午后保留轻任务。"
    report.totalHabitStreak <= 0 -> "下周建议从一个 2 分钟微习惯开始，先建立连续性。"
    else -> "下周可以保留当前节奏，并增加一个中等挑战任务。"
}
