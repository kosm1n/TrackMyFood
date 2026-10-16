package com.cosmin.trackmyfood

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cosmin.trackmyfood.ui.screens.HomeScreen
import com.cosmin.trackmyfood.ui.screens.IngredientListScreen
import com.cosmin.trackmyfood.ui.screens.MealListScreen
import com.cosmin.trackmyfood.ui.screens.ReviewScreen
import com.cosmin.trackmyfood.ui.theme.TrackMyFoodTheme
import com.cosmin.trackmyfood.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TrackMyFoodTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") {
                            HomeScreen(
                                viewModel = viewModel,
                                onNavigateToReview = { navController.navigate("review") },
                                onNavigateToList = { navController.navigate("list") },
                                onNavigateToIngredients = { navController.navigate("ingredients") }
                            )
                        }
                        composable("review") {
                            ReviewScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("list") {
                            MealListScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("ingredients") {
                            IngredientListScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
