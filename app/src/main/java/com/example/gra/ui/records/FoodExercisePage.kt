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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlin.math.hypot
import kotlin.math.sqrt

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
            ) {

                // 放在 FoodExercisePage.kt 的 LazyColumn 内、饮食卡 item 之前
                item {
                    TrendCard(
                        days   = trendDays,
                        intake = trendIn,
                        burn   = trendBurn
                    )
                    Spacer(Modifier.height(16.dp))
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
                    Spacer(Modifier.height(12.dp))
                }
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        //elevation = CardDefaults.elevatedCardElevation(2.dp)
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
                        SmallBarLabel(text = "饮食", color = MaterialTheme.colorScheme.tertiary)

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
                        targetValue = totalKcal / recommendedIntake.toFloat(),
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
                            GradientRingProgress(
                                progress   = progress,
                                baseColor  = MaterialTheme.colorScheme.tertiary,     // 你想要的主色
                                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),     // 轨道浅灰
                                startAngle = -90f,
                                stroke     = 14.dp
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
                containerColor = MaterialTheme.colorScheme.tertiary,
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
fun GradientRingProgress(
    progress: Float,
    baseColor: Color,
    trackColor: Color,
    startAngle: Float = -90f,
    stroke: Dp = 16.dp,
    counterClockwise: Boolean = true   // 默认逆时针
) {

    Canvas(Modifier.fillMaxSize()) {
        val s = stroke.toPx()
        val r = (size.minDimension - s) / 2f
        val rect = androidx.compose.ui.geometry.Rect(
            center.x - r, center.y - r, center.x + r, center.y + r
        )

        // 底层轨道
        drawCircle(
            color = trackColor,
            radius = r,
            center = center,
            style = Stroke(width = s, cap = StrokeCap.Round)
        )
        if (progress <= 0f) return@Canvas

        // 渐变
        val startC = baseColor
        val endC   = baseColor.copy(alpha = 0.35f)

        val totalSweep = 360f * progress
        val dir = if (counterClockwise) -1f else 1f

        // 第一段：最多画到“整圈 - ε”，避免 360° 空笔
        val epsilon = 0.001f
        val sweep1 = minOf(totalSweep, 360f - epsilon)
        val extra  = (totalSweep - 360f).coerceAtLeast(0f)

        // 旋转到正确的起笔处，然后始终用“正角度”作图
        val pivotGrad  = if (dir < 0f) startAngle - sweep1 else startAngle
        val pivotExtra = if (dir < 0f) startAngle - extra  else startAngle

        // 渐变 stops：把第一段的弧长压缩到 [0..1]，之后保持终点色
        val t = (sweep1 / 360f).coerceIn(0.001f, 1f)
        val brush = Brush.sweepGradient(
            colorStops = arrayOf(
                0f to startC,
                t  to endC,
                1f to startC
            ),
            center = center
        )

        // —— 第一圈（或不足一圈）渐变 —— //
        if (sweep1 > 0f) {
            withTransform({ rotate(pivotGrad, pivot = center) }) {
                val path = Path().apply {
                    arcTo(
                        rect = rect,
                        startAngleDegrees = 0f,
                        sweepAngleDegrees = sweep1,   // 注意：< 360
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

        // —— 超出 1.0 的部分：用终点色覆盖起点处（叠加在第一圈之上）—— //
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
                    color = startC, // 纯终点色，叠加覆盖
                    style = Stroke(width = s, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }
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
        shape = RoundedCornerShape(16.dp),
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
                            GradientRingProgress(
                                progress   = progress,
                                baseColor  = MaterialTheme.colorScheme.secondary,     // 你想要的主色
                                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),     // 轨道浅灰
                                startAngle = -90f,
                                stroke     = 14.dp
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)

    ) {
        Column(Modifier.fillMaxSize()) {
            // 顶部中间的切换按钮（与 Sleep 页风格一致）
            Row(
                Modifier
                    .fillMaxWidth(),
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

            Spacer(Modifier.height(12.dp))

            // 内容区
            Box(
                Modifier.fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))                  // 让背景有圆角
                    .background(Color.White.copy(alpha = 0.5f))       // 半透明背景,
            ) {
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
    val cs = MaterialTheme.colorScheme

    val yAxisWidth = 44.dp
    val density    = LocalDensity.current
    val layoutDir  = LocalLayoutDirection.current

    // 交互 & UI 常量
    val pointRadiusDp      = 3.dp         // 空心圆半径
    val pointStrokeDp      = 2.dp           // 空心圆线宽
    val hitRadiusDp        = 16.dp          // 命中半径（手指可点击范围）
    val bubblePaddingDp    = 8.dp
    val bubbleCornerDp     = 8.dp
    val bubbleArrowSizeDp  = 6.dp
    val bubbleTextSizeSp   = 12.sp

    // 对齐天序列
    val count = minOf(days.size, intake.size, burn.size)
    if (count == 0) {
        Box(Modifier.fillMaxSize()) { }
        return
    }
    val d = days.take(count)
    val a = intake.take(count)
    val b = burn.take(count)

    // 纵向范围
    val rawMax = max(a.maxOrNull() ?: 0, b.maxOrNull() ?: 0)
    val yMax   = ((rawMax + 199) / 200) * 200   // 向上取整到 200
    val yStep  = 200

    // 内/外边距
    val outerPadPlot   = PaddingValues(start = 0.dp, end = 12.dp, top = 16.dp, bottom = 28.dp)
    val innerTopGutter = 10.dp
    val innerBotGutter = 22.dp

    // 横向滚动
    val hScroll = rememberScrollState(Int.MAX_VALUE)

    // 选中点状态（索引 + 系列：0=摄入(A), 1=消耗(B)）
    data class Selection(val index: Int, val series: Int)
    var selection by remember { mutableStateOf<Selection?>(null) }

    BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        val viewportPx = with(density) {
            (maxWidth - yAxisWidth
                    - outerPadPlot.calculateStartPadding(layoutDir)
                    - outerPadPlot.calculateEndPadding(layoutDir)).toPx()
        }.coerceAtLeast(1f)

        val desiredDays = maxDaysOnScreen.coerceAtLeast(1)
        val stridePxRaw = viewportPx / desiredDays
        val minGapPx    = with(density) { 3.dp.toPx() }
        val gapPx       = max(stridePxRaw * 0.25f, minGapPx)
        val stridePx    = stridePxRaw

        val pointRadiusPx   = with(density) { pointRadiusDp.toPx() }
        val pointStrokePx   = with(density) { pointStrokeDp.toPx() }
        val hitRadiusPx     = with(density) { hitRadiusDp.toPx() }
        val bubblePaddingPx = with(density) { bubblePaddingDp.toPx() }
        val bubbleCornerPx  = with(density) { bubbleCornerDp.toPx() }
        val bubbleArrowPx   = with(density) { bubbleArrowSizeDp.toPx() }

        Row(Modifier.fillMaxSize()) {
            // 左轴：固定刻度列
            Canvas(modifier = Modifier.width(yAxisWidth).fillMaxHeight()) {
                val topY = outerPadPlot.calculateTopPadding().toPx() + innerTopGutter.toPx()
                val botY = size.height - (outerPadPlot.calculateBottomPadding().toPx() + innerBotGutter.toPx())
                val usableH = (botY - topY).coerceAtLeast(1f)

                val gridColor = axisColor.copy(alpha = 0.55f)
                val zeroColor = axisColor.copy(alpha = 0.90f)
                for (v in 0..yMax step yStep) {
                    val y = botY - (v.toFloat() / yMax.toFloat()) * usableH
                    drawLine(
                        color = if (v == 0) zeroColor else gridColor,
                        start = Offset(x = size.width, y = y),
                        end   = Offset(x = 0f,        y = y),
                        strokeWidth = if (v == 0) 1.2f else 1f
                    )
                    val paint = android.graphics.Paint().apply {
                        color = labelColor.toArgb()
                        textSize = with(this) { 10.sp.toPx() }
                        isAntiAlias = true
                    }
                    drawContext.canvas.nativeCanvas.drawText("$v", 0f, y, paint)
                }
            }

            // 右侧：主画布（横向可滚）
            Box(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(hScroll)
            ) {
                val contentW = with(density) { (d.size * stridePx).toDp() } + outerPadPlot.calculateEndPadding(layoutDir)

                // 用 Box 叠一层指针输入，内部是 Canvas（方便把点击坐标传入）
                Box(
                    modifier = Modifier
                        .width(contentW)
                        .fillMaxHeight()
                        .padding(outerPadPlot)
                ) {
                    Canvas(
                        modifier = Modifier
                            .matchParentSize()
                            // 点击命中最近圆点，计算并记录 selection
                            .pointerInput(d, a, b, yMax, stridePx, hScroll.value) {
                                detectTapGestures { offset ->
                                    // 几何参数
                                    val topY  = innerTopGutter.toPx()
                                    val botY  = size.height - innerBotGutter.toPx()
                                    val usableH = (botY - topY).coerceAtLeast(1f)

                                    fun xFor(i: Int): Float = i * stridePx + stridePx / 2f
                                    fun yFor(v: Int): Float = botY - (v / yMax.toFloat()) * usableH

                                    // 找到最近的列索引
                                    val rawIndex = ((offset.x) / stridePx).toInt().coerceIn(0, d.lastIndex)

                                    // 在附近 1 列范围内挑最近点，提高命中率
                                    val candidates = (rawIndex - 1..rawIndex + 1).filter { it in 0..d.lastIndex }

                                    var bestSel: Selection? = null
                                    var bestDist = Float.MAX_VALUE

                                    candidates.forEach { i ->
                                        val xa = xFor(i)
                                        val ya = yFor(a[i])
                                        val xb = xFor(i)
                                        val yb = yFor(b[i])

                                        val da = hypot(offset.x - xa, offset.y - ya)
                                        val db = hypot(offset.x - xb, offset.y - yb)

                                        if (da < bestDist) { bestDist = da; bestSel = Selection(i, 0) }
                                        if (db < bestDist) { bestDist = db; bestSel = Selection(i, 1) }
                                    }

                                    selection = if (bestDist <= hitRadiusPx) bestSel else null
                                }
                            }
                    ) {
                        val topY  = innerTopGutter.toPx()
                        val botY  = size.height - innerBotGutter.toPx()
                        val leftX = 0f
                        val rightX= size.width
                        val usableH = (botY - topY).coerceAtLeast(1f)

                        // 网格
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

                        // 像素映射保持原样
                        fun xFor(i: Int): Float = i * stridePx + stridePx / 2f
                        fun yFor(v: Int): Float = botY - (v / yMax.toFloat()) * usableH

                        // 摄入 A
                        val ptsA = List(d.size) { i -> Offset(xFor(i), yFor(a[i])) }
                        val pathA = buildMonotonePath(ptsA)
                        drawPath(
                            path = pathA,
                            color = lineAColor,
                            style = Stroke(width = with(density){ lineStrokeDp.toPx() }, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )

                        // 消耗 B
                        val ptsB = List(d.size) { i -> Offset(xFor(i), yFor(b[i])) }
                        val pathB = buildMonotonePath(ptsB)
                        drawPath(
                            path = pathB,
                            color = lineBColor,
                            style = Stroke(width = with(density){ lineStrokeDp.toPx() }, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )

                        // 选中点额外高亮（外圈描边一圈）
                        selection?.let { sel ->
                            val i = sel.index
                            val isA = sel.series == 0
                            val v   = if (isA) a[i] else b[i]
                            val c   = if (isA) lineAColor else lineBColor
                            val cx  = xFor(i)
                            val cy  = yFor(v)

                            drawCircle(
                                color = c,
                                radius = 4.dp.toPx(), // 外圈半径稍大
                                center = Offset(cx, cy),
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 2.dp.toPx(), // 外圈半径稍大
                                center = Offset(cx, cy),
                            )
                        }

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
                                "${day.monthValue}/${day.dayOfMonth}",
                                x,
                                botY + with(density){12.dp.toPx()},
                                textPaint
                            )
                        }

                        selection?.let { sel ->
                            val i = sel.index
                            val isA = sel.series == 0
                            val value = if (isA) a[i] else b[i]
                            var color = if (isA) lineAColor else lineBColor
                            val cx = xFor(i)
                            val cy = yFor(value)
                            val seriesLabel = if (isA) "摄入" else "消耗"
                            val text = "$seriesLabel ${value} kcal"

                            // 文本测量
                            val bubbleTextPaint = android.graphics.Paint().apply {
                                color = Color.Black                      // ✅ 黑字
                                textSize = with(density) { bubbleTextSizeSp.toPx() }
                                isAntiAlias = true
                            }
                            val textWidth = bubbleTextPaint.measureText(text)
                            val fm = bubbleTextPaint.fontMetrics
                            val textHeight = fm.bottom - fm.top

                            val bubbleW = textWidth + bubblePaddingPx * 2
                            val bubbleH = textHeight + bubblePaddingPx * 2

                            // 默认放在点的上方，越界则放下方
                            val gap = 6.dp.toPx()
                            var left = (cx - bubbleW / 2f).coerceIn(0f, size.width - bubbleW)
                            var top  = (cy - gap - bubbleH)      // 上方
                            val placeAbove = top >= 0f
                            if (!placeAbove) {
                                top = (cy + gap).coerceIn(0f, size.height - bubbleH)
                            }

                            // —— 阴影（简易）：下方 2dp 的半透明圆角矩形 —— //
                            val shadowOffsetY = 2.dp.toPx()
                            drawRoundRect(
                                color = Color.Black.copy(alpha = 0.12f),
                                topLeft = Offset(left, top + shadowOffsetY),
                                size = Size(bubbleW, bubbleH),
                                cornerRadius = CornerRadius(bubbleCornerPx, bubbleCornerPx)
                            )

                            // —— 白底主框 —— //
                            drawRoundRect(
                                color = Color.White,                               // ✅ 白底
                                topLeft = Offset(left, top),
                                size = Size(bubbleW, bubbleH),
                                cornerRadius = CornerRadius(bubbleCornerPx, bubbleCornerPx)
                            )

                            // —— 细描边（低饱和） —— //
                            drawRoundRect(
                                color = cs.outline.copy(alpha = 0.25f),
                                topLeft = Offset(left, top),
                                size = Size(bubbleW, bubbleH),
                                cornerRadius = CornerRadius(bubbleCornerPx, bubbleCornerPx),
                                style = Stroke(width = 1.dp.toPx())
                            )

                            // —— 文本 —— //
                            val tx = left + bubblePaddingPx
                            val ty = top + bubblePaddingPx - fm.top
                            drawContext.canvas.nativeCanvas.drawText(text, tx, ty, bubbleTextPaint)
                        }
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

    BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
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
                        val labelY = if (v > 0) (top - labelGap + 2.dp.toPx())
                        else (bottom + labelGap + textPaint.textSize * 0.15f + 2.dp.toPx())
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

// 通过所有点，且在每段 [Pi, Pi+1] 内不越过两端 y 值范围
private fun buildMonotonePath(points: List<Offset>): Path {
    val n = points.size
    val path = Path()
    if (n == 0) return path
    path.moveTo(points[0].x, points[0].y)
    if (n == 1) return path

    val x = FloatArray(n) { points[it].x }
    val y = FloatArray(n) { points[it].y }

    // 斜率（割线）与端点切线
    val d = FloatArray(n - 1)
    for (i in 0 until n - 1) {
        val dx = x[i + 1] - x[i]
        d[i] = if (dx != 0f) (y[i + 1] - y[i]) / dx else 0f
    }
    val m = FloatArray(n)
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






