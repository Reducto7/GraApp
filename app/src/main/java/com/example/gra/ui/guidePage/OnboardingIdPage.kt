package com.example.gra.ui.guidePage

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gra.ui.data.Remote
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gra.ui.viewmodel.BodyMeasureViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingIdPage(
    navController: NavController
) {
    val ctx = LocalContext.current
    val uid = remember { FirebaseAuth.getInstance().currentUser?.uid.orEmpty() }
    val remote = remember { Remote.create() }
    val scope = rememberCoroutineScope()

    var uniqueId by rememberSaveable { mutableStateOf("") }
    var checking by remember { mutableStateOf(false) }
    var available by remember { mutableStateOf<Boolean?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }

    // 500ms 去抖校验
    LaunchedEffect(uniqueId) {
        available = null; error = null
        val t = uniqueId.trim()
        if (t.isEmpty()) return@LaunchedEffect
        checking = true
        delay(500)
        try {
            available = remote.isUniqueIdAvailable(t)
        } catch (e: Exception) {
            error = e.localizedMessage
        } finally {
            checking = false
        }
    }

    Scaffold(
        bottomBar = {
            Button(
                onClick = {
                    if (uid.isBlank()) {
                        Toast.makeText(ctx, "未登录，无法设置", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    scope.launch {
                        submitting = true
                        try {
                            val ok = remote.setUniqueId(uid, uniqueId.trim())
                            if (ok) {
                                Toast.makeText(ctx, "已设置：$uniqueId", Toast.LENGTH_SHORT).show()
                                navController.navigate("onboarding/basic")
                            } else {
                                available = false
                                Toast.makeText(ctx, "该 ID 已被占用", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(ctx, "保存失败：${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        } finally {
                            submitting = false
                        }
                    }
                },
                enabled = !submitting && uniqueId.isNotBlank() && (available != false),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) { Text(if (submitting) "保存中…" else "下一步") }
        }
    ) { inner ->
        Column(
            Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(1f))
            Text("设置你的全局唯一 ID", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text("将作为公开昵称，3–20 个字符（a–z, 0–9, 下划线）。", textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = uniqueId,
                onValueChange = { uniqueId = it },
                singleLine = true,
                label = { Text("唯一 ID") },
                supportingText = {
                    when {
                        checking      -> Text("检查中…")
                        error != null -> Text("检查失败：$error")
                        available == true  -> Text("✅ 可以使用")
                        available == false -> Text("❌ 已被占用")
                    }
                },
                isError = (available == false),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.weight(1f))
        }
    }
}

