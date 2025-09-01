package com.example.gra.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieClipSpec
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.gra.R
import com.example.gra.ui.viewmodel.GrowthViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.floor
// 原有的你可能已经有，这里一并列出，重复的没关系，IDE 会自动去掉
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.window.Dialog
import com.example.gra.ui.data.Remote
import com.example.gra.ui.viewmodel.FriendsViewModel



data class Segment(val fromSec: Float, val toSec: Float)

val TREE_SEGMENTS = listOf(
    Segment(0f, 3f),
    Segment(3f, 6f),
    Segment(6f, 7.5f),
    Segment(7.5f, 9.5f),
    Segment(9.5f, 11.1f),
    Segment(11.1f, 13.8f),
    Segment(13.8f, 16.5f),
    Segment(16.5f, 19.5f)
)

// 示例默认值（仅示意，你自己改）
val LEAF_FX_PER_STAGE: List<LeafFx> = listOf(
    LeafFx(enabled = false),                          // 0：树太小，不触发
    LeafFx(enabled = false),                          // 1：不触发
    LeafFx(enabled = true,  scale = 0.5f, offsetY = 25.dp),
    LeafFx(enabled = true,  scale = 0.6f, offsetY = 40.dp), //3
    LeafFx(enabled = true,  scale = 0.8f, offsetY = 70.dp),
    LeafFx(enabled = true,  scale = 1f, offsetY = 80.dp), //5
    LeafFx(enabled = true,  scale = 1.2f, offsetY = 100.dp, speed = 0.9f),
    LeafFx(enabled = true,  scale = 1.3f,offsetY = 100.dp, speed = 0.9f)
)

enum class FlipMode { ALWAYS_NORMAL, ALWAYS_FLIPPED, ALTERNATE, RANDOM }

data class LeafFx(
    val enabled: Boolean = true,
    val scale: Float = 1f,
    val offsetX: Dp = 0.dp,
    val offsetY: Dp = (-8).dp,
    val speed: Float = 1.0f,
    val alpha: Float = 1.0f,
    val zIndex: Float = 1f
)

// 运行时的一个“落叶实例”
data class ActiveLeaf(
    val id: Int,
    val cfg: LeafFx,
    val flipped: Boolean          // true=水平镜像
)

val topBlue = Color(0xFFBFDFFF)    // 浅蓝，带点天蓝色
val bottomGreen = Color(0xFFCCF2D1) // 浅绿，柔和青草色

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainPage(navController: NavHostController, initialShow: String? = null)
 {

     var showFriends by rememberSaveable { mutableStateOf(false) }
     var showGroups by rememberSaveable { mutableStateOf(false) }
     LaunchedEffect(initialShow) {
         when (initialShow) {
             "friends" -> showFriends = true
             "groups"  -> showGroups = true
         }
     }


    val grassStart = Color(0xFF8FD596)
    val grassEnd   = Color(0xFF5DB667)

    // ✅ 只在顶层创建一次，保证全页使用同一个 GrowthViewModel 和 TreeStageViewModel
    val growVm: GrowthViewModel = viewModel()
    val stageVm: GrowthViewModel.TreeStageViewModel = viewModel()

    // ✅ 不要用 remember 固化 uid；直接取当前用户（或用 rememberUpdatedState 也可）
    val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    // ✅ 新增：是否正在播放雨
    // 运行时“雨实例池”
    val rains = remember { mutableStateListOf<Int>() }
    var nextRainId by remember { mutableStateOf(1) }
    // ✅ 新增：外部“只呼吸一次”的信号计数器
    var breathTick by remember { mutableStateOf(0) }

    // ✅ 监听播放状态（用于禁用按钮）
    val playing by stageVm.playingFlow.collectAsState()

    // ✅ 在页面可见时启动一次监听
    LaunchedEffect(uid) {
        if (uid.isNotBlank()) growVm.start(uid)
    }

    Box(Modifier.fillMaxSize()) {
        // 底层：天空渐变
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(topBlue, bottomGreen)
                    )
                )
        )

        // 草地：宽度全屏，高度 130dp，圆角 300dp
        RoundedRectLayer(
            widthFraction = 1f,
            heightDp = 150,     // 草地整体高度
            topDp = 300,    // 顶部圆角半径（越大弧度越圆）
            offsetYDp = 30,
            colors = listOf(grassStart, grassEnd),
        )

        // 椭圆坑：画在草地之上、树之下
        EllipsePit(
            modifier = Modifier.fillMaxSize(),
            widthFraction = 0.28f,   // 宽一些
            heightDp = 30,           // 更扁
            offsetYDp = -85,         // 抬高让它“嵌”在草地里
        )

        Scaffold(
            containerColor = Color.Transparent,
            //bottomBar = { BottomNavigationBar(navController) }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                //按钮
                YourHomeSection(
                    growVm = growVm,
                    stageVm = stageVm,
                    uid = uid,
                    navController = navController,
                    onOpenFriends = { showFriends = true },  // 顶层开关
                    onOpenGroups  = { showGroups  = true }   // 顶层开关
                )

                //树木
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .offset(x = 8.dp, y = 10.dp)
                ) {
                    TreeStageController(
                        growVm = growVm,
                        stageVm = stageVm,
                        uid = uid,
                        breathKey = breathTick
                    )
                    // 叠加所有正在下雨的实例
                    rains.forEach { rid ->
                        RainOnceOverlay(
                            id = rid,
                            speed = 0.8f,
                            fillWidthFraction = 0.6f,       // 铺满宽
                            offsetY = 94.dp,               // 需要就调
                            onFinished = { doneId ->
                                rains.removeAll { it == doneId }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
        // 左下角：成长 / 重置
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 成长（测试用）
            Button(
                onClick = {
                    if (uid.isBlank()) return@Button
                    val start = stageVm.stageIndex
                    // 与原 GrowthControls 一致：先强制升级，再从当前段播放到下一段
                    growVm.forceLevelUp(uid) {
                        stageVm.markManualOnce()
                        stageVm.playFrom(start)
                    }
                },
                enabled = !playing && stageVm.stageIndex < TREE_SEGMENTS.lastIndex
            ) { Text("成长") }

            // 重置
            Button(
                onClick = {
                    stageVm.reset()
                    if (uid.isNotBlank()) growVm.resetLevel0(uid)
                },
                enabled = !playing
            ) { Text("重置") }
        }

        // 右下角：浇水大圆按钮
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(72.dp)
                .clip(CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                .clickable {
                    // 1) 下雨动画
                    rains += nextRainId++
                    // 2) 树呼吸一次（只呼吸，不落叶）
                    breathTick += 1
                    // 3) 任务标记已完成（每天一次，重复点击保持完成）
                    val today = java.time.LocalDate.now().toString()
                    growVm.markWaterDone(uid, today)
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.water3),
                contentDescription = "浇水",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
        }
    }
     // 悬浮窗（Dialog）——使用顶层的布尔值
     if (showFriends) {
         FriendsDialog(onDismiss = { showFriends = false })
     }
     if (showGroups) {
         GroupsDialog(onDismiss = { showGroups = false })
     }
}

@Composable
private fun TreeStageController(
    growVm: GrowthViewModel,
    stageVm: GrowthViewModel.TreeStageViewModel,
    uid: String,
    breathKey: Int
) {
    val tree by growVm.tree.collectAsState()
    val playing by stageVm.playingFlow.collectAsState()
    val manual by stageVm.manualFlow.collectAsState()

    // 真实 level 回流后，仅在“未播放”时同步静态阶段，避免回跳
    LaunchedEffect(tree.level, playing) {
        if (!playing && tree.level >= 0) {
            stageVm.setStageFromLevel(tree.level)
        }
    }

    TreeStageArea(
        stageIndex = stageVm.stageIndex,
        playing = playing,
        onPlayFinished = {
            stageVm.onOneSegmentFinished()

            // ✅ 动画刚播完就先“乐观切换”到下一阶段静止帧（避免看到旧阶段）
            val next = (stageVm.stageIndex + 1).coerceAtMost(TREE_SEGMENTS.lastIndex)
            stageVm.setStageFromLevel(next)

            // 不再在这里升级：Feed/成长都已在点击时“先升级再播放”完成
            stageVm.clearManual()
        },
        spec = LottieCompositionSpec.Asset("tree.json"),
        modifier = Modifier.fillMaxSize(),
        externalBreathKey = breathKey
    )
}


@Composable
fun YourHomeSection(
    growVm: GrowthViewModel,
    stageVm: GrowthViewModel.TreeStageViewModel,
    uid: String,
    navController: NavHostController,
    onOpenFriends: () -> Unit,   // ← 新增
    onOpenGroups: () -> Unit     // ← 新增
) {
    val tree by growVm.tree.collectAsState()

    TreeSection(
        state = tree,
        onFeed = {
            if (uid.isBlank()) return@TreeSection
            // 先把 pending → fed（你的 feedAll 已经不升级，仅加 fed）
            growVm.feedAll(uid) { r ->
                // 如果这次换算后 fed 达标，立即“先升级，再播放”
                if (r.newFed >= 1000 && !stageVm.playing) {
                    val start = stageVm.stageIndex
                    // 先升级（扣 1000，level+1）：这样避免重复触发与回跳
                    growVm.upgrade(uid) {
                        // 升级提交后立刻播放 start -> start+1（播放中不被回流覆盖）
                        stageVm.playFrom(start)
                    }
                }
            }
        }
    )
    // ▶ 在 TreeSection “下方”加一个容器，把菜单贴到右上
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp) // 与整体左右留白一致
    ) {
        //右上角按钮
        VerticalActionMenu(
            navController = navController,
            modifier = Modifier
                .align(Alignment.TopEnd)
        )
        // 左上角：新增“好友 / 群组”
        Column(
            modifier = Modifier.align(Alignment.TopStart),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircleIconWithText(
                label = "好友",
                icon = Icons.Default.ThumbUp,
                onClick = { onOpenFriends() }
            )
            CircleIconWithText(
                label = "群组",
                icon = Icons.Default.Home,
                onClick = { onOpenGroups() }
            )

        }
    }
}

@Composable
fun TreeSection(
    state: com.example.gra.ui.data.Remote.TreeState,
    onFeed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = (state.fed / 1000f).coerceIn(0f, 1f)
    val level = "Lv.${state.level}"
    val expText = "${state.fed}/1000"

    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        /*
        // 等级：大号 + 居中
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                text = "Lv.${state.level}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

         */

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 进度条容器：圆角 + 主题色包边 + 透明轨道
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(24.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(15.dp)
                    )
            ) {
                // 进度条本体（轨道透明）
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.matchParentSize(),
                    trackColor = Color.Transparent,
                    color = MaterialTheme.colorScheme.primary
                )

                // —— 居中文本：底层常规色 —— //
                Box(
                    modifier = Modifier.matchParentSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$level  ·  $expText",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // —— 居中文本：顶层“反色”，仅在进度范围内显示 —— //
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .drawWithContent {
                            // 仅绘制左侧进度宽度的区域
                            clipRect(
                                left = 0f,
                                top = 0f,
                                right = size.width * progress,
                                bottom = size.height
                            ) {
                                this@drawWithContent.drawContent()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$level  ·  $expText",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White, // 或 MaterialTheme.colorScheme.onPrimary
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Button(
                onClick = onFeed,
                enabled = state.pending > 0
            ) {
                Text(text = "+${state.pending}")
            }
        }
    }
}

@Composable
fun VerticalActionMenu(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircleIconWithText(
            label = "记录",
            icon = Icons.Default.List,
            onClick = {
                navController.navigate("records") {
                    popUpTo("records") { inclusive = true }
                }
            }
        )

        CircleIconWithText(
            label = "任务",
            icon = Icons.Default.CheckCircle,
            onClick = { navController.navigate("tasks") }
        )

        CircleIconWithText(
            label = "我的",
            icon = Icons.Default.Person,
            onClick = {
                navController.navigate("mine") {
                    popUpTo("mine") { inclusive = true }
                }
            }
        )
    }
}

@Composable
private fun CircleIconWithText(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    size: Dp = 48.dp
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape) // ✅ 圆形包边
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary  // ✅ 用主题色突出
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}


@Composable
fun BottomNavigationBar(navController: NavController) {
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "养成") },
            label = { Text("养成") },
            selected = false,
            onClick = {
                navController.navigate("main") {
                    popUpTo("main") { inclusive = true }
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.List, contentDescription = "记录") },
            label = { Text("记录") },
            selected = false,
            onClick = {
                navController.navigate("records") {
                    popUpTo("records") { inclusive = true }
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = "任务") },
            label = { Text("任务") },
            selected = false,
            onClick = {
                navController.navigate("tasks")
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Person, contentDescription = "我的") },
            label = { Text("我的") },
            selected = false,
            onClick = {
                navController.navigate("mine") {
                    popUpTo("mine") { inclusive = true }
                }
            }
        )
    }
}

@Composable
fun TreeStageArea(
    // 当前阶段（0..7）
    stageIndex: Int,
    // 是否正在播放“成长动画段”
    playing: Boolean,
    // 播放结束回调（用于把 playing=false、阶段+1）
    onPlayFinished: () -> Unit,
    // 资源：Asset 或 RawRes 二选一，按你放文件的方式来
    spec: LottieCompositionSpec = LottieCompositionSpec.Asset("tree.json"),
    treeScale: Float = 2f,
    leafFxByStage: List<LeafFx> = LEAF_FX_PER_STAGE,
    flipMode: FlipMode = FlipMode.ALTERNATE, //ALTERNATE / RANDOM / ALWAYS_NORMAL / ALWAYS_FLIPPED 交替 / 随机 / 始终正常 / 始终翻转
    modifier: Modifier = Modifier,
    externalBreathKey: Int = 0
) {

    val composition by rememberLottieComposition(spec)

    val scope = rememberCoroutineScope()
    val breathY = remember { Animatable(1f) } // 默认静止 = 1f
    // 运行时“落叶实例池”（并发）
    val leaves = remember { mutableStateListOf<ActiveLeaf>() }
    var nextId by remember { mutableStateOf(1) }

    // 交替模式下，记录上次是否翻转
    var lastFlipped by remember { mutableStateOf(false) }

    fun nextFlip(): Boolean = when (flipMode) {
        FlipMode.ALWAYS_NORMAL -> false
        FlipMode.ALWAYS_FLIPPED -> true
        FlipMode.RANDOM -> kotlin.random.Random.nextBoolean()
        FlipMode.ALTERNATE -> { lastFlipped = !lastFlipped; lastFlipped }
    }

    // ✅ 提取一个“只呼吸”的动作（不落叶）
    fun triggerBreathOnly() {
        scope.launch {
            breathY.animateTo(0.98f, tween(durationMillis = 280, easing = FastOutSlowInEasing))
            breathY.animateTo(1f,    tween(durationMillis = 440, easing = FastOutSlowInEasing))
        }
    }

    fun triggerBreathAndLeaves() {
        // 1) 呼吸（一次缩放）
        scope.launch {
            breathY.animateTo(0.98f, tween(durationMillis = 280, easing = FastOutSlowInEasing))
            breathY.animateTo(1f,    tween(durationMillis = 440, easing = FastOutSlowInEasing))
        }
        // 2) 落叶（可并发）：该阶段允许才添加一个新实例
        val cfg = leafFxByStage.getOrNull(stageIndex)
        if (cfg != null && cfg.enabled) {
            leaves += ActiveLeaf(
                id = nextId++,
                cfg = cfg,
                flipped = nextFlip()
            )
        }
    }
    // ✅ 当 externalBreathKey 变化→只做一次呼吸（不落叶）
    LaunchedEffect(externalBreathKey) {
        if (externalBreathKey > 0) {
            triggerBreathOnly()
        }
    }

    // 计算本阶段的裁剪 & 静止进度
    val clipSpec: LottieClipSpec.Frame? = remember(composition, stageIndex+1) {
        composition?.let { comp ->
            val seg = TREE_SEGMENTS.getOrNull(stageIndex+1) ?: return@let null
            val minF = secToFrame(comp, seg.fromSec)
            val maxF = secToFrame(comp, seg.toSec)
            LottieClipSpec.Frame(
                min = floor(minF).toInt(), // ✅ Int
                max = ceil(maxF).toInt()   // ✅ Int
            )
        }
    }

    // 该阶段停留的“最终帧进度”
    val holdProgress: Float? = remember(composition, stageIndex) {
        composition?.let { comp ->
            val seg = TREE_SEGMENTS.getOrNull(stageIndex) ?: return@let null
            val endFrame = secToFrame(comp, seg.toSec)
            frameToProgress(comp, endFrame)
        }
    }

    // 播放状态（只在 playing=true 时创建，播放一次）
    val lottieAnimState =
        if (playing && composition != null && clipSpec != null)
            animateLottieCompositionAsState(
                composition = composition,
                iterations = 1,
                clipSpec = clipSpec,
                speed = 1f,
                restartOnPlay = true
            )
        else null

    // 监听播放结束：走到片段末尾就回调
    LaunchedEffect(lottieAnimState?.isAtEnd, lottieAnimState?.isPlaying) {
        if (lottieAnimState != null && lottieAnimState.isAtEnd && !lottieAnimState.isPlaying) {
            onPlayFinished()
        }
    }

    // 布局：让树的底端“贴住” BottomBar 的上缘
    // 关键：这个 Composable 放在 Scaffold 的 innerPadding 内部，并且 align 到 BottomCenter
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        if (composition != null && ((playing && lottieAnimState != null) || (!playing && holdProgress != null))) {
            LottieAnimation(
                composition = composition,
                progress = {
                    if (playing && lottieAnimState != null) lottieAnimState.progress else holdProgress ?: 0f
                },
                modifier = Modifier
                    .fillMaxWidth(0.93f)
                    .aspectRatio(1f)
                    // 点击触发“呼吸”
                    .clickable(
                        enabled = true, // 若不想播放时可点：改为 !playing
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { triggerBreathAndLeaves() }
                    // 一个图层里统一设置，保证以底边为锚点缩放
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(0.5f, 1f) // 底部中心
                        clip = false

                        // X 方向保持 treeScale，Y 方向叠加点击呼吸系数
                        scaleX = treeScale
                        scaleY = treeScale * breathY.value
                    }
            )
        }
        // —— 并发渲染所有“活跃”的落叶实例 ——
        // 想让落叶在树后面：把这段移到树前面，或把 cfg.zIndex 设更小
        leaves.forEach { inst ->
            key(inst.id) {  // 确保 Compose 正确区分每个实例
                LeavesOnceOverlay(
                    instance = inst,
                    assetName = "leaves.json",
                    onFinished = { finishedId ->
                        leaves.removeAll { it.id == finishedId }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}


private fun secToFrame(comp: LottieComposition, sec: Float): Float {
    val start = comp.startFrame
    val end   = comp.endFrame
    val fps   = comp.frameRate
    return (start + sec * fps).coerceIn(start, end)
}

private fun frameToProgress(comp: LottieComposition, frame: Float): Float {
    val start = comp.startFrame
    val end   = comp.endFrame
    return ((frame - start) / (end - start)).coerceIn(0f, 1f)
}

@Composable
fun RoundedRectLayer(
    modifier: Modifier = Modifier,
    widthFraction: Float = 1f,   // 占屏宽比例
    heightDp: Int = 200,         // 高度
    topDp: Int = 160,         // 顶部圆角半径
    bottomDp: Int = 0,      // 底部圆角半径
    offsetYDp: Int = 0,          // 垂直偏移（负值=往上抬）
    colors: List<Color>          // 渐变色
) {
    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .height(heightDp.dp)
                .align(Alignment.BottomCenter)
                .offset(y = offsetYDp.dp)
                .clip(RoundedCornerShape(topStart = topDp.dp, topEnd = topDp.dp, bottomEnd = bottomDp.dp, bottomStart = bottomDp.dp) )
                .background(
                    Brush.verticalGradient(colors)
                )
        )
    }
}

@Composable
fun EllipsePit(
    modifier: Modifier = Modifier,
    widthFraction: Float = 0.2f,         // 椭圆宽度占屏宽的比例
    heightDp: Int = 48,                   // 椭圆高度（数值越小越“扁”）
    offsetYDp: Int = -100,                  // 相对底部的偏移（负值=向上抬）
    // 主体渐变（上→下），深一点会更像坑
    colors: List<Color> = listOf(
        Color(0xFFD7CCC8), // 沙土色
        Color(0xFF8D6E63)  // 棕色
    ),
    // 可选：中间偏亮的高光，增加层次（alpha 小）
    highlightColor: Color = Color.White.copy(alpha = 0.08f),
    // 可选：外沿暗边（用深色+透明做一圈）
    rimDarkColor: Color = Color.Black.copy(alpha = 0.10f)
) {
    val heightPx = with(LocalDensity.current) { heightDp.dp.toPx() }
    val offsetYPx = with(LocalDensity.current) { offsetYDp.dp.toPx() }

    Canvas(modifier) {
        val w = size.width * widthFraction
        val h = heightPx

        // 让椭圆居中并贴近底部（向上抬 offsetYPx）
        val left = (size.width - w) / 2f
        val top  = size.height - h + offsetYPx
        val ovalRect = Rect(left = left, top = top, right = left + w, bottom = top + h)

        // 1) 主体：竖向渐变的椭圆
        drawOval(
            brush = Brush.verticalGradient(colors),
            topLeft = Offset(ovalRect.left, ovalRect.top),
            size = Size(ovalRect.width, ovalRect.height)
        )

        // 2) 内部高光：更小一点的椭圆（靠上），让坑看起来更柔和
        val hiInsetW = w * 0.12f
        val hiInsetH = h * 0.35f
        val hiRect = Rect(
            left = left + hiInsetW,
            top = top + hiInsetH,
            right = left + w - hiInsetW,
            bottom = top + h - hiInsetH
        )
        drawOval(
            brush = Brush.verticalGradient(
                listOf(highlightColor, Color.Transparent)
            ),
            topLeft = Offset(hiRect.left, hiRect.top),
            size = Size(hiRect.width, hiRect.height)
        )

        // 3) 外沿暗边：略大一点的椭圆，透明向外扩散，制造“压边阴影”
        val rimPad = 4f
        val rimRect = Rect(
            left = left - rimPad,
            top = top - rimPad,
            right = left + w + rimPad,
            bottom = top + h + rimPad
        )
        drawOval(
            brush = Brush.verticalGradient(
                listOf(rimDarkColor, Color.Transparent)
            ),
            topLeft = Offset(rimRect.left, rimRect.top),
            size = Size(rimRect.width, rimRect.height)
        )
    }
}

@Composable
fun LeavesOnceOverlay(
    instance: ActiveLeaf,
    assetName: String = "leaves.json",
    onFinished: (Int) -> Unit,     // 播完后回调，把该实例从列表里移除
    modifier: Modifier = Modifier
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.Asset(assetName))
    val isPlaying = remember { mutableStateOf(true) }
    val lottie = if (composition != null)
        animateLottieCompositionAsState(
            composition = composition,
            iterations = 1,
            speed = instance.cfg.speed,
            restartOnPlay = true,
            isPlaying = isPlaying.value
        ) else null

    LaunchedEffect(lottie?.isAtEnd, lottie?.isPlaying) {
        if (lottie != null && lottie.isAtEnd && !lottie.isPlaying) {
            isPlaying.value = false
            onFinished(instance.id)
        }
    }

    if (composition == null || lottie == null || !isPlaying.value) return

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        LottieAnimation(
            composition = composition,
            progress = { lottie.progress },
            modifier = Modifier
                .zIndex(instance.cfg.zIndex)
                .offset(x = instance.cfg.offsetX, y = instance.cfg.offsetY)
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0.5f, 1f)
                    clip = false
                    alpha = instance.cfg.alpha
                    // 水平翻转：scaleX 取负值即可；以底部为锚点，不会“飘脚”
                    scaleX = (if (instance.flipped) -instance.cfg.scale else instance.cfg.scale)
                    scaleY = instance.cfg.scale
                }
        )
    }
}

@Composable
fun RainOnceOverlay(
    id: Int,
    assetName: String = "rain.json",
    speed: Float = 1f,
    zIndex: Float = 20f,
    alignment: Alignment = Alignment.TopCenter,
    fillWidthFraction: Float = 1f,
    offsetX: Dp = 0.dp,
    offsetY: Dp = 0.dp,
    onFinished: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.Asset(assetName))
    val isPlaying = remember { mutableStateOf(true) }
    val lottie = if (composition != null)
        animateLottieCompositionAsState(
            composition = composition,
            iterations = 1,
            speed = speed,
            restartOnPlay = true,
            isPlaying = isPlaying.value
        ) else null

    LaunchedEffect(lottie?.isAtEnd, lottie?.isPlaying) {
        if (lottie != null && lottie.isAtEnd && !lottie.isPlaying) {
            isPlaying.value = false
            onFinished(id)
        }
    }

    if (composition == null || lottie == null || !isPlaying.value) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(zIndex),
        contentAlignment = alignment
    ) {
        LottieAnimation(
            composition = composition,
            progress = { lottie.progress },
            modifier = Modifier
                .fillMaxWidth(fillWidthFraction)
                .offset(x = offsetX, y = offsetY)
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0.5f, 0f)
                    clip = false
                    this.alpha = 1f
                }
        )
    }
}

@Composable
fun FriendsDialog(
    onDismiss: () -> Unit
) {
    val ctx = LocalContext.current
    val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    val vm: com.example.gra.ui.viewmodel.FriendsViewModel = viewModel()
    LaunchedEffect(uid) { vm.start(uid) }

    var tab by remember { mutableStateOf(0) }
    var searchText by remember { mutableStateOf("") }

    val friends by vm.friends.collectAsState()
    val profiles by vm.profiles.collectAsState()
    val trees by vm.trees.collectAsState()
    val busy by vm.busy.collectAsState()
    val deleteMode by vm.deleteMode.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth(1.0f)   // ✅ 更大
                .fillMaxHeight(0.80f)  // ✅ 更高
        ) {
            // 用 Box 方便放“悬浮”按钮
            Box(Modifier.fillMaxSize()) {

                // 主内容：标题 + Tabs + 列表/输入
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Spacer(Modifier.height(10.dp))
                    TabRow(selectedTabIndex = tab) {
                        Tab(text = { Text("已添加") }, selected = tab==0, onClick = { tab = 0 })
                        Tab(text = { Text("待处理") }, selected = tab==1, onClick = { tab = 1 })
                        Tab(text = { Text("添加") }, selected = tab==2, onClick = { tab = 2 })
                    }
                    Spacer(Modifier.height(10.dp))

                    when (tab) {
                        // ======== 已添加 ========
                        0 -> {
                            if (friends.isEmpty()) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("暂无好友")
                                }
                            } else {
                                // 列表区域给底部悬浮按钮留出空间（底部内边距 88dp）
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(bottom = 88.dp)
                                ) {
                                    items(friends) { f ->
                                        val p = profiles[f.uid]
                                        val t = trees[f.uid]
                                        val unique = p?.uniqueId ?: f.uniqueId ?: f.uid

                                        // 是否可赠送（判断是否今天已送）
                                        val canGift = remember(f.lastGiftToFriend) {
                                            val ts = f.lastGiftToFriend?.toDate()
                                            if (ts == null) true else {
                                                val cal = java.util.Calendar.getInstance().apply { time = ts }
                                                val giftedKey = cal.get(java.util.Calendar.YEAR) * 10000 +
                                                        (cal.get(java.util.Calendar.MONTH) + 1) * 100 +
                                                        cal.get(java.util.Calendar.DAY_OF_MONTH)
                                                val now = java.util.Calendar.getInstance()
                                                val todayKey = now.get(java.util.Calendar.YEAR) * 10000 +
                                                        (now.get(java.util.Calendar.MONTH) + 1) * 100 +
                                                        now.get(java.util.Calendar.DAY_OF_MONTH)
                                                giftedKey != todayKey
                                            }
                                        }

                                        FriendRow(
                                            uniqueId = unique,
                                            level = t?.level ?: 0,
                                            fed = t?.fed ?: 0,
                                            pendingFromFriend = f.pendingFromFriend,
                                            canGiftToday = canGift,
                                            deleteMode = deleteMode,
                                            onGift = {
                                                vm.gift(uid, f.uid) { Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show() }
                                            },
                                            onClaim = {
                                                vm.claim(uid, f.uid) { Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show() }
                                            },
                                            onDelete = {
                                                vm.remove(uid, f.uid) { Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show() }
                                            }
                                        )
                                        Divider()
                                    }
                                }
                            }
                        }

                        // ======== 待处理 ========
                        1 -> {
                            val requests by vm.requests.collectAsState()
                            if (requests.isEmpty()) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("没有新的申请")
                                }
                            }else {
                                LazyColumn(Modifier.fillMaxSize()) {
                                    items(requests) { r ->
                                        ListItem(
                                            headlineContent = { Text(r.fromUniqueId ?: r.fromUid) },
                                            trailingContent = {
                                                Row {
                                                    TextButton(
                                                        onClick = {
                                                            vm.accept(uid, r.fromUid) {
                                                                Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show()
                                                            }
                                                        },
                                                        enabled = !busy
                                                    ) { Text("同意") }
                                                    TextButton(
                                                        onClick = {
                                                            vm.reject(uid, r.fromUid) {
                                                                Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show()
                                                            }
                                                        },
                                                        enabled = !busy
                                                    ) { Text("拒绝") }
                                                }
                                            }
                                        )
                                        Divider()
                                    }
                                }
                            }
                        }

                        // ======== 添加 ========
                        2 -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(bottom = 88.dp)
                            ) {
                                OutlinedTextField(
                                    value = searchText,
                                    onValueChange = { searchText = it },
                                    singleLine = true,
                                    label = { Text("通过对方唯一ID搜索") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        vm.send(uid, searchText.trim()) {
                                            Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    enabled = !busy && searchText.isNotBlank(),
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text(if (busy) "发送中…" else "发送好友申请") }

                                Spacer(Modifier.height(10.dp))
                                Text("提示：唯一ID区分大小写显示，但搜索不区分大小写。", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                // ===== 悬浮操作区 =====

                // 左下角：删除模式切换
                OutlinedIconButton(
                    onClick = { vm.toggleDeleteMode() },
                    enabled = !busy,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = if (deleteMode) "退出删除模式" else "删除好友"
                    )
                }

                // 右下角：一键赠送 / 一键领取（垂直排列）
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    //verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalIconButton(
                        onClick = {
                            vm.giftAll(uid) { Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show() }
                        },
                        enabled = !busy
                    ) { Icon(Icons.Default.Favorite, contentDescription = "一键赠送") }

                    FilledIconButton(
                        onClick = {
                            vm.claimAll(uid) { Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show() }
                        },
                        enabled = !busy
                    ) { Icon(Icons.Default.Done, contentDescription = "一键领取") }
                }
            }
        }
    }
}


@Composable
private fun FriendRow(
    uniqueId: String,
    level: Int,
    fed: Int,
    pendingFromFriend: Int,   // 待领取数
    canGiftToday: Boolean,    // 今天是否还能赠送
    deleteMode: Boolean,      // 删除模式开关
    onGift: () -> Unit,
    onClaim: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 10.dp)
    ) {
        // 第一行：唯一ID + 右侧图标操作
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = uniqueId,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )

            if (deleteMode) {
                // 只显示“删除好友”icon
                OutlinedIconButton(onClick = onDelete) {
                    Icon(Icons.Default.Close, contentDescription = "删除好友")
                }
            } else {
                // 赠送：icon（今天送过则禁用）
                FilledTonalIconButton(
                    onClick = onGift,
                    enabled = canGiftToday
                ) {
                    Icon(Icons.Default.Favorite, contentDescription = "赠送 +5")
                }

                Spacer(Modifier.width(8.dp))

                // 领取：icon + 角标（显示可领取数量）
                BadgedBox(
                    badge = {
                        if (pendingFromFriend > 0) {
                            Badge { Text(pendingFromFriend.toString()) }
                        }
                    }
                ) {
                    OutlinedIconButton(
                        onClick = onClaim,
                        enabled = pendingFromFriend > 0
                    ) {
                        Icon(Icons.Default.Star, contentDescription = "领取")
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // 第二行：反色进度条（Lv.X · fed/1000）
        val progress = (fed / 1000f).coerceIn(0f, 1f)
        val expText = "${fed}/1000"

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(22.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(
                    width = 1.2.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.matchParentSize(),
                trackColor = Color.Transparent,
                color = MaterialTheme.colorScheme.primary
            )
            Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Lv.$level  ·  $expText",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .drawWithContent {
                        clipRect(
                            left = 0f, top = 0f,
                            right = size.width * progress,
                            bottom = size.height
                        ) { this@drawWithContent.drawContent() }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Lv.$level  ·  $expText",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun GroupsDialog(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    val vm: com.example.gra.ui.viewmodel.GroupsViewModel = viewModel()
    LaunchedEffect(uid) { vm.start(uid) }

    var tab by remember { mutableStateOf(0) }
    var roomName by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }

    val myRooms by vm.myRooms.collectAsState()
    val results by vm.searchResults.collectAsState()
    val busy by vm.busy.collectAsState()

    val selectedRoomId by vm.selectedRoomId.collectAsState()
    val members by vm.members.collectAsState()
    val profiles by vm.profiles.collectAsState()
    val trees by vm.trees.collectAsState()
    val checkedCount by vm.todayCheckedCount.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth(1.0f).fillMaxHeight(0.80f)
        ) {
            Column(Modifier.fillMaxSize().padding(16.dp)) {

                // 顶部：两种模式切换
                if (selectedRoomId == null) {
                    Spacer(Modifier.height(10.dp))
                    TabRow(selectedTabIndex = tab) {
                        Tab(text = { Text("已加入") }, selected = tab == 0, onClick = { tab = 0 })
                        Tab(text = { Text("创建") }, selected = tab == 1, onClick = { tab = 1 })
                        Tab(text = { Text("加入") }, selected = tab == 2, onClick = { tab = 2 })
                    }
                    Spacer(Modifier.height(10.dp))
                } else {
                    // 详情页顶部：返回 + 居中房间名
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { vm.closeRoom() }) {
                            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "返回")
                        }
                        val rn = myRooms.firstOrNull { it.id == selectedRoomId }?.name ?: selectedRoomId!!
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Text(rn, style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(Modifier.width(48.dp)) // 右侧留白，近似对称
                    }
                }

                // 主体
                if (selectedRoomId == null) {
                    when (tab) {
                        0 -> { // 已加入列表：点击进入详情
                            if (myRooms.isEmpty()) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("尚未加入任何群组")
                                }
                            } else {
                                LazyColumn(Modifier.fillMaxSize()) {
                                    items(myRooms) { r ->
                                        Row(
                                            Modifier
                                                .fillMaxWidth()
                                                .clickable { vm.openRoom(r.id) }  // ← 点击进入详情
                                                .padding(vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(Modifier.weight(1f)) {
                                                Text("${r.name}  (#${r.id})", style = MaterialTheme.typography.titleMedium)
                                                Text("角色：${r.role ?: "member"}", style = MaterialTheme.typography.bodySmall)
                                            }
                                            if (r.role == "owner") {
                                                OutlinedButton(enabled = !busy, onClick = {
                                                    vm.dissolve(uid, r.id) { Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show() }
                                                }) { Text("解散") }
                                            } else {
                                                OutlinedButton(enabled = !busy, onClick = {
                                                    vm.leave(uid, r.id) { Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show() }
                                                }) { Text("退出") }
                                            }
                                        }
                                        Divider()
                                    }
                                }
                            }
                        }
                        // 创建
                        1 -> {
                            Column(Modifier.fillMaxSize()) {
                                OutlinedTextField(
                                    value = roomName,
                                    onValueChange = { roomName = it },
                                    singleLine = true,
                                    label = { Text("房间名称") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(8.dp))
                                val scope = rememberCoroutineScope()
                                Button(
                                    onClick = {
                                        vm.create(uid, roomName) { msg ->
                                            Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    enabled = !busy && roomName.isNotBlank(),
                                    modifier = Modifier.fillMaxWidth(),
                                ) { Text("创建") }
                                Spacer(Modifier.height(8.dp))
                                Text("创建成功会返回一个唯一的房间ID，加入可用 ID 或名称精确匹配。", style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        // 加入
                        2 -> {
                            Column(Modifier.fillMaxSize()) {
                                OutlinedTextField(
                                    value = query,
                                    onValueChange = { query = it },
                                    singleLine = true,
                                    label = { Text("通过房间ID或名称搜索") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = { vm.search(query) },
                                    enabled = query.isNotBlank(),
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("搜索") }

                                Spacer(Modifier.height(12.dp))

                                if (results.isEmpty()) {
                                    Text(
                                        "未找到匹配的房间",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                } else {
                                    LazyColumn(Modifier.fillMaxWidth()) {
                                        items(results) { r ->
                                            Row(
                                                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(Modifier.weight(1f)) {
                                                    Text(
                                                        "${r.name}  (#${r.id})",
                                                        style = MaterialTheme.typography.titleMedium
                                                    )
                                                    Text(
                                                        "房主：${r.ownerUid.take(6)}…",
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                }
                                                Button(
                                                    enabled = !busy,
                                                    onClick = {
                                                        vm.join(uid, r.id) { msg ->
                                                            Toast.makeText(
                                                                ctx,
                                                                msg,
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                    }
                                                ) { Text("加入") }
                                            }
                                            Divider()
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // ====== 详情页：成员列表 + 左下退出 + 右下统计 ======
                    Box(Modifier.fillMaxSize()) {

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 88.dp)
                        ) {
                            items(members, key = { it.uid }) { m ->
                                val p = profiles[m.uid]
                                val t = trees[m.uid]
                                val unique = p?.uniqueId ?: m.uid
                                val level = t?.level ?: 0
                                val fed = t?.fed ?: 0

                                val today = remember {
                                    val c = java.util.Calendar.getInstance()
                                    c.get(java.util.Calendar.YEAR) * 10000 +
                                            (c.get(java.util.Calendar.MONTH) + 1) * 100 +
                                            c.get(java.util.Calendar.DAY_OF_MONTH)
                                }
                                val checked = m.checkDate == today
                                val mine = (m.uid == uid)

                                val room by vm.selectedRoom.collectAsState()
                                val ownerUid = room?.ownerUid
                                val isOwner = (m.uid == ownerUid)

                                GroupMemberRow(
                                    uniqueId = unique,
                                    level = level,
                                    fed = fed,
                                    isOwner = isOwner,
                                    isMe = mine,
                                    checkedToday = checked,
                                    busy = busy,
                                    onCheckin = {
                                        vm.checkin(uid) { Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show() }
                                    }
                                )
                                Divider()
                            }
                        }

                        // 左下角：退出/解散（看角色）
                        val meRole = myRooms.firstOrNull { it.id == selectedRoomId }?.role
                        OutlinedButton(
                            onClick = {
                                val rid = selectedRoomId!!
                                if (meRole == "owner") {
                                    vm.dissolve(uid, rid) { Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show() }
                                } else {
                                    vm.leave(uid, rid) { Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show() }
                                }
                            },
                            enabled = !busy,
                            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
                        ) { Text(if (meRole == "owner") "解散该群" else "退出该群") }

                        // 右下角：统计
                        FilledTonalButton(
                            onClick = { /* no-op */ },
                            enabled = false,
                            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                        ) {
                            val total = members.size
                            Text("已打卡 $checkedCount / $total")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupMemberRow(
    uniqueId: String,
    level: Int,
    fed: Int,
    isOwner: Boolean,
    isMe: Boolean,
    checkedToday: Boolean,
    busy: Boolean,
    onCheckin: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 10.dp)
    ) {
        // —— 第 1 行：ID（群主小图标） + 右侧打卡按钮 —— //
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = uniqueId,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (isOwner) {
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Person,      // 小皇冠可换其他icon；这里用 Star 轻量明确
                        contentDescription = "群主",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (isMe) {
                Button(
                    onClick = onCheckin,
                    Modifier.height(36.dp),
                    shape = RoundedCornerShape(6.dp),
                    enabled = !busy && !checkedToday
                ) { Text(if (checkedToday) "已打卡" else "打卡") }
            } else {
                AssistChip(
                    onClick = { /* no-op */ },
                    enabled = false,
                    label = { Text(if (checkedToday) "已打卡" else "未打卡") }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // —— 第 2 行：Lv + fed 进度条（与 FriendRow 同款） —— //
        val progress = (fed / 1000f).coerceIn(0f, 1f)
        val expText = "Lv.$level  ·  ${fed}/1000"

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(22.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(
                    width = 1.2.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.matchParentSize(),
                trackColor = Color.Transparent,
                color = MaterialTheme.colorScheme.primary
            )
            Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = expText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .drawWithContent {
                        clipRect(
                            left = 0f, top = 0f,
                            right = size.width * progress,
                            bottom = size.height
                        ) { this@drawWithContent.drawContent() }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = expText,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
            }
        }
    }
}
