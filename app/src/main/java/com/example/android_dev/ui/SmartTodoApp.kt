@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.android_dev.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.dp
import com.example.android_dev.ai.AiBreakdownRequest
import com.example.android_dev.domain.AiBreakdownResult
import com.example.android_dev.domain.SmartTask
import com.example.android_dev.domain.TaskStatus
import com.example.android_dev.domain.UserCognitiveSignal
import com.example.android_dev.ui.screens.AiChatScreen
import com.example.android_dev.ui.theme.AppPalette
import com.example.android_dev.viewmodel.AiChatUiState
import com.example.android_dev.ui.components.SmartTodoBottomBar
import com.example.android_dev.ui.components.SmartTodoTab
import com.example.android_dev.ui.components.TaskEditorDialog
import com.example.android_dev.ui.screens.CalendarScreen
import com.example.android_dev.ui.screens.InsightScreen
import com.example.android_dev.ui.screens.KanbanScreen
import com.example.android_dev.ui.screens.ProfileScreen
import com.example.android_dev.ui.screens.StatisticsScreen
import com.example.android_dev.ui.screens.TaskScreen
import com.example.android_dev.ui.screens.TodayScreen
import com.example.android_dev.viewmodel.SmartTodoUiState
import java.time.LocalDate
import com.example.android_dev.domain.Countdown  // === 新增导入 ===
import com.example.android_dev.ui.screens.CountdownScreen  // === 新增导入 ===
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.android_dev.ui.screens.CountdownScreen

// 应用壳层功能：顶部栏、底部导航、悬浮新增按钮，并路由今日/看板/日历/列表/我的五个页面。
@Composable
fun SmartTodoApp(
    uiState: SmartTodoUiState,
    username: String,
    palette: AppPalette,
    onChangeTheme: (AppPalette) -> Unit,
    plusModeEnabled: Boolean,
    onChangePlusMode: (Boolean) -> Unit,
    chatState: AiChatUiState,
    onSendChat: (String) -> Unit,
    onClearChat: () -> Unit,
    onExtractTasks: (String, (List<com.example.android_dev.domain.ExtractedTask>) -> Unit, (String) -> Unit) -> Unit,
    onAddExtractedTasks: (List<com.example.android_dev.domain.ExtractedTask>) -> Unit,
    onSignalChange: (UserCognitiveSignal) -> Unit,
    onQuickAddTask: (String, String) -> Unit,
    onCreateTask: (SmartTask) -> Unit,
    onUpdateTask: (SmartTask) -> Unit,
    onToggleTask: (SmartTask) -> Unit,
    onDeleteTask: (SmartTask) -> Unit,
    onMoveStatus: (SmartTask, TaskStatus) -> Unit,
    onRequestAiBreakdown: (AiBreakdownRequest, (AiBreakdownResult) -> Unit, () -> Unit) -> Unit,
    onLogout: () -> Unit,
    countdowns: List<Countdown>,
    onAddCountdown: (Countdown) -> Unit,
    onEditCountdown: (Countdown) -> Unit,
    onDeleteCountdown: (String) -> Unit
) {
    var selectedTab by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(SmartTodoTab.TODAY) }
    var showNewTaskEditor by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var editingTask by androidx.compose.runtime.remember { mutableStateOf<SmartTask?>(null) }
    var showInsights by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var showStatistics by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var showAiChat by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var showCountdown by rememberSaveable { mutableStateOf(false) }

    // AI 拆解适配器：把对话框的 (goal,dueDate,onResult,onError) 转成仓库请求。
    val aiBridge: (String, LocalDate?, (AiBreakdownResult) -> Unit, () -> Unit) -> Unit =
        { goal, dueDate, onResult, onError ->
            onRequestAiBreakdown(AiBreakdownRequest(goal = goal, dueDate = dueDate), onResult, onError)
        }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = if (plusModeEnabled) Color(0xFF07110F) else MaterialTheme.colorScheme.background,
        topBar = {
            if (!plusModeEnabled) CenterAlignedTopAppBar(
                expandedHeight = 48.dp,
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "智能 TodoLife",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${uiState.tasks.count { !it.isCompleted }} 项待办 · ${uiState.snapshot.level.label}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    androidx.compose.material3.TextButton(
                        onClick = { selectedTab = SmartTodoTab.PROFILE },
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Text(
                            SmartTodoTab.PROFILE.mark,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == SmartTodoTab.PROFILE) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                },
                actions = {
                    androidx.compose.material3.TextButton(
                        onClick = { showAiChat = true },
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Text(
                            "🤖 AI 助手",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            if (!plusModeEnabled) {
                SmartTodoBottomBar(selectedTab = selectedTab, onSelect = { selectedTab = it })
            }
        },
        floatingActionButton = {
            if (!plusModeEnabled && selectedTab != SmartTodoTab.PROFILE) {
                FloatingActionButton(onClick = { showNewTaskEditor = true }) {
                    Text("+", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (plusModeEnabled) androidx.compose.foundation.layout.PaddingValues() else innerPadding)
        ) {
            when (selectedTab) {
                SmartTodoTab.TODAY -> TodayScreen(
                    tasks = uiState.tasks,
                    signal = uiState.signal,
                    snapshot = uiState.snapshot,
                    schedule = uiState.schedule,
                    nextTask = uiState.nextTask,
                    nextRecommendation = uiState.nextRecommendation,
                    plusModeEnabled = plusModeEnabled,
                    onSignalChange = onSignalChange,
                    onQuickAddTask = onQuickAddTask,
                    onToggleTask = onToggleTask,
                    onDeleteTask = onDeleteTask,
                    onEditTask = { editingTask = it },
                    countdowns = countdowns  // === 新增传参 ===
                )

                SmartTodoTab.BOARD -> KanbanScreen(
                    tasks = uiState.tasks,
                    onMoveStatus = onMoveStatus,
                    onEditTask = { editingTask = it },
                    onDeleteTask = onDeleteTask
                )

                SmartTodoTab.CALENDAR -> CalendarScreen(
                    tasks = uiState.tasks,
                    onToggleTask = onToggleTask,
                    onEditTask = { editingTask = it },
                    countdowns = countdowns,  // === 新增传参 ===
                    onEditCountdown = onEditCountdown,
                    onDeleteCountdown = onDeleteCountdown
                )

                SmartTodoTab.TASKS -> TaskScreen(
                    tasks = uiState.tasks,
                    signal = uiState.signal,
                    onToggleTask = onToggleTask,
                    onDeleteTask = onDeleteTask,
                    onEditTask = { editingTask = it },
                    onUpdateTask = onUpdateTask
                )

                SmartTodoTab.PROFILE -> ProfileScreen(
                    username = username,
                    tasks = uiState.tasks,
                    palette = palette,
                    onChangeTheme = onChangeTheme,
                    plusModeEnabled = plusModeEnabled,
                    onChangePlusMode = onChangePlusMode,
                    onOpenInsights = { showInsights = true },
                    onOpenStatistics = { showStatistics = true },
                    onOpenCountdown = { showCountdown = true },  // === 新增回调 ===
                    onLogout = onLogout
                )
            }

            if (plusModeEnabled) {
                PlusShellOverlay(
                    selectedTab = selectedTab,
                    onSelectTab = { selectedTab = it },
                    onNewTask = { showNewTaskEditor = true },
                    onOpenAi = { showAiChat = true }
                )
            }
        }
    }

    // 新建任务弹窗。
    if (showNewTaskEditor) {
        TaskEditorDialog(
            signal = uiState.signal,
            onDismiss = { showNewTaskEditor = false },
            onCreate = { task ->
                onCreateTask(task)
                showNewTaskEditor = false
            },
            onRequestAiBreakdown = aiBridge
        )
    }

    // 编辑任务弹窗。
    editingTask?.let { task ->
        TaskEditorDialog(
            signal = uiState.signal,
            initialTask = task,
            onDismiss = { editingTask = null },
            onCreate = { updated ->
                onUpdateTask(updated)
                editingTask = null
            },
            onRequestAiBreakdown = aiBridge
        )
    }

    // 高级洞察 / 统计弹层。
    if (showInsights) {
        AdvancedSheet(onClose = { showInsights = false }) {
            InsightScreen(
                tasks = uiState.tasks,
                signal = uiState.signal,
                snapshot = uiState.snapshot,
                insights = uiState.insights,
                nextTask = uiState.nextTask
            )
        }
    }
    if (showStatistics) {
        AdvancedSheet(onClose = { showStatistics = false }) {
            StatisticsScreen(
                tasks = uiState.tasks,
                weeklyReport = uiState.weeklyReport,
                heatmapData = uiState.heatmapData,
                loadCurve = uiState.loadCurve,
                predictedLoadCurve = uiState.predictedLoadCurve,
                achievements = uiState.achievements,
                plusModeEnabled = plusModeEnabled
            )
        }
    }

    // AI 对话弹层。
    if (showAiChat) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showAiChat = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = MaterialTheme.colorScheme.background,
                topBar = {
                    CenterAlignedTopAppBar(
                        title = { Text("AI 助手", fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            androidx.compose.material3.TextButton(onClick = { showAiChat = false }) { Text("← 返回") }
                        },
                        actions = {
                            androidx.compose.material3.TextButton(onClick = onClearChat) { Text("清空") }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            ) { innerPadding ->
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    AiChatScreen(
                        state = chatState,
                        plusModeEnabled = plusModeEnabled,
                        onSend = onSendChat,
                        onExtractTasks = onExtractTasks,
                        onAddTasks = onAddExtractedTasks
                    )
                }
            }
        }
    }

    if (showCountdown) {
        Dialog(
            onDismissRequest = { showCountdown = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            CountdownScreen(
                countdowns = countdowns,
                onAdd = onAddCountdown,
                onEdit = onEditCountdown,
                onDelete = onDeleteCountdown,
                onClose = { showCountdown = false }
            )
        }
    }
}

// 高级功能弹层功能：用全屏对话框承载洞察/统计页，顶部提供返回按钮。
@Composable
private fun PlusShellOverlay(
    selectedTab: SmartTodoTab,
    onSelectTab: (SmartTodoTab) -> Unit,
    onNewTask: () -> Unit,
    onOpenAi: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (selectedTab != SmartTodoTab.PROFILE) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 18.dp, start = 14.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
                color = Color.White.copy(alpha = 0.10f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                onClick = { onSelectTab(SmartTodoTab.PROFILE) }
            ) {
                Text(
                    SmartTodoTab.PROFILE.mark,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    color = Color.White.copy(alpha = 0.82f),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium
                )
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 18.dp, end = 14.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
                color = Color.White.copy(alpha = 0.10f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PlusPill("AI", onOpenAi)
                    PlusPill("+", onNewTask)
                }
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 18.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(26.dp),
            color = Color.White.copy(alpha = 0.10f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(SmartTodoTab.TODAY).forEach { tab ->
                    Surface(
                        shape = CircleShape,
                        color = if (selectedTab == tab) Color(0xFF8FE0A8).copy(alpha = 0.26f) else Color.Transparent,
                        onClick = { onSelectTab(tab) }
                    ) {
                        Text(
                            tab.mark,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            color = if (selectedTab == tab) Color(0xFFB8F5D0) else Color.White.copy(alpha = 0.68f),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlusPill(text: String, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.12f),
        onClick = onClick
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun AdvancedSheet(onClose: () -> Unit, content: @Composable () -> Unit) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onClose,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("高级", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        androidx.compose.material3.TextButton(onClick = onClose) { Text("← 返回") }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                content()
            }
        }
    }
}
