package com.example.gra.ui.records

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import android.app.DatePickerDialog
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.gra.ui.data.AppDatabase
import com.example.gra.ui.viewmodel.ExerciseViewModel
import com.example.gra.ui.data.ExerciseEntity
import com.example.gra.ui.data.prepopulateExercisesIfEmpty
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseRecordPage(
    navController: NavHostController,
    viewModel: ExerciseViewModel = viewModel()
) {
    val context = LocalContext.current

    // ✅ 保底：页面第一次进入时确保本地表非空（仅空表时导入）
    LaunchedEffect(Unit) {
        val db = AppDatabase.getInstance(context)
        val before = db.exerciseDao().count()
        Log.i("ExSeedCheck", "exercises count(before) = $before")
        if (before == 0) {
            try {
                prepopulateExercisesIfEmpty(context, db)   // 你之前已有的导入函数
                val after = db.exerciseDao().count()
                Log.i("ExSeedCheck", "exercises count(after) = $after")
                Toast.makeText(context, "运动库已导入 $after 条", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("ExSeedCheck", "导入失败", e)
                Toast.makeText(context, "运动库导入失败：${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // —— 收藏监听（进入时即启动）——
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    LaunchedEffect(uid) {
        if (!uid.isNullOrBlank()) viewModel.startFavoritesListener(uid)
    }
    val favIds by viewModel.favoriteIds.collectAsState()

    // —— 分类与搜索 ——
    val dbCategories by viewModel.categories.collectAsState()
    val categories = remember(dbCategories) { listOf("收藏", "全部", "自定义") + dbCategories }

    var selectedCategory by remember { mutableStateOf("全部") }
    LaunchedEffect(selectedCategory) { viewModel.setModeByLabel(selectedCategory) }

    var searchQuery by remember { mutableStateOf("") }
    LaunchedEffect(searchQuery) { viewModel.updateSearch(searchQuery) }

    // —— 列表数据（分页）——
    val pagingItems = viewModel.exercisesPaged.collectAsLazyPagingItems()

    // —— 其它 UI 状态 ——
    val sheetState = rememberModalBottomSheetState()
    val coroutineScope = rememberCoroutineScope()
    var showInputSheet by remember { mutableStateOf(false) }
    var showCustomSheet by remember { mutableStateOf(false) }
    var selectedExercise by remember { mutableStateOf<ExerciseEntity?>(null) }
    var showSelectedSheet by remember { mutableStateOf(false) }

    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    val selectedExercises by viewModel.selectedItemsFlow.collectAsState()
    val totalExKcal = selectedExercises.sumOf { it.kcal }

    Scaffold(
        topBar = {
            ExerciseTopBar(
                navController = navController,
                selectedDate = selectedDate,
                onDateSelected = { newDate -> selectedDate = newDate },
                context = context
            )
        },
        bottomBar = {
            // 复用你的 FoodRecordBottomBar：文案“共计 X kcal”保持一致
            var isSaving by remember { mutableStateOf(false) }
            val userId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

            FoodRecordBottomBar(
                totalItems = selectedExercises.size,
                totalKcal = totalExKcal,
                onShowSelectedItems = { showSelectedSheet = true },
                onFinish = {
                    if (isSaving) return@FoodRecordBottomBar
                    when {
                        userId.isBlank() -> {
                            Toast.makeText(context, "请先登录后再保存", Toast.LENGTH_SHORT).show()
                            return@FoodRecordBottomBar
                        }
                        viewModel.totalItemsCount() == 0 -> {
                            Toast.makeText(context, "还没有添加任何运动", Toast.LENGTH_SHORT).show()
                            return@FoodRecordBottomBar
                        }
                    }
                    isSaving = true
                    viewModel.saveBurn(
                        userId = userId,
                        date = selectedDate.toString(),
                        onComplete = {
                            isSaving = false
                            Toast.makeText(context, "已保存到云端", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        },
                        onError = { e ->
                            isSaving = false
                            Toast.makeText(context, "保存失败: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("搜索运动名称") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            Row(Modifier.fillMaxSize()) {
                // —— 左侧分类 ——
                Column(
                    modifier = Modifier
                        .width(80.dp)
                        .fillMaxHeight()
                        .background(Color.LightGray)
                ) {
                    categories.forEach { category ->
                        Text(
                            text = category,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                                .clickable { selectedCategory = category },
                            color = if (selectedCategory == category) Color.Blue else Color.Black
                        )
                    }
                }

                // —— 右侧列表 ——
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .padding(16.dp)
                ) {
                    // 选中“自定义”时露出新增按钮
                    if (selectedCategory == "自定义") {
                        Button(
                            onClick = { showCustomSheet = true },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("新增自定义运动") }
                        Spacer(Modifier.height(12.dp))
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // 用 foundation 的 items(count, key)；key 用 peek(index)?.id，避免触发加载
                        items(
                            count = pagingItems.itemCount,
                            key = { index -> pagingItems.peek(index)?.id ?: "placeholder-$index" }
                        ) { index ->
                            val ex = pagingItems[index] ?: return@items
                            val fav = favIds.contains(ex.id)

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 星标收藏
                                    IconButton(onClick = {
                                        val userId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
                                        if (userId.isBlank()) {
                                            Toast.makeText(context, "请先登录后再收藏", Toast.LENGTH_SHORT).show()
                                            return@IconButton
                                        }
                                        viewModel.toggleFavorite(
                                            userId = userId,
                                            ex = ex,
                                            onSuccess = { added ->
                                                Toast.makeText(
                                                    context,
                                                    if (added) "已加入收藏" else "已取消收藏",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            },
                                            onError = { e ->
                                                Log.e("ExFavorites", "toggle failed", e)
                                                Toast.makeText(
                                                    context,
                                                    "收藏失败：${e.localizedMessage}",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        )
                                    }) {
                                        Icon(
                                            imageVector = if (fav) Icons.Filled.Star else Icons.Outlined.Star,
                                            contentDescription = if (fav) "取消收藏" else "加入收藏",
                                            tint = if (fav) MaterialTheme.colorScheme.primary else Color.Gray
                                        )
                                    }

                                    Spacer(Modifier.width(12.dp))

                                    Column(Modifier.weight(1f)) {
                                        Text(ex.name)
                                        Text(
                                            "MET ${"%.1f".format(ex.met)}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }

                                    IconButton(onClick = {
                                        selectedExercise = ex
                                        coroutineScope.launch { sheetState.show() }
                                        showInputSheet = true
                                    }) {
                                        Icon(Icons.Default.Add, contentDescription = "添加")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // —— 选中数据库运动：用 MET 计算 ——
    if (showInputSheet && selectedExercise != null) {
        ModalBottomSheet(
            onDismissRequest = { showInputSheet = false },
            sheetState = sheetState
        ) {
            ExerciseBottomSheet(
                ex = selectedExercise!!,
                onSave = { minutes, weightKg ->
                    viewModel.addExercise(selectedExercise!!, minutes, weightKg)
                    showInputSheet = false
                }
            )
        }
    }

    // —— 自定义运动：直接填名称/时间/卡路里 ——
    if (showCustomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCustomSheet = false }
        ) {
            CustomExerciseBottomSheet(
                onSave = { name, minutes, kcal ->
                    viewModel.addCustomExercise(name, minutes, kcal)
                    showCustomSheet = false
                }
            )
        }
    }

    // —— 已选运动列表：BottomSheet ——
    if (showSelectedSheet) {
        ModalBottomSheet(onDismissRequest = { showSelectedSheet = false }) {
            SelectedExercisesSheet(
                selectedItems = selectedExercises,
                totalKcal = totalExKcal,
                onRemove = { idx -> viewModel.removeSelectedAt(idx) }
            )
        }
    }
}

/** 顶部栏：保持你的日期选择 UI，只把右侧文案改为“运动” */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseTopBar(
    navController: NavController,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    context: Context
) {
    var openDatePicker by remember { mutableStateOf(false) }

    if (openDatePicker) {
        val today = LocalDate.now()
        val datePicker = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newDate = LocalDate.of(year, month + 1, dayOfMonth)
                if (!newDate.isAfter(today)) onDateSelected(newDate)
                openDatePicker = false
            },
            selectedDate.year,
            selectedDate.monthValue - 1,
            selectedDate.dayOfMonth
        )
        datePicker.datePicker.maxDate = System.currentTimeMillis()
        datePicker.show()
    }

    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = selectedDate.format(DateTimeFormatter.ofPattern("M月d日")),
                    modifier = Modifier.clickable { openDatePicker = true }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "运动")
            }
        },
        navigationIcon = {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "返回")
            }
        }
    )
}

@Composable
fun ExerciseBottomSheet(
    ex: ExerciseEntity,
    defaultWeightKg: Double = 60.0,
    onSave: (minutes: Int, weightKg: Double) -> Unit
) {
    var minutesText by remember { mutableStateOf("") }
    var weightText by remember { mutableStateOf(defaultWeightKg.toString()) }

    val addKcal by remember(minutesText, weightText, ex.met) {
        mutableStateOf(
            runCatching {
                val m = minutesText.toInt()
                val w = weightText.toDouble()
                ((ex.met * 3.5 * w * m) / 200.0).toInt()
            }.getOrDefault(0)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text("添加 ${ex.name}", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text("MET: ${"%.1f".format(ex.met)}")
        Text(
            "kcal = MET × 3.5 × 体重(kg) × 分钟 ÷ 200",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = minutesText,
            onValueChange = { minutesText = it },
            label = { Text("时长（分钟）") },
            keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = weightText,
            onValueChange = { weightText = it },
            label = { Text("体重（kg）") },
            keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                val m = minutesText.toIntOrNull() ?: 0
                val w = weightText.toDoubleOrNull() ?: defaultWeightKg
                onSave(m, w)
            },
            enabled = addKcal > 0,                                  // ✅ 没有有效输入时禁用
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (addKcal > 0) "添加 ${addKcal} kcal" else "添加")
        }
    }
}


/** 自定义运动的弹窗：直接填名称/分钟/kcal（和 Firestore 记录字段一致） */
@Composable
fun CustomExerciseBottomSheet(
    onSave: (name: String, minutes: Int, kcal: Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var minutes by remember { mutableStateOf("") }
    var kcal by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text("新增自定义运动", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = name, onValueChange = { name = it },
            label = { Text("名称") }, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = minutes, onValueChange = { minutes = it },
            label = { Text("时长（分钟）") },
            keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = kcal, onValueChange = { kcal = it },
            label = { Text("卡路里(kcal)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                val m = minutes.toIntOrNull() ?: 0
                val k = kcal.toIntOrNull() ?: 0
                onSave(name.ifBlank { "사용자 정의 운동" }, m, k)
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("保存") }
    }
}

@Composable
fun SelectedExercisesSheet(
    selectedItems: List<ExerciseViewModel.SelectedExercise>,
    totalKcal: Int,
    onRemove: (Int) -> Unit    // ✅ 新增
) {
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Text("已选 ${totalKcal} kcal", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        selectedItems.forEachIndexed { index, item ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onRemove(index) }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(item.name)
                    Text("${item.minutes} 분   ${item.kcal} kcal", style = MaterialTheme.typography.bodySmall)
                }
            }
            Divider()
        }
    }
}


