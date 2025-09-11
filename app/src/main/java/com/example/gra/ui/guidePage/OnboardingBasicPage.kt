package com.example.gra.ui.guidePage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

import androidx.navigation.NavHostController
import com.example.gra.ui.bottomGreen
import com.example.gra.ui.topBlue
import com.example.gra.ui.viewmodel.BodyMeasureViewModel

@Composable
fun OnboardingBasicPage(
    navController: NavHostController
) {
    val vm: BodyMeasureViewModel = viewModel()
    var sex by remember { mutableStateOf("male") }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }

    val h by vm.health.collectAsState(null)
    LaunchedEffect(h) {
        h?.let {
            sex = it.sex
            if (it.heightCm > 0) height = it.heightCm.toString()
            if (it.weightKg > 0) weight = it.weightKg.toString()
            if (it.age > 0) age = it.age.toString()
        }
    }

    Scaffold { inner ->
        Box(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(colors = listOf(topBlue, bottomGreen))
                )
                .padding(horizontal = 36.dp)
        ) {
            // 中间部分：标题、输入框
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("完善基本信息", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = sex == "male", onClick = { sex = "male" })
                    Text("男")
                    Spacer(Modifier.width(16.dp))
                    RadioButton(selected = sex == "female", onClick = { sex = "female" })
                    Text("女")
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = height, onValueChange = { height = it }, label = { Text("身高 cm") })
                OutlinedTextField(value = weight, onValueChange = { weight = it }, label = { Text("体重 kg") })
                OutlinedTextField(value = age, onValueChange = { age = it }, label = { Text("年龄") })
            }

            // 底部按钮
            Button(
                onClick = {
                    val hCm = height.toDoubleOrNull() ?: 0.0
                    val wKg = weight.toDoubleOrNull() ?: 0.0
                    val a = age.toIntOrNull() ?: 0
                    vm.saveHealthProfile(sex, hCm, wKg, a)
                    navController.navigate("onboarding/result")
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .offset(y = -150.dp)
            ) {
                Text("下一步")
            }
        }
    }
}


