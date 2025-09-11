package com.example.gra.ui


import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.gra.ui.data.Remote
import com.example.gra.ui.viewmodel.BodyMeasureViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import com.example.gra.R
import com.example.gra.ui.theme.AppPalette
import com.example.gra.ui.theme.AppThemeState
import com.example.gra.ui.theme.Palettes
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MinePage(
    navController: NavController,
) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val accountName = currentUser?.email ?: "Guest" // 默认显示 "Guest" 如果用户未登录

    val ctx = LocalContext.current

    // === 新增：监听 uniqueId ===
    val uid = currentUser?.uid.orEmpty()
    val remote = remember { Remote.create() }
    var uniqueId by remember { mutableStateOf<String?>(null) }

    var avatarUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uid) {
        if (uid.isBlank()) return@LaunchedEffect
        remote.observeUserProfile(uid).collectLatest { p ->
            uniqueId = p?.uniqueId
            avatarUrl = p?.avatarUrl   // ← 你的 profile 模型里新增/已有该字段
            android.util.Log.d("Mine", "profile update: uniqueId=${p?.uniqueId}, avatarUrl=${p?.avatarUrl}")
        }
    }


    LaunchedEffect(uid) {
        if (uid.isBlank()) return@LaunchedEffect
        remote.observeUserProfile(uid).collectLatest { p ->
            uniqueId = p?.uniqueId
            android.util.Log.d("Mine", "profile update: uniqueId=${p?.uniqueId}")
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "Setting") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "返回")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        },
        content = { paddingValues ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(colors = listOf(topBlue, bottomGreen))
                        )
                        .padding(32.dp)
                        // ✅ 让整页能上下滚
                        .verticalScroll(rememberScrollState()),
                    //horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AvatarHeader(
                        uniqueId = uniqueId,
                        uid = uid,
                        avatarUrl = avatarUrl,
                        onAvatarChanged = { newUrl ->
                            avatarUrl = newUrl
                        }
                    )


                    Spacer(Modifier.height(32.dp))

                    Text(
                        "  User Details",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(16.dp))
                    PersonalHealthCard(navController = navController)
                    // 内部展开再多，也能跟随页面滚动

                    Spacer(Modifier.height(32.dp))

                    Text(
                        "  Help Center",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(16.dp))
                    // 顶层 Help Center 下面的 Card：用这段替换原先固定高度且空内容的 Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(12.dp)) {
// Help Center 下的 Card 里：
                            var accountExpanded by remember { mutableStateOf(false) }
                            var editingUid by remember { mutableStateOf(false) }
                            // 行 1：账号信息（TODO）
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("账号信息", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.weight(1f))
                                IconButton(onClick = { accountExpanded = !accountExpanded }) {
                                    Icon(
                                        imageVector = Icons.Outlined.KeyboardArrowDown,
                                        contentDescription = null,
                                        modifier = Modifier.rotate(if (accountExpanded) 180f else 0f),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }


                            AnimatedVisibility(visible = accountExpanded) {
                                Column(Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // email
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Email：", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(accountName, style = MaterialTheme.typography.bodyMedium)
                                    }
                                    // UID + 编辑
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("UID：", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(uniqueId ?: "未设置", style = MaterialTheme.typography.bodyMedium)
                                        Spacer(Modifier.width(8.dp))
                                        IconButton(onClick = { editingUid = !editingUid }) {
                                            Icon(Icons.Default.Edit, contentDescription = "修改 UID", tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }

                                    AnimatedVisibility(visible = editingUid) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            InlineUniqueIdEditor(
                                                current = uniqueId,
                                                onSaved = {
                                                    editingUid = false
                                                    Toast.makeText(ctx, "UID 已更新：$it", Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Divider(Modifier.padding(vertical = 8.dp))

                            // 行 2：退出账号（保留原逻辑）
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ExitToApp,
                                    contentDescription = "退出账号",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "退出账号",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
                                )
                                Spacer(Modifier.weight(1f))
                                IconButton(
                                    onClick = {
                                        FirebaseAuth.getInstance().signOut()
                                        navController.navigate("login") {
                                            popUpTo("my") { inclusive = true }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ExitToApp,
                                        contentDescription = "退出账号",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
                                    )
                                }
                            }
                        }
                    }

                }
            }
        }
    )
}

// 轻量版的“唯一ID编辑器”，只负责当前页内联编辑
@Composable
private fun InlineUniqueIdEditor(
    current: String?,
    onSaved: (String) -> Unit = {}
) {
    val ctx   = LocalContext.current
    val uid   = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    val remote = remember { Remote.create() }
    val scope  = rememberCoroutineScope()

    var text       by remember { mutableStateOf(current.orEmpty()) }
    var checking   by remember { mutableStateOf(false) }
    var available  by remember { mutableStateOf<Boolean?>(null) }
    var error      by remember { mutableStateOf<String?>(null) }
    var saving     by remember { mutableStateOf(false) }

    // 输入防抖 + 唯一性检查（沿用你现有逻辑）
    LaunchedEffect(text) {
        available = null; error = null
        val t = text.trim()
        if (t.isEmpty() || t.equals(current ?: "", ignoreCase = true)) return@LaunchedEffect
        checking = true
        delay(500)
        try {
            val ok = remote.isUniqueIdAvailable(t)
            available = ok
        } catch (e: Exception) {
            error = e.localizedMessage
        } finally {
            checking = false
        }
    }

    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        singleLine = true,
        label = { Text("唯一ID") },
        supportingText = {
            when {
                checking -> Text("检查中…")
                error != null -> Text("检查失败：$error")
                text.equals(current ?: "", ignoreCase = true) -> Text("与当前一致")
                available == true -> Text("✅ 可用")
                available == false -> Text("❌ 已被占用")
                else -> {}
            }
        },
        isError = (available == false),
        modifier = Modifier.fillMaxWidth()
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedButton(
            onClick = { text = current.orEmpty() },
            enabled = !saving,
            modifier = Modifier.weight(1f)
        ) { Text("重置") }

        Button(
            onClick = {
                if (uid.isBlank()) {
                    Toast.makeText(ctx, "未登录，无法保存", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                scope.launch {
                    saving = true
                    try {
                        val ok = remote.setUniqueId(uid, text)
                        if (ok) {
                            Toast.makeText(ctx, "已更新：$text", Toast.LENGTH_SHORT).show()
                            onSaved(text) // 让上层回调 & 收起
                        } else {
                            Toast.makeText(ctx, "保存失败，请稍后重试", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(ctx, "保存失败：${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    } finally {
                        saving = false
                    }
                }
            },
            enabled = !saving && (text.trim().isNotEmpty()) && (available != false),
            modifier = Modifier.weight(1f)
        ) {
            if (saving) {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(if (saving) "保存中…" else "保存")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalHealthCard(
    navController: NavController,
    vm: BodyMeasureViewModel = viewModel()
){
    val ctx = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // 订阅档案（保存成功后会自动回流刷新这里）
    val h by vm.health.collectAsState(null)   // 来自 ViewModel 的 observeHealthProfile() 结果  :contentReference[oaicite:3]{index=3}

    // ----- 本地编辑态 -----
    var sex by remember { mutableStateOf(h?.sex ?: "male") }
    var height by remember { mutableStateOf(h?.heightCm?.takeIf { it > 0 }?.toString() ?: "") }
    var age by remember { mutableStateOf(h?.age?.takeIf { it > 0 }?.toString() ?: "") }


    // ----- 展开 & 箭头动画 -----
    var infoExpanded by remember { mutableStateOf(false) }
    var planExpanded by remember { mutableStateOf(false) }
    val infoArrow by animateFloatAsState(if (infoExpanded) 180f else 0f, label = "infoArrow")
    val planArrow by animateFloatAsState(if (planExpanded) 180f else 0f, label = "planArrow")

    // ----- 计划目标 -----
    val tdee = h?.tdee ?: 0
    val recoMaintain = h?.recoMaintain ?: tdee
    val recoCut = h?.recoCut ?: (tdee * 85 / 100)
    val recoBulk = (tdee * 110 / 100)

    val intakeOptions = listOf("维持", "减脂", "增肌", "自定义")
    var intakeSel by remember { mutableStateOf(intakeOptions.first()) }
    var customIntake by remember { mutableStateOf("") }
    val intakeTarget by remember(intakeSel, customIntake, recoMaintain, recoCut, recoBulk) {
        mutableStateOf(when (intakeSel) {
            "维持" -> recoMaintain
            "减脂" -> recoCut
            "增肌" -> recoBulk
            else -> customIntake.toIntOrNull()?.coerceAtLeast(0) ?: 0
        })
    }

    val workoutOptions = listOf("轻量活动", "中等活动", "高量活动", "自定义")
    var workoutSel by remember { mutableStateOf(workoutOptions.first()) }
    var customWorkoutBurn by remember { mutableStateOf("") }
    fun suggestBurnByWorkout(t: Int, sel: String) = when {
        sel.startsWith("轻量") -> (t * 0.15).toInt().coerceAtLeast(150)
        sel.startsWith("中等") -> (t * 0.25).toInt().coerceAtLeast(250)
        sel.startsWith("高量") -> (t * 0.35).toInt().coerceAtLeast(350)
        else -> customWorkoutBurn.toIntOrNull()?.coerceAtLeast(0) ?: 0
    }
    val burnTarget by remember(workoutSel, customWorkoutBurn, tdee) {
        mutableStateOf(suggestBurnByWorkout(tdee, workoutSel))
    }

    // ----- 交互反馈：加载/成功条幅/自动收起/回到顶部 -----
    var savingInfo by remember { mutableStateOf(false) }
    var savingPlan by remember { mutableStateOf(false) }
    var savedInfo by remember { mutableStateOf(false) }
    var savedPlan by remember { mutableStateOf(false) }

    val screenH = LocalConfiguration.current.screenHeightDp
    val cardMaxH = (screenH * 0.7f).dp
    val cardScroll = rememberScrollState()

    var savedTheme by remember { mutableStateOf(false) }

    // 顶部：统一管理 Banner
    var bannerText by remember { mutableStateOf<String?>(null) } // null=不显示
    var bannerTextStable by remember { mutableStateOf("") }      // 锁定显示用的文本

// 当要显示新 Banner 时：锁定文本，1.6s 后自动消失
    LaunchedEffect(bannerText) {
        if (bannerText != null) {
            bannerTextStable = bannerText!!     // 锁住本次文本（退出动画期间不变）
            delay(1600)
            bannerText = null                   // 触发淡出
        }
    }

    LaunchedEffect(h) {
        h?.let {
            // 个人信息同步
            sex    = it.sex
            age    = it.age.takeIf { v -> v > 0 }?.toString() ?: ""

            // ★ 补上这两行（关键）：
            height = it.heightCm?.takeIf { v -> v > 0 }?.toString() ?: ""
            // 如果以后还显示体重输入框，也一并同步：
            // weight = it.weightKg?.takeIf { v -> v > 0 }?.toString() ?: ""

            // 计划同步（你已有）
            val savedIntakeMode = it.planIntakeMode ?: intakeOptions.first()
            val savedIntakeKcal = (it.planIntakeKcalPerDay ?: 0).takeIf { v -> v > 0 }?.toString() ?: ""
            val savedWorkoutMode = it.planWorkoutMode ?: workoutOptions.first()
            val savedBurnKcal    = (it.planBurnKcalPerDay ?: 0).takeIf { v -> v > 0 }?.toString() ?: ""

            intakeSel         = savedIntakeMode
            customIntake      = if (savedIntakeMode == "自定义") savedIntakeKcal else ""
            workoutSel        = savedWorkoutMode
            customWorkoutBurn = if (savedWorkoutMode == "自定义") savedBurnKcal else ""
        }
    }


    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = cardMaxH),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(cardScroll),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ======= 顶部成功提示（任一保存成功时出现） =======
            AnimatedVisibility(
                visible = bannerText != null,
                enter = fadeIn(),
                exit  = fadeOut()
            ) {
                SuccessBanner(text = bannerTextStable)
            }

            // ======= 第一行：个人信息 =======
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
                Text("个人信息", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { infoExpanded = !infoExpanded }) {
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.rotate(infoArrow),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(
                visible = infoExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = sex == "male", onClick = { sex = "male" }); Text("男")
                        Spacer(Modifier.width(16.dp))
                        RadioButton(selected = sex == "female", onClick = { sex = "female" }); Text("女")
                    }
                    OutlinedTextField(value = height, onValueChange = { height = it }, label = { Text("身高 cm") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = age, onValueChange = { age = it }, label = { Text("年龄") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                    Button(
                        onClick = {
                            val hCm = height.toDoubleOrNull() ?: 0.0
                            val a   = age.toIntOrNull() ?: 0
                            // 体重不再从输入框取（见第2条），直接用当前档案的值避免被清零
                            val wKg = h?.weightKg ?: 0.0

                            scope.launch {
                                savingInfo = true
                                try {
                                    vm.saveHealthProfile(sex, hCm, wKg, a)
                                    savedInfo = true
                                    bannerText = "个人信息已保存"        // ✅ 明确的成功反馈
                                    infoExpanded = false
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    cardScroll.animateScrollTo(0)
                                } catch (e: Exception) {
                                    Toast.makeText(ctx, "保存失败：${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                } finally {
                                    savingInfo = false
                                }
                            }
                        },
                        enabled = !savingInfo,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (savingInfo) {
                            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(if (savingInfo) "保存中…" else "保存")
                    }
                }
            }

            Divider()

            // ======= 第二行：设置计划 =======
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Create,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
                Text("目标设置", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { planExpanded = !planExpanded }) {
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.rotate(planArrow),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(
                visible = planExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                // 在 PersonalHealthCard() -> 目标设置 planExpanded 的 AnimatedVisibility 内，
// 用下面这段替换你现有的摄入/消耗 UI 一大块内容。

                var intakeExpanded by remember { mutableStateOf(false) }
                var burnExpanded by remember { mutableStateOf(false) }
                val intakeArrow by animateFloatAsState(if (intakeExpanded) 180f else 0f, label = "intakeArrow")
                val burnArrow   by animateFloatAsState(if (burnExpanded) 180f else 0f, label = "burnArrow")

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "计算依据：BMR/TDEE（Mifflin–St Jeor + 活动系数），维持≈TDEE；减脂≈TDEE×0.85；增肌≈TDEE×1.10。运动消耗按 TDEE 的 15%/25%/35% 建议，也可自定义。",
                        style = MaterialTheme.typography.bodySmall
                    )

                    // —— 二级菜单 1：每日摄入目标 ——
// 标题：实时显示当前 kcal
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "每日摄入目标：$intakeTarget kcal/日",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { intakeExpanded = !intakeExpanded }) {
                            Icon(
                                imageVector = Icons.Outlined.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.rotate(intakeArrow),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
                        AnimatedVisibility(
                            visible = intakeExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                // 第一行：维持 / 减脂 / 增肌
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    listOf("维持", "减脂", "增肌").forEachIndexed { idx, opt ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(end = if (idx < 2) 24.dp else 0.dp)
                                        ) {
                                            RadioButton(
                                                selected = intakeSel == opt,
                                                onClick = { intakeSel = opt },
                                                modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(opt, style = MaterialTheme.typography.titleSmall)
                                        }
                                    }
                                }
                                // 第二行：“自定义 + 输入框”（同一行，不换行）
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    RadioButton(
                                        selected = intakeSel == "自定义",
                                        onClick = { intakeSel = "自定义" },
                                        modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("自定义", style = MaterialTheme.typography.titleSmall)
                                    Spacer(Modifier.width(12.dp))
                                    OutlinedTextField(
                                        value = customIntake,
                                        onValueChange = { input ->
                                            // 只保留数字
                                            if (input.all { it.isDigit() }) {
                                                customIntake = input
                                            }
                                        },
                                        placeholder = {
                                            Text(
                                                "kcal/日",
                                                style = MaterialTheme.typography.titleSmall
                                            )
                                        },
                                        textStyle = MaterialTheme.typography.titleSmall,  // ← 输入框内文字变小
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        enabled = intakeSel == "自定义"
                                    )
                                }
                            }
                        }
                    }
                    Divider()

// —— 二级菜单 2：运动目标（每日消耗） ——
// 标题：实时显示当前 kcal
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "每日消耗目标：$burnTarget kcal/日",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { burnExpanded = !burnExpanded }) {
                            Icon(
                                imageVector = Icons.Outlined.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.rotate(burnArrow),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
                        AnimatedVisibility(
                            visible = burnExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            // —— 运动目标（每日消耗）——（第一行3个固定，第二行“自定义+输入框”同一行）
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                // 第一行：轻/中/高
                                val fixedWorkout = listOf("轻量", "中等", "高量")
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    fixedWorkout.forEachIndexed { idx, opt ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(end = if (idx < 2) 24.dp else 0.dp)
                                        ) {
                                            RadioButton(
                                                selected = workoutSel == opt,
                                                onClick = { workoutSel = opt })
                                            Spacer(Modifier.width(4.dp))
                                            Text(opt, style = MaterialTheme.typography.titleSmall)
                                        }
                                    }
                                }
                                // 第二行：“自定义 + 输入框”（同一行，不换行）
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = intakeSel == "自定义",
                                        onClick = { intakeSel = "自定义" })
                                    Spacer(Modifier.width(4.dp))
                                    Text("自定义", style = MaterialTheme.typography.titleSmall)

                                    Spacer(Modifier.width(12.dp))
                                    OutlinedTextField(
                                        value = customIntake,
                                        onValueChange = { input ->
                                            // 只保留数字
                                            if (input.all { it.isDigit() }) {
                                                customIntake = input
                                            }
                                        },
                                        placeholder = {
                                            Text(
                                                "kcal/日",
                                                style = MaterialTheme.typography.titleSmall
                                            )
                                        },
                                        textStyle = MaterialTheme.typography.titleSmall,  // ← 输入框内文字变小
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), // ← 数字键盘
                                        modifier = Modifier.weight(1f),
                                        enabled = intakeSel == "自定义"
                                    )
                                }
                            }
                        }
                    }

                    // 原“保存计划”按钮保持不变
                    Button(
                        onClick = {
                            scope.launch {
                                savingPlan = true
                                try {
                                    vm.updateHealth(
                                        mapOf(
                                            "planIntakeKcalPerDay" to intakeTarget,
                                            "planIntakeMode"       to intakeSel,
                                            "planBurnKcalPerDay"   to burnTarget,
                                            "planWorkoutMode"      to workoutSel
                                        )
                                    )
                                    savedPlan = true
                                    bannerText = "目标设置已保存"       // ✅ 明确的成功反馈
                                    planExpanded = false
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    cardScroll.animateScrollTo(0)
                                } catch (e: Exception) {               // ✅ 失败显式提示
                                    Toast.makeText(ctx, "保存失败：${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                } finally {
                                    savingPlan = false
                                }
                            }
                        },
                        enabled = !savingPlan,
                        modifier = Modifier.fillMaxWidth()
                    ){
                        if (savingPlan) {
                            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(if (savingPlan) "保存中…" else "保存计划")
                    }
                }

            }

            Divider()

            // ====== 第三行：主题颜色（可展开） ======
            var themeExpanded by remember { mutableStateOf(false) }
            val themeArrow by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (themeExpanded) 180f else 0f, label = "themeArrow"
            )

            // 选中的色板 key（默认从当前 AppThemeState 取）
            var themeSel by remember { mutableStateOf(AppThemeState.current.key) }
            var savingTheme by remember { mutableStateOf(false) }

            // 行标题
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.colors ),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
                Text("主题颜色", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { themeExpanded = !themeExpanded }) {
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.rotate(themeArrow),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(
                visible = themeExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                    Text("选择一个色板，保存后全局主色将立即生效。", style = MaterialTheme.typography.bodySmall)

                    // 色板选择（显示每个选项的 primary 预览）
                    ThemePaletteRadioGrid(
                        options = Palettes.all,
                        selectedKey = themeSel,
                        onSelected = { themeSel = it }
                    )

                    Button(
                        onClick = {
                            savingTheme = true
                            scope.launch {
                                try {
                                    // 1) 立即切换运行时主题
                                    AppThemeState.setPalette(themeSel)

                                    // 2) 可选：落盘用户偏好
                                    vm.updateHealth(mapOf("themePalette" to themeSel))

                                    // 3) 可见反馈
                                    savedTheme = true
                                    themeExpanded = false            // 自动收起
                                    bannerText = "主题颜色已保存"
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress) // 触感（已有 haptic）
                                    cardScroll.animateScrollTo(0)    // 回到卡片顶部看到 Banner
                                } finally {
                                    savingTheme = false
                                }
                            }
                        },
                        enabled = !savingTheme,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (savingTheme) {
                            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(if (savingTheme) "保存中…" else "保存主题")
                    }
                }
            }
        }
    }
}

@Composable
private fun SuccessBanner(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.primary, // ✅ 主色背景
        tonalElevation = 0.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary // ✅ 图标颜色跟随主色
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimary // ✅ 文本颜色跟随主色
            )
        }
    }
}


@Composable
private fun ThemePaletteRadioGrid(
    options: List<AppPalette>,
    selectedKey: String,
    onSelected: (String) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        maxItemsInEachRow = 4, // ✅ 四个一行
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        options.forEach { p ->
            // 用“仅圆形色块 + 外环高亮”来表现 Radio 选中态
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(p.primary)               // 纯色圆点
                    .clickable { onSelected(p.key) },     // 点击即选
                contentAlignment = Alignment.Center
            ) {
                if ( selectedKey != p.key){
                Box(
                    Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.35f))
                )
                }
            }
        }
    }
}

@Composable
private fun AvatarHeader(
    uniqueId: String?,
    uid: String,
    avatarUrl: String?,
    onAvatarChanged: (String) -> Unit,
    size: Dp = 96.dp
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var uploading by remember { mutableStateOf(false) }

    // Android 13+：系统相册
    val remote = remember { Remote.create() }

    val pickPhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null && uid.isNotBlank()) {
            scope.launch {
                try {
                    val url = remote.uploadAvatarAndSave(uid, uri) // ← 调用 Remote
                    onAvatarChanged(url)                                 // 本地立即刷新
                    Toast.makeText(ctx, "头像已更新", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(ctx, "上传失败：${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 旧版回退：GetContent
    val pickContent = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null && uid.isNotBlank()) {
            scope.launch {
                uploading = true
                try {
                    val url = uploadAvatarAndSave(uid, uri)
                    onAvatarChanged(url)
                    Toast.makeText(ctx, "头像已更新", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(ctx, "头像上传失败：${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                } finally {
                    uploading = false
                }
            }
        }
    }

    fun launchPicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } else {
            pickContent.launch("image/*")
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 圆形头像
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(3.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                .clickable(enabled = !uploading) { launchPicker() },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(ctx)
                    .data(avatarUrl)
                    .crossfade(true)
                    .placeholder(android.R.color.darker_gray)
                    .error(android.R.color.darker_gray)
                    .build(),
                contentDescription = "avatar",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            if (uploading) {
                CircularProgressIndicator(
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(28.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        // 第二行：UID（居中）
        Text(
            text = (uniqueId ?: "").ifBlank { "未登录" },
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/** 上传头像到 Firebase Storage 并把下载 URL 存入 Firestore（users/{uid}.avatarUrl） */
private suspend fun uploadAvatarAndSave(uid: String, uri: Uri): String {
    // 1) Storage：按用户 ID 存一份固定命名（覆盖旧图）
    val ref = FirebaseStorage.getInstance()
        .reference.child("avatars/$uid.jpg")
    ref.putFile(uri).await()

    val downloadUrl = ref.downloadUrl.await().toString()

    // 2) Firestore：merge 写入用户文档
    val doc = FirebaseFirestore.getInstance().collection("users").document(uid)
    doc.set(mapOf("avatarUrl" to downloadUrl), SetOptions.merge()).await()

    return downloadUrl
}