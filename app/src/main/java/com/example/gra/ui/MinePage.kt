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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.style.TextAlign
import com.example.gra.ui.theme.AppPalette
import com.example.gra.ui.theme.AppThemeState
import com.example.gra.ui.theme.Palettes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MinePage(
    navController: NavController,
) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val accountName = currentUser?.email ?: "Guest" // 默认显示 "Guest" 如果用户未登录

    // === 新增：监听 uniqueId ===
    val uid = currentUser?.uid.orEmpty()
    val remote = remember { Remote.create() }
    var uniqueId by remember { mutableStateOf<String?>(null) }

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
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Spacer(Modifier.height(48.dp))
                    TopIdentityBlock(
                        uniqueId   = uniqueId,
                        email      = accountName,
                        onSaved    = { /* 保存后如果要额外处理就写这里；一般不用，监听会回流 */ }
                    )

                    Spacer(Modifier.height(32.dp))

                    Text("User Details")
                    Spacer(Modifier.height(4.dp))
                    PersonalHealthCard(navController = navController)
                    // 内部展开再多，也能跟随页面滚动

                    Spacer(Modifier.height(32.dp))

                    Text("Help Center")
                    Spacer(Modifier.height(4.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ){  }
                }
            }
        }
    )
}


// 顶部两行 + 展开编辑 UID
@Composable
private fun TopIdentityBlock(
    uniqueId: String?,
    email: String?,
    onSaved: (String) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }

    // 第1行：居中显示“用户ID + 编辑图标”
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = uniqueId?.ifBlank { "未设置" } ?: "未设置",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.width(8.dp))
        /*
        IconButton(onClick = { expanded = !expanded }) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "编辑唯一ID",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

         */
    }

    Spacer(Modifier.height(8.dp))

    // 第2行：居中灰色小字显示注册邮箱
    Text(
        text = email ?: "未绑定邮箱",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth(),
        // 居中
        textAlign = TextAlign.Center
    )

    // 展开：内联的“修改唯一ID”编辑区
    AnimatedVisibility(
        visible = expanded,
        enter = expandVertically() + fadeIn(),
        exit  = shrinkVertically() + fadeOut()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            InlineUniqueIdEditor(
                current = uniqueId,
                onSaved = {
                    onSaved(it)
                    expanded = false // 保存后收起
                }
            )
        }
    }
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
    var weight by remember { mutableStateOf(h?.weightKg?.takeIf { it > 0 }?.toString() ?: "") }
    var age by remember { mutableStateOf(h?.age?.takeIf { it > 0 }?.toString() ?: "") }

    LaunchedEffect(h) { // 后端回流时，同步到输入框
        h?.let {
            sex = it.sex
            height = it.heightCm.takeIf { v -> v > 0 }?.toString() ?: ""
            weight = it.weightKg.takeIf { v -> v > 0 }?.toString() ?: ""
            age = it.age.takeIf { v -> v > 0 }?.toString() ?: ""
        }
    }

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

    val workoutOptions = listOf("轻量活动（约15%TDEE）", "中等活动（约25%TDEE）", "高量活动（约35%TDEE）", "自定义")
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
                    OutlinedTextField(value = weight, onValueChange = { weight = it }, label = { Text("体重 kg") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = age, onValueChange = { age = it }, label = { Text("年龄") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                    Button(
                        onClick = {
                            val hCm = height.toDoubleOrNull() ?: 0.0
                            val wKg = weight.toDoubleOrNull() ?: 0.0
                            val a = age.toIntOrNull() ?: 0
                            scope.launch {
                                savingInfo = true
                                try {
                                    vm.saveHealthProfile(sex, hCm, wKg, a) // 保存 -> 远端 -> 回流刷新  :contentReference[oaicite:4]{index=4}
                                    savedInfo = true
                                    infoExpanded = false               // ✅ 自动收起
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    cardScroll.animateScrollTo(0)      // ✅ 回到卡片顶部看到提示
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
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "计算依据：BMR/TDEE（Mifflin–St Jeor + 活动系数），维持≈TDEE；减脂≈TDEE×0.85；增肌≈TDEE×1.10。运动消耗按 TDEE 的 15%/25%/35% 建议，也可自定义。",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(Modifier.height(4.dp))
                    Text("每日摄入目标", style = MaterialTheme.typography.titleSmall,fontWeight = FontWeight.SemiBold)
                    ChoiceChipsFlowRow(intakeOptions, intakeSel) { intakeSel = it }
                    if (intakeSel == "自定义") {
                        OutlinedTextField(
                            value = customIntake, onValueChange = { customIntake = it },
                            label = { Text("自定义每日摄入 (kcal)") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        AssistChip(onClick = {}, label = { Text("建议：$intakeTarget kcal/日") })
                    }

                    Spacer(Modifier.height(4.dp))
                    Text("运动目标（每日消耗）", style = MaterialTheme.typography.titleSmall,fontWeight = FontWeight.SemiBold)
                    ChoiceChipsFlowRow(workoutOptions, workoutSel) { workoutSel = it }
                    if (workoutSel == "自定义") {
                        OutlinedTextField(
                            value = customWorkoutBurn, onValueChange = { customWorkoutBurn = it },
                            label = { Text("自定义每日消耗 (kcal)") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        AssistChip(onClick = {}, label = { Text("建议：$burnTarget kcal/日") })
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                savingPlan = true
                                try {
                                    vm.updateHealth(
                                        mapOf(
                                            "planIntakeKcalPerDay" to intakeTarget,
                                            "planIntakeMode" to intakeSel,
                                            "planBurnKcalPerDay" to burnTarget,
                                            "planWorkoutMode" to workoutSel
                                        )
                                    ) // Firestore merge 写入  :contentReference[oaicite:5]{index=5}
                                    savedPlan = true
                                    planExpanded = false             // ✅ 自动收起
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    cardScroll.animateScrollTo(0)    // ✅ 回到卡片顶部看到提示
                                } finally {
                                    savingPlan = false
                                }
                            }
                        },
                        enabled = !savingPlan,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (savingPlan) {
                            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(if (savingPlan) "保存中…" else "保存计划")
                    }

                    Divider()
                    Text("当前推荐", style = MaterialTheme.typography.titleSmall)
                    Text("BMR: ${h?.bmr ?: 0}  |  TDEE: $tdee")
                    Text("维持摄入: $recoMaintain  |  减脂摄入: $recoCut  |  增肌摄入: $recoBulk")
                    Text("运动消耗建议: $burnTarget kcal/日（当前选择）")
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
                    ThemePaletteOptions(
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

            Divider()

            // ===== 第三行：退出账号 =====
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                        // 跳转到登录页
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
private fun ChoiceChipsFlowRow(
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        maxItemsInEachRow = 3,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { opt ->
            FilterChip(
                selected = (selected == opt),
                onClick = { onSelected(opt) },
                label = { Text(opt, maxLines = 1, softWrap = false) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThemePaletteOptions(
    options: List<AppPalette>,
    selectedKey: String,
    onSelected: (String) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        maxItemsInEachRow = 3,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        options.forEach { p ->
            FilterChip(
                selected = (selectedKey == p.key),
                onClick = { onSelected(p.key) },
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // 小色卡：显示 primary
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(MaterialTheme.shapes.extraSmall)
                                .background(p.primary)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(p.label, maxLines = 1, softWrap = false)
                    }
                }
            )
        }
    }
}
