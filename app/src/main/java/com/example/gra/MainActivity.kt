package com.example.gra

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.gra.tasks.TasksPage
import com.example.gra.ui.LoginPage
import com.example.gra.ui.records.FoodExercisePage
import com.example.gra.ui.MainPage
import com.example.gra.ui.MinePage
import com.example.gra.ui.RegisterPage
import com.example.gra.ui.data.AppDatabase
import com.example.gra.ui.records.ExercisePage
import com.example.gra.ui.records.RecordsPage
import com.example.gra.ui.data.prepopulateFromAssetsIfEmpty
import com.example.gra.ui.records.BodyMeasurePage
import com.example.gra.ui.records.FoodPage
import com.example.gra.ui.theme.GraTheme
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.gra.ui.guidePage.OnboardingBasicPage
import com.example.gra.ui.guidePage.OnboardingIdPage
import com.example.gra.ui.guidePage.OnboardingResultPage
import com.example.gra.ui.records.SleepPage
import com.example.gra.ui.records.WaterPage

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.Q)
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
                    startDestination = "login"
                ) {
                    composable(route = "login") {
                        LoginPage(navController, context = this@MainActivity)
                    }
                    composable("register") {
                        RegisterPage(navController)
                    }

                    // ✅ main 支持可选查询参数 show=friends|groups（也可为空）
                    composable(
                        route = "main?show={show}",
                        arguments = listOf(
                            navArgument("show") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            }
                        )
                    ) { backStackEntry ->
                        val show = backStackEntry.arguments?.getString("show")
                        MainPage(navController = navController, initialShow = show)
                    }

                    composable("tasks") {
                        TasksPage(navController)
                    }
                    composable("records") {
                        RecordsPage(navController)
                    }
                    composable("food_exercise") {
                        FoodExercisePage(navController)
                    }
                    composable("exercise") {
                        ExercisePage(navController)
                    }
                    composable("food") {
                        FoodPage(navController)
                    }
                    composable("body") {
                        BodyMeasurePage(navController)
                    }
                    composable("water") {
                        WaterPage(navController)
                    }
                    composable("sleep") {
                        SleepPage(navController)
                    }
                    composable("mine") {
                        MinePage(navController)
                    }
                    composable("onboarding/id")     { OnboardingIdPage(navController) }
                    composable("onboarding/basic")  { OnboardingBasicPage(navController) }
                    composable("onboarding/result") { OnboardingResultPage(navController) }
                }
            }
        }
    }
}
