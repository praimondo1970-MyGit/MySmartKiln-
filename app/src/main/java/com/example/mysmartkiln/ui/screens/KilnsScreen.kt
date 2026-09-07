package com.example.mysmartkiln.ui.screens

import android.Manifest
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.mysmartkiln.R
import com.example.mysmartkiln.ui.components.AppPrimaryButton
import com.example.mysmartkiln.ui.components.HomeIndicatorWhite
import com.example.mysmartkiln.ui.components.Plusbtn
import com.example.mysmartkiln.ui.components.ResponsiveBottomNavigation
import com.example.mysmartkiln.ui.components.BottomNavBarBackground
import com.example.mysmartkiln.ui.theme.AppColors
import com.example.mysmartkiln.ui.theme.LocalKilnColors
import com.example.mysmartkiln.ui.theme.MySmartKilnTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.example.mysmartkiln.data.firebase.FirebaseRealtimeManager
import com.example.mysmartkiln.data.repository.KilnRepository
import com.example.mysmartkiln.ui.components.KilnCard
import com.example.mysmartkiln.ui.components.KilnDiscoveryDialog
import com.example.mysmartkiln.ui.components.KilnSearchInProgressOverlay
import com.example.mysmartkiln.ui.components.ScrollGradient
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import com.example.mysmartkiln.ble.HybridConnectionState
import com.example.mysmartkiln.utils.WifiUtils
import com.example.mysmartkiln.data.auth.AuthViewModel
import com.example.mysmartkiln.utils.UserIdUtils

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun KilnsScreen(
    bleViewModel: com.example.mysmartkiln.ble.BleViewModel,
    authViewModel: AuthViewModel,
    onDevicesFound: () -> Unit = {},
    onNavigateToPrograms: () -> Unit = {},
    onNavigateToActivity: () -> Unit = {},
    onCreateProgram: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onKilnSelected: (String, String) -> Unit = { _, _ -> }, // Para navegar a monitoreo del horno (macAddress, kilnName)
    onNavigateToSelectProgram: (String, String) -> Unit = { _, _ -> },
    onEditKiln: (String) -> Unit = { _ -> }, // Para navegar a edición de horno (macAddress)
    // MAC limpia, nombre, modelo y volumen sugeridos por el horno (vienen
    // de GET /api/info al terminar la auto-conexión durante el alta).
    onNavigateToConfigureKiln: (String, String, String, String) -> Unit = { _, _, _, _ -> },
    startScanningAutomatically: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isScanning by bleViewModel.isScanning.collectAsState()
    val scannedDevices by bleViewModel.scannedDevices.collectAsState()
    val lastError by bleViewModel.lastError.collectAsState()
    
    var scanCompleted by remember { mutableStateOf(false) }
    var hasStartedScanning by remember { mutableStateOf(false) }
    
    // Contexto y repositorio para hornos registrados
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val firebaseManager = remember { FirebaseRealtimeManager() }
    val kilnRepository = remember { KilnRepository(context, firebaseManager) }
    
    // Obtener el UID del usuario actual
    val currentUserId = authViewModel.getCurrentNormalizedUserId()
    android.util.Log.d("KilnsScreen", "👤 UID del usuario actual: $currentUserId")
    
    // Obtener hornos registrados
    val registeredKilns = remember { mutableStateOf<List<com.example.mysmartkiln.data.local.entity.KilnEntity>>(emptyList()) }
    
    val isOnKilnWifi = remember { mutableStateOf(WifiUtils.isOnSmartKilnNetwork(context)) }
    LaunchedEffect(Unit) {
        while (true) {
            isOnKilnWifi.value = WifiUtils.isOnSmartKilnNetwork(context)
            delay(3000)
        }
    }

    // Permisos BLE (solo para escaneo al agregar horno por Bluetooth)
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        listOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    } else {
        listOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }
    
    val permissionsState = rememberMultiplePermissionsState(permissions)
    
    // Cargar hornos registrados al iniciar la pantalla
    LaunchedEffect(Unit) {
        kilnRepository.getAllKilns().collect { kilns ->
            registeredKilns.value = kilns
        }
    }
    
    // Sistema híbrido maneja automáticamente la lectura de temperatura
    // No necesitamos el ciclo anterior - el HybridConnectionManager lo hace
    
    // Determinar si no se encontraron hornos después de escanear
    val noKilnsFound = !isScanning && scanCompleted && scannedDevices.isEmpty() && hasStartedScanning
    
    // Determinar si aún está procesando (escaneo terminó pero aún no completó)
    val isStillProcessing = !isScanning && hasStartedScanning && !scanCompleted
    
    // Estado para controlar el menú "+"
    var isMenuOpen by remember { mutableStateOf(false) }
    var showDiscoveryDialog by remember { mutableStateOf(false) }

    // El nuevo flujo de alta encapsula el escaneo, la auto-conexión y el GET
    // /api/info dentro de KilnDiscoveryDialog, así que ya no necesitamos el
    // overlay de polling ni la alerta de timeout que existían antes.
    fun beginAddKilnWifiFlow() {
        isMenuOpen = false
        showDiscoveryDialog = true
    }

    // Desde menú "+" u otras pantallas: mismas instrucciones que el primer horno
    LaunchedEffect(startScanningAutomatically) {
        if (startScanningAutomatically) {
            beginAddKilnWifiFlow()
        }
    }
    
    // Detectar cuando termina el escaneo y navegar si hay dispositivos
    LaunchedEffect(isScanning, scannedDevices, hasStartedScanning) {
        if (!isScanning && hasStartedScanning && !scanCompleted) {
            scanCompleted = true
            
            // Navegar inmediatamente si se encontraron dispositivos
            if (scannedDevices.isNotEmpty()) {
                onDevicesFound()
            }
        }
    }
    val kilnColors = LocalKilnColors.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = kilnColors.screenBackground)
    ) {
        // Contenido principal - Mostrar hornos registrados o estado vacío
        // Si hay hornos registrados, mostrar lista siempre (mismo formato que ProgramsScreen)
        if (registeredKilns.value.isNotEmpty()) {
            Column(
                modifier = Modifier.padding(vertical = 10.dp)
            ) {
                // Título para hornos registrados (posición igual a "Actividad" y "Programas")
                Text(
                    text = "Mis Hornos",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = kilnColors.title,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 70.dp, bottom = 16.dp)
                )
                
                // Lista de hornos registrados (mismo padding que ProgramsScreen)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 29.dp, end = 24.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = registeredKilns.value,
                            key = { kiln -> kiln.macAddress }
                        ) { kiln ->
                            val cardConnectionState = if (isOnKilnWifi.value) {
                                HybridConnectionState.WIFI_ONLINE
                            } else {
                                HybridConnectionState.OFFLINE
                            }
                            key(kiln.macAddress, isOnKilnWifi.value) {
                                KilnCard(
                                    kiln = kiln,
                                    onClick = { onKilnSelected(kiln.macAddress, kiln.name) },
                                    onEditClick = { onEditKiln(kiln.macAddress) },
                                    onDeleteClick = {
                                        coroutineScope.launch {
                                            kilnRepository.deleteKiln(kiln.macAddress)
                                        }
                                    },
                                    onLoadProgramClick = {
                                        onNavigateToSelectProgram(kiln.macAddress, kiln.name)
                                    },
                                    bleTemperature = null,
                                    isBleConnected = false,
                                    bleStatus = null,
                                    connectionState = cardConnectionState,
                                    currentUserId = currentUserId,
                                    programName = null
                                )
                            }
                        }
                        
                        // Espacio adicional al final para el gradiente
                        item {
                            Spacer(modifier = Modifier.height(40.dp))
                        }
                    }
                    
                    // Gradiente en la parte inferior
                    ScrollGradient(
                        height = 40.dp,
                        backgroundColor = kilnColors.screenBackground,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
                
                // Bottom spacing for navigation
                Spacer(modifier = Modifier.height(160.dp))
            }
        } else {
            // Estado vacío o escaneando - Usar Box para posicionamiento absoluto
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // Imagen posicionada en el centro-superior
                Image(
                    painter = painterResource(
                        id = if (noKilnsFound) R.drawable.img_no_kiln_found else R.drawable.img_sin_hornos
                    ),
                    contentDescription = if (noKilnsFound) "No se encontraron hornos" else "Sin hornos detectados",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = (-165).dp)
                        .size(200.dp)
                )
                
                // Error message
                lastError?.let { error ->
                    androidx.compose.material3.Card(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(top = 100.dp),
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = androidx.compose.ui.graphics.Color(0xFFFFEBEE)
                        )
                    ) {
                        androidx.compose.material3.Text(
                            text = error,
                            color = androidx.compose.ui.graphics.Color(0xFFC62828),
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                // TÍTULO posicionado a 400dp desde el fondo (todos los estados)
                Text(
                    text = when {
                        isScanning || isStillProcessing -> "Escaneando..."
                        noKilnsFound -> "Lo siento, no pudimos encontrar hornos disponibles"
                        scannedDevices.isNotEmpty() -> "Escaneando..."
                        else -> "¿Listo para comenzar?"
                    },
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = kilnColors.title,
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 400.dp)
                )
                
                // SUBTÍTULO posicionado según estado: 341dp (listo), 325dp (escaneando), 325dp (no encontrados)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 24.dp)
                        .padding(bottom = when {
                            noKilnsFound -> 325.dp  // Pantalla 4: No encontrados
                            isScanning || isStillProcessing -> 325.dp  // Pantallas 2 y 3: Escaneando
                            else -> 341.dp  // Pantalla 1: Listo para comenzar
                        }),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when {
                        isScanning || isStillProcessing -> {
                            Text(
                                text = "Por favor ten paciencia ya que este",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = kilnColors.title
                                ),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "proceso puede durar hasta 10 segundos",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = kilnColors.title
                                ),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "en completarse.",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = kilnColors.title
                                ),
                                textAlign = TextAlign.Center
                            )
                        }
                        noKilnsFound -> {
                            Text(
                                text = "Asegúrate que el horno al que intentas conectarte esté encendido y dentro del alcance del bluetooth",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = kilnColors.title
                                ),
                                textAlign = TextAlign.Center
                            )
                        }
                        else -> {
                            Text(
                                text = "Cuando estés listo presiona comenzar",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = kilnColors.title
                                ),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "para detectar los hornos disponibles",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = kilnColors.title
                                ),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                
                // Botón posicionado absolutamente a 200dp desde el fondo
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 200.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Start Button
                    androidx.compose.material3.Button(
                        onClick = {
                            if (!isScanning && !isStillProcessing) {
                                // Si faltan permisos los pedimos primero, sin
                                // importar en qué sub-estado estemos. El label
                                // "Activar Permisos" tiene que coincidir con la
                                // acción del clic.
                                if (!permissionsState.allPermissionsGranted) {
                                    permissionsState.launchMultiplePermissionRequest()
                                    return@Button
                                }
                                when {
                                    noKilnsFound -> {
                                        scanCompleted = false
                                        hasStartedScanning = true
                                        bleViewModel.scanDevices(5000)
                                    }
                                    else -> showDiscoveryDialog = true
                                }
                            }
                        },
                        enabled = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = kilnColors.accent,
                            contentColor = kilnColors.onAccent,
                            disabledContainerColor = kilnColors.accent,
                            disabledContentColor = kilnColors.onAccent
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                    ) {
                        if (isScanning || isStillProcessing) {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = kilnColors.onAccent,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = when {
                                    !permissionsState.allPermissionsGranted -> "Activar Permisos"
                                    noKilnsFound -> "Intentar de nuevo"
                                    else -> "Comenzar"
                                },
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        }
        
        // Overlay BLE (menú +) — misma estética que búsqueda WiFi
        if ((isScanning || isStillProcessing) && registeredKilns.value.isNotEmpty()) {
            KilnSearchInProgressOverlay(
                title = "Escaneando...",
                subtitleLine1 = "Por favor ten paciencia ya que este",
                subtitleLine2 = "proceso puede durar hasta 10 segundos",
                subtitleLine3 = "en completarse.",
                modifier = Modifier.zIndex(3f),
                bottomNav = {
                    ResponsiveBottomNavigation(
                        onKilnsClick = { },
                        onProgramsClick = onNavigateToPrograms,
                        onActivityClick = onNavigateToActivity,
                        onSettingsClick = onNavigateToSettings,
                        onPlusClick = { isMenuOpen = !isMenuOpen },
                        isKilnsActive = true,
                        isProgramsActive = false,
                        isActivityActive = false,
                        isSettingsActive = false,
                        showFab = true,
                        isMenuOpen = isMenuOpen,
                        modifier = Modifier.align(alignment = Alignment.BottomStart)
                    )
                },
                homeIndicator = {
                    HomeIndicatorWhite(
                        modifier = Modifier
                            .align(alignment = Alignment.TopStart)
                            .offset(x = 0.dp, y = 810.dp)
                    )
                }
            )
        }

        // Overlay del menú de agregar - SE SUPERPONE cuando isMenuOpen = true
        if (isMenuOpen) {
            AddMenuScreen(
                onNewKilnClick = {
                    beginAddKilnWifiFlow()
                },
                onNewProgramClick = {
                    isMenuOpen = false
                    onCreateProgram()
                },
                onDismiss = {
                    // Cerrar el menú cuando se hace click en el fondo
                    isMenuOpen = false
                },
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f)
            )
        }
        
        // Bottom Navigation Bar
        Box(
            modifier = Modifier
                .align(alignment = Alignment.BottomStart)
                .fillMaxWidth()
                .requiredHeight(height = 200.dp)
                .zIndex(2f) // Por encima del overlay del menú
        ) {
            // Navigation Bar Background
            BottomNavBarBackground(
                modifier = Modifier
                    .align(alignment = Alignment.TopStart)
                    .offset(x = 0.dp, y = 60.dp)
            )
        
        // Responsive Bottom Navigation Bar
        ResponsiveBottomNavigation(
            onKilnsClick = { /* Already on Kilns screen */ },
            onProgramsClick = onNavigateToPrograms,
                onActivityClick = onNavigateToActivity,
            onSettingsClick = onNavigateToSettings,
                onPlusClick = { isMenuOpen = !isMenuOpen }, // Toggle menu
            isKilnsActive = true,
            isProgramsActive = false,
                isActivityActive = false,
                isSettingsActive = false,
                showFab = true, // Mostrar el botón FAB
                isMenuOpen = isMenuOpen, // Pass state to ResponsiveBottomNavigation
            modifier = Modifier.align(alignment = Alignment.BottomStart)
        )
        }
        
        // Home Indicator
        HomeIndicatorWhite(
            modifier = Modifier
                .align(alignment = Alignment.TopStart)
                .offset(x = 0.dp, y = 810.dp)
        )

        if (showDiscoveryDialog) {
            KilnDiscoveryDialog(
                onDismiss = { showDiscoveryDialog = false },
                onKilnSelected = { info ->
                    // El AP del horno ya está unido al proceso vía
                    // KilnNetworkConnector. ConfigureKilnScreen va a poder
                    // hacer un POST /api/kiln-info al terminar el alta sin
                    // necesidad de volver a conectarse manualmente.
                    showDiscoveryDialog = false
                    onNavigateToConfigureKiln(
                        info.macAddress.replace(":", "").uppercase(),
                        info.name,
                        info.model,
                        info.volume
                    )
                }
            )
        }
    }
}

// Preview comentado temporalmente - requiere Context para BleViewModel
// @Preview(widthDp = 390, heightDp = 844)
// @Composable
// private fun KilnsScreenPreview() {
//     MySmartKilnTheme {
//         KilnsScreen()
//     }
// }
