package com.example.gra.ui.records

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Observer
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gra.ui.DateSelector
import com.example.gra.ui.viewmodel.FoodViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodExercisePage(
    navController: NavHostController,
    foodViewModel: FoodViewModel = viewModel()
) {

    val currentBackStackEntry = navController.currentBackStackEntry

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("饮食/运动记录") },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.navigate("records") {
                            popUpTo("records") { inclusive = true }
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            val context = LocalContext.current
            var selectedDate by remember { mutableStateOf(LocalDate.now()) }

            LaunchedEffect(selectedDate) {
                foodViewModel.loadDataByDate(selectedDate)
            }

            DateSelector(
                context = context,
                selectedDate = selectedDate,
                onDateSelected = { newDate -> selectedDate = newDate }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text("总摄入: ${foodViewModel.totalIntakeKcal.value} kcal")
            Text("总消耗: ${foodViewModel.totalBurnKcal.value} kcal")

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                items(foodViewModel.meals) { meal ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(meal.name)
                            Text("${meal.kcal} kcal")
                        }
                    }
                }
            }


            Spacer(modifier = Modifier.height(24.dp))

            // 按钮区域
            Button(
                onClick = {
                    navController.navigate("food")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("添加新饮食记录")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    navController.navigate("exercise")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("添加新运动记录")
            }
        }
    }
}

