package com.example.gra.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.remember
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import com.example.gra.R
import com.example.gra.ui.data.Remote
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun navigateAfterLogin(remote: Remote, navController: NavController) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
        android.util.Log.w("Auth", "no uid after login")
        return
    }
    CoroutineScope(Dispatchers.Main).launch {
        try {
            val p = remote.getUserProfile(uid)
            val route = if (p?.uniqueId.isNullOrBlank()) "set_unique_id" else "main"
            android.util.Log.d("Auth", "post-login route=$route (uniqueId=${p?.uniqueId})")
            navController.navigate(route) {
                popUpTo("login") { inclusive = true }
            }
        } catch (e: Exception) {
            android.util.Log.e("Auth", "profile load failed", e)
            // 出错时直接去设置ID，避免卡住
            navController.navigate("set_unique_id") {
                popUpTo("login") { inclusive = true }
            }
        }
    }
}

@Composable
fun LoginPage(navController: NavController, modifier: Modifier = Modifier, context: Context) {
    // 添加 Google Sign-In 客户端配置
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(context.getString(R.string.your_web_client_id))
        .requestEmail()
        .build()

    val googleSignInClient = GoogleSignIn.getClient(context, gso)
    val remote = remember { Remote.create() }


    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        handleGoogleSignInResult(task, context, navController, remote)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
    ) { innerPadding ->
        Column(
            modifier = modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // 邮箱登录部分
            EmailLoginSection(navController, context, remote)

            Spacer(modifier = Modifier.height(8.dp))

            // 灰线分隔
            DividerWithText("간편하게 로그인하기")

            // Google 登录部分
            GoogleLoginSection(googleSignInClient, launcher, context, remote)

            AnonymousLoginSection(navController, context,  remote)
        }
    }
}

@Composable
fun EmailLoginSection(navController: NavController, context: Context,  remote: Remote) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var isDialogVisible by rememberSaveable { mutableStateOf(false) }
    var dialogMessage by rememberSaveable { mutableStateOf("") }
    var autoLogin by rememberSaveable { mutableStateOf(false) }
    val passwordVisible = remember { mutableStateOf(false) }
    var showErrors by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val sharedPreferences = context.getSharedPreferences("autoLoginPrefs", Context.MODE_PRIVATE)

    // 加载自动登录状态
    LaunchedEffect(Unit) {
        if (sharedPreferences.getBoolean("autoLoginEnabled", false)) {
            email = sharedPreferences.getString("savedEmail", "") ?: ""
            autoLogin = true
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Mail") },
            isError = showErrors && email.isEmpty(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            isError = showErrors && password.isEmpty(),
            keyboardActions = KeyboardActions(
                onDone = {
                    loginUser(email, password) { success, message ->
                        if (success) {
                            if (autoLogin) {
                                coroutineScope.launch {
                                    sharedPreferences.edit()
                                        .putBoolean("autoLoginEnabled", true)
                                        .putString("savedEmail", email)
                                        .apply()
                                }
                            } else {
                                coroutineScope.launch {
                                    sharedPreferences.edit()
                                        .putBoolean("autoLoginEnabled", false)
                                        .remove("savedEmail")
                                        .apply()
                                }
                            }
                            navigateAfterLogin(remote, navController)
                        } else {
                            dialogMessage = message
                            isDialogVisible = true
                        }
                    }

                }
            ),
            visualTransformation = if (passwordVisible.value) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible.value = !passwordVisible.value }) {
                    Icon(
                        painter = painterResource(id = if (passwordVisible.value) R.drawable.visibility else R.drawable.visibility_off),
                        contentDescription = "Toggle password visibility"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                showErrors = true
                if (email.isNotBlank() && password.isNotBlank()) {
                    loginUser(email, password) { success, message ->
                        if (success) {
                            if (autoLogin) {
                                coroutineScope.launch {
                                    sharedPreferences.edit()
                                        .putBoolean("autoLoginEnabled", true)
                                        .putString("savedEmail", email)
                                        .apply()
                                }
                            } else {
                                coroutineScope.launch {
                                    sharedPreferences.edit()
                                        .putBoolean("autoLoginEnabled", false)
                                        .remove("savedEmail")
                                        .apply()
                                }
                            }
                            navigateAfterLogin(remote, navController)
                        } else {
                            dialogMessage = message
                            isDialogVisible = true
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "로그인", style = typography.titleMedium)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = autoLogin,
                    onCheckedChange = { autoLogin = it }
                )
                Text("자동 로그인")
            }

            TextButton(
                onClick = { navController.navigate("register") }
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(textDecoration = TextDecoration.Underline)) {
                            append("회원가입")
                        }
                    },
                    style = typography.titleMedium
                )
            }

        }
    }

    if (isDialogVisible) {
        LoginDialog(
            message = dialogMessage,
            onDismiss = { isDialogVisible = false }
        )
    }
}

@Composable
fun DividerWithText(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = Color.Gray)
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp),
            color = Color.Gray,
            style = typography.bodyMedium
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = Color.Gray)
    }
}

@Composable
fun GoogleLoginSection(
    googleSignInClient: GoogleSignInClient,
    launcher: ManagedActivityResultLauncher<Intent, ActivityResult>,
    context: Context,
    remote: Remote
) {
    OutlinedButton(
        onClick = {
            googleSignInClient.signOut().addOnCompleteListener {
                val signInIntent = googleSignInClient.signInIntent
                launcher.launch(signInIntent)
            }
        },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
    ) {
        Text(text = "Google", style = typography.titleMedium)
    }
}

@Composable
fun AnonymousLoginSection(navController: NavController, context: Context,
                          remote: Remote) {
    OutlinedButton(
        onClick = {
            performAnonymousLogin(navController, remote)
        },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
    ) {
        Text(text = "익명", style = typography.titleMedium)
    }
}

fun performAnonymousLogin(navController: NavController, remote: Remote) {
    val auth = FirebaseAuth.getInstance()
    auth.signInAnonymously()
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("AnonymousLogin", "signInAnonymously:success")
                navigateAfterLogin(remote, navController)
            } else {
                Log.e("AnonymousLogin", "signInAnonymously:failure", task.exception)
            }
        }
}

fun loginUser(email: String, password: String, onResult: (Boolean, String) -> Unit) {
    val auth = FirebaseAuth.getInstance()
    auth.signInWithEmailAndPassword(email.trim(), password)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                onResult(true, "")
            } else {
                val ex = task.exception
                // 统一打详细日志
                Log.e("Login", "signIn failed: ${ex?.javaClass?.name}: ${ex?.message}", ex)

                // 拿 FirebaseAuthException 的标准错误码（可能为 null）
                val code = (ex as? com.google.firebase.auth.FirebaseAuthException)?.errorCode

                val msg = when {
                    // 设备校验相关（你这次的日志就是这个分支）
                    (ex?.message?.contains("RecaptchaAction", ignoreCase = true) == true) -> {
                        "设备校验失败：请使用带 Google Play 的模拟器/真机，并在 Firebase 控制台为此构建添加 SHA-256 指纹。"
                    }
                    code == "ERROR_INVALID_EMAIL"       -> "邮箱格式不正确"
                    code == "ERROR_USER_NOT_FOUND"      -> "该邮箱未注册"
                    code == "ERROR_WRONG_PASSWORD"      -> "密码错误"
                    code == "ERROR_USER_DISABLED"       -> "账号已被禁用"
                    else -> "登录失败：${ex?.localizedMessage ?: "未知错误"} (code=$code)"
                }
                onResult(false, msg)
            }
        }
}


@Composable
fun LoginDialog(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "로그인 실패") },
        text = { Text(text = message) },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("확인")
            }
        },
        modifier = modifier
    )
}

fun handleGoogleSignInResult(
    task: Task<GoogleSignInAccount>,
    context: Context,
    navController: NavController,
    remote: Remote
) {
    try {
        val account = task.getResult(ApiException::class.java)
        val idToken = account?.idToken

        if (idToken != null) {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val auth = FirebaseAuth.getInstance()

            auth.signInWithCredential(credential).addOnCompleteListener { authTask ->
                if (authTask.isSuccessful) {
                    navigateAfterLogin(remote, navController)
                } else {
                    Toast.makeText(
                        context,
                        "Firebase 로그인 실패: ${authTask.exception?.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        } else {
            Toast.makeText(context, "Google ID Token 얻을 수 없습니다", Toast.LENGTH_LONG).show()
        }
    } catch (e: ApiException) {
        Log.e("GoogleSignIn", "statusCode=${e.statusCode}, message=${e.message}", e)
        Log.e("GoogleSignIn", "GoogleSignInResult: ${GoogleSignInStatusCodes.getStatusCodeString(e.statusCode)}")
        Toast.makeText(context, "Google 로그인 실패: ${e.statusCode}", Toast.LENGTH_LONG).show()
    }
}

