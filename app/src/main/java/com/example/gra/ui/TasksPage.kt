package com.example.gra.tasks

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.gra.ui.BottomNavigationBar
import com.example.gra.ui.bottomGreen
import com.example.gra.ui.data.Remote
import com.example.gra.ui.topBlue
import com.example.gra.ui.viewmodel.TasksViewModel
import com.google.firebase.auth.FirebaseAuth
import java.time.LocalDate

private val taskTitles = mapOf(
    "login" to "登录一次 App",
    "water" to "浇水一次",
    "meal" to "记录一次饮食",
    "workout" to "记录一次运动",
    "body" to "记录一次身体数据",
    "test" to "测试任务：每次+1",
    "feed" to "领取成长值：每次+1000（无限）",
    "gift_once" to "赠送一次好友成长值",
    "claim_once" to "领取一次好友成长值",
    "group_checkin" to "群内打卡一次",
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksPage(
    navController: NavHostController,
    vm: TasksViewModel = viewModel()
) {
    val context = LocalContext.current
    val uid = remember { FirebaseAuth.getInstance().currentUser?.uid.orEmpty() }
    val today = remember { LocalDate.now().toString() }

    // 启动监听 + 兜底刷新 + 登录任务标记
    LaunchedEffect(uid, today) {
        if (uid.isNotBlank()) {
            vm.start(uid, today)
            vm.markLoginDone(uid, today)
        }
    }

    val tasks by vm.tasks.collectAsState()
    val claiming by vm.claiming.collectAsState() // 新增

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("每日任务") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "返回")
                    }
                },
                modifier = Modifier.fillMaxWidth().shadow(8.dp)
            )
        }
    ) { innerPadding ->

        if (uid.isBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text("请先登录后查看每日任务")
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(topBlue, bottomGreen)
                    )
                )
                .padding(16.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                //verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(tasks) { t ->
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(4.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .background(Color.White)
                                .fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = taskTitles[t.id] ?: t.id,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "+${t.reward} 成长值",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                when {
                                    t.completed && !t.claimed -> {
                                        val enumId = Remote.TaskId.from(t.id)
                                        if (enumId == null) {
                                            OutlinedButton(onClick = {}, enabled = false) { Text("未知任务") }
                                        } else {
                                            val isLoading = claiming.contains(t.id)
                                            Button(
                                                onClick = {
                                                    vm.claim(
                                                        uid, today, enumId,
                                                        onReward = { gained ->
                                                            val msg = if (gained > 0) "领取成功：+$gained" else "已领取或未完成"
                                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                        },
                                                        onError = { e ->
                                                            Toast.makeText(
                                                                context,
                                                                "领取失败：${e.localizedMessage ?: e::class.simpleName}",
                                                                Toast.LENGTH_LONG
                                                            ).show()
                                                        }
                                                    )
                                                },
                                                enabled = !isLoading
                                            ) {
                                                if (isLoading) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(18.dp),
                                                        strokeWidth = 2.dp
                                                    )
                                                } else {
                                                    Text("领取")
                                                }
                                            }
                                        }
                                    }
                                    !t.completed -> {
                                        OutlinedButton(onClick = {
                                            when (t.id) {
                                                "water" -> navController.navigate("main")
                                                "meal" -> navController.navigate("food")
                                                "workout" -> navController.navigate("exercise")
                                                "body" -> navController.navigate("body")
                                                "gift_once", "claim_once" -> navController.navigate("main?show=friends")
                                                "group_checkin"           -> navController.navigate("main?show=groups")
                                            }
                                        }) { Text("去完成") }
                                    }
                                    else -> {
                                        OutlinedButton(onClick = {}, enabled = false) {
                                            Text("已领取")
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}
