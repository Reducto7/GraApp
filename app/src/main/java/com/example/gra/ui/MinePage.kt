package com.example.gra.ui


import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.gra.R
import com.example.gra.ui.data.Remote
import com.google.firebase.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MinePage(
    navController: NavController,
) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val accountName = currentUser?.email?.substringBefore("@") ?: "Guest" // 默认显示 "Guest" 如果用户未登录

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
                title = { Text(text = "마이") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "返回")
                    }
                },
                modifier = Modifier.fillMaxWidth().shadow(8.dp)
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
                            brush = Brush.verticalGradient(
                                colors = listOf(topBlue, bottomGreen)
                            )
                        )
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 头像和账户名
                    HeaderSection(accountName)

                    //修改昵称
                    Spacer(Modifier.height(16.dp))
                    UniqueIdEditor(current = uniqueId) { new ->
                        // 保存后回调；这里不用手动改 uniqueId，监听会自动回流。
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 功能按钮
                    FunctionButtonsSection(navController)
                }
            }
        }
    )
}


@Composable
fun HeaderSection(accountName: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 使用默认头像
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_background),
            contentDescription = "photograph",
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = accountName,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            //color = Color.White // 设置字体颜色为白色
        )
    }
}

@Composable
fun FunctionButtonsSection(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = {
                // 退出账号并跳转到登录页面
                navController.navigate("login") {
                    popUpTo("my") { inclusive = true }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(text = "로그아웃")
        }
    }
}

@Composable
private fun UniqueIdEditor(
    current: String?,
    onSaved: (String) -> Unit = {}
) {
    val ctx = LocalContext.current
    val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    val remote = remember { Remote.create() }
    val scope = rememberCoroutineScope()

    var editing by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf(current.orEmpty()) }
    var checking by remember { mutableStateOf(false) }
    var available by remember { mutableStateOf<Boolean?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    // 当前变化时做去抖检查
    LaunchedEffect(text) {
        available = null; error = null
        val t = text.trim()
        if (t.isEmpty() || t.equals(current, ignoreCase = true)) return@LaunchedEffect
        checking = true
        delay(500)
        try {
            val ok = remote.isUniqueIdAvailable(t)
            available = ok
            android.util.Log.d("UniqueId", "check '$t' -> $ok")
        } catch (e: Exception) {
            error = e.localizedMessage
            android.util.Log.e("UniqueId", "check failed", e)
        } finally {
            checking = false
        }
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "昵称（唯一ID）", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = {
                    editing = !editing
                    if (editing) text = current.orEmpty()
                }) {
                    Text(if (editing) "取消" else "修改")
                }
            }

            if (!editing) {
                Text(text = current ?: "未设置")
            } else {
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
                        onClick = { editing = false },
                        enabled = !saving,
                        modifier = Modifier.weight(1f)
                    ) { Text("取消") }

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
                                        android.util.Log.d("UniqueId", "rename ok: $text")
                                        editing = false
                                        onSaved(text)
                                    } else {
                                        Toast.makeText(ctx, "该ID已被占用", Toast.LENGTH_SHORT).show()
                                        available = false
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(ctx, "保存失败：${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                    android.util.Log.e("UniqueId", "rename error", e)
                                } finally {
                                    saving = false
                                }
                            }
                        },
                        enabled = !saving
                                && text.isNotBlank()
                                && !text.equals(current ?: "", ignoreCase = true)
                                && available != false,
                        modifier = Modifier.weight(1f)
                    ) { Text(if (saving) "保存中…" else "保存") }
                }
            }
        }
    }
}
