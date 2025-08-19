package com.example.gra.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gra.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterPage(navController: NavController, modifier: Modifier = Modifier) {

    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var isDialogVisible by rememberSaveable { mutableStateOf(false) }
    var isRegistrationSuccessful by rememberSaveable { mutableStateOf(false) }
    val passwordVisible = remember { mutableStateOf(false) }
    var dialogMessage by rememberSaveable { mutableStateOf("") }
    var showErrors by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "회원가입") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigate("login") }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "back"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().shadow(8.dp)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("E-mail") },
                isError = showErrors && email.isEmpty(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier
                    .fillMaxWidth()
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                isError = showErrors && password.isEmpty(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = if (passwordVisible.value) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible.value = !passwordVisible.value }) {
                        Icon(
                            painter = painterResource(id = if (passwordVisible.value) R.drawable.visibility else R.drawable.visibility_off),
                            contentDescription = "Toggle password visibility"
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
            )

            OutlinedTextField(
                value = confirm,
                onValueChange = { confirm = it },
                label = { Text("Confirm Password") },
                isError = showErrors && (confirm.isEmpty() || confirm != password),
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done // 将转行键设置为“确认键”
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        registerUser(email, password) { success, message ->
                            isRegistrationSuccessful = success
                            dialogMessage = message
                            isDialogVisible = true
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
                modifier = Modifier
                    .fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    showErrors = true
                    if (email.isNotBlank() && password.isNotBlank() && password == confirm) {
                        registerUser(email, password) { success, message ->
                            isRegistrationSuccessful = success
                            dialogMessage = message
                            isDialogVisible = true
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(
                    text = "회원가입",
                    style = typography.titleMedium
                )
            }

        }

        if (isDialogVisible) {
            RegisterDialog(
                message = dialogMessage,
                onDismiss = {
                    isDialogVisible = false
                    if (isRegistrationSuccessful) {
                        navController.navigate("login")
                    }
                }
            )
        }
    }
}


fun registerUser(email: String, password: String, onResult: (Boolean, String) -> Unit) {
    val auth = FirebaseAuth.getInstance()
    auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
        if (task.isSuccessful) {
            onResult(true, "회원가입이 성공적으로 완료되었습니다!")
        } else {
            val errorMessage = when (val exception = task.exception) {
                is FirebaseAuthUserCollisionException -> "이 이메일은 이미 등록되었습니다"
                else -> "회원가입에 실패했습니다. 다시 시도해주세요. 원인: ${exception?.localizedMessage ?: "알 수 없는 오류"}"
            }
            onResult(false, errorMessage)
        }
    }
}


@Composable
fun RegisterDialog(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = if (message == "회원가입이 성공적으로 완료되었습니다!") "회원가입 성공" else "회원가입 실패") },
        text = { Text(text = message) },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("확인")
            }
        },
        modifier = modifier
    )
}



