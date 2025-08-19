package com.example.gra.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.example.gra.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainPage(navController: NavHostController) {
    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 植物形象
            /*
            Image(
                painter = painterResource(id = R.drawable.plant_stage_1),
                contentDescription = "植物",
                modifier = Modifier.size(200.dp)
            )
             */

            Spacer(modifier = Modifier.height(16.dp))

            // 成长进度条
            LinearProgressIndicator(
                progress = 0.3f, // 示例值
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text("今日成长点数：+30", fontSize = 18.sp)

            Spacer(modifier = Modifier.height(24.dp))

            Button(onClick = { /* TODO: 去任务页 */ }) {
                Text(text = "去完成任务")
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "养成") },
            label = { Text("养成") },
            selected = false,
            onClick = {
                navController.navigate("main") {
                    popUpTo("main") { inclusive = true }
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.List, contentDescription = "记录") },
            label = { Text("记录") },
            selected = false,
            onClick = {
                navController.navigate("records") {
                    popUpTo("records") { inclusive = true }
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = "任务") },
            label = { Text("任务") },
            selected = false,
            onClick = {
                //navController.navigate("missions")
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Person, contentDescription = "我的") },
            label = { Text("我的") },
            selected = false,
            onClick = {
                navController.navigate("mine") {
                    popUpTo("mine") { inclusive = true }
                }
            }
        )
    }
}

