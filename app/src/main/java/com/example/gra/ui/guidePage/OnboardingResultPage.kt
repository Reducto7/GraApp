package com.example.gra.ui.guidePage

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.gra.ui.viewmodel.BodyMeasureViewModel

@Composable
fun OnboardingResultPage(
    navController: NavHostController
) {
    val vm: BodyMeasureViewModel = viewModel()
    val h by vm.health.collectAsState(null)
    val maintain = h?.recoMaintain ?: 0
    val cut = h?.recoCut ?: 0
    val tdee = h?.tdee ?: 0
    val bmr = h?.bmr ?: 0

    Scaffold { inner ->
        Column(Modifier.padding(inner).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("你的每日推荐", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text("BMR 基础代谢: $bmr kcal")
            Text("TDEE 总消耗: $tdee kcal")
            Text("维持体重推荐摄入: $maintain kcal")
            Text("减脂期推荐摄入: $cut kcal（约 -15%）")
            Spacer(Modifier.height(24.dp))
            Button(onClick = { navController.navigate("main") }, modifier = Modifier.fillMaxWidth()) { Text("开始使用") }
        }
    }
}



