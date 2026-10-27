package ro.upt.qraft.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import ro.upt.qraft.data.repository.AuthRepository
import ro.upt.qraft.ui.auth.AuthScreen
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import ro.upt.qraft.ui.inventory.InventoryListScreen
import ro.upt.qraft.ui.location.LocationListScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    authRepository: AuthRepository
) {
    val currentUser by authRepository.currentUser.collectAsState(initial = null)

    NavHost(
        navController = navController,
        startDestination = if (currentUser != null) "inventory_list" else "auth"
    ) {
        composable("auth") {
            AuthScreen(
                onAuthSuccess = {
                    navController.navigate("inventory_list") {
                        popUpTo("auth") { inclusive = true }
                    }
                }
            )
        }

        composable("inventory_list") {
            InventoryListScreen(
                onInventoryClick = { inventoryId ->
                    navController.navigate("location_list/$inventoryId/root")
                },
                onLogoutClick = {
                    navController.navigate("auth") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable("location_list/{inventoryId}/{locationId}") { backStackEntry ->
            val inventoryId = backStackEntry.arguments?.getString("inventoryId")
            val locationId = backStackEntry.arguments?.getString("locationId")?.takeIf { it != "root" }

            LocationListScreen(
                onLocationClick = { clickedLocationId ->
                    navController.navigate("location_list/$inventoryId/$clickedLocationId")
                },
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
