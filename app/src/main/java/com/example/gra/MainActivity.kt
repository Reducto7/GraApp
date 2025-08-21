package com.example.gra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.gra.ui.LoginPage
import com.example.gra.ui.records.FoodExercisePage
import com.example.gra.ui.MainPage
import com.example.gra.ui.MinePage
import com.example.gra.ui.RegisterPage
import com.example.gra.ui.data.AppDatabase
import com.example.gra.ui.records.FoodRecordPage
import com.example.gra.ui.records.ExerciseRecordPage
import com.example.gra.ui.records.RecordsPage
import com.example.gra.ui.data.prepopulateFromAssetsIfEmpty
import com.example.gra.ui.records.BodyMeasurePage
import com.example.gra.ui.theme.GraTheme
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ① 启动时把 CSV 导入到 Room（只在第一次为空时导入）
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getInstance(applicationContext)
            prepopulateFromAssetsIfEmpty(applicationContext, db)
        }
        FirebaseApp.initializeApp(this)
        enableEdgeToEdge()
        setContent {
            GraTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "body"
                ) {
                    composable(route = "login") {
                        LoginPage(navController, context = this@MainActivity)
                    }
                    composable("register") {
                        RegisterPage(navController)
                    }
                    composable("main") {
                        MainPage(navController)
                    }
                    composable("records") {
                        RecordsPage(navController)
                    }
                    composable("food_exercise") {
                        FoodExercisePage(navController)
                    }
                    composable("exercise") {
                        ExerciseRecordPage(navController)
                    }
                    composable("food") {
                        FoodRecordPage(navController)
                    }
                    composable("body") {
                        BodyMeasurePage(navController)
                    }
                    composable("mine") {
                        MinePage(navController)
                    }
                }
            }
        }
    }
}
