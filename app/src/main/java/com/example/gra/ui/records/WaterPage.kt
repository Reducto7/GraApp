package com.example.gra.ui.records

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaterPage(
    navController: NavHostController,
    vm: WaterViewModel = viewModel()   // ✅ 符合你的页面写法
) {
    val ui by vm.ui.collectAsState()

    // 首次进入加载目标
    LaunchedEffect(Unit) { vm.loadGoal() }

    var showGoalEditor by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("喝水记录") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { pad ->
        Row(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(topBlue, bottomGreen)
                    )
                )
                .padding(horizontal = 16.dp, vertical = 44.dp)
                .padding(bottom = 48.dp)
        ) {
            // 左侧：纵向进度条（加粗 + primary 包边 + 背景透明）
            Box(
                Modifier
                    .width(42.dp) // ⬅️ 加粗
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                // 进度填充（仍然使用 primary；如果想更柔和可改为 .copy(alpha = 0.9f)）
                Box(
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(fraction = ui.progress)
                        .align(Alignment.BottomCenter)
                        .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f))
                )
            }

            Spacer(Modifier.width(24.dp))

            // 右侧：信息区
            Column(
                Modifier
                    .fillMaxSize()
            ) {
                // 右上角：当前饮水/目标 + 编辑
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "${ui.totalMl} / ${ui.goalMl} ml",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "今天 · ${ui.date}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { showGoalEditor = !showGoalEditor },
                        //enabled = ui.signedIn
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = "修改目标")
                    }
                }

                AnimatedVisibility(showGoalEditor) {
                    GoalEditCard(
                        current = ui.goalMl,
                        enabled = ui.signedIn,
                        onSave = {
                            vm.setGoal(it)
                            showGoalEditor = false
                        }
                    )
                }

                Spacer(Modifier.height(12.dp))

                // 添加喝水按钮
                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = ui.signedIn
                ) { Text("添加喝水") }

                if (showAddDialog) {
                    AddWaterDialog(
                        onDismiss = { showAddDialog = false },
                        onConfirm = { ml ->
                            vm.addWater(ml)
                            showAddDialog = false
                        }
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text("历史记录", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))

                // 只让历史区域滚动；容器 4.dp 圆角
                HistoryListCards(
                    entries = ui.entries,
                    onDelete = { ts -> vm.deleteEntry(ts) }   // ⬅️ 传给列表
                )
            }
        }
    }
}

@Composable
private fun GoalEditCard(current: Int, enabled: Boolean, onSave: (Int) -> Unit) {
    var text by remember(current) { mutableStateOf(current.coerceAtLeast(0).toString()) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.85f),                   // ⬅️ 纯白背景
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            OutlinedTextField(
                value = text,
                onValueChange = { new ->
                    val digits = new.filter { it.isDigit() }.take(5)
                    text = digits
                },
                label = { Text("目标(ml)") },
                singleLine = true,
                enabled = enabled
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { text = current.toString() }, enabled = enabled) { Text("重置") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { onSave(text.toIntOrNull() ?: current) }, enabled = enabled) { Text("保存") }
            }
        }
    }
}


@Composable
private fun AddWaterDialog(onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加喝水量") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.filter { c -> c.isDigit() }.take(5) },
                label = { Text("ml") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = {
                val ml = text.toIntOrNull() ?: 0
                onConfirm(ml)
            }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun HistoryListCards(
    entries: List<Pair<Int, Long>>,
    onDelete: (Long) -> Unit   // ⬅️ 新增
) {
    val fmt = remember { java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        //contentPadding = PaddingValues(12.dp),               // 列表与容器四周留白
        verticalArrangement = Arrangement.spacedBy(12.dp),    // 条目之间留空
    ) {
        if (entries.isEmpty()) {
            item {
                Text(
                    "暂无记录",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(entries.size) { index ->
                val (ml, ts) = entries[index]

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = Color.White   // ⬅️ 每条卡片白底
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "+$ml ml",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = fmt.format(java.util.Date(ts)),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        // ⬅️ 右侧删除按钮
                        IconButton(onClick = { onDelete(ts) }) {
                            Icon(Icons.Default.Close, contentDescription = "删除记录")
                        }
                    }
                }
            }
        }
    }
}

