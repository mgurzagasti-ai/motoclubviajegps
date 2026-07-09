package com.motoclubgps.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.motoclubgps.ui.screen.CreateTripScreen
import com.motoclubgps.ui.screen.HomeScreen
import com.motoclubgps.ui.screen.JoinTripScreen
import com.motoclubgps.ui.screen.LoginScreen
import com.motoclubgps.ui.screen.MapScreen
import com.motoclubgps.ui.screen.ProfileScreen

@Composable
fun MotoClubGpsApp(appViewModel: AppViewModel = viewModel()) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Route.Home.path) {
        composable(Route.Home.path) {
            HomeScreen(
                viewModel = appViewModel,
                onLogin = { navController.navigate(Route.Login.path) },
                onCreateTrip = { navController.navigate(Route.CreateTrip.path) },
                onJoinTrip = { navController.navigate(Route.JoinTrip.path) },
                onProfile = { navController.navigate(Route.Profile.path) },
            )
        }
        composable(Route.Login.path) {
            LoginScreen(
                viewModel = appViewModel,
                onLoggedIn = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Route.CreateTrip.path) {
            CreateTripScreen(
                viewModel = appViewModel,
                onTripReady = { tripId -> navController.navigate(Route.Map.create(tripId)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Route.JoinTrip.path) {
            JoinTripScreen(
                viewModel = appViewModel,
                onTripReady = { tripId -> navController.navigate(Route.Map.create(tripId)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Route.Map.path,
            arguments = listOf(navArgument("tripId") { type = NavType.StringType }),
        ) { entry ->
            MapScreen(
                viewModel = appViewModel,
                tripId = entry.arguments?.getString("tripId").orEmpty(),
                onBack = { navController.popBackStack(Route.Home.path, inclusive = false) },
            )
        }
        composable(Route.Profile.path) {
            ProfileScreen(
                viewModel = appViewModel,
                onBack = { navController.popBackStack() },
            )
        }
    }
}

private sealed class Route(val path: String) {
    data object Home : Route("home")
    data object Login : Route("login")
    data object CreateTrip : Route("create-trip")
    data object JoinTrip : Route("join-trip")
    data object Profile : Route("profile")
    data object Map : Route("map/{tripId}") {
        fun create(tripId: String) = "map/$tripId"
    }
}
