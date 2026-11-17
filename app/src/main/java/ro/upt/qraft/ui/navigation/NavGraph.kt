package ro.upt.qraft.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import ro.upt.qraft.data.repository.AuthRepository
import ro.upt.qraft.ui.auth.AuthScreen
import ro.upt.qraft.ui.inventory.InventoryListScreen
import ro.upt.qraft.ui.item.ItemDetailsScreen
import ro.upt.qraft.ui.item.ItemEditScreen
import ro.upt.qraft.ui.location.LocationEditScreen
import ro.upt.qraft.ui.location.LocationListScreen
import ro.upt.qraft.ui.scanner.ScannerScreen
import ro.upt.qraft.ui.settings.InventorySettingsScreen

/** Rutele aplicatiei. Argumentele optionale sunt transmise ca query (?a=b). */
object Routes {
    const val AUTH = "auth"
    const val INVENTORY_LIST = "inventory_list"
    const val INVENTORY_SETTINGS = "inventory_settings/{inventoryId}"
    const val LOCATION_LIST = "location_list/{inventoryId}/{locationId}"
    const val LOCATION_EDIT = "location_edit/{inventoryId}?parentId={parentId}&locationId={locationId}"
    const val ITEM_DETAILS = "item/{itemId}"
    const val ITEM_EDIT = "item_edit/{inventoryId}?locationId={locationId}&itemId={itemId}"
    const val SCANNER = "scanner"

    fun inventorySettings(inventoryId: String) = "inventory_settings/$inventoryId"
    fun locationList(inventoryId: String, locationId: String? = null) = "location_list/$inventoryId/${locationId ?: "root"}"
    fun locationEdit(inventoryId: String, parentId: String? = null, locationId: String? = null) =
        "location_edit/$inventoryId?parentId=${parentId.orEmpty()}&locationId=${locationId.orEmpty()}"
    fun itemDetails(itemId: String) = "item/$itemId"
    /** [locationId] null = locatia se alege din formular. */
    fun itemEdit(inventoryId: String, locationId: String? = null, itemId: String? = null) =
        "item_edit/$inventoryId?locationId=${locationId.orEmpty()}&itemId=${itemId.orEmpty()}"
}

private fun optionalArg(name: String) = navArgument(name) {
    type = NavType.StringType
    defaultValue = ""
}

@Composable
fun NavGraph(
    navController: NavHostController,
    authRepository: AuthRepository
) {
    // Starea de autentificare e citita sincron o singura data, ca sa nu apara ecranul de login
    // pentru o clipa la pornire cand utilizatorul este deja logat.
    val startDestination = remember {
        if (authRepository.currentUserId != null) Routes.INVENTORY_LIST else Routes.AUTH
    }

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Routes.AUTH) {
            AuthScreen(
                onAuthSuccess = {
                    navController.navigate(Routes.INVENTORY_LIST) {
                        popUpTo(Routes.AUTH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.INVENTORY_LIST) {
            InventoryListScreen(
                onInventoryClick = { navController.navigate(Routes.locationList(it)) },
                onSettingsClick = { navController.navigate(Routes.inventorySettings(it)) },
                onLogoutClick = {
                    navController.navigate(Routes.AUTH) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onScanClick = { navController.navigate(Routes.SCANNER) }
            )
        }

        composable(Routes.INVENTORY_SETTINGS) {
            InventorySettingsScreen(
                onBackClick = { navController.popBackStack() },
                onInventoryDeleted = {
                    navController.popBackStack(Routes.INVENTORY_LIST, inclusive = false)
                }
            )
        }

        composable(Routes.LOCATION_LIST) { backStackEntry ->
            val inventoryId = backStackEntry.arguments?.getString("inventoryId").orEmpty()
            LocationListScreen(
                onLocationClick = { navController.navigate(Routes.locationList(inventoryId, it)) },
                onItemClick = { navController.navigate(Routes.itemDetails(it)) },
                onAddLocation = { parentId -> navController.navigate(Routes.locationEdit(inventoryId, parentId = parentId)) },
                onAddItem = { locationId -> navController.navigate(Routes.itemEdit(inventoryId, locationId)) },
                onEditLocation = { locationId -> navController.navigate(Routes.locationEdit(inventoryId, locationId = locationId)) },
                onBackClick = { navController.popBackStack() },
                onScanClick = { navController.navigate(Routes.SCANNER) }
            )
        }

        composable(
            Routes.LOCATION_EDIT,
            arguments = listOf(optionalArg("parentId"), optionalArg("locationId"))
        ) {
            LocationEditScreen(onDone = { navController.popBackStack() })
        }

        composable(Routes.ITEM_DETAILS) {
            ItemDetailsScreen(
                onBackClick = { navController.popBackStack() },
                onEditClick = { item ->
                    navController.navigate(Routes.itemEdit(item.inventoryId, item.locationId, item.id))
                },
                onLocationClick = { inventoryId, locationId ->
                    navController.navigate(Routes.locationList(inventoryId, locationId))
                }
            )
        }

        composable(
            Routes.ITEM_EDIT,
            arguments = listOf(optionalArg("locationId"), optionalArg("itemId"))
        ) { backStackEntry ->
            val inventoryId = backStackEntry.arguments?.getString("inventoryId").orEmpty()
            // Item creat din radacina inventarului: radacina nu afiseaza itemi, deci
            // dupa salvare deschidem locatia aleasa, ca utilizatorul sa vada itemul creat.
            val createdFromRoot = backStackEntry.arguments?.getString("locationId").isNullOrEmpty()
            ItemEditScreen(
                onCancel = { navController.popBackStack() },
                onSaved = { locationId ->
                    if (createdFromRoot) {
                        navController.navigate(Routes.locationList(inventoryId, locationId)) {
                            popUpTo(Routes.ITEM_EDIT) { inclusive = true }
                        }
                    } else {
                        navController.popBackStack()
                    }
                }
            )
        }

        composable(Routes.SCANNER) {
            ScannerScreen(
                onClose = { navController.popBackStack() },
                // Scannerul e inlocuit de ecranul rezultat, astfel incat "Inapoi" revine unde eram inainte.
                onLocationScanned = { inventoryId, locationId ->
                    navController.navigate(Routes.locationList(inventoryId, locationId)) {
                        popUpTo(Routes.SCANNER) { inclusive = true }
                    }
                },
                onItemScanned = { itemId ->
                    navController.navigate(Routes.itemDetails(itemId)) {
                        popUpTo(Routes.SCANNER) { inclusive = true }
                    }
                }
            )
        }
    }
}
