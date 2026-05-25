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
import com.example.mysmartkiln.ui.theme.AppColors
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
import com.example.mysmartkiln.data.local.KilnConnectionVerifier
import com.example.mysmartkiln.data.local.KilnHttpClient
import com.example.mysmartkiln.ui.components.KilnCard
import com.example.mysmartkiln.ui.components.KilnSearchInProgressOverlay
import com.example.mysmartkiln.ui.components.KilnWifiLinkDialog
import com.example.mysmartkiln.ui.components.ScrollGradient
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import com.example.mysmartkiln.ble.HybridConnectionState
import com.example.mysmartkiln.utils.WifiUtils
import com.example.mysmartkiln.data.auth.AuthViewModel
import com.example.mysmartkiln.utils.UserIdUtils

private const val WIFI_KILN_SEARCH_TIMEOUT_MS = 25_000L
private const val WIFI_KILN_SEARCH_POLL_MS = 2_000L
private const val WIFI_KILN_SEARCH_MIN_DISPLAY_MS = 1_500L

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
    onNavigateToConfigureKiln: (String, String) -> Unit = { _, _ -> }, // MAC limpia, nombre sugerido
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
    var showWifiLinkDialog by remember { mutableStateOf(false) }
    var showWifiLinkTimeoutAlert by remember { mutableStateOf(false) }
    var isWifiKilnSearchActive by remember { mutableStateOf(false) }
    var wifiSearchHandled by remember { mutableStateOf(false) }
    val kilnHttpClient = remember { KilnHttpClient(context.applicationContext) }

    // Sondeo continuo mientras el overlay está activo (no depende solo de ON_RESUME)
    LaunchedEffect(isWifiKilnSearchActive) {
        if (!isWifiKilnSearchActive || wifiSearchHandled) return@LaunchedEffect

        val searchStartedAt = System.currentTimeMillis()
        val deadline = searchStartedAt + WIFI_KILN_SEARCH_TIMEOUT_MS
        var foundMac: String? = null
        var foundName: String? = null

        delay(1500)

        while (System.currentTimeMillis() < deadline && foundMac == null && isWifiKilnSearchActive) {
            val result = KilnConnectionVerifier.verify(context, kilnHttpClient)
            if (result.isSuccess) {
                val (mac, name) = result.getOrThrow()
                foundMac = mac
                foundName = name
            } else {
                delay(WIFI_KILN_SEARCH_POLL_MS)
            }
        }

        if (foundMac != null && foundName != null) {
            val elapsed = System.currentTimeMillis() - searchStartedAt
            if (elapsed < WIFI_KILN_SEARCH_MIN_DISPLAY_MS) {
                delay(WIFI_KILN_SEARCH_MIN_DISPLAY_MS - elapsed)
            }
            wifiSearchHandled = true
            isWifiKilnSearchActive = false
            onNavigateToConfigureKiln(foundMac, foundName)
        } else if (!wifiSearchHandled && isWifiKilnSearchActive) {
            isWifiKilnSearchActive = false
            showWifiLinkTimeoutAlert = true
        }
    }
    
    fun beginAddKilnWifiFlow() {
        isMenuOpen = false
        wifiSearchHandled = false
        showWifiLinkDialog = true
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
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = AppColors.Primary100)
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
                        color = Color(0xFF1E3A8A),
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
                        backgroundColor = AppColors.Primary100,
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
                        color = Color(0xFF1E3A8A),
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
                                    color = Color(0xFF1E3A8A)
                                ),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "proceso puede durar hasta 10 segundos",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = Color(0xFF1E3A8A)
                                ),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "en completarse.",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = Color(0xFF1E3A8A)
                                ),
                                textAlign = TextAlign.Center
                            )
                        }
                        noKilnsFound -> {
                            Text(
                                text = "Asegúrate que el horno al que intentas conectarte esté encendido y dentro del alcance del bluetooth",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = Color(0xFF1E3A8A)
                                ),
                                textAlign = TextAlign.Center
                            )
                        }
                        else -> {
                            Text(
                                text = "Cuando estés listo presiona comenzar",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = Color(0xFF1E3A8A)
                                ),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "para detectar los hornos disponibles",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = Color(0xFF1E3A8A)
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
                                    else -> showWifiLinkDialog = true
                                }
                            }
                        },
                        enabled = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = AppColors.Primary700,
                            contentColor = Color.White,
                            disabledContainerColor = AppColors.Primary700,
                            disabledContentColor = Color.White
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                    ) {
                        if (isScanning || isStillProcessing) {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
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

        // Búsqueda del horno por WiFi (al volver de Ajustes) — misma pantalla que escaneo BT
        if (isWifiKilnSearchActive) {
            KilnSearchInProgressOverlay(
                title = "Buscando horno...",
                subtitleLine1 = "Por favor ten paciencia ya que este",
                subtitleLine2 = "proceso puede durar hasta 15 segundos",
                subtitleLine3 = "en completarse.",
                modifier = Modifier.zIndex(4f),
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
            // Navigation Bar Background - Full width with precise curved top
            Box(
                modifier = Modifier
                    .align(alignment = Alignment.TopStart)
                    .offset(x = 0.dp, y = 60.dp)
                    .fillMaxWidth()
                    .requiredHeight(height = 141.dp)
                   .background(
                       color = Color(0xFF003D99),
                       shape = object : androidx.compose.ui.graphics.Shape {
                            override fun createOutline(
                                size: androidx.compose.ui.geometry.Size,
                                layoutDirection: androidx.compose.ui.unit.LayoutDirection,
                                density: androidx.compose.ui.unit.Density
                            ): androidx.compose.ui.graphics.Outline {
                                val path = androidx.compose.ui.graphics.Path().apply {
                                    val width = size.width
                                    val height = size.height
                                    
                                    // Convert dp to pixels for precise positioning
                                    val densityValue = density.density
                                    
                                    // Start from left edge at y=34 (relative to top of shape)
                                    val startY = 34f * densityValue
                                    val curveStartY = startY
                                    
                                    // Move to start position (left edge, y=34)
                                    moveTo(0f, curveStartY)
                                    
                                    // Left side straight line (from 0,34 to 147,34)
                                    lineTo(width * (147f/390f), curveStartY)
                                    
                                    // First curve: C158.046,34 166.586,43.8745 173.422,52.5508
                                    cubicTo(
                                        width * (158.046f/390f), curveStartY,
                                        width * (166.586f/390f), curveStartY + (9.8745f * densityValue),
                                        width * (173.422f/390f), curveStartY + (18.5508f * densityValue)
                                    )
                                    
                                    // Second curve: C178.915,59.5232 187.435,64 197,64
                                    cubicTo(
                                        width * (178.915f/390f), curveStartY + (25.5232f * densityValue),
                                        width * (187.435f/390f), curveStartY + (30f * densityValue),
                                        width * (197f/390f), curveStartY + (30f * densityValue)
                                    )
                                    
                                    // Third curve: C206.565,64 215.085,59.5232 220.578,52.5508
                                    cubicTo(
                                        width * (206.565f/390f), curveStartY + (30f * densityValue),
                                        width * (215.085f/390f), curveStartY + (25.5232f * densityValue),
                                        width * (220.578f/390f), curveStartY + (18.5508f * densityValue)
                                    )
                                    
                                    // Fourth curve: C227.414,43.8745 235.954,34 247,34
                                    cubicTo(
                                        width * (227.414f/390f), curveStartY + (9.8745f * densityValue),
                                        width * (235.954f/390f), curveStartY,
                                        width * (247f/390f), curveStartY
                                    )
                                    
                                    // Right side straight line (from 247,34 to right edge, y=34)
                                    lineTo(width, curveStartY)
                                    
                                    // Continue to right edge at y=54
                                    lineTo(width, curveStartY + (20f * densityValue))
                                    
                                    // Bottom right corner
                                    lineTo(width, height)
                                    
                                    // Bottom edge
                                    lineTo(0f, height)
                                    
                                    // Left side back to start
                                    lineTo(0f, curveStartY + (20f * densityValue))
                                    
                                    // Close the path back to start
                                    close()
                                }
                                return androidx.compose.ui.graphics.Outline.Generic(path)
                            }
                        }
                    )
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

        if (showWifiLinkDialog) {
            KilnWifiLinkDialog(
                onDismiss = { showWifiLinkDialog = false },
                onOpenWifiSettings = {
                    showWifiLinkDialog = false
                    wifiSearchHandled = false
                    isWifiKilnSearchActive = true
                }
            )
        }

        if (showWifiLinkTimeoutAlert) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showWifiLinkTimeoutAlert = false },
                title = {
                    Text(
                        text = "No se detectó el horno",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E3A8A)
                        )
                    )
                },
                text = {
                    Text(
                        text = "No pudimos conectar con el horno en 15 segundos.\n\n" +
                            "• ¿Te conectaste a la red SmartKiln-XXXX?\n" +
                            "• ¿El horno está encendido y cerca?\n\n" +
                            "Pulsa Comenzar para intentar de nuevo.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF475569)
                        )
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showWifiLinkTimeoutAlert = false }) {
                        Text("Entendido", color = AppColors.Primary700)
                    }
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
