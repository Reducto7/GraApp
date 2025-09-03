package com.example.gra.ui.records

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Observer
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gra.ui.DateSelector
import com.example.gra.ui.viewmodel.FoodViewModel
import java.time.LocalDate
import android.app.DatePickerDialog
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.roundToInt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.ui.draw.rotate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import kotlin.math.abs
import kotlin.math.max
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first

data class ExerciseUi(
    val index: Int,
    val kcal: Int,
    val items: List<ExerciseItemUi> = emptyList()
)
data class ExerciseItemUi(
    val name: String,
    val minutes: Int,
    val kcal: Int
)

private enum class TrendMode { Line, Diff }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodExercisePage(
    navController: NavHostController,
    foodViewModel: FoodViewModel = viewModel()
) {
    val context = LocalContext.current
    var recordDate by remember { mutableStateOf(LocalDate.now()) }

    LaunchedEffect(recordDate) {
        foodViewModel.loadDataByDate(recordDate)
    }

    val trendDays  by foodViewModel.trendDays.collectAsState()
    val trendIn    by foodViewModel.trendIntake.collectAsState()
    val trendBurn  by foodViewModel.trendBurn.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("饮食/运动记录") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            // ✅ 仅允许选择 <= 今天
                            val today = LocalDate.now()
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    val picked = LocalDate.of(y, m + 1, d)
                                    if (!picked.isAfter(today)) {
                                        recordDate = picked
                                    }
                                },
                                recordDate.year,
                                recordDate.monthValue - 1,
                                recordDate.dayOfMonth
                            ).apply {
                                datePicker.maxDate = System.currentTimeMillis()
                            }.show()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "选择记录日期"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        // 背景渐变放在内容层
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(com.example.gra.ui.topBlue, com.example.gra.ui.bottomGreen)
                    )
                )
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                // 放在 FoodExercisePage.kt 的 LazyColumn 内、饮食卡 item 之前
                item {
                    TrendCard(
                        days   = trendDays,
                        intake = trendIn,
                        burn   = trendBurn
                    )
                }

                // —— 饮食卡 —— //
                item {
                    FoodOnlyCard(
                        totalKcal = foodViewModel.totalIntakeKcal.value,
                        meals = foodViewModel.meals.map { m ->
                            MealUi(
                                index = m.index,
                                title = m.name,
                                kcal  = m.kcal,
                                items = m.items.map { FoodItemUi(it.name, it.grams.toInt(), it.kcal) }
                            )
                        },
                        onAddClick = { navController.navigate("food") },
                        recommendedIntake = 2200,
                        onDeleteItem = { mealIdx, itemIdx ->
                            foodViewModel.deleteMealItem(recordDate, mealIdx, itemIdx)
                        }
                    )
                }
                item { Spacer(Modifier.height(8.dp)) } // 底部留白
                // —— 运动卡 —— //
                item {
                    ExerciseOnlyCard(
                        totalKcal = foodViewModel.totalBurnKcal.value,
                        sessions = foodViewModel.exercises.map { e ->
                            ExerciseUi(
                                index = e.index,
                                kcal  = e.kcal,
                                items = listOf(ExerciseItemUi(e.name, e.minutes, e.kcal))
                            )
                        },
                        onAddClick = { navController.navigate("exercise") },
                        recommendedBurn = 750,
                        onDeleteSession = { sessionIdx ->
                            foodViewModel.deleteExerciseItem(recordDate, sessionIdx)
                        }
                    )
                }
                item { Spacer(Modifier.height(8.dp)) } // 底部留白
            }
        }
    }
}

// —— UI 数据模型（便于从 VM 映射过来） —— //
data class MealUi(
    val index: Int,
    val title: String,
    val kcal: Int,
    val items: List<FoodItemUi> = emptyList()
)

data class FoodItemUi(
    val name: String,
    val grams: Int,
    val kcal: Int
)

// —— 只有“饮食”的卡片（★更新版） —— //
@Composable
private fun FoodOnlyCard(
    totalKcal: Int,
    meals: List<MealUi>,
    onAddClick: () -> Unit,
    recommendedIntake: Int,
    onDeleteItem: (mealIndex: Int, itemIndex: Int) -> Unit
) {
    var listExpanded by remember { mutableStateOf(false) }   // ★ 新增：一级总开关
    val listArrowDeg by animateFloatAsState(if (listExpanded) 180f else 0f)
    var expandedIndex by remember { mutableStateOf<Int?>(null) }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(2.dp)
    ) {
        Box(Modifier.fillMaxWidth()) {

            Column(Modifier
                .fillMaxWidth()
                .animateContentSize(                 // ⭐ 高度变化自动过渡
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ).padding(16.dp)
            ) {
                // 顶部：左文案 + 右小环
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {// —— 顶部左侧文案 —— //
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp)
                    ) {

                        Spacer(Modifier.height(4.dp))
                        SmallBarLabel(text = "饮食", color = MaterialTheme.colorScheme.primary)

                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "${totalKcal} kcal",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        // 推荐摄入 + 问号（小气泡）
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "推荐摄入 ${recommendedIntake} kcal",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(4.dp))
                            InfoBubbleIcon(
                                text = "根据个人资料（性别、年龄、身高体重）与活动水平估算的每日能量目标；你可在设置中调整。"
                            )
                        }
                    }

                    val progress by animateFloatAsState(
                        targetValue = (totalKcal / recommendedIntake.toFloat()).coerceIn(0f, 1f),
                        animationSpec = tween(900)
                    )
                    // 预留右上角 + 的空间（不参与测量的内容尺寸）
                    Box(modifier = Modifier.padding(end = 44.dp)) {
                        // 真实的环形绘制区域：给出确定的宽高，Row 才能算出“行高”
                        Box(
                            modifier = Modifier
                                .size(112.dp)        // ✅ 关键：给“高度”！(任意你喜欢的尺寸)
                                .offset(x = (-8).dp),
                            contentAlignment = Alignment.Center
                        ) {
                            SingleRingProgress(
                                progress = progress,
                                ringColor = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                startAngle = -90f,
                                counterClockwise = true,
                                stroke = 18.dp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                AnimatedVisibility(
                    visible = listExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit  = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        Spacer(Modifier.height(12.dp))
                // 下方：每餐小计行（点按展开餐内明细）
                meals.forEachIndexed { i, meal ->
                    val isExpanded = expandedIndex == i
                    val arrowDeg by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedIndex = if (isExpanded) null else i }
                            .padding(vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedIndex = if (isExpanded) null else i }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "第${meal.index}餐 ${meal.kcal} kcal",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = Icons.Outlined.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.rotate(arrowDeg),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        AnimatedVisibility(
                            visible = isExpanded,
                            enter = expandVertically(
                                expandFrom = Alignment.Top,
                                animationSpec = tween(durationMillis = 220, easing = LinearOutSlowInEasing)
                            ) + fadeIn(animationSpec = tween(180)),
                            exit = shrinkVertically(
                                shrinkTowards = Alignment.Top,
                                animationSpec = tween(durationMillis = 200, easing = FastOutLinearInEasing)
                            ) + fadeOut(animationSpec = tween(120))
                        ) {
                            Column(Modifier.padding(start = 12.dp, top = 6.dp, bottom = 4.dp)) {
                                meal.items.forEachIndexed { itemIdx, it ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // 左：名称 + 克数
                                        Text(
                                            text = "${it.name} ${it.grams}g",
                                            modifier = Modifier.weight(1f),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        // 中：kcal
                                        Text(
                                            text = "${it.kcal} kcal     ",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        // 右：删除
                                        IconButton(
                                            onClick = { onDeleteItem(meal.index, itemIdx) },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Clear,
                                                contentDescription = "删除该项"
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (i != meals.lastIndex) {
                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    }
                }
                    }
                }
                // —— 分隔线（可选）——
                Divider(
                    modifier = Modifier
                        .padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f)
                )
// —— 底部“一级开关”行：底部居中，只放一个图标（点击展开/收起第1/2/3餐|次）——
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 2.dp)
                        .height(24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { listExpanded = !listExpanded }) {
                        Icon(
                            imageVector = Icons.Outlined.KeyboardArrowDown,
                            contentDescription = if (listExpanded) "收起列表" else "展开列表",
                            modifier = Modifier.rotate(listArrowDeg),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            // ★ 右上角 小型悬浮 + 按钮（支持你后续微调 x/y）
            SmallFloatingActionButton(
                onClick = onAddClick,
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-4).dp, y = 4.dp) // 你说后续会自行微调；向左请用负值
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加")
            }
        }
    }
}

// —— 单环进度：起始 -90°、支持逆时针（保持与外部一致） —— //
@Composable
private fun SingleRingProgress(
    progress: Float,                 // 0f..1f
    ringColor: Color,                // 注意：这里传“纯色”，不要再做 alpha 放大
    trackColor: Color,
    startAngle: Float = -90f,
    counterClockwise: Boolean = true,
    stroke: Dp = 16.dp
) {
    val sweep = (if (counterClockwise) -360f else 360f) * progress

    Canvas(Modifier.fillMaxSize()) {
        val s = stroke.toPx()
        val r = (size.minDimension - s) / 2f
        val rectTopLeft = Offset(center.x - r, center.y - r)
        val rectSize = Size(r * 2, r * 2)

        // 轨道
        drawCircle(
            color = trackColor,
            radius = r,
            center = center,
            style = Stroke(width = s, cap = StrokeCap.Round)
        )

        val total = kotlin.math.abs(sweep)
        if (total < 0.1f) return@Canvas

        // —— 使用分段绘制实现“沿弧线渐变” —— //
        val dir = if (counterClockwise) -1f else 1f
        val startAlpha = 0.35f
        val endAlpha = 1.0f

        // 每段角度（越小越细腻，性能略降）；4° ≈ 90 段/整圈
        val segDeg = 4f
        val segments = kotlin.math.max(1, (total / segDeg).roundToInt())
        val overlap = 0.12f // 小重叠避免缝隙

        for (i in 0 until segments) {
            val t0 = i / segments.toFloat()
            val t1 = (i + 1) / segments.toFloat()
            val tMid = (t0 + t1) / 2f
            val alpha = startAlpha + (endAlpha - startAlpha) * tMid
            val color = ringColor.copy(alpha = alpha)

            val segStart = startAngle + dir * (total * t0)
            val segSweep = dir * (total / segments + if (i < segments - 1) overlap else 0f)

            drawArc(
                color = color,
                startAngle = segStart,
                sweepAngle = segSweep,
                useCenter = false,
                topLeft = rectTopLeft,
                size = rectSize,
                // 用 Butt 防止重叠处变粗；两端圆帽在下面单独补
                style = Stroke(width = s, cap = StrokeCap.Butt)
            )
        }

        // 两端小“圆帽”，保证整体端点圆润
        val capSweep = dir * 0.0001f
        drawArc(
            color = ringColor.copy(alpha = startAlpha),
            startAngle = startAngle,
            sweepAngle = capSweep,
            useCenter = false,
            topLeft = rectTopLeft,
            size = rectSize,
            style = Stroke(width = s, cap = StrokeCap.Round)
        )
        drawArc(
            color = ringColor.copy(alpha = endAlpha),
            startAngle = startAngle + sweep - capSweep,
            sweepAngle = capSweep,
            useCenter = false,
            topLeft = rectTopLeft,
            size = rectSize,
            style = Stroke(width = s, cap = StrokeCap.Round)
        )
    }
}


// —— 小色条标签（与外部 SummaryCard 的 LabelWithIndicator 视觉一致思路） —— //
@Composable
private fun SmallBarLabel(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .width(4.dp)
                .height(16.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, color = color)
    }
}

@Composable
private fun InfoBubbleIcon(
    text: String,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = "推荐摄入说明",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            offset = DpOffset(x = 0.dp, y = (-8).dp),
            modifier = Modifier
                .shadow(8.dp, RoundedCornerShape(10.dp))
                // ✅ 白底
                .background(Color.White, RoundedCornerShape(10.dp))
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                    RoundedCornerShape(10.dp)
                )
        ) {
            Box(
                Modifier
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .widthIn(max = 280.dp)
            ) {
                // ✅ 黑字
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Black
                )
            }
        }
    }
}

@Composable
private fun ExerciseOnlyCard(
    totalKcal: Int,
    sessions: List<ExerciseUi>,
    onAddClick: () -> Unit,
    recommendedBurn: Int,
    onDeleteSession: (sessionIndex: Int) -> Unit
) {
    var listExpanded by remember { mutableStateOf(false) }   // ★ 新增：一级总开关
    val listArrowDeg by animateFloatAsState(if (listExpanded) 180f else 0f)
    var expandedIndex by remember { mutableStateOf<Int?>(null) }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        //elevation = CardDefaults.elevatedCardElevation(2.dp)
    ) {
        Box(Modifier.fillMaxWidth()) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(                 // ⭐ 高度变化自动过渡
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ).padding(16.dp)
            ) {
                // 顶部：左文案 + 右小环
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 左侧文案
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp)
                    ) {
                        SmallBarLabel(text = "运动", color = MaterialTheme.colorScheme.secondary)

                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "${totalKcal} kcal",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "推荐消耗 ${recommendedBurn} kcal",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(6.dp))
                            InfoBubbleIcon(
                                text = "推荐消耗根据你的活动目标推算；可在设置中调整。"
                            )
                        }
                    }

                    // 右侧单环（尺寸固定 + 避开右上角 + 号）
                    val progress by animateFloatAsState(
                        targetValue = (totalKcal / recommendedBurn.toFloat()).coerceIn(0f, 1f),
                        animationSpec = tween(900)
                    )
                    Box(modifier = Modifier.padding(end = 44.dp)) {
                        Box(
                            modifier = Modifier
                                .size(112.dp)
                                .offset(x = (-8).dp),
                            contentAlignment = Alignment.Center
                        ) {
                            SingleRingProgress(
                                progress = progress,
                                ringColor = MaterialTheme.colorScheme.secondary,
                                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                startAngle = -90f,
                                counterClockwise = true,
                                stroke = 18.dp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                AnimatedVisibility(
                    visible = listExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit  = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        Spacer(Modifier.height(12.dp))
                        // 下方：每次运动小计（点按展开明细）
                        sessions.forEachIndexed { i, one ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        expandedIndex = if (expandedIndex == i) null else i
                                    }
                                    .padding(vertical = 8.dp)
                            ) {
                                val isExpanded = expandedIndex == i
                                val arrowDeg by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { expandedIndex = if (isExpanded) null else i }
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                        ) {
                        Text(
                            text = "第${one.index}次 ${one.kcal} kcal",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                            Icon(
                                imageVector = Icons.Outlined.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.rotate(arrowDeg),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        AnimatedVisibility(
                            visible = isExpanded,
                            enter = expandVertically(
                                expandFrom = Alignment.Top,
                                animationSpec = tween(durationMillis = 220, easing = LinearOutSlowInEasing)
                            ) + fadeIn(animationSpec = tween(180)),
                            exit = shrinkVertically(
                                shrinkTowards = Alignment.Top,
                                animationSpec = tween(durationMillis = 200, easing = FastOutLinearInEasing)
                            ) + fadeOut(animationSpec = tween(120))
                        ) {
                            Column(Modifier.padding(start = 12.dp, top = 6.dp, bottom = 4.dp)) {
                                one.items.forEach { it ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // 左：名称 + 时长
                                        Text(
                                            text = "${it.name} ${it.minutes} 分钟",
                                            modifier = Modifier.weight(1f),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        // 中：kcal
                                        Text(
                                            text = "${it.kcal} kcal     ",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        // 右：删除按钮（按“第X次”的索引删除）
                                        IconButton(
                                            onClick = { onDeleteSession(one.index - 1) },   // ★ 0-based
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Clear,
                                                contentDescription = "删除该次运动"
                                            )
                                        }
                                    }
                                }

                            }
                        }

                    }
                    if (i != sessions.lastIndex) {
                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    }
                }
                    }
                }
                // —— 分隔线（可选）——
                Divider(
                    modifier = Modifier
                        .padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f)
                )

// —— 底部“一级开关”行：底部居中，只放一个图标（点击展开/收起第1/2/3餐|次）——
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 2.dp)
                        .height(24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { listExpanded = !listExpanded }) {
                        Icon(
                            imageVector = Icons.Outlined.KeyboardArrowDown,
                            contentDescription = if (listExpanded) "收起列表" else "展开列表",
                            modifier = Modifier.rotate(listArrowDeg),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

            }

            // 右上角 “+”
            SmallFloatingActionButton(
                onClick = onAddClick,
                containerColor = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .align(Alignment.TopEnd)   // 注意：放在 Box 作用域中才能用 align
                    .offset(x = (-4).dp, y = 4.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加运动")
            }
        }
    }
}

@Composable
private fun TrendCard(
    days: List<LocalDate>,
    intake: List<Int>,
    burn: List<Int>
) {
    var mode by remember { mutableStateOf(TrendMode.Line) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(Color.Transparent)
    ) {
        Column(Modifier.fillMaxSize()) {
            // 顶部中间的切换按钮（与 Sleep 页风格一致）
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val cs = MaterialTheme.colorScheme
                @Composable
                fun TabBtn(title: String, selected: Boolean, onClick: () -> Unit) {
                    Button(
                        onClick = onClick,
                        modifier = Modifier.width(140.dp).height(36.dp),
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selected) cs.primary else Color.White,
                            contentColor   = if (selected) cs.onTertiary else cs.onSurface
                        ),
                        border = if (!selected) BorderStroke(1.dp, cs.outlineVariant) else null,
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) { Text(title, style = MaterialTheme.typography.labelLarge) }
                }
                TabBtn("双折线图", mode == TrendMode.Line) { mode = TrendMode.Line }
                Spacer(Modifier.width(12.dp))
                TabBtn("卡路里差额柱状图", mode == TrendMode.Diff) { mode = TrendMode.Diff }
            }

            // 内容区
            Box(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 0.dp)) {
                when (mode) {
                    TrendMode.Line -> DualLineChart(days, intake, burn)
                    TrendMode.Diff -> DeficitBarChart(days, intake, burn)
                }
            }
        }
    }
}

@Composable
private fun DualLineChart(
    days: List<LocalDate>,
    intake: List<Int>,
    burn: List<Int>,
    maxDaysOnScreen: Int = 7,
    lineStrokeDp: Dp = 2.dp
) {
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val axisColor  = MaterialTheme.colorScheme.outlineVariant
    val lineAColor = MaterialTheme.colorScheme.tertiary     // 摄入
    val lineBColor = MaterialTheme.colorScheme.secondary    // 消耗

    val yAxisWidth = 44.dp
    val density    = LocalDensity.current
    val layoutDir  = LocalLayoutDirection.current

    // 对齐天序列
    val count = minOf(days.size, intake.size, burn.size)
    if (count == 0) {
        Box(Modifier.fillMaxSize()) { }
        return
    }
    val d = days.take(count)
    val a = intake.take(count)
    val b = burn.take(count)

    // 纵向范围：取两条序列的最大值，向上取整到 200 的倍数
    val rawMax = max(a.maxOrNull() ?: 0, b.maxOrNull() ?: 0)
    val yMax   = ((rawMax + 199) / 200) * 200   // 向上取整到 200
    val yStep  = 200

    // 内/外边距（左轴独立画布，右侧主画布可横向滚动）
    val outerPadPlot   = PaddingValues(start = 0.dp, end = 12.dp, top = 16.dp, bottom = 28.dp)
    val innerTopGutter = 10.dp
    val innerBotGutter = 22.dp

    val hScroll = rememberScrollState(Int.MAX_VALUE)

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val viewportPx = with(density) {
            (maxWidth - yAxisWidth
                    - outerPadPlot.calculateStartPadding(layoutDir)
                    - outerPadPlot.calculateEndPadding(layoutDir)).toPx()
        }.coerceAtLeast(1f)

        val desiredDays = maxDaysOnScreen.coerceAtLeast(1)
        val stridePxRaw = viewportPx / desiredDays
        val minGapPx    = with(density) { 3.dp.toPx() }
        val gapPx       = max(stridePxRaw * 0.25f, minGapPx)   // 折线图无柱宽，保留列间距
        val stridePx    = stridePxRaw

        Row(Modifier.fillMaxSize()) {
            // 左轴：固定刻度列（与右侧主画布同一几何：outerPad + innerGutter）
            Canvas(modifier = Modifier.width(yAxisWidth).fillMaxHeight()) {
                val topY = outerPadPlot.calculateTopPadding().toPx() + innerTopGutter.toPx()
                val botY = size.height - (outerPadPlot.calculateBottomPadding().toPx() + innerBotGutter.toPx())
                val usableH = (botY - topY).coerceAtLeast(1f)

                val stepPx = usableH / yMax.toFloat() * yStep

                // 刻度 + 网格（把颜色加深）
                val gridColor = axisColor.copy(alpha = 0.55f)     // ← 比之前更清晰
                val zeroColor = axisColor.copy(alpha = 0.90f)
                for (v in 0..yMax step yStep) {
                    val y = botY - (v.toFloat() / yMax.toFloat()) * usableH
                    // 在左轴也画一条细网格，保证两侧视觉同步
                    drawLine(
                        color = if (v == 0) zeroColor else gridColor,
                        start = Offset(x = size.width, y = y),
                        end   = Offset(x = 0f,        y = y),
                        strokeWidth = if (v == 0) 1.2f else 1f
                    )
                    // 刻度文字
                    val paint = android.graphics.Paint().apply {
                        color = labelColor.toArgb()
                        textSize = with(this) { 10.sp.toPx() }
                        isAntiAlias = true
                    }
                    drawContext.canvas.nativeCanvas.drawText("$v", 0f, y, paint)
                }
            }


            // 右侧：主画布（横向滚动）
            Box(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(hScroll)
            ) {
                val contentW = with(density) { (d.size * stridePx).toDp() } + outerPadPlot.calculateEndPadding(layoutDir)
                Canvas(
                    modifier = Modifier
                        .width(contentW)
                        .fillMaxHeight()
                        .padding(outerPadPlot)
                ) {
                    val topY  = innerTopGutter.toPx()
                    val botY  = size.height - innerBotGutter.toPx()
                    val leftX = 0f
                    val rightX= size.width
                    val usableH = (botY - topY).coerceAtLeast(1f)

                    // 网格（每 200 kcal）
                    val gridColor = axisColor.copy(alpha = 0.55f)
                    val zeroColor = axisColor.copy(alpha = 0.90f)
                    for (v in 0..yMax step yStep) {
                        val y = botY - (v.toFloat() / yMax.toFloat()) * usableH
                        drawLine(
                            color = if (v == 0) zeroColor else gridColor,
                            start = Offset(leftX, y),
                            end   = Offset(rightX, y),
                            strokeWidth = if (v == 0) 1.2f else 1f
                        )
                    }

                    // 将值映射到像素
                    fun xFor(i: Int): Float = i * stridePx + stridePx / 2f
                    fun yFor(v: Int): Float = botY - (v / yMax.toFloat()) * usableH

                    // 路径 A（摄入）
                    val pathA = Path().apply {
                        moveTo(xFor(0), yFor(a[0]))
                        for (i in 1 until d.size) lineTo(xFor(i), yFor(a[i]))
                    }
                    drawPath(
                        path = pathA,
                        color = lineAColor,
                        style = Stroke(width = with(density) { lineStrokeDp.toPx() }, cap = StrokeCap.Round)
                    )

                    // 路径 B（消耗）
                    val pathB = Path().apply {
                        moveTo(xFor(0), yFor(b[0]))
                        for (i in 1 until d.size) lineTo(xFor(i), yFor(b[i]))
                    }
                    drawPath(
                        path = pathB,
                        color = lineBColor,
                        style = Stroke(width = with(density) { lineStrokeDp.toPx() }, cap = StrokeCap.Round)
                    )

                    // 底部日期标签
                    val textPaint = android.graphics.Paint().apply {
                        color = labelColor.toArgb()
                        textSize = with(density) { 10.sp.toPx() }
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    d.forEachIndexed { i, day ->
                        val x = xFor(i)
                        drawContext.canvas.nativeCanvas.drawText(
                            "${day.monthValue}/${day.dayOfMonth}", x, botY + with(density){12.dp.toPx()}, textPaint
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeficitBarChart(
    days: List<java.time.LocalDate>,
    intake: List<Int>,
    burn: List<Int>,
    maxDaysOnScreen: Int = 7,
    barWidthFraction: Float = 0.30f
) {
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val axisColor  = MaterialTheme.colorScheme.outlineVariant
    val barColor   = MaterialTheme.colorScheme.primary

    val yAxisWidth = 44.dp
    val outerPadPlot   = PaddingValues(start = 0.dp, end = 12.dp, top = 16.dp, bottom = 28.dp)
    val innerTopGutter = 12.dp
    val innerBotGutter = 22.dp

    val density   = LocalDensity.current
    val layoutDir = LocalLayoutDirection.current

    val n = minOf(days.size, intake.size, burn.size)
    if (n == 0) { Box(Modifier.fillMaxSize()) {}; return }

    val d = days.take(n)
    val a = intake.take(n)
    val b = burn.take(n)
    val diff = IntArray(n) { i -> a[i] - b[i] }   // 正=盈余，负=赤字

    // 正/负分开取整到 200；统一坐标 yMin..yMax（不强制 0 居中）
    val posMax = diff.filter { it > 0 }.maxOrNull() ?: 0
    val negMin = diff.filter { it < 0 }.minOrNull() ?: 0
    fun roundUp200(v: Int)   = if (v <= 0) 0 else ((v + 199) / 200) * 200
    fun roundDown200(v: Int) = if (v >= 0) 0 else -(((-v) + 199) / 200) * 200
    val yMax = roundUp200(posMax).coerceAtLeast(200)       // 顶部 ≥ 200
    val yMin = roundDown200(negMin).coerceAtMost(-200)     // 底部 ≤ -200

    fun yFor(value: Int, topY: Float, botY: Float): Float {
        val range = (yMax - yMin).toFloat().coerceAtLeast(1f)
        return botY - ((value - yMin).toFloat() / range) * (botY - topY)
    }

    // 只让右侧主画布滚动；首帧在最右
    val hScroll = rememberScrollState(Int.MAX_VALUE)

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val viewportPx = with(density) {
            (maxWidth - yAxisWidth
                    - outerPadPlot.calculateStartPadding(layoutDir)
                    - outerPadPlot.calculateEndPadding(layoutDir)).toPx()
        }.coerceAtLeast(1f)

        val desiredDays = maxDaysOnScreen.coerceAtLeast(1)
        val stridePxRaw = viewportPx / desiredDays
        val minBarPx    = with(density) { 6.dp.toPx() }
        val minGapPx    = with(density) { 3.dp.toPx() }
        val barW        = (stridePxRaw * barWidthFraction).coerceAtLeast(minBarPx)
        val gapPx       = (stridePxRaw - barW).coerceAtLeast(minGapPx)
        val stridePx    = barW + gapPx

        Row(Modifier.fillMaxSize()) {
            // 左：固定刻度列（使用 outerPad + innerGutter 的几何）
            Canvas(
                modifier = Modifier
                    .width(yAxisWidth)
                    .fillMaxHeight()
            ) {
                val topY = outerPadPlot.calculateTopPadding().toPx() + with(this){ innerTopGutter.toPx() }
                val botY = size.height - (outerPadPlot.calculateBottomPadding().toPx() + with(this){ innerBotGutter.toPx() })

                val paint = android.graphics.Paint().apply {
                    color = labelColor.toArgb()
                    textSize = with(this@Canvas) { 10.sp.toPx() }
                    isAntiAlias = true
                }
                for (v in yMin..yMax step 200) {
                    val y = yFor(v, topY, botY)
                    drawContext.canvas.nativeCanvas.drawText(v.toString(), 0f, y, paint)
                }
            }

            // 右：主画布（横向滚动；注意：这里 Canvas 已经 .padding(outerPadPlot) —— 因此 draw 内部不再加 outerPad）
            Box(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(hScroll)
            ) {
                val contentPx = d.size * stridePx
                val contentW = with(density) { kotlin.math.max(contentPx, viewportPx).toDp() } +
                        outerPadPlot.calculateEndPadding(layoutDir)

                Canvas(
                    modifier = Modifier
                        .width(contentW)
                        .fillMaxHeight()
                        .padding(outerPadPlot)    // ← 右侧 Canvas 已有 padding
                ) {
                    // ⚠️ 这里不要再把 outerPad 加进 topY/botY，否则会和左侧不一致
                    val topY  = with(this){ innerTopGutter.toPx() }
                    val botY  = size.height - with(this){ innerBotGutter.toPx() }
                    val leftX = 0f
                    val rightX= size.width

                    // 网格（每 200；0 线加重）
                    val gridColor = axisColor.copy(alpha = 0.55f)
                    val zeroColor = axisColor.copy(alpha = 0.95f)
                    for (v in yMin..yMax step 200) {
                        val y = yFor(v, topY, botY)
                        drawLine(
                            color = if (v == 0) zeroColor else gridColor,
                            start = Offset(leftX, y), end = Offset(rightX, y),
                            strokeWidth = if (v == 0) 1.2f else 1f
                        )
                    }

                    // 柱与标签
                    val rPx      = with(this) { 2.dp.toPx() }   // 圆角半径 2dp
                    val labelGap = with(this) { 6.dp.toPx() }

                    val textPaint = android.graphics.Paint().apply {
                        color = labelColor.toArgb()
                        textSize = with(this@Canvas) { 10.sp.toPx() }
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    fun xLeft(i: Int): Float = i * stridePx + (stridePx - barW) / 2f

                    diff.forEachIndexed { i, v ->
                        if (v == 0) return@forEachIndexed  // 0 值不画柱/标签

                        val x  = xLeft(i)
                        val y0 = yFor(0, topY, botY)
                        val yV = yFor(v, topY, botY)
                        val top    = kotlin.math.min(y0, yV)
                        val bottom = kotlin.math.max(y0, yV)

                        // 四角独立圆角（正值：上圆下直；负值：上直下圆）
                        val rr = if (v > 0) {
                            androidx.compose.ui.geometry.RoundRect(
                                left = x, top = top, right = x + barW, bottom = bottom,
                                topLeftCornerRadius     = androidx.compose.ui.geometry.CornerRadius(rPx, rPx),
                                topRightCornerRadius    = androidx.compose.ui.geometry.CornerRadius(rPx, rPx),
                                bottomRightCornerRadius = androidx.compose.ui.geometry.CornerRadius.Zero,
                                bottomLeftCornerRadius  = androidx.compose.ui.geometry.CornerRadius.Zero
                            )
                        } else {
                            androidx.compose.ui.geometry.RoundRect(
                                left = x, top = top, right = x + barW, bottom = bottom,
                                topLeftCornerRadius     = androidx.compose.ui.geometry.CornerRadius.Zero,
                                topRightCornerRadius    = androidx.compose.ui.geometry.CornerRadius.Zero,
                                bottomRightCornerRadius = androidx.compose.ui.geometry.CornerRadius(rPx, rPx),
                                bottomLeftCornerRadius  = androidx.compose.ui.geometry.CornerRadius(rPx, rPx)
                            )
                        }
                        val path = Path().apply { addRoundRect(rr) }
                        drawPath(path = path, color = barColor)

                        // 数值标签：正值在柱顶上方；负值在柱底下方
                        val labelY = if (v > 0) (top - labelGap)
                        else (bottom + labelGap + textPaint.textSize * 0.15f)
                        drawContext.canvas.nativeCanvas.drawText(
                            v.toString(), x + barW / 2f, labelY, textPaint
                        )
                    }

                    // 底部日期标签
                    val datePaint = android.graphics.Paint().apply {
                        color = labelColor.toArgb()
                        textSize = with(this@Canvas) { 10.sp.toPx() }
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    d.forEachIndexed { i, day ->
                        val cx = i * stridePx + stridePx / 2f
                        drawContext.canvas.nativeCanvas.drawText(
                            "${day.monthValue}/${day.dayOfMonth}",
                            cx,
                            botY + with(this) { 12.dp.toPx() },
                            datePaint
                        )
                    }
                }
            }
        }
    }
}








