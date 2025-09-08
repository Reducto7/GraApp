package com.example.gra.ui.records

import android.app.DatePickerDialog
import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.example.gra.ui.viewmodel.BodyMeasureViewModel
import com.example.gra.ui.viewmodel.BodyType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import com.example.gra.ui.viewmodel.BODY_DISPLAY_ORDER
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class MeasureItemUi(
    val id: String,          // 没有真实 id 时，用 "typeKey-YYYY-MM-DD"
    val date: LocalDate,
    val value: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyMeasurePage(
    navController: NavHostController,
    vm: BodyMeasureViewModel = viewModel()
) {
    val latest by vm.latestMap.collectAsState()
    val selected by vm.selectedType.collectAsState()
    val history by vm.history.collectAsState()

    // ✅ 记录日期（默认今天），供所有“添加记录”弹窗使用
    var recordDate by remember { mutableStateOf(LocalDate.now()) }

    var showAddSheet by remember { mutableStateOf<BodyType?>(null) }
    val context = LocalContext.current

    // 生成 labels：把 history.date -> "M/d"
    val xLabels = remember(history) {
        history.map { LocalDate.parse(it.date).format(DateTimeFormatter.ofPattern("M/d")) }
    }

    var editTarget by remember { mutableStateOf<MeasureItemUi?>(null) } // 自己的 UI 结构：包含 id/date/value


    Scaffold(
        topBar = {
            // === 居中标题 + 右上角日历 ===
            TopAppBar(
                title = {
                    Box(Modifier.fillMaxWidth()) {
                        Text(
                            "围度记录",
                            modifier = Modifier.align(Alignment.Center),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
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
                            imageVector = Icons.Default.DateRange, // 如果没有导入，可用 Filled.Event
                            contentDescription = "选择记录日期"
                        )
                    }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(com.example.gra.ui.topBlue, com.example.gra.ui.bottomGreen)
                    )
                )
                .padding(inner)
                .padding(16.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            // 1) 六个卡片，3列网格
            for (row in BODY_DISPLAY_ORDER.chunked(3)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEach { t ->
                        MeasureCard(
                            title = t.label,
                            value = latest[t]?.value,
                            unit = t.unit,
                            onAdd = { showAddSheet = t },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(16.dp))

            // 2) Tab 切换不同维度的曲线
            val selectedIndex = BODY_DISPLAY_ORDER.indexOf(selected).coerceAtLeast(0)

            val shape = RoundedCornerShape(8.dp)

            Surface(
                shape = shape,
                color = Color.White,
                tonalElevation = 0.dp,
                modifier = Modifier.clip(shape) // 保证子内容遵循圆角
            ) {
                ScrollableTabRow(
                    selectedTabIndex = selectedIndex,
                    edgePadding = 0.dp,
                    containerColor = Color.Transparent,   // 让外层白底生效（或 Color.White）
                    divider = {},                         // 去掉灰色底线
                    indicator = { tabPositions ->         // 可保留默认；这里演示细一点的指示器（可选）
                        TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                            height = 2.dp
                        )
                    }
                ) {
                    BODY_DISPLAY_ORDER.forEach { t ->
                        Tab(
                            selected = selected == t,
                            onClick = { vm.select(t) },
                            text = { Text(t.label) }
                        )
                    }
                }
            }

            //3.折线图
            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(16.dp))                  // 让背景有圆角
                    .background(Color.White.copy(alpha = 0.7f))       // 半透明背景
            ) {
                PrettyLineChart(
                    points = history.map { it.value },
                    labels = xLabels,
                    color = MaterialTheme.colorScheme.primary,
                    onPointTap = { index ->
                        val rec = history.getOrNull(index) ?: return@PrettyLineChart
                        editTarget = MeasureItemUi(
                            id = (rec.id ?: "${selected.key}-${rec.date}").toString(),
                            date = LocalDate.parse(rec.date),
                            value = rec.value
                        )
                    }
                )
            }

            Spacer(Modifier.height(12.dp))

            // 4) 记录按钮（当前维度）
            Button(
                onClick = { showAddSheet = selected },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) { Text("记录${selected.label}") }
        }

        if (showAddSheet != null) {
            AddMeasureSheet(
                type = showAddSheet!!,
                currentDate = recordDate,                    // 仅作为初始值
                onDismiss = { showAddSheet = null },
                onSave = { value, chosenDate ->              // ← 接收 Sheet 回传的日期
                    vm.addRecord(showAddSheet!!, value, chosenDate)
                    showAddSheet = null
                }
            )
        }

        if (editTarget != null) {
            EditMeasureSheet(
                type = selected,                 // 你当前选中的 BodyType
                item = editTarget!!,
                onDismiss = { editTarget = null },
                onUpdate = { id, newValue, newDate ->
                    vm.updateRecord(selected, id, newValue, newDate)  // 第 3 步实现
                    editTarget = null
                },
                onDelete = { id ->
                    vm.deleteRecord(selected, id)                      // 第 3 步实现
                    editTarget = null
                }
            )
        }
    }
}

@Composable
private fun MeasureCard(
    title: String,
    value: Double?,
    unit: String,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 2.dp,
        color = Color.White.copy(alpha = 0.7f),
        modifier = modifier.height(110.dp)
    ) {
        Box(Modifier
            .fillMaxSize()
            .padding(14.dp)) {
            Column(Modifier.align(Alignment.TopStart)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (value == null) "-" else "${"%.1f".format(value)} $unit",
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1
                )
            }
            // ✅ 改为右下角，避免遮挡文字
            SmallFloatingActionButton(
                onClick = onAdd,
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (12).dp, y = (12).dp)   // ✅ 向左、向上各偏移 6dp
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMeasureSheet(
    type: BodyType,
    currentDate: LocalDate,
    onDismiss: () -> Unit,
    onSave: (Double, LocalDate) -> Unit         // ← 改这里
) {
    var input by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(currentDate) }   // Sheet 内可修改的日期
    val context = LocalContext.current

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier
            .fillMaxWidth()
            .padding(16.dp)) {
            Text("记录${type.label}（单位：${type.unit}）", style = MaterialTheme.typography.titleMedium)

            Spacer(Modifier.height(6.dp))
            Text(
                text = "日期：${date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))}",
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.clickable {
                    val today = LocalDate.now()
                    DatePickerDialog(
                        context,
                        { _, y, m, d ->
                            val picked = LocalDate.of(y, m + 1, d)
                            if (!picked.isAfter(today)) date = picked
                        },
                        date.year, date.monthValue - 1, date.dayOfMonth
                    ).apply { datePicker.maxDate = System.currentTimeMillis() }
                        .show()
                }
            )

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("请输入数值") },
                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    val v = input.toDoubleOrNull() ?: 0.0
                    if (v > 0) onSave(v, date)   // ← 把 Sheet 里选择的 date 回传
                },
                enabled = (input.toDoubleOrNull() ?: 0.0) > 0,
                modifier = Modifier.fillMaxWidth()
            ) { Text("保存") }
            Spacer(Modifier.height(12.dp))
        }
    }
}



@Composable
private fun PrettyLineChart(
    points: List<Double>,
    labels: List<String>,                    // 与 points 一一对应的日期，如 "8/13"
    color: Color = MaterialTheme.colorScheme.primary,
    onPointTap: (Int) -> Unit = {}
) {
    if (points.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("暂无数据")
        }
        return
    }

    Canvas(
        Modifier
            .fillMaxWidth()
            .height(260.dp)
            .padding(horizontal = 12.dp, vertical = 12.dp)
            .pointerInput(points) {                         // ✅ 点击映射到最近点
                detectTapGestures { offset ->
                    // 下面这些局部变量要和你上面绘图保持一致
                    val leftPad = 40.dp.toPx()
                    val rightPad = 8.dp.toPx()
                    val chartW = size.width - leftPad - rightPad

                    if (points.size == 1) {
                        onPointTap(0)
                    } else {
                        val t = ((offset.x - leftPad) / chartW)
                            .coerceIn(0f, 1f)
                        val idx = (t * (points.size - 1)).roundToInt()
                            .coerceIn(0, points.lastIndex)
                        onPointTap(idx)
                    }
                }
            }
    ) {
        val w = size.width
        val h = size.height

        // 布局间距
        val leftPad = 40.dp.toPx()     // ✅ 给 Y 轴刻度留空间
        val rightPad = 8.dp.toPx()
        val topPad = 10.dp.toPx()
        val bottomPad = 28.dp.toPx()   // ✅ 给 X 轴标签留空间

        val chartW = w - leftPad - rightPad
        val chartH = h - topPad - bottomPad

        val minV = points.minOrNull() ?: 0.0
        val maxV = points.maxOrNull() ?: 0.0
        val span = (maxV - minV).let { if (it < 1e-6) 1.0 else it }

        fun posX(i: Int): Float =
            leftPad + chartW * (if (points.size == 1) 0f else (i / (points.size - 1f)))
        fun posY(v: Double): Float =
            topPad + chartH * (1f - ((v - minV) / span).toFloat())

        // ====== 网格 & Y 轴刻度 ======
        val gridColor = Color.LightGray.copy(alpha = 0.35f)
        val yTicks = 4
        val labelPaint = android.graphics.Paint().apply {
            //color = Color.Gray.toArgb()
            textAlign = android.graphics.Paint.Align.RIGHT
            textSize = 11.sp.toPx()
            isAntiAlias = true
        }
        repeat(yTicks + 1) { i ->
            val y = topPad + chartH * (i / yTicks.toFloat())
            drawLine(gridColor, Offset(leftPad, y), Offset(leftPad + chartW, y), strokeWidth = 1f)

            val v = maxV - (span * (i / yTicks.toDouble()))
            drawContext.canvas.nativeCanvas.drawText(
                String.format("%.1f", v),
                leftPad - 6.dp.toPx(),
                y + 4.dp.toPx(),
                labelPaint
            )
        }

        // ==== 生成平滑路径（通过所有采样点） ====
        val pts = points.indices.map { i -> Offset(posX(i), posY(points[i])) }

// 折线路径（平滑）
        val linePath = buildMonotonePath(pts)

// 面积路径（在折线基础上往下闭合）
        val areaPath = Path().apply {
            addPath(linePath)
            lineTo(posX(points.lastIndex), topPad + chartH)
            lineTo(posX(0),               topPad + chartH)
            close()
        }

// ==== 先绘制面积，再绘制线 ====
        val lineColor = color
        val areaColor = color.copy(alpha = 0.15f)
        drawPath(areaPath, areaColor)

        drawPath(
            path = linePath,
            color = lineColor,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 3f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round
            )
        )

        // ====== 中空圆点 + 数值标签 ======
        val dotStroke = 3f
        val dotOuter = 6f
        val dotInner = 3.5f
        val valuePaint = android.graphics.Paint().apply {
            //color = color.toArgb()
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = 11.sp.toPx()
            isAntiAlias = true
        }
        points.indices.forEach { i ->
            val p = Offset(posX(i), posY(points[i]))
            // 外圈（描边）
            drawCircle(color = lineColor, radius = dotOuter, center = p, style = androidx.compose.ui.graphics.drawscope.Stroke(width = dotStroke))
            // 内圈白色（中空效果）
            drawCircle(color = Color.White, radius = dotInner, center = p)
            // 数值标签（在点上方）
            drawContext.canvas.nativeCanvas.drawText(
                String.format("%.1f", points[i]),
                p.x,
                p.y - 8.dp.toPx(),
                valuePaint
            )
        }

        // ====== X 轴标签（最多 6 个，含首尾） ======
        val maxTicks = 6
        val (idxs, labs) = if (labels.size <= maxTicks) {
            labels.indices.toList() to labels
        } else {
            val step = (labels.size - 1).toFloat() / (maxTicks - 1)
            val ids = (0 until maxTicks).map { (it * step).toInt().coerceIn(0, labels.lastIndex) }
            ids to ids.map { labels[it] }
        }
        val xPaint = android.graphics.Paint().apply {
            //color = Color.Gray.toArgb()
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = 11.sp.toPx()
            isAntiAlias = true
        }
        idxs.forEachIndexed { i, ix ->
            val x = posX(ix)
            drawContext.canvas.nativeCanvas.drawText(
                labs[i],
                x,
                topPad + chartH + 18.dp.toPx(),
                xPaint
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditMeasureSheet(
    type: BodyType,
    item: MeasureItemUi,
    onDismiss: () -> Unit,
    onUpdate: (id: String, newValue: Double, newDate: LocalDate) -> Unit,
    onDelete: (id: String) -> Unit
) {
    var valueText by remember { mutableStateOf("%.1f".format(item.value)) }
    var date by remember { mutableStateOf(item.date) }
    val context = LocalContext.current

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier
            .fillMaxWidth()
            .padding(16.dp)) {
            Text("编辑${type.label}", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            // 日期（可点改）
            Text(
                text = "日期：${date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))}",
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.clickable {
                    val today = LocalDate.now()
                    DatePickerDialog(
                        context,
                        { _, y, m, d ->
                            val picked = LocalDate.of(y, m + 1, d)
                            if (!picked.isAfter(today)) date = picked
                        },
                        date.year, date.monthValue - 1, date.dayOfMonth
                    ).apply { datePicker.maxDate = System.currentTimeMillis() }.show()
                }
            )

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = valueText,
                onValueChange = { valueText = it },
                label = { Text("数值（${type.unit}）") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // 删除
                OutlinedButton(
                    onClick = { onDelete(item.id) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("删除") }

                // 保存
                Button(
                    onClick = {
                        val v = valueText.toDoubleOrNull() ?: return@Button
                        onUpdate(item.id, v, date)
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("保存") }
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

// 通过所有点，且在每段 [Pi, Pi+1] 内不越过两端 y 值范围（Fritsch–Carlson）
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

