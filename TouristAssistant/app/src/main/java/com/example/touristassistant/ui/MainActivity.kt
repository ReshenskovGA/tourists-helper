package com.example.touristassistant.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.touristassistant.ui.map.MapScreen
import com.example.touristassistant.ui.places.PlacesScreen
import com.example.touristassistant.ui.routes.RoutesScreen
import com.example.touristassistant.ui.theme.TouristAssistantTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Главная Activity приложения Tourist Assistant.
 * Использует Hilt для внедрения зависимостей и Compose для UI.
 * Управляет навигацией между основными экранами приложения.
 */

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val TAG = "MainActivity"

    /**
     * Вызывается при создании Activity. Инициализирует Compose UI и навигацию.
     *
     * @param savedInstanceState сохраненное состояние Activity
     */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "onCreate started")

        setContent {
            TouristAssistantTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TouristAssistantApp()
                }
            }
        }
        Log.i(TAG, "onCreate completed")
    }

    /**
     * Вызывается при старте Activity.
     */

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart")
    }

    /**
     * Вызывается при возобновлении Activity.
     */

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
    }

    /**
     * Вызывается при приостановке Activity.
     */

    override fun onPause() {
        Log.d(TAG, "onPause")
        super.onPause()
    }

    /**
     * Вызывается при остановке Activity.
     */

    override fun onStop() {
        Log.d(TAG, "onStop")
        super.onStop()
    }

    /**
     * Вызывается при уничтожении Activity.
     */

    override fun onDestroy() {
        Log.i(TAG, "onDestroy")
        super.onDestroy()
    }

}

/**
 * Граф навигации приложения с анимациями переходов.
 * Определяет три основных экрана (map, places, routes) и параметры переходов между ними.
 *
 * @return Composable-компонент приложения
 */

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun TouristAssistantApp() {
    val navController = rememberNavController()
    Log.d("TouristAssistantApp", "Navigation initialized")

    NavHost(
        navController = navController,
        startDestination = "map",
        // Глобальные анимации переходов (по умолчанию fade)
        enterTransition = { fadeIn(animationSpec = tween(300)) },
        exitTransition = { fadeOut(animationSpec = tween(300)) },
        popEnterTransition = { fadeIn(animationSpec = tween(300)) },
        popExitTransition = { fadeOut(animationSpec = tween(300)) }
    ) {
        composable(
            // Экран карты с поддержкой параметров в URL
            route = "map?placeId={placeId}&routeId={routeId}&routeMode={routeMode}",
            arguments = listOf(
                navArgument("placeId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("routeId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("routeMode") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            ),
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) }
        ) { backStackEntry ->
            // Извлечение параметров из deep link
            val placeId = backStackEntry.arguments?.getString("placeId")
            val routeId = backStackEntry.arguments?.getString("routeId")
            val routeMode = backStackEntry.arguments?.getString("routeMode")

            Log.d("TouristAssistantApp", "Navigating to MapScreen with params: placeId=$placeId, routeId=$routeId, routeMode=$routeMode")

            MapScreen(
                onPlacesClick = {
                    Log.i("TouristAssistantApp", "Navigating to places screen")

                    // Навигация на экран мест с горизонтальной анимацией
                    navController.navigate("places")
                },
                onRoutesClick = {
                    Log.i("TouristAssistantApp", "Navigating to routes screen")
                    // Навигация на экран маршрутов с горизонтальной анимацией
                    navController.navigate("routes")
                },
                selectedPlaceId = placeId,
                selectedRouteId = routeId,
                routeMode = routeMode,
                onRouteViewComplete = {
                    Log.d("TouristAssistantApp", "Route view complete, navigating back")
                    navController.popBackStack()
                }
            )
        }

        // Экран мест с кастомной анимацией slide-in справа
        composable(
            "places",
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth },// Начало за правой границей
                    animationSpec = tween(300)
                )
            },
            exitTransition = { fadeOut(animationSpec = tween(300)) },
            // Анимация возврата (pop) - slide-out влево
            popEnterTransition = { fadeIn(animationSpec = tween(300)) },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> -fullWidth }, // Конец за левой границей
                    animationSpec = tween(300)
                )
            }
        ) {
            Log.d("TouristAssistantApp", "Navigating to PlacesScreen")
            PlacesScreen(
                onBackClick = {
                    Log.i("TouristAssistantApp", "Back from places screen")
                    navController.popBackStack() // Возврат на предыдущий экран
                },
                onPlaceClick = { place ->
                    Log.i("TouristAssistantApp", "Place clicked: ${place.name}, navigating to map")
                    navController.navigate("map?placeId=${place.id}") {
                        popUpTo("map") { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(
            "routes",
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(300)
                )
            },
            exitTransition = { fadeOut(animationSpec = tween(300)) },
            popEnterTransition = { fadeIn(animationSpec = tween(300)) },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> -fullWidth },
                    animationSpec = tween(300)
                )
            }
        ) {
            Log.d("TouristAssistantApp", "Navigating to RoutesScreen")
            RoutesScreen(
                onBackClick = {
                    Log.i("TouristAssistantApp", "Back from routes screen")
                    navController.popBackStack()
                },
                onRouteClick = { routeId, mode ->
                    Log.i("TouristAssistantApp", "Route clicked: $routeId, mode: $mode, navigating to map")
                    navController.navigate("map?routeId=$routeId&routeMode=$mode") {
                        popUpTo("map") { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}

/**
 * Предпросмотр главного компонента приложения для Android Studio.
 */

@Preview(showBackground = true)
@Composable
fun PreviewTouristAssistantApp() {
    TouristAssistantTheme {
        TouristAssistantApp()
    }
}