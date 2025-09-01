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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(com.example.gra.ui.topBlue, com.example.gra.ui.bottomGreen)
                    )
                )
                .padding(16.dp)
        ) {

            // —— 仅做“饮食卡” —— //
            FoodOnlyCard(
                totalKcal = foodViewModel.totalIntakeKcal.value,
                meals = foodViewModel.meals.map { m ->
                    MealUi(
                        index = m.index,
                        title = m.name,
                        kcal = m.kcal,
                        items = m.items.map { it ->
                            FoodItemUi(
                                name = it.name,
                                grams = it.grams.toInt(),   // UI 用整数克数显示
                                kcal = it.kcal
                            )
                        }
                    )
                },
                onAddClick = { navController.navigate("food") },
                recommendedIntake = 2200,
                onDeleteItem = { mealIdx, itemIdx ->
                    foodViewModel.deleteMealItem(recordDate, mealIdx, itemIdx)
                }
            )

            Spacer(Modifier.height(16.dp))

            // —— 运动卡（新增） —— //
            // 你已有 selectedDate（顶部 DateSelector 的状态）
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
                ringColor = MaterialTheme.colorScheme.secondary,
                onDeleteSession = { sessionIdx ->
                    foodViewModel.deleteExerciseItem(recordDate, sessionIdx)
                }
            )

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
            ).padding(16.dp)) {

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
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = "推荐摄入说明",
                tint = tint
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
    ringColor: Color = MaterialTheme.colorScheme.secondary,
    onDeleteSession: (sessionIndex: Int) -> Unit
) {
    var expandedIndex by remember { mutableStateOf<Int?>(null) }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(2.dp)
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
                        SmallBarLabel(text = "运动", color = ringColor)

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
                                ringColor = ringColor,
                                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                startAngle = -90f,
                                counterClockwise = true,
                                stroke = 18.dp
                            )
                        }
                    }
                }

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
                                // —— 饮食卡 —— //
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

            // 右上角 “+”
            SmallFloatingActionButton(
                onClick = onAddClick,
                containerColor = ringColor,
                modifier = Modifier
                    .align(Alignment.TopEnd)   // 注意：放在 Box 作用域中才能用 align
                    .offset(x = (-4).dp, y = 4.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加运动")
            }
        }
    }
}
