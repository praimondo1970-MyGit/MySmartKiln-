package com.example.mysmartkiln.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mysmartkiln.R
import com.example.mysmartkiln.data.auth.AuthViewModel
import com.example.mysmartkiln.data.firebase.FirebaseRealtimeManager
import com.example.mysmartkiln.data.local.KilnHttpClient
import com.example.mysmartkiln.data.local.KilnLocalLinkBootstrap
import com.example.mysmartkiln.data.local.OperationModeEspSync
import com.example.mysmartkiln.data.repository.KilnRepository
import com.example.mysmartkiln.ui.components.HomeIndicatorWhite
import com.example.mysmartkiln.ui.components.KilnCommandProgressBanner
import com.example.mysmartkiln.ui.components.ResponsiveBottomNavigation
import com.example.mysmartkiln.ui.components.BottomNavBarBackground
import com.example.mysmartkiln.ui.theme.AppColors
import com.example.mysmartkiln.ui.theme.DialogBrand
import com.example.mysmartkiln.ui.theme.LocalKilnColors
import com.example.mysmartkiln.ui.theme.LocalThemeController
import com.example.mysmartkiln.ui.theme.MySmartKilnTheme
import com.example.mysmartkiln.ui.theme.ThemeController
import com.example.mysmartkiln.ui.theme.ThemeMode
import com.example.mysmartkiln.utils.AppOperationMode
import com.example.mysmartkiln.utils.KilnNetworkConnector
import com.example.mysmartkiln.utils.LocalOperationModeController
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@Composable
fun SettingsScreen(
    onNavigateToKilns: () -> Unit = {},
    onNavigateToPrograms: () -> Unit = {},
    onNavigateToActivity: () -> Unit = {},
    onCreateProgram: () -> Unit = {},
    onNavigateToKilnsWithScan: () -> Unit = {},
    onLogout: () -> Unit = {},
    viewModel: AuthViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    // Estados para controlar el menú "+"
    var isMenuOpen by remember { mutableStateOf(false) }
    val kilnColors = LocalKilnColors.current
    val themeController = LocalThemeController.current
    val themeMode by themeController.mode.collectAsState()
    val operationController = LocalOperationModeController.current
    val operationMode by operationController.mode.collectAsState()

    // Borrador de conexión: al elegir Local/Online se aplica política app al instante;
    // ACTUALIZAR HORNO empuja offlineOnly al ESP.
    var draftConnectionMode by remember(operationMode) {
        mutableStateOf(
            if (operationMode == AppOperationMode.LOCAL) "Local" else "Online"
        )
    }
    val appliedConnectionMode =
        if (operationMode == AppOperationMode.LOCAL) "Local" else "Online"
    val connectionModeDirty = draftConnectionMode != appliedConnectionMode

    var showAccountMenu by remember { mutableStateOf(false) }
    var isUpdatingKilns by remember { mutableStateOf(false) }
    var updatingPhase by remember { mutableStateOf("") }
    var updatingDetail by remember { mutableStateOf("") }
    var updateErrorMessage by remember { mutableStateOf<String?>(null) }
    var updateSuccessMessage by remember { mutableStateOf<String?>(null) }
    /** True si falta empujar offlineOnly al ESP (SoftAP). */
    var pendingEspSync by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val firebaseManager = remember { FirebaseRealtimeManager() }
    val kilnRepository = remember { KilnRepository(context, firebaseManager) }
    val kilnHttpClient = remember { KilnHttpClient(context) }

    var permissionContinuation by remember {
        mutableStateOf<((Boolean) -> Unit)?>(null)
    }
    val nearbyWifiPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionContinuation?.invoke(granted)
        permissionContinuation = null
    }

    suspend fun ensureNearbyWifiPermission(): Boolean {
        if (!KilnNetworkConnector.isSupported) return true
        if (KilnNetworkConnector.hasRequiredPermissions(context)) return true
        return suspendCancellableCoroutine { cont ->
            permissionContinuation = { granted ->
                if (cont.isActive) cont.resume(granted)
            }
            nearbyWifiPermissionLauncher.launch(
                KilnNetworkConnector.requiredRuntimePermission()
            )
            cont.invokeOnCancellation { permissionContinuation = null }
        }
    }

    // Estados para los dropdowns
    var selectedLanguage by remember { mutableStateOf("Español") }
    var selectedUnits by remember { mutableStateOf("Métricas") }
    val selectedTheme = if (themeMode == ThemeMode.ESP) "Oscuro" else "Claro"

    // Estados para mostrar/ocultar dropdowns
    var showLanguageDropdown by remember { mutableStateOf(false) }
    var showUnitsDropdown by remember { mutableStateOf(false) }
    var showThemeDropdown by remember { mutableStateOf(false) }
    var showConnectionDropdown by remember { mutableStateOf(false) }

    val accountEmail = remember {
        viewModel.getCurrentUserEmail()?.takeIf { it.isNotBlank() }
            ?: "praimondo1970@hotmail.com"
    }
    val accountName = remember(accountEmail) {
        FirebaseAuth.getInstance().currentUser?.displayName?.takeIf { it.isNotBlank() }
            ?: accountEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
                .ifBlank { "Usuario" }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = kilnColors.screenBackground)
    ) {
        // Contenido principal con gradiente
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
            ) {
            Spacer(modifier = Modifier.height(80.dp)) // Espacio desde el top

            // Título "Detalles de la cuenta" (fuera de la tarjeta)
            Text(
                text = "Detalles de la cuenta",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = kilnColors.title,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Tarjeta de cuenta → menú (cerrar sesión y más)
            AccountDetailsCard(
                displayName = accountName,
                email = accountEmail,
                onClick = { showAccountMenu = true }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Campo de idioma
            LanguageSelector(
                selectedLanguage = selectedLanguage,
                onLanguageSelected = { selectedLanguage = it },
                showDropdown = showLanguageDropdown,
                onDropdownToggle = { showLanguageDropdown = !showLanguageDropdown }
            )

            // Espaciado dinámico basado en si el dropdown de idioma está abierto
            Spacer(modifier = Modifier.height(if (showLanguageDropdown) 80.dp else 16.dp))

            // Campo de unidades
            UnitsSelector(
                selectedUnits = selectedUnits,
                onUnitsSelected = { selectedUnits = it },
                showDropdown = showUnitsDropdown,
                onDropdownToggle = { showUnitsDropdown = !showUnitsDropdown }
            )

            // Espaciado dinámico basado en si el dropdown de unidades está abierto
            Spacer(modifier = Modifier.height(if (showUnitsDropdown) 80.dp else 16.dp))

            ConnectionModeSelector(
                selectedMode = draftConnectionMode,
                onModeSelected = { label ->
                    draftConnectionMode = label
                    val newMode = if (label == "Local") {
                        AppOperationMode.LOCAL
                    } else {
                        AppOperationMode.ONLINE
                    }
                    if (newMode != operationMode) {
                        // Modo app YA (tarjeta Hornos: LOCAL/REMOTO al instante).
                        // SoftAP / Room en background.
                        operationController.setMode(newMode)
                        scope.launch {
                            OperationModeEspSync.applyAppFlags(
                                kilnRepository = kilnRepository,
                                userId = viewModel.getCurrentNormalizedUserId(),
                                offlineOnly = newMode == AppOperationMode.LOCAL
                            )
                            pendingEspSync = true
                            updateErrorMessage = null
                            updateSuccessMessage = null
                            if (newMode == AppOperationMode.LOCAL) {
                                if (ensureNearbyWifiPermission()) {
                                    // Scope de app: SoftAP sigue aunque salgás de Ajustes.
                                    KilnLocalLinkBootstrap.startLocalMode(
                                        context = context,
                                        kilnRepository = kilnRepository,
                                        kilnHttpClient = kilnHttpClient,
                                        userId = viewModel.getCurrentNormalizedUserId(),
                                        pushKilnInfo = true
                                    )
                                }
                            } else {
                                // No soltar SoftAP aún: ACTUALIZAR HORNO reutiliza
                                // el enlace Local→Online (evita re-diálogo SoftAP).
                                KilnLocalLinkBootstrap.armOnlineHandoffKeepSoftAp()
                            }
                        }
                    }
                },
                showDropdown = showConnectionDropdown,
                onDropdownToggle = { showConnectionDropdown = !showConnectionDropdown }
            )

            Spacer(modifier = Modifier.height(if (showConnectionDropdown) 180.dp else 16.dp))

            // Campo de tema
            ThemeSelector(
                selectedTheme = selectedTheme,
                onThemeSelected = { label ->
                    themeController.setMode(
                        if (label == "Oscuro") ThemeMode.ESP else ThemeMode.CLASSIC
                    )
                },
                showDropdown = showThemeDropdown,
                onDropdownToggle = { showThemeDropdown = !showThemeDropdown }
            )

            Spacer(modifier = Modifier.height(if (showThemeDropdown) 80.dp else 0.dp))

            // Spacer para empujar el botón hacia abajo
            Spacer(modifier = Modifier.weight(1f))

            // Visible si hay cambio de modo o quedó sync ESP pendiente
            if (connectionModeDirty || pendingEspSync) {
                SettingsPrimaryButton(
                    label = "ACTUALIZAR HORNO",
                    onClick = {
                        val newMode = if (draftConnectionMode == "Local") {
                            AppOperationMode.LOCAL
                        } else {
                            AppOperationMode.ONLINE
                        }
                        val offlineOnly = newMode == AppOperationMode.LOCAL
                        // Chip REMOTO/LOCAL en lista al tocar ACTUALIZAR, no al terminar SoftAP.
                        operationController.setMode(newMode)
                        scope.launch {
                            isUpdatingKilns = true
                            updatingPhase = "Actualizando horno…"
                            updatingDetail = "Conectando al WiFi del horno…"
                            try {
                                if (!ensureNearbyWifiPermission()) {
                                    pendingEspSync = true
                                    updateErrorMessage =
                                        "No se pudo conectar al horno para actualizar el modo. " +
                                            "Activá el permiso de WiFi cercano (o ubicación) e intentá de nuevo."
                                    return@launch
                                }
                                // Evitar carrera con bootstrap SoftAP al elegir Local.
                                updatingDetail = if (offlineOnly) {
                                    "Esperando enlace SoftAP…"
                                } else {
                                    "Preparando actualización…"
                                }
                                if (offlineOnly) {
                                    KilnLocalLinkBootstrap.awaitIdle()
                                } else {
                                    // Mantener SoftAP si venimos de Local; solo
                                    // cancelar bootstrap que pudiera empujar offlineOnly.
                                    KilnLocalLinkBootstrap.armOnlineHandoffKeepSoftAp()
                                }
                                updatingDetail = "Enviando configuración al horno…"
                                val userId = viewModel.getCurrentNormalizedUserId()
                                val result = OperationModeEspSync.applyToUserKilns(
                                    context = context,
                                    kilnRepository = kilnRepository,
                                    kilnHttpClient = kilnHttpClient,
                                    userId = userId,
                                    offlineOnly = offlineOnly,
                                    onDetail = { updatingDetail = it }
                                )
                                when {
                                    result.total == 0 -> {
                                        pendingEspSync = true
                                        updateErrorMessage =
                                            "No se pudo actualizar el horno. " +
                                                (result.lastFailureReason
                                                    ?: "No hay hornos registrados en la app.")
                                    }
                                    result.synced > 0 -> {
                                        pendingEspSync = false
                                        val names = result.syncedNames.joinToString(", ")
                                        updateSuccessMessage =
                                            if (result.syncedNames.size == 1) {
                                                "Se actualizó «$names»."
                                            } else {
                                                "Se actualizaron: $names."
                                            }
                                    }
                                    else -> {
                                        pendingEspSync = true
                                        val detail = result.lastFailureReason
                                            ?.takeIf { it.isNotBlank() }
                                        updateErrorMessage =
                                            "No se pudo conectar al horno para actualizar el modo." +
                                                (if (detail != null) " ($detail)" else "") +
                                                " Aceptá el diálogo de WiFi «SmartKiln-…» " +
                                                "(o unite a mano) e intentá de nuevo."
                                    }
                                }
                            } finally {
                                isUpdatingKilns = false
                                updatingPhase = ""
                                updatingDetail = ""
                            }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(180.dp))
            }
        }

        if (showAccountMenu) {
            AccountMenuDialog(
                onDismiss = { showAccountMenu = false },
                onLogout = {
                    showAccountMenu = false
                    viewModel.logout()
                    onLogout()
                }
            )
        }

        // Igual que comandos/programas: banner (no Dialog) para no tapar SoftAP nativo.
        if (isUpdatingKilns) {
            KilnCommandProgressBanner(
                phase = updatingPhase.ifBlank { "Actualizando horno…" },
                detail = updatingDetail.ifBlank { null },
                modifier = Modifier
                    .align(Alignment.Center)
                    .zIndex(8f)
            )
        }

        updateSuccessMessage?.let { message ->
            AlertDialog(
                onDismissRequest = {
                    updateSuccessMessage = null
                    onNavigateToKilns()
                },
                containerColor = kilnColors.cardBackground,
                shape = RoundedCornerShape(16.dp),
                title = {
                    Text(
                        text = "Horno actualizado",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = kilnColors.title
                        )
                    )
                },
                text = {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = kilnColors.body,
                            lineHeight = 22.sp
                        )
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            updateSuccessMessage = null
                            onNavigateToKilns()
                        }
                    ) {
                        Text(
                            text = "Entendido",
                            color = DialogBrand.primaryButton(),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
        }

        updateErrorMessage?.let { message ->
            AlertDialog(
                onDismissRequest = { updateErrorMessage = null },
                containerColor = kilnColors.cardBackground,
                shape = RoundedCornerShape(16.dp),
                title = {
                    Text(
                        text = "No se actualizó el horno",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = kilnColors.title
                        )
                    )
                },
                text = {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = kilnColors.body,
                            lineHeight = 22.sp
                        )
                    )
                },
                confirmButton = {
                    TextButton(onClick = { updateErrorMessage = null }) {
                        Text(
                            text = "Entendido",
                            color = DialogBrand.primaryButton(),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
        }

        // Overlay del menú de agregar - SE SUPERPONE cuando isMenuOpen = true
        if (isMenuOpen) {
            AddMenuScreen(
                onNewKilnClick = {
                    isMenuOpen = false
                    onNavigateToKilnsWithScan() // Navegar a Hornos e iniciar escaneo
                },
                onNewProgramClick = {
                    isMenuOpen = false
                    onCreateProgram() // Navegar a crear programa directamente
                },
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f) // Por encima del contenido pero debajo de la navegación
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
                onKilnsClick = onNavigateToKilns,
                onProgramsClick = onNavigateToPrograms,
                onActivityClick = onNavigateToActivity,
                onSettingsClick = { /* Already on Settings screen */ },
                onPlusClick = { isMenuOpen = !isMenuOpen }, // Toggle menu
                isKilnsActive = false,
                isProgramsActive = false,
                isActivityActive = false,
                isSettingsActive = true,
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
    }
}

// Componente para la tarjeta de detalles de la cuenta
@Composable
private fun AccountDetailsCard(
    displayName: String,
    email: String,
    onClick: () -> Unit
) {
    val kilnColors = LocalKilnColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(kilnColors.cardBackground)
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_user),
                contentDescription = "Usuario",
                modifier = Modifier.size(40.dp),
                tint = kilnColors.title
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = kilnColors.title,
                        fontWeight = FontWeight.Medium,
                        fontSize = 17.sp
                    ),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = kilnColors.body
                    )
                )
            }
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_right),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = kilnColors.muted
            )
        }
    }
}

// Componente selector de idioma
@Composable
private fun LanguageSelector(
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit,
    showDropdown: Boolean,
    onDropdownToggle: () -> Unit
) {
    val languages = listOf("Español", "Inglés")
    
    Column {
        Text(
            text = "Idioma",
            style = MaterialTheme.typography.bodyLarge.copy(
                color = LocalKilnColors.current.title,
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Box {
            // Campo principal
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(LocalKilnColors.current.cardBackground)
                    .border(1.dp, LocalKilnColors.current.cardBorder, RoundedCornerShape(12.dp))
                    .clickable { onDropdownToggle() }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedLanguage,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = LocalKilnColors.current.body
                        )
                    )
                    
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_down),
                        contentDescription = "Dropdown",
                        modifier = Modifier
                            .size(16.dp)
                            .rotate(if (showDropdown) 180f else 0f),
                        tint = LocalKilnColors.current.body
                    )
                }
            }
            
            // Dropdown options
            if (showDropdown) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = 44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(LocalKilnColors.current.cardBackground)
                        .border(1.dp, LocalKilnColors.current.cardBorder, RoundedCornerShape(12.dp))
                        .zIndex(10f) // Aumentar zIndex para que aparezca por encima de todos los elementos
                ) {
                    Column {
                        languages.forEach { language ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onLanguageSelected(language)
                                        onDropdownToggle()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = language,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = LocalKilnColors.current.body
                                    )
                                )
                                
                                // Tilde para opción activa
                                if (language == selectedLanguage) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_check),
                                        contentDescription = "Seleccionado",
                                        tint = LocalKilnColors.current.body,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            if (language != languages.last()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(LocalKilnColors.current.accent.copy(alpha = 0.15f))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Componente selector de unidades
@Composable
private fun UnitsSelector(
    selectedUnits: String,
    onUnitsSelected: (String) -> Unit,
    showDropdown: Boolean,
    onDropdownToggle: () -> Unit
) {
    val units = listOf("Métricas", "Imperial")
    
    Column {
        Text(
            text = "Unidades",
            style = MaterialTheme.typography.bodyLarge.copy(
                color = LocalKilnColors.current.title,
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Box {
            // Campo principal
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(LocalKilnColors.current.cardBackground)
                    .border(1.dp, LocalKilnColors.current.cardBorder, RoundedCornerShape(12.dp))
                    .clickable { onDropdownToggle() }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedUnits,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = LocalKilnColors.current.body
                        )
                    )
                    
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_down),
                        contentDescription = "Dropdown",
                        modifier = Modifier
                            .size(16.dp)
                            .rotate(if (showDropdown) 180f else 0f),
                        tint = LocalKilnColors.current.body
                    )
                }
            }
            
            // Dropdown options
            if (showDropdown) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = 44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(LocalKilnColors.current.cardBackground)
                        .border(1.dp, LocalKilnColors.current.cardBorder, RoundedCornerShape(12.dp))
                        .zIndex(10f) // Aumentar zIndex para que aparezca por encima de todos los elementos
                ) {
                    Column {
                        units.forEach { unit ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onUnitsSelected(unit)
                                        onDropdownToggle()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = unit,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = LocalKilnColors.current.body
                                    )
                                )
                                
                                // Tilde para opción activa
                                if (unit == selectedUnits) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_check),
                                        contentDescription = "Seleccionado",
                                        tint = LocalKilnColors.current.body,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            if (unit != units.last()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(LocalKilnColors.current.accent.copy(alpha = 0.15f))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Componente selector de tema
@Composable
private fun ThemeSelector(
    selectedTheme: String,
    onThemeSelected: (String) -> Unit,
    showDropdown: Boolean,
    onDropdownToggle: () -> Unit
) {
    val themes = listOf("Claro", "Oscuro")
    val kilnColors = LocalKilnColors.current

    Column {
        Text(
            text = "Tema",
            style = MaterialTheme.typography.bodyLarge.copy(
                color = kilnColors.title,
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Box {
            // Campo principal
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(kilnColors.cardBackground)
                    .border(1.dp, kilnColors.cardBorder, RoundedCornerShape(12.dp))
                    .clickable { onDropdownToggle() }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedTheme,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = kilnColors.body
                        )
                    )
                    
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_down),
                        contentDescription = "Dropdown",
                        modifier = Modifier
                            .size(16.dp)
                            .rotate(if (showDropdown) 180f else 0f),
                        tint = kilnColors.body
                    )
                }
            }
            
            // Dropdown options
            if (showDropdown) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = 44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(kilnColors.cardBackground)
                        .border(1.dp, kilnColors.cardBorder, RoundedCornerShape(12.dp))
                        .zIndex(10f) // Aumentar zIndex para que aparezca por encima de todos los elementos
                ) {
                    Column {
                        themes.forEach { theme ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onThemeSelected(theme)
                                        onDropdownToggle()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = theme,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = kilnColors.body
                                    )
                                )
                                
                                // Tilde para opción activa
                                if (theme == selectedTheme) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_check),
                                        contentDescription = "Seleccionado",
                                        tint = kilnColors.accent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            if (theme != themes.last()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(kilnColors.cardBorder)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectionModeSelector(
    selectedMode: String,
    onModeSelected: (String) -> Unit,
    showDropdown: Boolean,
    onDropdownToggle: () -> Unit
) {
    data class ModeOption(val label: String, val description: String)

    val modes = listOf(
        ModeOption(
            label = "Online",
            description = "Usa Firebase cuando haya nube; SoftAP solo si hace falta. El ESP vuelve a STA/Firebase."
        ),
        ModeOption(
            label = "Local",
            description = "Prioriza SoftAP en todos los hornos. El ESP queda solo local (sin STA/nube)."
        )
    )
    val kilnColors = LocalKilnColors.current
    val selected = modes.firstOrNull { it.label == selectedMode } ?: modes.first()

    Column {
        Text(
            text = "Conexión con el horno/datalogger",
            style = MaterialTheme.typography.bodyLarge.copy(
                color = kilnColors.title,
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Box {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(kilnColors.cardBackground)
                    .border(1.dp, kilnColors.cardBorder, RoundedCornerShape(12.dp))
                    .clickable { onDropdownToggle() }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = selected.label,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = kilnColors.title,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            text = selected.description,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = kilnColors.body
                            ),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_down),
                        contentDescription = "Dropdown",
                        modifier = Modifier
                            .size(16.dp)
                            .rotate(if (showDropdown) 180f else 0f),
                        tint = kilnColors.title
                    )
                }
            }

            if (showDropdown) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = 72.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(kilnColors.cardBackground)
                        .border(1.dp, kilnColors.cardBorder, RoundedCornerShape(12.dp))
                        .zIndex(10f)
                ) {
                    Column {
                        modes.forEach { mode ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onModeSelected(mode.label)
                                        onDropdownToggle()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                    Text(
                                        text = mode.label,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = kilnColors.title,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                    Text(
                                        text = mode.description,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = kilnColors.body
                                        ),
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                                if (mode.label == selectedMode) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_check),
                                        contentDescription = "Seleccionado",
                                        tint = kilnColors.title,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .padding(top = 2.dp)
                                    )
                                }
                            }
                            if (mode != modes.last()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(kilnColors.accent.copy(alpha = 0.15f))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Botón principal inferior (ACTUALIZAR HORNO)
@Composable
private fun SettingsPrimaryButton(
    label: String,
    onClick: () -> Unit
) {
    val kilnColors = LocalKilnColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(kilnColors.accent)
            .clickable { onClick() }
            .padding(vertical = 16.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium.copy(
                color = kilnColors.onAccent,
                fontWeight = FontWeight.Medium
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AccountMenuDialog(
    onDismiss: () -> Unit,
    onLogout: () -> Unit
) {
    val kilnColors = LocalKilnColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = kilnColors.cardBackground,
        title = {
            Text(
                text = "Cuenta",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = kilnColors.title,
                    fontWeight = FontWeight.Bold
                )
            )
        },
        text = {
            Column {
                AccountMenuRow(
                    label = "Cerrar sesión",
                    onClick = onLogout
                )
                // Espacio para más opciones (perfil, contraseña, etc.)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cerrar",
                    color = kilnColors.body,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    )
}

@Composable
private fun AccountMenuRow(
    label: String,
    onClick: () -> Unit
) {
    val kilnColors = LocalKilnColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = kilnColors.title,
                fontWeight = FontWeight.Medium
            )
        )
        Icon(
            painter = painterResource(id = R.drawable.ic_arrow_right),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = kilnColors.muted
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val controller = remember { ThemeController(context) }
    val opController = remember {
        com.example.mysmartkiln.utils.OperationModeController(context)
    }
    CompositionLocalProvider(
        LocalThemeController provides controller,
        LocalOperationModeController provides opController
    ) {
        MySmartKilnTheme(themeMode = ThemeMode.CLASSIC) {
            SettingsScreen()
        }
    }
}