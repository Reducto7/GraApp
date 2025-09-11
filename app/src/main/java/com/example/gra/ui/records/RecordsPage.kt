package com.example.gra.ui.records

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.gra.ui.viewmodel.BodyMeasureViewModel
import com.example.gra.ui.viewmodel.BodyType
import com.example.gra.ui.viewmodel.FoodViewModel
import com.example.gra.ui.viewmodel.SleepViewModel
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.sqrt
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.*

val topBlue = Color(0xFFBFDFFF)    // 浅蓝，带点天蓝色
val bottomGreen = Color(0xFFCCF2D1) // 浅绿，柔和青草色

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordsPage(
    navController: NavHostController,
    foodViewModel: FoodViewModel = viewModel()  // 用于读取今日摄入/消耗
) {
    // 加载今日数据
    val today = remember { LocalDate.now() }
    LaunchedEffect(today) {
        foodViewModel.loadDataByDate(today)
    }
    val intake = foodViewModel.totalIntakeKcal.value
    val burn = foodViewModel.totalBurnKcal.value

    // 拿到身体数据 VM（与 BodyMeasurePage 同一个类）
    val bodyVm: BodyMeasureViewModel = viewModel()
    //val targets by bodyVm.targets.collectAsState()
// 让 VM 切到“体重”维度
    LaunchedEffect(Unit) { bodyVm.select(BodyType.WEIGHT) }

// 收集体重历史（真实数据）
    val weightHistory by bodyVm.history.collectAsState()

    val waterVm: WaterViewModel = viewModel()
    val waterUi by waterVm.ui.collectAsState()
    LaunchedEffect(Unit) { waterVm.loadGoal() }

    // —— 睡眠数据（用 SleepViewModel 现有的 sessions） ——
    val sleepVm: SleepViewModel = viewModel()
    val sleepSessions by sleepVm.sessions.collectAsState(emptyList())

    // 1) 取健康档案（含 bmr / tdee / recoMaintain / planIntakeKcalPerDay / planBurnKcalPerDay）
    val health = bodyVm.health.collectAsState(initial = null).value

// 2) 计算“目标摄入 / 目标消耗”——优先使用 MinePage 保存的计划值；没有时回退到旧逻辑
    val targetIntake = (health?.planIntakeKcalPerDay ?: 0)
        .takeIf { it > 0 }
        ?: (health?.recoMaintain ?: health?.tdee ?: 0)

    val targetBurn = (health?.planBurnKcalPerDay ?: 0)
        .takeIf { it > 0 }
        ?: ((health?.tdee ?: 0) - (health?.bmr ?: 0)).coerceAtLeast(0)


// 近 7 天（含今天），每天 0:00~24:00 的总睡眠小时（浮点）
    val zone = remember { ZoneId.systemDefault() }
    val last7Hours: List<Float> = remember(sleepSessions, today) {
        val startDate = today.minusDays(6)
        val days = buildList {
            var d = startDate
            while (!d.isAfter(today)) { add(d); d = d.plusDays(1) }
        }
        days.map { d ->
            val dayStart = d.atStartOfDay(zone).toInstant()
            val dayEnd   = d.plusDays(1).atStartOfDay(zone).toInstant()
            val minutes = sleepSessions.sumOf { s ->
                val sStart = s.startTs.toDate().toInstant()
                val sEnd   = s.endTs.toDate().toInstant()
                // 计算与当天窗口的重叠分钟
                val overlapStart = maxOf(sStart, dayStart)
                val overlapEnd   = minOf(sEnd, dayEnd)
                val m = Duration.between(overlapStart, overlapEnd).toMinutes().toInt()
                m.coerceAtLeast(0)
            }
            minutes / 60f
        }
    }

    // 转成画图用的 Float 列表；只取最近 14 条，按日期升序
    val weightPoints: List<Float> = remember(weightHistory) {
        weightHistory
            .sortedBy { it.date }           // 如果 VM 已经排序，这行也没问题
            .takeLast(14)
            .map { it.value.toFloat() }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("健康记录") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "返回")
                    }
                },
                modifier = Modifier.fillMaxWidth().shadow(8.dp)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(topBlue, bottomGreen)
                    )
                )
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            // 第一块：饮食 & 运动（整块可点，跳转 food_exercise）
            SummaryCard(
                intakeKcal = intake,          // 今日摄入（来自你的 VM）
                burnKcal   = burn,            // 今日消耗
                recommendedIntake = targetIntake,
                recommendedBurn   = targetBurn,
                onClick = { navController.navigate("food_exercise") }
            )

            // 第二块 第三块 第四块：并列放置
            RecordsMosaic3x3(
                bodyHeightRatio = 0.6f,
                onBodyClick = { navController.navigate("body") },
                onWaterClick = { navController.navigate("water") },
                onSleepClick = { navController.navigate("sleep") },
                weights = weightPoints,
                drunkMl = waterUi.totalMl,
                goalMl  = waterUi.goalMl,
                sleepHours = last7Hours                 // ✅ 接入真实“近7天睡眠时长”
            )
        }
    }
}

@Composable
private fun SummaryCard(
    intakeKcal: Int,
    burnKcal: Int,
    recommendedIntake: Int,
    recommendedBurn: Int,
    onClick: () -> Unit
) {
    fun safeProgress(current: Int, target: Int): Float {
        if (target <= 0) return 0f
        val v = current.toFloat() / target.toFloat()
        val c = v.coerceAtLeast(0f)
        return if (c.isFinite()) c else 0f
    }

    val intakeTarget = safeProgress(intakeKcal, recommendedIntake)
    val burnTarget   = safeProgress(burnKcal,   recommendedBurn)

    val intakeProgress by animateFloatAsState(
        targetValue = intakeTarget,
        animationSpec = tween(durationMillis = 900),
        label = "intakeProgress"
    )
    val burnProgress by animateFloatAsState(
        targetValue = burnTarget,
        animationSpec = tween(durationMillis = 900),
        label = "burnProgress"
    )

    ElevatedCard(
        onClick = onClick, // ← ripple 铺满整个卡片
        modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                // 左侧信息
                Spacer(Modifier.height(8.dp))
                LabelWithIndicator(
                    text = "饮食",
                    color = MaterialTheme.colorScheme.tertiary,
                    style = IndicatorStyle.Bar
                )
                Text("$intakeKcal kcal", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(24.dp))
                LabelWithIndicator(
                    text = "运动",
                    color = MaterialTheme.colorScheme.secondary,
                    style = IndicatorStyle.Bar
                )
                Text("$burnKcal kcal", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }

            // 右侧双环（同色同粗，颜色随进度加深）
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Box(Modifier.size(132.dp)) {
                    DualGradientRingProgress(
                        intakeProgress = intakeProgress,
                        burnProgress   = burnProgress,
                        intakeBaseColor = MaterialTheme.colorScheme.tertiary,  // 饮食主色
                        burnBaseColor   = MaterialTheme.colorScheme.secondary, // 运动主色
                        trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        startAngle = -90f,
                        counterClockwise = true,
                        stroke = 16.dp,
                        gapBetweenRings = 12.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun DualGradientRingProgress(
    intakeProgress: Float,
    burnProgress: Float,
    intakeBaseColor: Color,
    burnBaseColor: Color,
    trackColor: Color,
    startAngle: Float = -90f,
    counterClockwise: Boolean = true,
    stroke: Dp = 14.dp,
    gapBetweenRings: Dp = 12.dp
) {
    // 进度可在外部做 clamp；这里保持与你单环一致的行为（允许 >1 做溢出）
    val dir = if (counterClockwise) -1f else 1f

    Canvas(modifier = Modifier.fillMaxSize()) {
        val s   = stroke.toPx()
        val gap = gapBetweenRings.toPx()

        val radiusOuter = (size.minDimension - s) / 2f
        val radiusInner = radiusOuter - s - gap

        val rectOuter = androidx.compose.ui.geometry.Rect(
            center.x - radiusOuter, center.y - radiusOuter,
            center.x + radiusOuter, center.y + radiusOuter
        )
        val rectInner = androidx.compose.ui.geometry.Rect(
            center.x - radiusInner, center.y - radiusInner,
            center.x + radiusInner, center.y + radiusInner
        )

        // —— 轨道永远先画（即使进度为 0） —— //
        drawCircle(
            color = trackColor,
            radius = radiusOuter,
            center = center,
            style = Stroke(width = s, cap = StrokeCap.Round)
        )
        drawCircle(
            color = trackColor,
            radius = radiusInner,
            center = center,
            style = Stroke(width = s, cap = StrokeCap.Round)
        )

        // 内部工具：画“首圈渐变 + 溢出覆盖”
        fun drawRing(
            progress: Float,
            baseColor: Color,
            rect: androidx.compose.ui.geometry.Rect
        ) {
            if (progress <= 0f) return  // 只跳过进度绘制，轨道已画

            val totalSweep = 360f * progress
            val epsilon    = 0.001f
            val sweep1     = minOf(totalSweep, 360f - epsilon)
            val extra      = (totalSweep - 360f).coerceAtLeast(0f)

            // 起/终颜色策略：起点不透明，终点半透明
            val startC = baseColor
            val endC   = baseColor.copy(alpha = 0.35f)

            // 旋转到正确起笔角后，统一用“正角度”构造路径
            val pivotGrad  = if (dir < 0f) startAngle - sweep1 else startAngle
            val pivotExtra = if (dir < 0f) startAngle - extra  else startAngle

            // 渐变 stops：把第一段弧长映射到 [0..1]，之后保持终点色
            val t = (sweep1 / 360f).coerceIn(0.001f, 1f)
            val brush = Brush.sweepGradient(
                colorStops = arrayOf(
                    0f to startC,
                    t  to endC,
                    1f to startC
                ),
                center = center
            )

            // —— 第一圈（或不足一圈）：渐变 —— //
            if (sweep1 > 0f) {
                withTransform({ rotate(pivotGrad, pivot = center) }) {
                    val path = Path().apply {
                        arcTo(
                            rect = rect,
                            startAngleDegrees = 0f,
                            sweepAngleDegrees = sweep1, // < 360
                            forceMoveTo = true
                        )
                    }
                    drawPath(
                        path = path,
                        brush = brush,
                        style = Stroke(width = s, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }

            // —— 溢出（> 1 圈）：用起点色覆盖起笔处 —— //
            if (extra > 0f) {
                withTransform({ rotate(pivotExtra, pivot = center) }) {
                    val path = Path().apply {
                        arcTo(
                            rect = rect,
                            startAngleDegrees = 0f,
                            sweepAngleDegrees = extra,
                            forceMoveTo = true
                        )
                    }
                    drawPath(
                        path = path,
                        color = startC,
                        style = Stroke(width = s, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }
        }

        // 先画外环（饮食），再画内环（运动）
        drawRing(progress = intakeProgress, baseColor = intakeBaseColor, rect = rectOuter)
        drawRing(progress = burnProgress,   baseColor = burnBaseColor,   rect = rectInner)
    }
}


enum class IndicatorStyle { Bar, Dot }

@Composable
private fun LabelWithIndicator(
    text: String,
    color: Color,
    style: IndicatorStyle,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (style) {
            IndicatorStyle.Bar -> Box(
                Modifier
                    .width(4.dp)                 // 细长矩形
                    .fillMaxHeight()             // 与文字同高
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
            IndicatorStyle.Dot -> Box(
                Modifier
                    .size(10.dp)                 // 小圆点
                    .clip(CircleShape)
                    .background(color)
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, color = color)
    }
}

@Composable
fun RecordsMosaic3x3(
    // 左列上：身体数据所占的高度比例（默认 0.6，比 2/3 更“扁”）
    bodyHeightRatio: Float = 0.7f,
    gap: Dp = 12.dp,
    onBodyClick: () -> Unit = {},
    onWaterClick: () -> Unit = {},
    onSleepClick: () -> Unit = {},
    weights: List<Float> = emptyList(),
    drunkMl: Int = 0,       // ⬅️ 新增
    goalMl: Int = 2000,      // ⬅️ 新增（给个默认）
    sleepHours: List<Float> = emptyList()
) {
    // 用容器固定一个合理的总高度；你可以调这个高度让整体更紧凑或更高
    BoxWithConstraints(Modifier.fillMaxWidth().padding(top = gap)) {
        val gridHeight = maxWidth * 1.1f    // 比如宽度的 0.9 倍高度
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(gridHeight),
        ) {
            // 左列：宽度 2 份
            Column(
                modifier = Modifier.weight(2.3f).fillMaxHeight()
            ) {
                BodyCard2x2(
                    weights = weights,            // ← 用真实数据
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(bodyHeightRatio)
                        .clip(RoundedCornerShape(12.dp)),
                    onClick = onBodyClick
                )
                Spacer(Modifier.height(gap))
                SleepCard2x1(
                    hours = sleepHours.takeLast(7),            // ✅ 用真实数据（保证最多7天）
                    goalHours = 8f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f - bodyHeightRatio)
                        .clip(RoundedCornerShape(12.dp)),
                    onClick = onSleepClick
                )
            }

            Spacer(Modifier.width(gap))

            // 右列：宽度 1 份，纵向占满（1×3）
            WaterCard1x3(
                drunkMl = drunkMl,       // ⬅️ 用真实值
                goalMl  = goalMl,        // ⬅️ 用真实值
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp)),
                onClick = onWaterClick
            )
        }
    }
}


@Composable
private fun BodyCard2x2(
    weights: List<Float>,                 // ← 最近 N 天体重
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {

    ElevatedCard(
        onClick = onClick, // ripple 覆盖整个卡片
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(2.dp),
        modifier = modifier
    ) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                "身体数据",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "体重",
                style = MaterialTheme.typography.labelMedium,               // 字体小一点
                color = MaterialTheme.colorScheme.onSurfaceVariant,         // 灰色
                modifier = Modifier.padding(top = 2.dp)
            )
            Spacer(Modifier.height(8.dp))

                MinimalAreaLineChart(
                    points = weights,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                )
        }
    }
}


@Composable
private fun WaterCard1x3(
    drunkMl: Int,
    goalMl: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val density = LocalDensity.current

    // 原有进度计算
    val maxProgress = if (drunkMl > goalMl) drunkMl else goalMl
    val targetP = if (maxProgress > 0) (drunkMl.toFloat() / maxProgress).coerceIn(0f, 1f) else 0f

    // NEW: 动画进度，从 0 -> targetP
    val progress = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(targetP) {
        // 首次进入会从 0f 动到 targetP；后续数据变化也会顺滑过渡到新值
        progress.animateTo(
            targetValue = targetP,
            animationSpec = androidx.compose.animation.core.tween(
                durationMillis = 900,
                easing = androidx.compose.animation.core.FastOutSlowInEasing
            )
        )
    }

    // 记录进度条像素高度
    var barHeightPx by remember { mutableStateOf(0) }
    val barHeightDp by remember(barHeightPx) { mutableStateOf(with(density) { barHeightPx.toDp() }) }

    // CHANGED: 用动画进度计算文本的垂直位置
    val levelTopDp = (barHeightDp * (1f - progress.value)).coerceAtLeast(0.dp)

    ElevatedCard(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.elevatedCardElevation(2.dp)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Text(
                "饮水",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(8.dp))

            Row(
                Modifier.fillMaxSize(),
                verticalAlignment = Alignment.Top
            ) {
                // 左侧：竖向进度条
                Box(
                    Modifier
                        .width(24.dp)
                        .fillMaxHeight()
                        .onGloballyPositioned { barHeightPx = it.size.height }
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                ) {
                    // CHANGED: 使用动画进度填充
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(progress.value) // ← 动画中的 0f..targetP
                            .align(Alignment.BottomCenter)
                            .clip(RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f))
                    )
                }

                Spacer(Modifier.width(10.dp))

                // 右侧：刻度式数值区域
                Box(Modifier.fillMaxSize()) {
                    if (drunkMl > goalMl) {
                        Text(
                            text = "$drunkMl ml",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.align(Alignment.TopStart)
                        )
                    } else {
                        Text(
                            text = "$goalMl ml",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.TopStart)
                        )

                        Column(Modifier.fillMaxHeight()) {
                            // CHANGED: 文本跟随动画进度一起移动
                            Spacer(Modifier.height(levelTopDp))
                            Text(
                                text = "$drunkMl ml",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}



@Composable
private fun SleepCard2x1(
    hours: List<Float>,              // 最近 N 天
    goalHours: Float,                // 目标睡眠
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val maxBase = maxOf(goalHours, hours.maxOrNull() ?: 0f).coerceAtLeast(1f)

    ElevatedCard(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(2.dp),
        modifier = modifier
    ) {
        Column(Modifier.fillMaxSize().padding(12.dp)) {
            Text("睡眠", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))

            // 柱状图区域占满剩余空间
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                // 使用 Row + fillMaxHeight(fraction) 画小柱子
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    hours.forEach { h ->
                        val frac = (h / maxBase).coerceIn(0f, 1f)
                        // bar
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(frac)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f))
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
            val avg = if (hours.isNotEmpty()) hours.sum() / hours.size else 0f
            Text("最近 ${hours.size} 天平均 ${"%.1f".format(avg)} 小时",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MinimalAreaLineChart(
    points: List<Float>,
    color: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) {
        Box(modifier, contentAlignment = Alignment.Center) { Text("暂无数据") }
        return
    }
    Canvas(modifier) {
        val padX = 12.dp.toPx()
        val padTop = 10.dp.toPx()
        val padBot = 10.dp.toPx()
        val w = size.width - padX * 2
        val h = size.height - padTop - padBot

        val minV = points.minOrNull() ?: 0f
        val maxV = points.maxOrNull() ?: 0f
        val span = (maxV - minV).let { if (it < 1e-6f) 1f else it }

        fun x(i: Int) = padX + w * (if (points.size == 1) 0f else i / (points.size - 1f))
        fun y(v: Float) = padTop + h * (1f - (v - minV) / span)

        if (points.size == 1) {
            // 单点：画一个短横线或小圆点
            val xi = x(0)
            val yi = y(points[0])
            drawLine(color, Offset(xi - 6f, yi), Offset(xi + 6f, yi), strokeWidth = 3f)
            return@Canvas
        }

        // 1) 先把数值转换为 Offset 点列
        val pts = points.indices.map { i -> Offset(x(i), y(points[i])) }
        val firstX = pts.first().x
        val lastX = pts.last().x
        val bottomY = padTop + h

        // 2) 用单调保形曲线生成“平滑折线”
        val line = buildMonotonePath(pts)

        // 3) 面积路径：在曲线两端落回到底边并闭合
        val area = Path().apply {
            addPath(line)
            lineTo(lastX, bottomY)
            lineTo(firstX, bottomY)
            close()
        }

        // 面积填充（同色淡化）
        drawPath(area, color.copy(alpha = 0.18f))

        // 平滑折线（无圆点、无数值）
        drawPath(
            path = line,
            color = color,
            style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}


private fun buildMonotonePath(points: List<Offset>): Path {
    val n = points.size
    val path = Path()
    if (n == 0) return path
    path.moveTo(points[0].x, points[0].y)
    if (n == 1) return path

    val x = FloatArray(n) { points[it].x }
    val y = FloatArray(n) { points[it].y }
    val d = FloatArray(n - 1) // 割线斜率

    for (i in 0 until n - 1) {
        val dx = x[i + 1] - x[i]
        d[i] = if (dx != 0f) (y[i + 1] - y[i]) / dx else 0f
    }

    val m = FloatArray(n) // 端点切线
    m[0] = d[0]
    for (i in 1 until n - 1) {
        m[i] = if (d[i - 1] * d[i] <= 0f) 0f else (d[i - 1] + d[i]) / 2f
    }
    m[n - 1] = d[n - 2]

    // Fritsch–Carlson 限制，防止过冲
    for (i in 0 until n - 1) {
        if (d[i] == 0f) {
            m[i] = 0f; m[i + 1] = 0f
        } else {
            val a = m[i] / d[i]
            val b = m[i + 1] / d[i]
            val s = a * a + b * b
            if (s > 9f) {
                val t = 3f / sqrt(s)
                m[i] = t * a * d[i]
                m[i + 1] = t * b * d[i]
            }
        }
    }

    // Hermite → Cubic Bézier
    for (i in 0 until n - 1) {
        val h = x[i + 1] - x[i]
        val c1x = x[i] + h / 3f
        val c1y = y[i] + m[i] * h / 3f
        val c2x = x[i + 1] - h / 3f
        val c2y = y[i + 1] - m[i + 1] * h / 3f
        path.cubicTo(c1x, c1y, c2x, c2y, x[i + 1], y[i + 1])
    }
    return path
}

@Composable
private fun rememberEnterKey(): Int {
    val owner = LocalLifecycleOwner.current
    var key by remember { mutableStateOf(0) }
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_START) key++ // 每次页面进入前台 +1
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    return key
}

@Composable
private fun animateProgressOnEnter(
    ready: Boolean,      // 目标是否就绪（>0 才算）
    target: Float,       // 允许 >1，用于第二圈
    enterKey: Any,       // 每次变化都重播
    maxLoops: Float = 2f // 最多几圈，避免跨度过大
): Float {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(enterKey) { anim.snapTo(0f) } // 进入页面先复位
    LaunchedEffect(ready, target, enterKey) {
        if (!ready) return@LaunchedEffect
        val t = target.let { if (it.isFinite()) it else 0f }
            .coerceIn(0f, maxLoops.coerceAtLeast(1f))
        anim.snapTo(0f)              // 从 0 开始播
        anim.animateTo(t, tween(900))
    }
    return anim.value
}

/** 允许溢出的安全进度（只防除 0/NaN，不夹到 1） */
private fun overflowProgress(current: Int, target: Int): Float {
    if (target <= 0) return 0f
    val raw = current.toFloat() / target.toFloat()
    return if (raw.isFinite()) raw else 0f
}