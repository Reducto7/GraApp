package com.example.gra.ui.guidePage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.gra.ui.bottomGreen
import com.example.gra.ui.topBlue
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
        Box(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(colors = listOf(topBlue, bottomGreen))
                )
                .padding(horizontal = 36.dp)
        ) {
            // 中间内容
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("你的每日推荐", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Text("BMR 基础代谢: $bmr kcal")
                Text("TDEE 总消耗: $tdee kcal")
                Text("维持体重推荐摄入: $maintain kcal")
                Text("减脂期推荐摄入: $cut kcal（约 -15%）")
            }

            // 底部按钮
            Button(
                onClick = { navController.navigate("main") },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .offset(y = -150.dp)
            ) {
                Text("开始使用")
            }
        }
    }
}




