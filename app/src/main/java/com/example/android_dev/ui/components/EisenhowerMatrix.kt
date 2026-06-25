package com.example.android_dev.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.android_dev.domain.SmartTask
import com.example.android_dev.domain.UserCognitiveSignal
import com.example.android_dev.engine.SmartTaskEngine

// 四象限分类功能：按「重要」(importance>=4) × 「紧急」(逾期 或 距目标时间 <=2 小时) 切分到四格。
private enum class Quadrant(
    val title: String,
    val strategy: String,    // 该象限的行动策略，作为「会说话」的固定话术。
    val important: Boolean,
    val urgent: Boolean
) {
    DO_FIRST("重要且紧急", "立刻做：优先清空，避免拖成危机。", true, true),
    SCHEDULE("重要不紧急", "排计划：投入这里收益最高，今天分配一段专注。", true, false),
    DELEGATE("紧急不重要", "快速做或委托：限时处理，别占用深度精力。", false, true),
    ELIMINATE("不重要不紧急", "能删则删：批量处理或直接舍弃。", false, false)
}

// 象限配色功能：四格用不同强调色区分轻重缓急。
private fun Quadrant.tint(): Color = when (this) {
    Quadrant.DO_FIRST -> Color(0xFFB13E4B)
    Quadrant.SCHEDULE -> Color(0xFF2F7D57)
    Quadrant.DELEGATE -> Color(0xFFC06A24)
    Quadrant.ELIMINATE -> Color(0xFF6E6E6E)
}

private fun classify(task: SmartTask, nowHour: Int): Quadrant {
    val important = task.importance >= 4 || task.priority.weight >= 3
    val urgent = task.isOverdue || (task.targetHour - nowHour) <= 2
    return when {
        important && urgent -> Quadrant.DO_FIRST
        important && !urgent -> Quadrant.SCHEDULE
        !important && urgent -> Quadrant.DELEGATE
        else -> Quadrant.ELIMINATE
    }
}

// 智能四象限视图功能：把未完成任务按艾森豪威尔矩阵分到四格；每格除任务外，
// 还展示一句「会说话」的建议 —— 复用 SmartTaskEngine 的可解释优先级，
// 把象限策略 + 该格最高优先级任务的推荐理由（PriorityScoreBreakdown.explanation）合成一句话。
@Composable
fun EisenhowerMatrix(
    tasks: List<SmartTask>,
    signal: UserCognitiveSignal,
    nowHour: Int,
    onToggleTask: (SmartTask) -> Unit,
    onEditTask: (SmartTask) -> Unit,
    onFocusTask: (SmartTask) -> Unit
) {
    val active = tasks.filterNot { it.isCompleted }
    val grouped = active.groupBy { classify(it, nowHour) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "智能四象限 · 把精力放在「重要不紧急」",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Quadrant.entries.forEach { quadrant ->
            val items = grouped[quadrant].orEmpty()
                .sortedByDescending { SmartTaskEngine.calculatePriorityScore(it, signal, nowHour) }
            // 该格最高优先级任务的可解释推荐语，作为「会说话」的动态建议。
            val topExplanation = items.firstOrNull()
                ?.let { SmartTaskEngine.explainPriorityScore(it, signal, nowHour).explanation }
            QuadrantCard(
                quadrant = quadrant,
                items = items,
                topExplanation = topExplanation,
                onToggleTask = onToggleTask,
                onEditTask = onEditTask,
                onFocusTask = onFocusTask
            )
        }
    }
}

@Composable
private fun QuadrantCard(
    quadrant: Quadrant,
    items: List<SmartTask>,
    topExplanation: String?,
    onToggleTask: (SmartTask) -> Unit,
    onEditTask: (SmartTask) -> Unit,
    onFocusTask: (SmartTask) -> Unit
) {
    val tint = quadrant.tint()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, tint.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // 象限标题行：色点 + 名称 + 数量。
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(tint)
                )
                Text(
                    quadrant.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "${items.size} 项",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 「会说话」的建议条：象限策略 + 动态推荐理由。
            Surface(shape = RoundedCornerShape(8.dp), color = tint.copy(alpha = 0.10f)) {
                Text(
                    text = buildString {
                        append("💡 ")
                        append(quadrant.strategy)
                        if (!topExplanation.isNullOrBlank() && topExplanation != "当前没有明显推荐因子。") {
                            append(" ")
                            append(topExplanation)
                        }
                    },
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = tint
                )
            }

            if (items.isEmpty()) {
                Text(
                    "暂无任务",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                items.forEach { task ->
                    MatrixTaskRow(
                        task = task,
                        tint = tint,
                        onToggleTask = { onToggleTask(task) },
                        onEditTask = { onEditTask(task) },
                        onFocusTask = { onFocusTask(task) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MatrixTaskRow(
    task: SmartTask,
    tint: Color,
    onToggleTask: () -> Unit,
    onEditTask: () -> Unit,
    onFocusTask: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEditTask() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 完成勾选：用小圆点切换，省空间。
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (task.isCompleted) tint else tint.copy(alpha = 0.18f))
                .clickable { onToggleTask() }
        )
        Text(
            task.title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
        // 专注入口：每格任务行右侧的「专注」轻按钮。
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = tint.copy(alpha = 0.12f),
            modifier = Modifier.clickable { onFocusTask() }
        ) {
            Text(
                "🍅 专注",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                color = tint
            )
        }
    }
}
