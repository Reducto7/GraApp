package com.example.gra.ui.records

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.gra.ui.BottomNavigationBar

@Composable
fun RecordsPage(navController: NavHostController) {
    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = "健康记录",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 1. 饮食记录
            RecordCard(
                title = "饮食记录",
                description = "记录每日摄入的食物和卡路里",
                onClick = {
                    navController.navigate("food_exercise")
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 2. 运动记录
            RecordCard(
                title = "运动记录",
                description = "记录每日运动类型和时长",
                onClick = {
                    navController.navigate("exercise")
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 3. 身体数据记录
            RecordCard(
                title = "身体数据记录",
                description = "记录体重、围度等身体数据",
                onClick = {
                    navController.navigate("body")
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 4. 饮水/睡眠记录
            RecordCard(
                title = "饮水/睡眠记录",
                description = "记录每日饮水量与睡眠情况",
                onClick = {
                    // TODO: 跳转到饮水/睡眠记录页面
                }
            )
        }
    }
}

@Composable
fun RecordCard(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = description, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
