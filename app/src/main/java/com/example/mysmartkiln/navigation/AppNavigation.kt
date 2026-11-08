package com.example.mysmartkiln.navigation

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.mysmartkiln.LoginActivity
import com.example.mysmartkiln.ui.viewmodels.ViewModelFactory
import com.example.mysmartkiln.data.auth.AuthViewModel
import com.example.mysmartkiln.ui.screens.ActivityScreen
import com.example.mysmartkiln.ui.screens.ConfigureKilnScreen
import com.example.mysmartkiln.ui.screens.CreateProgramScreen
import com.example.mysmartkiln.ui.screens.KilnDetailScreen
import com.example.mysmartkiln.ui.screens.KilnsScreen
import com.example.mysmartkiln.ui.screens.ProgramsScreen
import com.example.mysmartkiln.ui.screens.SettingsScreen

/**
 * Componente principal de navegación de la aplicación
 * Maneja todas las transiciones entre pantallas usando Compose Navigation
 */
@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val authViewModel: AuthViewModel = viewModel()
    
    NavHost(
        navController = navController,
        startDestination = Screen.Kilns.route
    ) {
        // Pantalla de Hornos (Pantalla Principal)
        composable(
            route = Screen.Kilns.route,
            arguments = listOf(
                navArgument("startScanning") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val startScanning = backStackEntry.arguments?.getBoolean("startScanning") ?: false
            
            KilnsScreen(
                authViewModel = authViewModel,
                onNavigateToPrograms = {
                    navController.navigate(Screen.Programs.route)
                },
                onNavigateToActivity = {
                    navController.navigate(Screen.Activity.route)
                },
                onCreateProgram = {
                    navController.navigate(Screen.CreateProgram.createRoute())
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onKilnSelected = { macAddress, kilnName, preferLocal ->
                    val encodedName = Uri.encode(kilnName)
                    navController.navigate(
                        "kiln_detail/$macAddress/$encodedName?preferLocal=$preferLocal"
                    )
                },
                onNavigateToSelectProgram = { macAddress, kilnName ->
                    val encodedName = Uri.encode(kilnName)
                    navController.navigate("kiln_detail/$macAddress/$encodedName")
                    navController.navigate("select_program/$macAddress/$encodedName")
                },
                onEditKiln = { macAddress ->
                    // Navegar a pantalla de edición de horno
                    navController.navigate("configure_kiln/$macAddress")
                },
                onNavigateToConfigureKiln = { macAddress, deviceName, deviceModel, deviceVolume, deviceType ->
                    val encodedName = Uri.encode(deviceName)
                    val encodedModel = Uri.encode(deviceModel)
                    val encodedVolume = Uri.encode(deviceVolume)
                    val encodedType = Uri.encode(deviceType.ifBlank { "KILN" })
                    navController.navigate(
                        "configure_kiln/$macAddress" +
                            "?deviceName=$encodedName" +
                            "&deviceModel=$encodedModel" +
                            "&deviceVolume=$encodedVolume" +
                            "&deviceType=$encodedType"
                    ) {
                        popUpTo("kilns") { inclusive = false }
                    }
                },
                startScanningAutomatically = startScanning
            )
        }
        
        // Pantalla de Programas
        composable(Screen.Programs.route) {
            ProgramsScreen(
                onNavigateToKilns = {
                    navController.navigate(Screen.Kilns.createRoute())
                },
                onNavigateToActivity = {
                    navController.navigate(Screen.Activity.route)
                },
                onCreateProgram = {
                    navController.navigate(Screen.CreateProgram.createRoute())
                },
                onEditProgram = { programId ->
                    // Modo editar: carga nombre y etapas
                    navController.navigate(Screen.CreateProgram.createRoute(programId, "edit"))
                },
                onDuplicateProgram = { programId ->
                    // Modo duplicar: carga solo etapas, nombre vacío
                    navController.navigate(Screen.CreateProgram.createRoute(programId, "duplicate"))
                },
                onNavigateToKilnsWithScan = {
                    navController.navigate(Screen.Kilns.createRoute(startScanning = true))
                }
            )
        }
        
        // Pantalla de Actividad
        composable(Screen.Activity.route) {
            ActivityScreen(
                onNavigateToKilns = {
                    navController.navigate(Screen.Kilns.createRoute())
                },
                onNavigateToPrograms = {
                    navController.navigate(Screen.Programs.route)
                },
                onCreateProgram = {
                    navController.navigate(Screen.CreateProgram.createRoute())
                },
                onNavigateToKilnsWithScan = {
                    navController.navigate(Screen.Kilns.createRoute(startScanning = true))
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
        
        // Pantalla de Configuración (Settings)
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateToKilns = {
                    navController.navigate(Screen.Kilns.createRoute())
                },
                onNavigateToPrograms = {
                    navController.navigate(Screen.Programs.route)
                },
                onNavigateToActivity = {
                    navController.navigate(Screen.Activity.route)
                },
                onCreateProgram = {
                    navController.navigate(Screen.CreateProgram.createRoute())
                },
                onNavigateToKilnsWithScan = {
                    navController.navigate(Screen.Kilns.createRoute(startScanning = true))
                },
                onLogout = {
                    // Cerrar sesión y volver a LoginActivity
                    val intent = Intent(context, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    context.startActivity(intent)
                    (context as? Activity)?.finish()
                }
            )
        }
        
        // Pantalla de Crear Programa
        composable(
            route = Screen.CreateProgram.route,
            arguments = listOf(
                navArgument("programId") {
                    type = NavType.StringType
                    nullable = false
                },
                navArgument("mode") {
                    type = NavType.StringType
                    nullable = false
                }
            )
        ) { backStackEntry ->
            val programId = backStackEntry.arguments?.getString("programId")
            val mode = backStackEntry.arguments?.getString("mode")
            
            val programIdToEdit = if (programId != "new") programId else null
            val isEditMode = mode == "edit"
            
            CreateProgramScreen(
                programIdToEdit = programIdToEdit,
                isEditMode = isEditMode,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToKilns = {
                    navController.navigate(Screen.Kilns.createRoute())
                },
                onNavigateToPrograms = {
                    navController.navigate(Screen.Programs.route)
                },
                onNavigateToActivity = {
                    navController.navigate(Screen.Activity.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToKilnsWithScan = {
                    navController.navigate(Screen.Kilns.createRoute(startScanning = true))
                }
            )
        }
        
        // Pantalla de Configuración del Horno
        composable(
            route = "configure_kiln/{macAddress}?deviceName={deviceName}&deviceModel={deviceModel}&deviceVolume={deviceVolume}&deviceType={deviceType}",
            arguments = listOf(
                navArgument("macAddress") {
                    type = NavType.StringType
                },
                navArgument("deviceName") {
                    type = NavType.StringType
                    defaultValue = ""
                    nullable = true
                },
                navArgument("deviceModel") {
                    type = NavType.StringType
                    defaultValue = ""
                    nullable = true
                },
                navArgument("deviceVolume") {
                    type = NavType.StringType
                    defaultValue = ""
                    nullable = true
                },
                navArgument("deviceType") {
                    type = NavType.StringType
                    defaultValue = "KILN"
                    nullable = true
                }
            )
        ) { backStackEntry ->
            val macAddress = backStackEntry.arguments?.getString("macAddress") ?: ""
            // Sugerencia de nombre desde GET /api/info (WiFi SoftAP); vacío si no viene.
            val deviceName = backStackEntry.arguments?.getString("deviceName")
                ?.takeIf { it.isNotBlank() && it != "Mi Kiln Cerámico" }
                .orEmpty()
            val deviceModel = backStackEntry.arguments?.getString("deviceModel")
                ?.takeIf { it.isNotBlank() }
                .orEmpty()
            val deviceVolume = backStackEntry.arguments?.getString("deviceVolume")
                ?.takeIf { it.isNotBlank() && it.toFloatOrNull()?.let { v -> v > 0f } == true }
                .orEmpty()
            val deviceType = backStackEntry.arguments?.getString("deviceType")
                ?.takeIf { it.isNotBlank() }
                ?: "KILN"

            ConfigureKilnScreen(
                macAddress = macAddress,
                deviceName = deviceName,
                deviceModel = deviceModel,
                deviceVolume = deviceVolume,
                deviceType = deviceType,
                authViewModel = authViewModel,
                existingKiln = null, // Se buscará automáticamente si existe
                onKilnConfigured = {
                    // Siempre volver a la lista de hornos después de guardar (tanto creación como edición)
                    // El usuario puede seleccionar el horno desde allí si quiere monitorearlo
                    navController.navigate("kilns") {
                        popUpTo("kilns") { inclusive = false }
                    }
                },
                onBack = {
                    navController.popBackStack()
                },
                onNavigateToKilns = {
                    navController.navigate("kilns") {
                        popUpTo("kilns") { inclusive = true }
                    }
                },
                onNavigateToPrograms = {
                    navController.navigate("programs") {
                        popUpTo("kilns") { inclusive = false }
                    }
                },
                onNavigateToActivity = {
                    navController.navigate("activity") {
                        popUpTo("kilns") { inclusive = false }
                    }
                },
                onNavigateToSettings = {
                    navController.navigate("settings") {
                        popUpTo("kilns") { inclusive = false }
                    }
                },
                onCreateProgram = {
                    navController.navigate("create_program") {
                        popUpTo("kilns") { inclusive = false }
                    }
                }
            )
        }
        
        // Pantalla de Detalles del Horno
        composable(
            route = "kiln_detail/{macAddress}/{kilnName}?preferLocal={preferLocal}",
            arguments = listOf(
                navArgument("macAddress") {
                    type = NavType.StringType
                },
                navArgument("kilnName") {
                    type = NavType.StringType
                },
                navArgument("preferLocal") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val macAddress = backStackEntry.arguments?.getString("macAddress") ?: ""
            val kilnName = Uri.decode(
                backStackEntry.arguments?.getString("kilnName") ?: "Mi Horno"
            )
            val preferLocal = backStackEntry.arguments?.getBoolean("preferLocal") ?: false
            
            // Usar el backStackEntry actual como viewModelStoreOwner
            val kilnDetailViewModel: com.example.mysmartkiln.ui.viewmodels.KilnDetailViewModel = 
                viewModel(
                    viewModelStoreOwner = backStackEntry,
                    factory = ViewModelFactory(context)
                )
            
            KilnDetailScreen(
                navController = navController,
                kilnName = kilnName,
                kilnMacAddress = macAddress,
                preferLocal = preferLocal,
                viewModel = kilnDetailViewModel
            )
        }
        
        // Pantalla de Selección de Programa (desde KilnDetailScreen)
        composable(
            route = "select_program/{macAddress}/{kilnName}",
            arguments = listOf(
                navArgument("macAddress") {
                    type = NavType.StringType
                },
                navArgument("kilnName") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val macAddress = backStackEntry.arguments?.getString("macAddress") ?: ""
            val kilnName = Uri.decode(
                backStackEntry.arguments?.getString("kilnName") ?: "Mi Horno"
            )
            
            // Mismo ViewModel que el detalle (debajo en el back stack).
            val parentEntry = navController.previousBackStackEntry
                ?: backStackEntry
            val kilnDetailViewModel: com.example.mysmartkiln.ui.viewmodels.KilnDetailViewModel =
                viewModel(
                    viewModelStoreOwner = parentEntry,
                    factory = ViewModelFactory(context)
                )
            
            com.example.mysmartkiln.ui.screens.SelectProgramScreen(
                onProgramSelected = { programId ->
                    // Debug log
                    android.util.Log.d("AppNavigation", "🎯 onProgramSelected llamado con programId: '$programId'")
                    // Cargar el programa seleccionado usando el ViewModel compartido
                    kilnDetailViewModel.loadSelectedProgram(programId)
                    // Volver a la pantalla de detalle del horno
                    navController.popBackStack()
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToKilns = {
                    navController.navigate(Screen.Kilns.createRoute()) {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                },
                onNavigateToPrograms = {
                    navController.navigate(Screen.Programs.route) {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                },
                onNavigateToActivity = {
                    navController.navigate(Screen.Activity.route) {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route) {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                },
                onCreateProgram = {
                    navController.navigate(Screen.CreateProgram.createRoute())
                },
                onNavigateToKilnsWithScan = {
                    navController.navigate(Screen.Kilns.createRoute(startScanning = true)) {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                }
            )
        }
        
        // Ruta legada: redirige al flujo unificado (detalle + selección con ViewModel compartido)
        composable(
            route = "select_program_from_kilns/{macAddress}/{kilnName}",
            arguments = listOf(
                navArgument("macAddress") { type = NavType.StringType },
                navArgument("kilnName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val macAddress = backStackEntry.arguments?.getString("macAddress") ?: ""
            val kilnName = backStackEntry.arguments?.getString("kilnName") ?: "Mi Horno"
            LaunchedEffect(macAddress, kilnName) {
                navController.navigate("kiln_detail/$macAddress/$kilnName") {
                    popUpTo(Screen.Kilns.createRoute()) { inclusive = false }
                }
                navController.navigate("select_program/$macAddress/$kilnName")
            }
        }
    }
}


