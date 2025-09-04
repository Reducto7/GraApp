package com.example.gra.ui.records

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.ButtonDefaults.buttonColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.gra.ui.data.Remote
import com.example.gra.ui.viewmodel.NightBar
import com.example.gra.ui.viewmodel.SleepFormState
import com.example.gra.ui.viewmodel.SleepViewModel
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filter
import java.time.ZoneId
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import java.time.Duration
import kotlin.math.max
import kotlin.math.min


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepPage(
    navController: NavHostController,
    vm: SleepViewModel = viewModel()
) {
    val ui by vm.ui.collectAsState()
    val saving by vm.saving.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) { vm.init() }

    val today = java.time.LocalDate.now()

    val startDate = ui.rangeStart
    val endDate   = ui.rangeEnd

    val hScroll = rememberScrollState()

    val sessions by vm.sessions.collectAsState(emptyList())

    // 收事件：保存成功/失败
    LaunchedEffect(Unit) {
        vm.events.collect { ev ->
            when (ev) {
                is SleepViewModel.SleepEvent.Saved -> snackbarHostState.showSnackbar("已保存")
                is SleepViewModel.SleepEvent.Error -> snackbarHostState.showSnackbar("保存失败：${ev.msg}")
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("睡眠") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .background(brush = Brush.verticalGradient(listOf(topBlue, bottomGreen)))
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 16.dp)
        ) {
            // ===== 顶部：大图（保持你已有的 SleepChart 组件） =====
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                shape = RoundedCornerShape(12.dp),
                //colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f))
                colors = CardDefaults.cardColors(Color.Transparent)
            ) {
                var chartTab by remember { mutableStateOf(1) } // 0=睡眠时长  1=具体时间（默认）

                Column(Modifier.fillMaxSize()) {
                    // —— 顶部中间的切换按钮（圆角4dp，选中=primary，未选中=白色） ——
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
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(36.dp),
                                shape = RoundedCornerShape(4.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selected) cs.primary else Color.White,
                                    contentColor   = if (selected) cs.onTertiary else cs.onSurface
                                ),
                                border = if (!selected) BorderStroke(1.dp, cs.outlineVariant) else null,
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                            ) { Text(title, style = MaterialTheme.typography.labelLarge) }
                        }

                        TabBtn("睡眠时长", chartTab == 0) { chartTab = 0 }
                        Spacer(Modifier.width(12.dp))
                        TabBtn("具体时间", chartTab == 1) { chartTab = 1 }
                    }

                    Spacer(Modifier.height(12.dp))

                    // 内容区
                    Box(
                        Modifier.fillMaxSize()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.4f))
                    ) {
                        when (chartTab) {
                            // ✅ 新增：按“睡眠时长”显示
                            0 -> SleepDurationBarChart(
                                sessions = sessions,
                                startDate = ui.rangeStart,
                                endDate   = ui.rangeEnd,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                maxDaysOnScreen = 7,   // 同屏显示的天数（可改）
                                barWidthFraction = 0.30f
                            )
                            // ✅ 原来的“具体时间”图（不改你的 SleepChart 实现）
                            1 -> SleepChart(
                                bars = ui.bars,
                                startDate = ui.rangeStart,
                                endDate   = ui.rangeEnd,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // 顶部双按钮 + 内容切换
            var tab by remember { mutableStateOf(0) } // 0=添加睡眠  1=历史记录
            val sessions by vm.sessions.collectAsState() // ⬅️ ② 里会在 VM 暴露

            val contentHeight = 380.dp   // ✅ 固定内容区高度（可按需调整）

            val contentMod = Modifier
                .fillMaxWidth()
                .height(contentHeight)

            Card(
                contentMod,
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(Modifier.padding(16.dp)) {
                    // —— 顶部切换按钮行 ——
                    // —— 顶部切换按钮行（圆角4dp，选中=tertiary，未选中=白色） ——
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val cs = MaterialTheme.colorScheme

                        @Composable
                        fun TabButton(title: String, selected: Boolean, onClick: () -> Unit) {
                            Button(
                                onClick = onClick,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp),
                                shape = RoundedCornerShape(4.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selected) cs.primary else Color.White,
                                    contentColor   = if (selected) cs.onTertiary else cs.onSurface
                                ),
                                border = if (!selected) BorderStroke(1.dp, cs.outlineVariant) else null,
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                            ) {
                                Text(title, style = MaterialTheme.typography.labelLarge)
                            }
                        }

                        TabButton("添加睡眠", tab == 0) { tab = 0 }
                        TabButton("历史记录", tab == 1) { tab = 1 }
                    }

                    Spacer(Modifier.height(12.dp))

                    when (tab) {
                        0 -> {
                            // 原来的表单（注意我们去掉了表单内部的“添加睡眠”标题行）
                            AddSleepCard(
                                state = ui.form,
                                saving = saving,
                                onStartDateChange = vm::setStartDate,
                                onStartTimeChange = vm::setStartTime,
                                onEndDateChange   = vm::setEndDate,
                                onEndTimeChange   = vm::setEndTime,
                                onSubmit = {
                                    focusManager.clearFocus()
                                    vm.submit()
                                }
                            )
                        }
                        1 -> {
                            // 历史记录列表（上下滚动）
                            SleepHistoryList(
                                list = sessions,
                                onDelete = { id -> vm.deleteSession(id) },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}

/* ---------------------- 添加睡眠卡片（可点可用） ---------------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSleepCard(
    state: SleepFormState,
    saving: Boolean,
    onStartDateChange: (LocalDate) -> Unit,
    onStartTimeChange: (LocalTime) -> Unit,
    onEndDateChange: (LocalDate) -> Unit,
    onEndTimeChange: (LocalTime) -> Unit,
    onSubmit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("添加睡眠", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))

            Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            DateField(
                label = "开始日期",
                date = state.startDate,
                onPick = onStartDateChange,
                dateUpperBound = java.time.LocalDate.now(),
                modifier = Modifier.weight(1f)   // ✅ weight 在 Row 作用域
            )
            TimeField(
                label = "开始时间",
                time = state.startTime,
                onPick = onStartTimeChange,
                modifier = Modifier.weight(1f)   // ✅
            )
        }

            Spacer(Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                DateField(
                    label = "结束日期",
                    date = state.endDate,
                    onPick = onEndDateChange,
                    dateUpperBound = java.time.LocalDate.now(),
                    modifier = Modifier.weight(1f)   // ✅
                )
                TimeField(
                    label = "结束时间",
                    time = state.endTime,
                    onPick = onEndTimeChange,
                    modifier = Modifier.weight(1f)   // ✅
                )
            }


            Spacer(Modifier.height(8.dp))
            Text(
                "预计时长：${state.durationText}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )

            val disableReason = remember(state) {
                when {
                    !state.isValid -> "请检查开始/结束时间（结束需晚于开始，且不得超过今天）"
                    else -> null
                }
            }
            if (disableReason != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    disableReason,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onSubmit,
                enabled = state.isValid && !saving,
                modifier = Modifier.fillMaxWidth(),
                colors = buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.30f),
                    disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.70f)
                )
            ) {
                if (saving) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("保存中…")
                } else {
                    Text("保存")
                }
            }
        }
    }
}

/* ---------------------- 可复用：日期选择 ---------------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(
    label: String,
    date: java.time.LocalDate,
    onPick: (java.time.LocalDate) -> Unit,
    dateLowerBound: java.time.LocalDate = java.time.LocalDate.of(2000,1,1),
    dateUpperBound: java.time.LocalDate = java.time.LocalDate.now(),
    modifier: Modifier = Modifier          // ⬅️ 由外部决定 weight/宽度
) {
    var open by remember { mutableStateOf(false) }

    // 外层点击层：不抢焦点、点击稳定
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { open = true }
    ) {
        OutlinedTextField(
            value = date.toString(),
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            enabled = false,
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                disabledContainerColor = MaterialTheme.colorScheme.surface,
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledIndicatorColor = MaterialTheme.colorScheme.outline
            )
        )
    }

    if (open) {
        val millisInit = date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        val state = rememberDatePickerState(
            initialSelectedDateMillis = millisInit,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val d = java.time.Instant.ofEpochMilli(utcTimeMillis)
                        .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                    return !d.isBefore(dateLowerBound) && !d.isAfter(dateUpperBound)
                }
            }
        )
        M3DatePickerDialog(
            onDismissRequest = { open = false },
            onConfirm = {
                val millis = state.selectedDateMillis ?: return@M3DatePickerDialog
                val picked = java.time.Instant.ofEpochMilli(millis)
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                onPick(picked)
                open = false
            }
        ) {
            DatePicker(state = state)
        }
    }
}


/* ---------------------- 可复用：时间选择（不依赖 android.app） ---------------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeField(
    label: String,
    time: java.time.LocalTime,
    onPick: (java.time.LocalTime) -> Unit,
    modifier: Modifier = Modifier      // ⬅️ 由外部决定 weight/宽度
) {
    var open by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { open = true }
    ) {
        OutlinedTextField(
            value = "%02d:%02d".format(time.hour, time.minute),
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            enabled = false,
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                disabledContainerColor = MaterialTheme.colorScheme.surface,
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledIndicatorColor = MaterialTheme.colorScheme.outline
            )
        )
    }

    if (open) {
        val state = rememberTimePickerState(
            initialHour = time.hour,
            initialMinute = time.minute,
            is24Hour = true
        )
        M3TimePickerDialog(
            state = state,
            onDismissRequest = { open = false },
            onConfirm = {
                onPick(java.time.LocalTime.of(state.hour, state.minute))
                open = false
            }
        )
    }
}


/* ---------------------- M3 对话框封装：兼容低版本 material3 ---------------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun M3TimePickerDialog(
    state: TimePickerState,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = { TextButton(onClick = onConfirm) { Text("确定") } },
        dismissButton = { TextButton(onClick = onDismissRequest) { Text("取消") } },
        text = { TimePicker(state = state) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun M3DatePickerDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismissRequest,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // 自己画一个 Material 风格的对话框外壳，强制宽度
        Surface(
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 6.dp,
            modifier = Modifier
                .widthIn(min = 100.dp)       // ✅ 关键：保证周列不挤
                .wrapContentHeight()
        ) {
            Column(Modifier.padding(24.dp)) {
                // DatePicker 内容
                Box(Modifier.fillMaxWidth()) { content() }

                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) { Text("取消") }
                    Spacer(Modifier.width(4.dp))
                    TextButton(onClick = onConfirm) { Text("确定") }
                }
            }
        }
    }
}


@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun SleepChart(
    bars: List<NightBar>,
    startDate: LocalDate,
    endDate: LocalDate,
    modifier: Modifier = Modifier,
    onBarClick: ((NightBar?) -> Unit)? = null,
    maxDaysOnScreen: Int = 7,          // ✅ 同屏最多显示几天（可改）
    barWidthFraction: Float = 0.30f      // ✅ 单天步距里柱子的占比（0.45~0.75 都可）
) {
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val axisColor  = MaterialTheme.colorScheme.outlineVariant
    val barColor   = MaterialTheme.colorScheme.primary

    val yAxisWidth = 44.dp               // 左侧固定刻度列的宽度
    val density = LocalDensity.current
    val layoutDir = LocalLayoutDirection.current

    // 完整日期序列（含端点），并映射当日数据
    val days = buildList {
        var d = startDate
        while (!d.isAfter(endDate)) { add(d); d = d.plusDays(1) }
    }
    val byDate = bars.associateBy { it.bedDate }

    // 仅用“有数据的天”计算上下范围；下半只在“入睡>=中午(12:00)”时计入
    val dataList = days.mapNotNull { byDate[it] }
    fun lowerBase(bedMin: Int) = if (bedMin >= 720) 1440 - bedMin else 0
    val rawUpper = dataList.maxOfOrNull { it.wakeMinutes } ?: 0
    val rawLower = dataList.maxOfOrNull { lowerBase(it.bedMinutes) } ?: 0
    val upperMax = (rawUpper + 120).coerceAtMost(720) // +2h 缓冲，≤12h
    val lowerMax = (rawLower + 120).coerceAtMost(720)

    // 内/外边距
    val outerPadPlot       = PaddingValues(start = 0.dp, end = 12.dp, top = 16.dp, bottom = 28.dp)
    val innerTopGutter     = 28.dp   // 给上端时间 & 顶部日期轴留空间
    val innerBottomGutter  = 18.dp   // 给下端时间留空间

    // 右侧横向滚动；进入默认滑到最右（最近一天）
    val hScroll = rememberScrollState()
    LaunchedEffect(days.size) {
        snapshotFlow { hScroll.maxValue }.filter { it > 0 }.first()
        hScroll.scrollTo(hScroll.maxValue)
    }

    // ======= 关键：按“同屏 N 天”动态计算单天步距（柱宽 + 间距） =======
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val viewportPx = with(density) {
            // 右侧可见区域宽度 = 整体最大宽 - 左刻度列 - 右画布左右内边距
            (maxWidth - yAxisWidth
                    - outerPadPlot.calculateStartPadding(layoutDir)
                    - outerPadPlot.calculateEndPadding(layoutDir)).toPx()
        }.coerceAtLeast(1f)

        val desiredDays = maxDaysOnScreen.coerceAtLeast(1)
        val stridePxRaw = viewportPx / desiredDays               // 单天“步距”
        val minBarPx    = with(density) { 6.dp.toPx() }
        val minGapPx    = with(density) { 3.dp.toPx() }

        var barW  = (stridePxRaw * barWidthFraction).coerceAtLeast(minBarPx)
        var gapPx = (stridePxRaw - barW).coerceAtLeast(minGapPx)

        // 若柱+间距总和偏离步距太多，微调一下柱宽
        val strideNow = barW + gapPx
        if (strideNow != stridePxRaw) {
            barW += (stridePxRaw - strideNow) // 调到刚好 = 步距
            if (barW < minBarPx) { barW = minBarPx }
        }

        // 右侧画布总宽 = 天数 * 步距 - 最后一天不加 gap + 右侧留白
        val totalW = if (days.isEmpty()) 0f else ((days.size - 1) * (barW + gapPx) + barW)
        val canvasWidthDp = with(density) { totalW.toDp() } + 24.dp

        Row(Modifier.fillMaxWidth()) {

            // ---------- 左侧固定列：时间刻度 ----------
            Canvas(
                Modifier
                    .width(yAxisWidth)
                    .fillMaxHeight()
                    .padding(
                        top = outerPadPlot.calculateTopPadding(),
                        bottom = outerPadPlot.calculateBottomPadding()
                    )
            ) {
                val contentH = size.height
                val topG = with(density) { innerTopGutter.toPx() }
                val botG = with(density) { innerBottomGutter.toPx() }
                val innerH = contentH - topG - botG

                val totalRange = (upperMax + lowerMax).coerceAtLeast(1)
                val upH   = innerH * (upperMax.toFloat() / totalRange.toFloat())
                val downH = innerH - upH
                val midY  = topG + upH

                val pxPerMinUp = if (upperMax > 0) upH / upperMax else 0f
                val pxPerMinDn = if (lowerMax > 0) downH / lowerMax else 0f

                val textX = size.width - with(density){ 8.dp.toPx() }

                // 24:00
                drawContext.canvas.nativeCanvas.apply {
                    val p = android.graphics.Paint().apply {
                        color = labelColor.toArgb()
                        textSize = with(density){ 12.sp.toPx() }
                        textAlign = android.graphics.Paint.Align.RIGHT
                        isAntiAlias = true
                    }
                    drawText("24:00", textX, midY + with(density){ 4.dp.toPx() }, p)
                }
                // 上半：01,03,...（每2小时）
                for (m in 120..upperMax step 120) {
                    val y = midY - m * pxPerMinUp
                    drawContext.canvas.nativeCanvas.apply {
                        val p = android.graphics.Paint().apply {
                            color = labelColor.toArgb()
                            textSize = with(density){ 10.sp.toPx() }
                            textAlign = android.graphics.Paint.Align.RIGHT
                            isAntiAlias = true
                        }
                        drawText("%d:00".format(m/60), textX, y + with(density){ 4.dp.toPx() }, p)
                    }
                }
                // 下半：23,21,...（每2小时）
                for (m in 120..lowerMax step 120) {
                    val y = midY + m * pxPerMinDn
                    val hour = (24 - (m / 60)) % 24
                    drawContext.canvas.nativeCanvas.apply {
                        val p = android.graphics.Paint().apply {
                            color = labelColor.toArgb()
                            textSize = with(density){ 10.sp.toPx() }
                            textAlign = android.graphics.Paint.Align.RIGHT
                            isAntiAlias = true
                        }
                        drawText("%d:00".format(hour), textX, y + with(density){ 4.dp.toPx() }, p)
                    }
                }
            }

            // ---------- 右侧：网格 + 柱 + 顶/底日期（可横向滚动） ----------
            Box(
                Modifier
                    .weight(1f)
                    .horizontalScroll(hScroll)
            ) {
                Canvas(
                    Modifier
                        .width(canvasWidthDp)
                        .fillMaxHeight()
                        .padding(outerPadPlot)
                ) {
                    val contentW = size.width
                    val contentH = size.height

                    val topG = with(density) { innerTopGutter.toPx() }
                    val botG = with(density) { innerBottomGutter.toPx() }
                    val innerH = contentH - topG - botG

                    val totalRange = (upperMax + lowerMax).coerceAtLeast(1)
                    val upH   = innerH * (upperMax.toFloat() / totalRange.toFloat())
                    val downH = innerH - upH
                    val midY  = topG + upH

                    val pxPerMinUp = if (upperMax > 0) upH / upperMax else 0f
                    val pxPerMinDn = if (lowerMax > 0) downH / lowerMax else 0f

                    // 中线与网格（每 2 小时）——仅画线，文字在左列
                    val gridStroke = 1.dp.toPx()
                    drawLine(axisColor, Offset(0f, midY), Offset(contentW, midY), gridStroke)
                    for (m in 120..upperMax step 120) {
                        val y = midY - m * pxPerMinUp
                        drawLine(axisColor.copy(alpha = 0.35f), Offset(0f, y), Offset(contentW, y), gridStroke)
                    }
                    for (m in 120..lowerMax step 120) {
                        val y = midY + m * pxPerMinDn
                        drawLine(axisColor.copy(alpha = 0.35f), Offset(0f, y), Offset(contentW, y), gridStroke)
                    }

                    // 从最左开始排
                    var x = 0f
                    days.forEachIndexed { idx, day ->
                        val bar = byDate[day]  // 可能为 null（无数据）

                        // 底部日期（当日）
                        val md = "${day.monthValue}/${day.dayOfMonth}"
                        drawContext.canvas.nativeCanvas.apply {
                            val p = android.graphics.Paint().apply {
                                color = labelColor.toArgb()
                                textSize = with(density){ 11.sp.toPx() }
                                textAlign = android.graphics.Paint.Align.CENTER
                                isAntiAlias = true
                            }
                            drawText(md, x + barW/2f, contentH - with(density){ 2.dp.toPx() }, p)
                        }

                        // 顶部日期（次日）
                        val next = day.plusDays(1)
                        val mdTop = "${next.monthValue}/${next.dayOfMonth}"
                        drawContext.canvas.nativeCanvas.apply {
                            val p = android.graphics.Paint().apply {
                                color = labelColor.toArgb()
                                textSize = with(density){ 11.sp.toPx() }
                                textAlign = android.graphics.Paint.Align.CENTER
                                isAntiAlias = true
                            }
                            drawText(mdTop, x + barW/2f, with(density){ (innerTopGutter / 2).toPx() }, p)
                        }

                        if (bar != null) {
                            // 上/下半像素高度
                            val dnMin = lowerBase(bar.bedMinutes).coerceIn(0, lowerMax)
                            val upMin = bar.wakeMinutes.coerceIn(0, upperMax)
                            val dnPix = dnMin * pxPerMinDn
                            val upPix = upMin * pxPerMinUp

                            // 上端柱子位置
                            val topY = if (upPix > 0f) midY - upPix else midY
                            // 下端柱子位置：如果有下半柱，显示在 midY + dnPix，否则保持在 midY
                            val bottomY = if (dnPix > 0f) midY + dnPix else midY

                            // 圆角柱
                            val height = upPix + dnPix
                            if (height > 0f) {
                                drawRoundRect(
                                    color = barColor,
                                    topLeft = Offset(x, topY),
                                    size = Size(barW, height),
                                    cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                                )
                            }

                            // 顶端时间（无上半时贴 24:00 线上方）
                            run {
                                val txt = "%d:%02d".format(bar.wakeMinutes/60, bar.wakeMinutes%60)
                                drawContext.canvas.nativeCanvas.apply {
                                    val p = android.graphics.Paint().apply {
                                        color = labelColor.toArgb()
                                        textSize = with(density){ 10.sp.toPx() }
                                        textAlign = android.graphics.Paint.Align.CENTER
                                        isAntiAlias = true
                                    }
                                    val yTopLabel = if (upPix > 0f) topY - with(density){ 6.dp.toPx() } else midY - with(density){ 6.dp.toPx() }
                                    drawText(txt, x + barW/2f, yTopLabel, p)
                                }
                            }
                            // 底端时间（无下半时贴 24:00 线下方）
                            run {
                                val txt = "%d:%02d".format(bar.bedMinutes/60, bar.bedMinutes%60)
                                drawContext.canvas.nativeCanvas.apply {
                                    val p = android.graphics.Paint().apply {
                                        color = labelColor.toArgb()
                                        textSize = with(density){ 10.sp.toPx() }
                                        textAlign = android.graphics.Paint.Align.CENTER
                                        isAntiAlias = true
                                    }
                                    val yBottomLabel = if (dnPix > 0f) (midY + dnPix) + with(density){ 12.dp.toPx() } else midY + with(density){ 12.dp.toPx() }
                                    drawText(txt, x + barW/2f, yBottomLabel, p)
                                }
                            }
                        }

                        // 下一天的起始 X（步距 = 柱宽 + 间距）
                        x += (barW + gapPx)
                    }
                }
            }
        }
    }
}




// 24h 格式 "HH:mm"（0..1439）
private fun formatClockHHMM(totalMin: Int): String {
    val m = ((totalMin % 1440) + 1440) % 1440
    val h = m / 60
    val mm = m % 60
    return "%d:%02d".format(h, mm)
}

@Composable
private fun SleepHistoryList(
    list: List<Remote.SleepSession>,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val zone = ZoneId.systemDefault()
    fun fmtHm(min: Int): String = "%d:%02d".format(min / 60, min % 60)
    fun minutesToText(m: Int): String = when {
        m < 60  -> "${m}分"
        m % 60 == 0 -> "${m / 60}小时"
        else -> "${m / 60}小时${m % 60}分"
    }

    // 用 LazyColumn 实现上下滚动
    LazyColumn(
        modifier = modifier.fillMaxWidth()
    ) {
        items(list, key = { it.id }) { s ->
            val start = s.startTs.toDate().toInstant().atZone(zone).toLocalDateTime()
            val end   = s.endTs.toDate().toInstant().atZone(zone).toLocalDateTime()
            val date  = start.toLocalDate().toString() // YYYY-MM-DD
            val startHm = fmtHm(start.hour * 60 + start.minute)
            val endHm   = fmtHm(end.hour * 60 + end.minute)

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(date, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "$startHm - $endHm · ${minutesToText(s.durationMin)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { onDelete(s.id) }) {
                    Icon(Icons.Default.Close, contentDescription = "删除")
                }
            }
            Divider()
        }
    }
}

@Composable
private fun SleepDurationBarChart(
    sessions: List<Remote.SleepSession>,
    startDate: LocalDate,
    endDate: LocalDate,
    modifier: Modifier = Modifier,
    maxDaysOnScreen: Int = 10,
    barWidthFraction: Float = 0.60f
) {
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val axisColor  = MaterialTheme.colorScheme.outlineVariant
    val barColor   = MaterialTheme.colorScheme.primary

    val density    = LocalDensity.current
    val zone       = ZoneId.systemDefault()
    val layoutDir  = LocalLayoutDirection.current

    // 完整日期序列
    val days = remember(startDate, endDate) {
        buildList {
            var d = startDate
            while (!d.isAfter(endDate)) { add(d); d = d.plusDays(1) }
        }
    }
    if (days.isEmpty()) {
        Box(modifier) { /* 空数据占位 */ }
        return
    }

    // 计算每天 0:00~24:00 内的睡眠总分钟（剪裁会话到日窗口）
    val dayIndex = remember(days) { days.withIndex().associate { it.value to it.index } }
    val totals = IntArray(days.size) { 0 }.also { arr ->
        sessions.forEach { s ->
            val sStart = s.startTs.toDate().toInstant()
            val sEnd   = s.endTs.toDate().toInstant()

            var d = sStart.atZone(zone).toLocalDate()
            val endD = sEnd.atZone(zone).toLocalDate()
            while (!d.isAfter(endD)) {
                val dayStart = d.atStartOfDay(zone).toInstant()
                val dayEnd = d.plusDays(1).atStartOfDay(zone).toInstant()

                val overlapStart = maxOf(sStart, dayStart)
                val overlapEnd = minOf(sEnd, dayEnd)
                val minutes = Duration.between(overlapStart, overlapEnd).toMinutes().toInt()

                // 确保每一天的数据是累加的
                dayIndex[d]?.let { idx -> arr[idx] = (arr[idx] + minutes).coerceAtLeast(0) }
                d = d.plusDays(1)
            }
        }
    }

    // 纵向范围：取区间内最大值 + 1小时缓冲，封顶 24h
    val maxMin = totals.maxOrNull() ?: 0
    val yMax   = min(1440, max(maxMin + 60, 60)) // 至少 1h

    // 外边距（这里不需要左侧固定刻度列）
    val outerPad = PaddingValues(start = 0.dp, end = 12.dp, top = 16.dp, bottom = 28.dp)

    // 横向滚动，默认滑到最右
    val hScroll = rememberScrollState()
    LaunchedEffect(days.size) {
        snapshotFlow { hScroll.maxValue }.filter { it > 0 }.first()
        hScroll.scrollTo(hScroll.maxValue)
    }

    // —— 关键：同屏 N 天 -> 动态算柱宽/间距（与你当前 SleepChart 逻辑保持一致） ——
    BoxWithConstraints(modifier) {
        val viewportPx = with(density) {
            (maxWidth
                    - outerPad.calculateStartPadding(layoutDir)
                    - outerPad.calculateEndPadding(layoutDir)).toPx()
        }.coerceAtLeast(1f)

        val desiredDays = maxDaysOnScreen.coerceAtLeast(1)
        val stridePxRaw = viewportPx / desiredDays
        val minBarPx    = with(density) { 6.dp.toPx() }
        val minGapPx    = with(density) { 3.dp.toPx() }

        var barW  = (stridePxRaw * barWidthFraction).coerceAtLeast(minBarPx)
        var gapPx = (stridePxRaw - barW).coerceAtLeast(minGapPx)
        val strideNow = barW + gapPx
        if (strideNow != stridePxRaw) {
            barW += (stridePxRaw - strideNow)
            if (barW < minBarPx) barW = minBarPx
        }

        val totalW = ((days.size - 1) * (barW + gapPx) + barW).coerceAtLeast(0f)
        val canvasWidthDp = with(density) { totalW.toDp() } + 24.dp

        Box(
            Modifier
                .fillMaxSize()
                .horizontalScroll(hScroll)
        ) {
            Canvas(
                Modifier
                    .width(canvasWidthDp)
                    .fillMaxHeight()
                    .padding(outerPad)
            ) {
                val contentW = size.width
                val contentH = size.height

                val topG = with(density) { 16.dp.toPx() }
                val botG = with(density) { 18.dp.toPx() }
                val innerH = contentH - topG - botG

                val pxPerMin = if (yMax > 0) innerH / yMax else 0f

                // 可选：一条底线
                drawLine(axisColor, Offset(0f, contentH - botG), Offset(contentW, contentH - botG), 1.dp.toPx())

                // 从最左开始画
                var x = 0f
                days.forEachIndexed { idx, day ->
                    val minutes = totals[idx].coerceAtLeast(0)
                    val hPix = minutes * pxPerMin
                    val topY = (contentH - botG) - hPix

                    // 底部日期
                    val md = "${day.monthValue}/${day.dayOfMonth}"
                    drawContext.canvas.nativeCanvas.apply {
                        val p = android.graphics.Paint().apply {
                            color = labelColor.toArgb()
                            textSize = with(density){ 11.sp.toPx() }
                            textAlign = android.graphics.Paint.Align.CENTER
                            isAntiAlias = true
                        }
                        drawText(md, x + barW/2f, contentH - with(density){ 2.dp.toPx() }, p)
                    }

                    // 柱子：底边贴基线为直角，顶端圆角 6.dp
                    if (hPix > 0f) {
                        val r = with(density) { 4.dp.toPx() }                  // ← 圆角半径
                        val bottom = contentH - botG                           // 基线（0:00-24:00 底线）
                        val rr = androidx.compose.ui.geometry.RoundRect(       // 每个角独立半径
                            left = x,
                            top = topY,
                            right = x + barW,
                            bottom = bottom,
                            topLeftCornerRadius = CornerRadius(r, r),
                            topRightCornerRadius = CornerRadius(r, r),
                            bottomRightCornerRadius = CornerRadius(0f, 0f),
                            bottomLeftCornerRadius = CornerRadius(0f, 0f)
                        )
                        val path = Path().apply { addRoundRect(rr) }
                        drawPath(path, barColor)
                    }

                    // 顶部显示“小时数（小数一位）”，例如 7.5
                    val hoursTxt = String.format("%.1f", minutes / 60f)
                    drawContext.canvas.nativeCanvas.apply {
                        val p = android.graphics.Paint().apply {
                            color = labelColor.toArgb()
                            textSize = with(density){ 10.sp.toPx() }
                            textAlign = android.graphics.Paint.Align.CENTER
                            isAntiAlias = true
                        }
                        val labelY = topY - with(density){ 6.dp.toPx() }
                        drawText(hoursTxt, x + barW/2f, labelY, p)
                    }

                    x += (barW + gapPx)
                }
            }
        }
    }
}
