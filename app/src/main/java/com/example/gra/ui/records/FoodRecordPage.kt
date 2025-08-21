package com.example.gra.ui.records

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import androidx.navigation.NavController
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import android.app.DatePickerDialog
import android.content.Context
import android.util.Log
import androidx.compose.material.icons.outlined.Star

import androidx.compose.ui.platform.LocalContext
import com.example.gra.ui.viewmodel.FoodViewModel
import com.example.gra.ui.viewmodel.SelectedFood
import com.google.firebase.auth.FirebaseAuth
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.compose.material.icons.filled.Star
import android.widget.Toast
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close

import com.example.gra.ui.data.FoodEntity


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodRecordPage(
    navController: NavHostController,
    foodViewModel: FoodViewModel = viewModel()
) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    LaunchedEffect(uid) {
        if (!uid.isNullOrBlank()) {
            foodViewModel.startFavoritesListener(uid)
        }
    }
    val favIds by foodViewModel.favoriteIds.collectAsState()

    val dbCategories by foodViewModel.categories.collectAsState()
    val categories = remember(dbCategories) { listOf("收藏", "全部") + dbCategories }

// 默认选中“全部”
    var selectedCategory by remember { mutableStateOf("全部") }

// 告诉 VM 当前模式
    LaunchedEffect(selectedCategory) {
        foodViewModel.setModeByLabel(selectedCategory)
    }

// 搜索词（保留你原逻辑，但要把值同步给 VM）
    var searchQuery by remember { mutableStateOf("") }
    LaunchedEffect(searchQuery) { foodViewModel.updateSearch(searchQuery) }

// 右侧列表的数据源（分页）
    val pagingItems = foodViewModel.foodsPaged.collectAsLazyPagingItems()

    val sheetState = rememberModalBottomSheetState()
    val coroutineScope = rememberCoroutineScope()
    var showInputSheet by remember { mutableStateOf(false) }

    val showSelectedSheet = remember { mutableStateOf(false) }
    val context = LocalContext.current
    val today = LocalDate.now().toString()

    val viewModel: FoodViewModel = viewModel()

    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var mealIndex by remember { mutableStateOf(1) }

    var selectedFood by remember { mutableStateOf<FoodEntity?>(null) }

    val auth = remember { FirebaseAuth.getInstance() }
    val userId by produceState(initialValue = auth.currentUser?.uid.orEmpty()) {
        val listener = FirebaseAuth.AuthStateListener { a -> value = a.currentUser?.uid.orEmpty() }
        auth.addAuthStateListener(listener)
        awaitDispose { auth.removeAuthStateListener(listener) }
    }

    val selectedFoods by foodViewModel.selectedItemsFlow.collectAsState()
    val totalFoodKcal = selectedFoods.sumOf { it.kcal }

// 👇 页面一进来就自动获取今天的第几餐
    LaunchedEffect(Unit) {
        viewModel.getTodayMealIndex(userId, today) {
            mealIndex = it
        }
    }

    Scaffold(
        topBar = {
            FoodRecordTopBar(
                navController = navController,
                mealIndex = mealIndex,
                selectedDate = selectedDate,
                onDateSelected = { newDate -> selectedDate = newDate },
                context = context
            )
        },
        bottomBar = {
            var isSaving by remember { mutableStateOf(false) }
            val context = LocalContext.current

            FoodRecordBottomBar(
                totalItems = selectedFoods.size,
                totalKcal = totalFoodKcal,
                onShowSelectedItems = { showSelectedSheet.value = true },
                onFinish = {
                    if (isSaving) return@FoodRecordBottomBar
                    val userId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

                    when {
                        userId.isBlank() -> {
                            Toast.makeText(context, "请先登录后再保存", Toast.LENGTH_SHORT).show()
                            return@FoodRecordBottomBar
                        }
                        foodViewModel.totalItemsCount() == 0 -> {
                            Toast.makeText(context, "还没有选择任何食物", Toast.LENGTH_SHORT).show()
                            return@FoodRecordBottomBar
                        }
                    }

                    isSaving = true
                    foodViewModel.saveMeal(
                        userId = userId,
                        date = selectedDate.toString(),
                        mealIndex = mealIndex,
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
                label = { Text("搜索食物名称") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            Row(Modifier.fillMaxSize()) {
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

                LazyColumn(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .padding(16.dp)
                ) {
                    // ✅ 用 foundation 的 items(count, key)。key 用 peek(index)?.id，避免触发加载
                    items(
                        count = pagingItems.itemCount,
                        key = { index -> pagingItems.peek(index)?.id ?: "placeholder-$index" }
                    ) { index ->
                        val food = pagingItems[index] ?: return@items
                        val fav = favIds.contains(food.id)

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
                                // 星标
                                IconButton(onClick = {
                                    if (userId.isBlank()) {
                                        Toast.makeText(context, "请先登录后再收藏", Toast.LENGTH_SHORT).show()
                                        return@IconButton
                                    }
                                    foodViewModel.toggleFavorite(
                                        userId = userId,
                                        food = food,
                                        onSuccess = { added ->
                                            // ✅ 成功提示
                                            Toast.makeText(
                                                context,
                                                if (added) "已加入收藏" else "已取消收藏",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        },
                                        onError = { e ->
                                            Log.e("Favorites", "toggle failed", e)
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
                                    Text(food.name)
                                    Text(
                                        "约 ${"%.0f".format(food.kcal100g ?: 0.0)} kcal/100g",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                IconButton(onClick = {
                                    selectedFood = food
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

    if (showInputSheet && selectedFood != null) {
        ModalBottomSheet(
            onDismissRequest = { showInputSheet = false },
            sheetState = sheetState
        ) {
            BottomSheetContent(
                food = selectedFood!!,
                onSave = { grams ->
                    foodViewModel.addFood(selectedFood!!, grams)  // ✅ 用真实 kcal
                    showInputSheet = false
                }
            )
        }
    }

    // 打开“已选列表”：
    if (showSelectedSheet.value) {
        ModalBottomSheet(onDismissRequest = { showSelectedSheet.value = false }) {
            SelectedItemsSheet(
                selectedItems = selectedFoods,                 // ✅ 用收集到的列表
                totalKcal = totalFoodKcal,                    // ✅ 用实时合计
                onRemove = { idx -> foodViewModel.removeSelectedAt(idx) }  // ✅ 即时删除
            )
        }
    }
}

@Composable
fun BottomSheetContent(
    food: FoodEntity,               // ✅ 改成直接拿 FoodEntity，里面有 kcal100g
    onSave: (Double) -> Unit        // 仍然传 grams 回去
) {
    var gramsText by remember { mutableStateOf("") }
    val per100 = food.kcal100g ?: 0.0

    // 实时计算：四舍五入为 Int（你之前的 SelectedFood.kcal 是 Int）
    val addKcal by remember(gramsText, per100) {
        mutableStateOf(
            runCatching {
                val g = gramsText.toDouble()
                ((per100 * g) / 100.0).toInt()
            }.getOrDefault(0)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text("添加 ${food.name}", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        // 展示营养信息
        fun fmt(x: Double?, unit: String) =
            if (x == null) "-" else "${"%.1f".format(x)} $unit"
        Text("에너지: ${fmt(food.kcal100g, "kcal/100g")}")
        Text("단백질: ${fmt(food.protein100g, "g/100g")}")
        Text("지방: ${fmt(food.fat100g, "g/100g")}")
        Text("탄수화물: ${fmt(food.carb100g, "g/100g")}")

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = gramsText,
            onValueChange = { gramsText = it },
            label = { Text("请输入克数") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                val g = gramsText.toDoubleOrNull() ?: 0.0
                onSave(g)
            },
            enabled = addKcal > 0,                                  // ✅ 没有有效输入时禁用
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (addKcal > 0) "添加 ${addKcal} kcal" else "添加")
        }
    }
}




@Composable
fun SelectedItemsSheet(
    selectedItems: List<SelectedFood>,
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
                // 左侧删除按钮
                IconButton(onClick = { onRemove(index) }) {
                    Icon(
                        imageVector = Icons.Default.Close, // 或 Icons.Default.RemoveCircle
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error
                    )
                }

                // 名称 + 数值
                Column(Modifier.weight(1f)) {
                    Text(item.name)
                    Text("${item.grams} g   ${item.kcal} kcal", style = MaterialTheme.typography.bodySmall)
                }
            }
            Divider()
        }
    }
}


@Composable
fun FoodRecordBottomBar(
    totalItems: Int,
    totalKcal: Int,
    onShowSelectedItems: () -> Unit,
    onFinish: () -> Unit
) {
    Surface(
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：已选数量
            Text(
                text = "$totalItems 项",
                modifier = Modifier
                    .clickable { onShowSelectedItems() }
                    .padding(end = 16.dp)
            )

            // 中间：总 kcal
            Text(
                text = "共计 $totalKcal kcal",
                modifier = Modifier.weight(1f)
            )

            // 右侧：完成按钮
            Button(onClick = { onFinish() }) {
                Text("完成")
            }
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodRecordTopBar(
    navController: NavController,
    mealIndex: Int,
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
                if (!newDate.isAfter(today)) {
                    onDateSelected(newDate)
                }
                openDatePicker = false
            },
            selectedDate.year,
            selectedDate.monthValue - 1,
            selectedDate.dayOfMonth
        )
        // 限制最大可选日期
        datePicker.datePicker.maxDate = System.currentTimeMillis()
        datePicker.show()
    }

    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${selectedDate.format(DateTimeFormatter.ofPattern("M月d日"))}",
                    modifier = Modifier.clickable { openDatePicker = true }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "第${mealIndex}餐")
            }
        },
        navigationIcon = {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "返回")
            }
        }
    )
}