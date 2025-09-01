package com.example.gra.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gra.ui.data.Remote
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetUniqueIdPage(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    val remote = remember { Remote.create() }
    val scope = rememberCoroutineScope()

    var text by remember { mutableStateOf("") }
    var checking by remember { mutableStateOf(false) }
    var available by remember { mutableStateOf<Boolean?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }

    // 输入后 500ms 去检查
    LaunchedEffect(text) {
        available = null; error = null
        val t = text.trim()
        if (t.isEmpty()) return@LaunchedEffect
        checking = true
        delay(500)
        try {
            val ok = remote.isUniqueIdAvailable(t)
            available = ok
            android.util.Log.d("UniqueId", "debounce check '$t' -> $ok")
        } catch (e: Exception) {
            error = e.localizedMessage
            android.util.Log.e("UniqueId", "check failed", e)
        } finally {
            checking = false
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("设置唯一ID") })
        }
    ) { inner ->
        Column(
            modifier = modifier
                .padding(inner)
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "这个ID将作为你的公开昵称，全局唯一（3–20个：a–z, 0–9, 下划线）。后续支持修改，但同样需要唯一。",
                style = MaterialTheme.typography.bodyMedium
            )

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                label = { Text("唯一ID") },
                supportingText = {
                    when {
                        checking -> Text("检查中…")
                        error != null -> Text("检查失败：$error")
                        available == true -> Text("✅ 可以使用")
                        available == false -> Text("❌ 已被占用")
                        else -> {}
                    }
                },
                isError = (available == false),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    if (uid.isBlank()) {
                        Toast.makeText(
                            (navController.context),
                            "未登录，无法设置", Toast.LENGTH_SHORT
                        ).show()
                        return@Button
                    }
                    scope.launch {
                        submitting = true
                        try {
                            val ok = remote.setUniqueId(uid, text)
                            if (ok) {
                                Toast.makeText(navController.context, "已设置：$text", Toast.LENGTH_SHORT).show()
                                android.util.Log.d("UniqueId", "set ok: $text")
                                navController.navigate("main") {
                                    popUpTo("login") { inclusive = true }
                                }
                            } else {
                                Toast.makeText(navController.context, "该ID已被占用", Toast.LENGTH_SHORT).show()
                                android.util.Log.w("UniqueId", "set fail: duplicated")
                                available = false
                            }
                        } catch (e: Exception) {
                            Toast.makeText(navController.context, "保存失败：${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            android.util.Log.e("UniqueId", "set error", e)
                        } finally {
                            submitting = false
                        }
                    }
                },
                enabled = !submitting && (available != false) && text.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (submitting) "保存中…" else "保存并进入主页")
            }
        }
    }
}
