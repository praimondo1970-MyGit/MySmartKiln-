package com.example.mysmartkiln.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import com.example.mysmartkiln.R
import com.example.mysmartkiln.ui.components.EspBrandHeader
import com.example.mysmartkiln.ui.components.HomeIndicatorWhite
import com.example.mysmartkiln.ui.components.ResponsiveBottomNavigation
import com.example.mysmartkiln.ui.components.BottomNavBarBackground
import com.example.mysmartkiln.ui.theme.DialogBrand
import com.example.mysmartkiln.ui.theme.LocalKilnColors
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.example.mysmartkiln.data.firebase.FirebaseRealtimeManager
import com.example.mysmartkiln.data.repository.KilnRepository
import com.example.mysmartkiln.ui.components.KilnCard
import com.example.mysmartkiln.ui.components.KilnDiscoveryDialog
import com.example.mysmartkiln.ui.components.KilnNoCloudEntryDialog
import androidx.compose.ui.platform.LocalContext
import com.example.mysmartkiln.utils.DeviceKind
import com.example.mysmartkiln.utils.KilnConnectionState
import com.example.mysmartkiln.utils.KilnOfflineSession
import com.example.mysmartkiln.utils.KilnNetworkConnector
import com.example.mysmartkiln.utils.AppOperationMode
import com.example.mysmartkiln.utils.LocalOperationModeController
import com.example.mysmartkiln.data.auth.AuthViewModel
import com.example.mysmartkiln.data.local.KilnHttpClient
import com.example.mysmartkiln.data.local.KilnLocalLinkBootstrap
import com.example.mysmartkiln.data.local.entity.KilnEntity
import androidx.compose.runtime.collectAsState

@Composable
fun KilnsScreen(
    authViewModel: AuthViewModel,
    onNavigateToPrograms: () -> Unit = {},
    onNavigateToActivity: () -> Unit = {},
    onCreateProgram: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onKilnSelected: (String, String, Boolean) -> Unit = { _, _, _ -> }, // mac, name, preferLocal
    onNavigateToSelectProgram: (String, String) -> Unit = { _, _ -> },
    onEditKiln: (String) -> Unit = { _ -> }, // Para navegar a edición de horno (macAddress)
    // MAC limpia, nombre, modelo, volumen y tipo (KILN / DATALOGGER).
    onNavigateToConfigureKiln: (String, String, String, String, String) -> Unit =
        { _, _, _, _, _ -> },
    startScanningAutomatically: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Contexto y repositorio para hornos registrados
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val firebaseManager = remember { FirebaseRealtimeManager() }
    val kilnRepository = remember { KilnRepository(context, firebaseManager) }
    val kilnHttpClient = remember { KilnHttpClient(context) }
    val operationMode by LocalOperationModeController.current.mode.collectAsState()
    
    // Obtener el UID del usuario actual
    val currentUserId = authViewModel.getCurrentNormalizedUserId()
    android.util.Log.d("KilnsScreen", "👤 UID del usuario actual: $currentUserId")
    
    // Obtener hornos registrados
    val registeredKilns = remember { mutableStateOf<List<com.example.mysmartkiln.data.local.entity.KilnEntity>>(emptyList()) }
    
    LaunchedEffect(operationMode, currentUserId) {
        if (operationMode == AppOperationMode.LOCAL) {
            // SoftAP en background (si Ajustes ya arrancó, el bootstrap es idempotente).
            if (KilnNetworkConnector.hasRequiredPermissions(context)) {
                KilnLocalLinkBootstrap.startLocalMode(
                    context = context,
                    kilnRepository = kilnRepository,
                    kilnHttpClient = kilnHttpClient,
                    userId = currentUserId,
                    pushKilnInfo = true
                )
            }
        } else {
            // ONLINE: siempre soltar SoftAP para que el chip sea REMOTO y Firebase funcione.
            KilnLocalLinkBootstrap.onOnlineMode(context)
        }
    }
    
    // Cargar hornos registrados al iniciar la pantalla
    var deviceTypesEnsured by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kilnRepository.getAllKilns().collect { kilns ->
            registeredKilns.value = kilns
            if (!deviceTypesEnsured && kilns.isNotEmpty()) {
                deviceTypesEnsured = true
                kilnRepository.ensureDeviceInfoTypes(kilns)
            }
        }
    }
    
    // Estado para controlar el menú "+"
    var isMenuOpen by remember { mutableStateOf(false) }
    var showDiscoveryDialog by remember { mutableStateOf(false) }
    var pendingNoCloudKiln by remember { mutableStateOf<KilnEntity?>(null) }

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
                if (kilnColors.isEsp) {
                    EspBrandHeader(
                        modifier = Modifier.padding(
                            start = 24.dp,
                            end = 24.dp,
                            top = 48.dp,
                            bottom = 8.dp
                        )
                    )
                    Text(
                        text = "Hornos",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = kilnColors.title,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(
                            start = 24.dp,
                            end = 24.dp,
                            top = 8.dp,
                            bottom = 16.dp
                        )
                    )
                } else {
                    Text(
                        text = "Mis Hornos",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = kilnColors.title,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(
                            start = 24.dp,
                            end = 24.dp,
                            top = 70.dp,
                            bottom = 16.dp
                        )
                    )
                }
                
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
                            val cardConnectionState = KilnConnectionState.OFFLINE
                            key(kiln.macAddress) {
                                KilnCard(
                                    kiln = kiln,
                                    onClick = { telemetryFresh ->
                                        val isLogger = DeviceKind.isDataLogger(kiln)
                                        when {
                                            isLogger ->
                                                onKilnSelected(kiln.macAddress, kiln.name, false)
                                            operationMode == AppOperationMode.LOCAL -> {
                                                KilnOfflineSession.markPreferLocal(kiln.macAddress)
                                                onKilnSelected(kiln.macAddress, kiln.name, true)
                                            }
                                            !telemetryFresh ->
                                                pendingNoCloudKiln = kiln
                                            else ->
                                                onKilnSelected(kiln.macAddress, kiln.name, false)
                                        }
                                    },
                                    onEditClick = { onEditKiln(kiln.macAddress) },
                                    onDeleteClick = {
                                        coroutineScope.launch {
                                            kilnRepository.deleteKiln(kiln.macAddress)
                                        }
                                    },
                                    onLoadProgramClick = {
                                        onNavigateToSelectProgram(kiln.macAddress, kiln.name)
                                    },
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
                }
                
                // Bottom spacing for navigation
                Spacer(modifier = Modifier.height(160.dp))
            }
        } else {
            EmptyKilnsContent(
                onComenzar = { showDiscoveryDialog = true },
                modifier = Modifier.fillMaxSize()
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
                        info.volume,
                        info.type.ifBlank {
                            if (info.model.equals("DATALOGGER", ignoreCase = true)) {
                                "DATALOGGER"
                            } else {
                                "KILN"
                            }
                        }
                    )
                }
            )
        }

        pendingNoCloudKiln?.let { kiln ->
            KilnNoCloudEntryDialog(
                kilnName = kiln.name,
                onConnectLocal = {
                    pendingNoCloudKiln = null
                    KilnOfflineSession.markPreferLocal(kiln.macAddress)
                    onKilnSelected(kiln.macAddress, kiln.name, true)
                },
                onEnterAnyway = {
                    pendingNoCloudKiln = null
                    onKilnSelected(kiln.macAddress, kiln.name, false)
                },
                onDismiss = { pendingNoCloudKiln = null }
            )
        }
    }
}

/**
 * Estado vacío de Hornos — layout ESP: marca, visual horno+WiFi, título, copy SoftAP, CTA.
 */
@Composable
private fun EmptyKilnsContent(
    onComenzar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val kilnColors = LocalKilnColors.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 58.dp, bottom = 200.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        EspBrandHeader(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Start)
        )

        Spacer(modifier = Modifier.weight(0.35f))

        EmptyKilnHeroVisual()

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Agregá tu primer dispositivo",
            style = MaterialTheme.typography.titleLarge.copy(
                color = kilnColors.title,
                fontWeight = FontWeight.Bold
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Conectate al WiFi SmartKiln del equipo para vincularlo. No necesitás internet.",
            style = MaterialTheme.typography.bodyLarge.copy(
                color = kilnColors.muted
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.weight(0.65f))

        Button(
            onClick = onComenzar,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = DialogBrand.primaryButton(),
                contentColor = kilnColors.onAccent
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Comenzar",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
private fun EmptyKilnHeroVisual() {
    val kilnColors = LocalKilnColors.current
    Box(
        modifier = Modifier
            .size(width = 220.dp, height = 168.dp)
            .border(1.dp, kilnColors.cardBorder, RoundedCornerShape(16.dp))
            .background(kilnColors.cardBackground, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.logoicon),
                contentDescription = "SmartKiln",
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Icon(
                painter = painterResource(id = R.drawable.icon_wifi),
                contentDescription = null,
                tint = kilnColors.muted,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
