package com.example.mysmartkiln.navigation

import android.app.Activity
import android.content.Intent
import android.app.Application
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.mysmartkiln.LoginActivity
import com.example.mysmartkiln.ble.BleViewModel
import com.example.mysmartkiln.ble.BleViewModelFactory
import com.example.mysmartkiln.data.local.KilnProgramUploader
import com.example.mysmartkiln.data.local.database.BakingDatabase
import com.example.mysmartkiln.data.repository.ProgramRepository
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.mysmartkiln.ui.viewmodels.ViewModelFactory
import com.example.mysmartkiln.data.auth.AuthViewModel
import com.example.mysmartkiln.ui.screens.ActivityScreen
import com.example.mysmartkiln.ui.screens.AddStageScreen
import com.example.mysmartkiln.ui.screens.BleMonitoringScreen
import com.example.mysmartkiln.ui.screens.ConfigureKilnScreen
import com.example.mysmartkiln.ui.screens.CreateProgramScreen
import com.example.mysmartkiln.ui.screens.DevicesFoundScreen
import com.example.mysmartkiln.ui.screens.ConnectKilnScreen
import com.example.mysmartkiln.ui.screens.KilnDetailScreen
import com.example.mysmartkiln.ui.screens.KilnsScreen
import com.example.mysmartkiln.ui.screens.ProgramsScreen
import com.example.mysmartkiln.ui.screens.ScanKilnScreen
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
    val bleViewModel: BleViewModel = viewModel(
        factory = BleViewModelFactory(context.applicationContext as Application)
    )
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
                bleViewModel = bleViewModel,
                authViewModel = authViewModel,
                onDevicesFound = {
                    // Navegar a pantalla de dispositivos encontrados
                    navController.navigate("devices_found")
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
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onKilnSelected = { macAddress, kilnName ->
                    // Navegar a detalles del horno seleccionado
                    navController.navigate("kiln_detail/$macAddress/$kilnName")
                },
                onNavigateToSelectProgram = { macAddress, kilnName ->
                    navController.navigate("kiln_detail/$macAddress/$kilnName")
                    navController.navigate("select_program/$macAddress/$kilnName")
                },
                onEditKiln = { macAddress ->
                    // Navegar a pantalla de edición de horno
                    navController.navigate("configure_kiln/$macAddress")
                },
                onNavigateToConfigureKiln = { macAddress, deviceName, deviceModel, deviceVolume ->
                    val encodedName = Uri.encode(deviceName)
                    val encodedModel = Uri.encode(deviceModel)
                    val encodedVolume = Uri.encode(deviceVolume)
                    navController.navigate(
                        "configure_kiln/$macAddress" +
                            "?deviceName=$encodedName" +
                            "&deviceModel=$encodedModel" +
                            "&deviceVolume=$encodedVolume"
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
                onNavigateToAddStage = {
                    navController.navigate(Screen.AddStage.route)
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
        
        // Pantalla de Agregar Etapa
        composable(Screen.AddStage.route) {
            AddStageScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // Pantalla de Dispositivos Encontrados
        composable("devices_found") {
            DevicesFoundScreen(
                bleViewModel = bleViewModel,
                onDeviceSelected = { macAddress ->
                    // Navegar a configuración del horno
                    navController.navigate("configure_kiln/$macAddress")
                },
                onNavigateToKilns = {
                    navController.popBackStack(Screen.Kilns.route, false)
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
                onCreateProgram = {
                    navController.navigate(Screen.CreateProgram.createRoute())
                }
            )
        }
        
        // Pantalla de Configuración del Horno
        composable(
            route = "configure_kiln/{macAddress}?deviceName={deviceName}&deviceModel={deviceModel}&deviceVolume={deviceVolume}",
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
                }
            )
        ) { backStackEntry ->
            val macAddress = backStackEntry.arguments?.getString("macAddress") ?: ""
            // Sugerencia de nombre: primero lo que vino del horno (HTTP info),
            // luego el nombre BLE del scan (compatibilidad con flujo legacy),
            // por último vacío para que el usuario lo complete.
            val deviceNameFromWifi = backStackEntry.arguments?.getString("deviceName")
                ?.takeIf { it.isNotBlank() && it != "Mi Kiln Cerámico" }
            val deviceName = deviceNameFromWifi
                ?: bleViewModel.scannedDevices.value.find {
                    it.address.equals(macAddress, ignoreCase = true) ||
                        it.address.replace(":", "").equals(macAddress, ignoreCase = true)
                }?.name
                ?: ""
            val deviceModel = backStackEntry.arguments?.getString("deviceModel")
                ?.takeIf { it.isNotBlank() }
                .orEmpty()
            val deviceVolume = backStackEntry.arguments?.getString("deviceVolume")
                ?.takeIf { it.isNotBlank() && it.toFloatOrNull()?.let { v -> v > 0f } == true }
                .orEmpty()

            ConfigureKilnScreen(
                macAddress = macAddress,
                deviceName = deviceName,
                deviceModel = deviceModel,
                deviceVolume = deviceVolume,
                bleViewModel = bleViewModel,
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
            route = "kiln_detail/{macAddress}/{kilnName}",
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
            val kilnName = backStackEntry.arguments?.getString("kilnName") ?: "Mi Horno"
            
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
                viewModel = kilnDetailViewModel
            )
        }

        composable(
            route = "connect_kiln/{macAddress}/{kilnName}",
            arguments = listOf(
                navArgument("macAddress") { type = NavType.StringType },
                navArgument("kilnName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val macAddress = backStackEntry.arguments?.getString("macAddress") ?: ""
            val kilnName = backStackEntry.arguments?.getString("kilnName") ?: "Mi Horno"
            val kilnDetailRoute = "kiln_detail/$macAddress/$kilnName"
            val parentEntry = navController.getBackStackEntry(kilnDetailRoute)
            val kilnDetailViewModel: com.example.mysmartkiln.ui.viewmodels.KilnDetailViewModel =
                viewModel(
                    viewModelStoreOwner = parentEntry,
                    factory = ViewModelFactory(context)
                )
            ConnectKilnScreen(
                kilnName = kilnName,
                viewModel = kilnDetailViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        
        // Pantalla de Monitoreo BLE (modo continuo)
        composable(
            route = "ble_monitoring/{macAddress}",
            arguments = listOf(
                navArgument("macAddress") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val macAddress = backStackEntry.arguments?.getString("macAddress") ?: ""
            
            BleMonitoringScreen(
                macAddress = macAddress,
                bleViewModel = bleViewModel,
                onProgramFinished = {
                    navController.popBackStack()
                },
                onBack = {
                    navController.popBackStack()
                }
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
            val kilnName = backStackEntry.arguments?.getString("kilnName") ?: "Mi Horno"
            
            // Obtener el MISMO ViewModel que KilnDetailScreen buscando en el backstack
            val kilnDetailRoute = "kiln_detail/$macAddress/$kilnName"
            val parentEntry = navController.getBackStackEntry(kilnDetailRoute)
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


