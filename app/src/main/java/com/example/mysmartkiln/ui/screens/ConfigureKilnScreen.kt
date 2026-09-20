package com.example.mysmartkiln.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.mysmartkiln.R
import com.example.mysmartkiln.ui.components.HomeIndicatorWhite
import com.example.mysmartkiln.ui.components.ResponsiveBottomNavigation
import com.example.mysmartkiln.ui.components.BottomNavBarBackground
import com.example.mysmartkiln.ui.screens.AddMenuScreen
import com.example.mysmartkiln.ui.theme.AppTypes
import com.example.mysmartkiln.ui.theme.DialogBrand
import com.example.mysmartkiln.ui.theme.LocalKilnColors
import com.example.mysmartkiln.ui.theme.MySmartKilnTheme
import com.example.mysmartkiln.data.repository.KilnRepository
import com.example.mysmartkiln.data.local.entity.KilnEntity
import com.example.mysmartkiln.data.auth.AuthViewModel
import com.example.mysmartkiln.utils.UserIdUtils
import com.example.mysmartkiln.data.firebase.FirebaseRealtimeManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.lazy.LazyColumn
import com.example.mysmartkiln.data.local.KilnHttpClient
import com.example.mysmartkiln.data.local.KilnInfoJsonBuilder
import com.example.mysmartkiln.data.local.SoftApDeviceType
import com.example.mysmartkiln.ui.components.KilnSendingCommandDialog
import com.example.mysmartkiln.ui.components.WifiScanDialog
import com.example.mysmartkiln.utils.KilnNetworkConnector
import com.example.mysmartkiln.utils.WifiUtils
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ConfigureKilnScreen(
    macAddress: String,
    deviceName: String,
    authViewModel: AuthViewModel,
    deviceModel: String = "",
    deviceVolume: String = "",
    deviceType: String = "KILN",
    existingKiln: KilnEntity? = null, // Opcional: horno existente para modo edición
    onKilnConfigured: () -> Unit = {},
    onBack: () -> Unit = {},
    onNavigateToKilns: () -> Unit = {},
    onNavigateToPrograms: () -> Unit = {},
    onNavigateToActivity: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onCreateProgram: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isDataLogger by remember {
        mutableStateOf(
            deviceType.equals("DATALOGGER", ignoreCase = true) ||
                deviceModel.equals("DATALOGGER", ignoreCase = true) ||
                (existingKiln != null &&
                    existingKiln.volume.let { it.isBlank() || it == "0" || it == "0.0" } &&
                    existingKiln.manufacturer.uppercase().let { m ->
                        m == "K" || m == "S" || m == "TYPE K" || m == "TYPE S" ||
                            m == "TIPO K" || m == "TIPO S"
                    })
        )
    }

    // Pre-cargar datos:
    //   - Si el horno ya existe en Room (modo edición), usamos esos valores.
    //   - Si no, prellenamos con lo que devolvió GET /api/info al alta
    //     (deviceName / deviceModel / deviceVolume). Para volumen ignoramos
    //     el "0.0" del firmware sin configurar para que el campo aparezca
    //     vacío y obligue al usuario a completarlo.
    var kilnName by remember { mutableStateOf(existingKiln?.name ?: deviceName) }
    var manufacturer by remember {
        mutableStateOf(
            if (isDataLogger) "" else (existingKiln?.manufacturer ?: deviceModel)
        )
    }
    var thermocoupleType by remember {
        mutableStateOf(
            when {
                !isDataLogger -> ""
                existingKiln != null -> {
                    val m = existingKiln.manufacturer.uppercase()
                    when {
                        m == "S" || m.contains("TIPO S") || m.contains("TYPE S") -> "S"
                        m == "K" || m.contains("TIPO K") || m.contains("TYPE K") -> "K"
                        else -> ""
                    }
                }
                else -> ""
            }
        )
    }
    var volume by remember {
        mutableStateOf(
            if (isDataLogger) {
                "0"
            } else {
                existingKiln?.volume
                    ?: deviceVolume.takeIf {
                        it.isNotBlank() && it.toFloatOrNull()?.let { v -> v > 0f } == true
                    }.orEmpty()
            }
        )
    }
    var wifiSSID by remember { mutableStateOf("") }
    var wifiPassword by remember { mutableStateOf("") }
    var isSendingToKiln by remember { mutableStateOf(false) }
    var sendingPhase by remember { mutableStateOf("") }
    var sendingDetail by remember { mutableStateOf("") }
    var isMenuOpen by remember { mutableStateOf(false) }
    var showValidationAlert by remember { mutableStateOf(false) }
    var showSaveErrorAlert by remember { mutableStateOf(false) }
    var saveErrorMessage by remember { mutableStateOf("") }
    var showWifiScanDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    // Variable de estado explícita para modo edición (no inferir de campos llenos)
    var isEditMode by remember { mutableStateOf(existingKiln != null) }
    
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val kilnColors = LocalKilnColors.current
    val fieldShape = RoundedCornerShape(12.dp)
    
    // Repository para manejo de datos
    val firebaseManager = remember { FirebaseRealtimeManager() }
    val kilnRepository = remember { KilnRepository(context, firebaseManager) }
    val kilnHttpClient = remember { KilnHttpClient(context.applicationContext) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Limpiar MAC address (eliminar ":") para coincidir con ESP32
    val cleanMacAddress = remember(macAddress, existingKiln) { 
        existingKiln?.macAddress ?: macAddress.replace(":", "") 
    }
    
    // Si no se proporciona existingKiln, intentar buscarlo por macAddress
    LaunchedEffect(macAddress) {
        if (existingKiln == null && macAddress.isNotBlank()) {
            try {
                val foundKiln = kilnRepository.getKilnByMacAddress(cleanMacAddress)
                if (foundKiln != null) {
                    // Pre-cargar datos del horno encontrado y marcar como modo edición
                    kilnName = foundKiln.name
                    val m = foundKiln.manufacturer.uppercase()
                    val looksLikeLogger =
                        (foundKiln.volume.isBlank() || foundKiln.volume == "0" ||
                            foundKiln.volume == "0.0") &&
                            (m == "K" || m == "S" || m == "TYPE K" || m == "TYPE S" ||
                                m == "TIPO K" || m == "TIPO S")
                    if (looksLikeLogger) {
                        isDataLogger = true
                        thermocoupleType = if (m.contains("S")) "S" else "K"
                        manufacturer = ""
                        volume = "0"
                    } else {
                        manufacturer = foundKiln.manufacturer
                        volume = foundKiln.volume
                    }
                    isEditMode = true // Establecer explícitamente que estamos en modo edición
                    android.util.Log.d("ConfigureKilnScreen", "✅ Horno encontrado y datos pre-cargados: ${foundKiln.name} - Modo: Edición")
                } else {
                    // No se encontró horno, es modo creación
                    isEditMode = false
                    android.util.Log.d("ConfigureKilnScreen", "📝 No se encontró horno existente - Modo: Creación")
                }
            } catch (e: Exception) {
                android.util.Log.d("ConfigureKilnScreen", "No se encontró horno existente: ${e.message}")
                isEditMode = false
            }
        } else if (existingKiln != null) {
            // Si se proporcionó existingKiln, estamos en modo edición
            isEditMode = true
        }
    }
    
    fun isKilnFormValid(): Boolean {
        if (kilnName.trim().isBlank()) return false
        return if (isDataLogger) {
            thermocoupleType == "K" || thermocoupleType == "S"
        } else {
            manufacturer.trim().isNotBlank() && volume.trim().isNotBlank()
        }
    }

    suspend fun saveKiln() {
        sendingPhase = when {
            isEditMode && isDataLogger -> "Actualizando dispositivo…"
            isEditMode -> "Actualizando horno…"
            isDataLogger -> "Configurando dispositivo…"
            else -> "Configurando horno…"
        }
        sendingDetail = "Guardando en la app…"
        isSendingToKiln = true

        val name = kilnName.trim()
        val brand = if (isDataLogger) thermocoupleType else manufacturer.trim()
        val vol = if (isDataLogger) "0" else volume.trim()
        val modelForEsp = if (isDataLogger) "DATALOGGER" else brand
        val currentUserId = authViewModel.getCurrentNormalizedUserId()

        val finalBleMacAddress = if (isEditMode && existingKiln != null) {
            existingKiln.bleMacAddress
        } else {
            formatBleMacAddress(macAddress.ifBlank { cleanMacAddress })
        }

        val kilnEntity = KilnEntity(
            macAddress = cleanMacAddress,
            bleMacAddress = finalBleMacAddress,
            name = name,
            manufacturer = brand,
            volume = vol,
            userId = currentUserId,
            deviceType = if (isDataLogger) "DATALOGGER" else "KILN"
        )

        val homeSsid = wifiSSID.trim()
        val homePassword = wifiPassword.trim()
        val shouldSendHomeWifi = homeSsid.isNotBlank() && homePassword.isNotBlank()
        val kilnInfoJson = KilnInfoJsonBuilder.build(
            name = name,
            model = modelForEsp,
            volume = vol,
            userId = currentUserId,
            type = if (isDataLogger) "DATALOGGER" else "KILN",
            homeWifiSsid = homeSsid.takeIf { shouldSendHomeWifi },
            homeWifiPassword = homePassword.takeIf { shouldSendHomeWifi },
            thermocoupleType = thermocoupleType.takeIf { isDataLogger && it.isNotBlank() }
        )

        try {
            withContext(Dispatchers.IO) {
                kilnRepository.saveKiln(kilnEntity)
            }

            val espSynced = syncKilnInfoToEsp(
                context = context,
                kilnMacNoColons = cleanMacAddress,
                kilnHttpClient = kilnHttpClient,
                kilnInfoJson = kilnInfoJson,
                deviceType = if (isDataLogger) SoftApDeviceType.DATALOGGER else SoftApDeviceType.KILN,
                onDetail = { sendingDetail = it }
            )

            if (espSynced) {
                onKilnConfigured()
            } else {
                val detail = if (!KilnNetworkConnector.isSupported) {
                    "Conéctate manualmente al WiFi del horno (SmartKiln-XXXXXXXX) y pulsa «Actualizar horno» de nuevo."
                } else if (!KilnNetworkConnector.hasRequiredPermissions(context)) {
                    "Activa el permiso de WiFi cercano en Ajustes de la app e inténtalo otra vez."
                } else {
                    "Acepta el diálogo de Android para unirte al AP del horno e inténtalo otra vez."
                }
                saveErrorMessage =
                    "Los datos quedaron guardados en la app, pero no se enviaron al ESP.\n\n$detail"
                showSaveErrorAlert = true
                android.util.Log.w("ConfigureKilnScreen", "kiln-info no enviado al ESP")
            }
        } finally {
            isSendingToKiln = false
            sendingPhase = ""
            sendingDetail = ""
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = kilnColors.screenBackground)
            .pointerInput(Unit) {
                detectTapGestures {
                    focusManager.clearFocus()
                }
            }
    ) {
        // Contenido principal
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 10.dp)
        ) {
            // Título
            Text(
                text = "Por favor completa la siguiente información",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = kilnColors.title,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 70.dp, bottom = 32.dp),
                lineHeight = 1.2.em
            )
            
            // Campos de entrada con scroll
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 29.dp, end = 24.dp)
            ) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(21.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                // Campo: Nombre
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isDataLogger) "Nombre del dispositivo" else "Nombre del horno",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = kilnColors.title,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shape = fieldShape)
                            .background(color = kilnColors.cardBackground)
                            .border(
                                border = BorderStroke(1.dp, kilnColors.cardBorder),
                                shape = fieldShape
                            )
                            .padding(horizontal = 16.dp, vertical = 11.dp)
                    ) {
                        BasicTextField(
                            value = kilnName,
                            onValueChange = { kilnName = it },
                            textStyle = TextStyle(
                                color = kilnColors.title,
                                fontSize = 18.sp
                            ),
                            cursorBrush = SolidColor(kilnColors.title),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
                    }

                    if (isDataLogger) {
                    item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Tipo de termocupla",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = kilnColors.title,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        listOf("K", "S").forEach { type ->
                            val selected = thermocoupleType == type
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(shape = fieldShape)
                                    .background(
                                        if (selected) DialogBrand.primaryButton()
                                        else kilnColors.cardBackground
                                    )
                                    .border(
                                        border = BorderStroke(
                                            1.dp,
                                            if (selected) DialogBrand.primaryButton()
                                            else kilnColors.cardBorder
                                        ),
                                        shape = fieldShape
                                    )
                                    .clickable { thermocoupleType = type }
                                    .padding(vertical = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Tipo $type",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = if (selected) kilnColors.onAccent
                                        else kilnColors.title,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }
                    }
                }
                    }
                    } else {
                    item {
                // Campo: Fabricante/Marca
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Fabricante/Marca",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = kilnColors.title,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shape = fieldShape)
                            .background(color = kilnColors.cardBackground)
                            .border(
                                border = BorderStroke(1.dp, kilnColors.cardBorder),
                                shape = fieldShape
                            )
                            .padding(horizontal = 16.dp, vertical = 11.dp)
                    ) {
                        BasicTextField(
                            value = manufacturer,
                            onValueChange = { manufacturer = it },
                            textStyle = TextStyle(
                                color = kilnColors.title,
                                fontSize = 18.sp
                            ),
                            cursorBrush = SolidColor(kilnColors.title),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
                    }

                    item {
                // Campo: Volumen
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Volumen (lts)",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = kilnColors.title,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shape = fieldShape)
                            .background(color = kilnColors.cardBackground)
                            .border(
                                border = BorderStroke(1.dp, kilnColors.cardBorder),
                                shape = fieldShape
                            )
                            .padding(horizontal = 16.dp, vertical = 11.dp)
                    ) {
                        BasicTextField(
                            value = volume,
                            onValueChange = { volume = it },
                            textStyle = TextStyle(
                                color = kilnColors.title,
                                fontSize = 18.sp
                            ),
                            cursorBrush = SolidColor(kilnColors.title),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }
                    }
                    }
                    
                    // Mostrar campos de WiFi siempre (tanto en creación como en edición)
                    // En modo edición, son opcionales para actualizar la configuración WiFi del horno
                    item {
                // Red de casa (opcional): no usar la red SoftAP del dispositivo
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Red Wifi (acceso a internet)",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = kilnColors.title,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = if (isDataLogger) {
                            "Opcional. El dispositivo funciona sin internet; solo si quieres sincronización remota."
                        } else {
                            "Opcional. El horno funciona sin internet; solo si quieres sincronización remota."
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = kilnColors.muted
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shape = fieldShape)
                            .background(color = kilnColors.cardBackground)
                            .border(
                                border = BorderStroke(1.dp, kilnColors.cardBorder),
                                shape = fieldShape
                            )
                            .padding(horizontal = 16.dp, vertical = 11.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicTextField(
                                value = wifiSSID,
                                onValueChange = { wifiSSID = it },
                                textStyle = TextStyle(
                                    color = kilnColors.title,
                                    fontSize = 18.sp
                                ),
                                cursorBrush = SolidColor(kilnColors.title),
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                decorationBox = { innerTextField ->
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        if (wifiSSID.isEmpty()) {
                                            Text(
                                                text = "Selecciona o escribe tu red de casa",
                                                style = TextStyle(
                                                    color = kilnColors.muted,
                                                    fontSize = 14.sp
                                                )
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                            IconButton(
                                onClick = { showWifiScanDialog = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.icon_wifi),
                                    contentDescription = "Seleccionar red WiFi",
                                    tint = kilnColors.title,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
                    }
                    
                    item {
                // Campo: Contraseña WiFi (opcional)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Contraseña WiFi (opcional)",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = kilnColors.title,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shape = fieldShape)
                            .background(color = kilnColors.cardBackground)
                            .border(
                                border = BorderStroke(1.dp, kilnColors.cardBorder),
                                shape = fieldShape
                            )
                            .padding(horizontal = 16.dp, vertical = 11.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.icon_lock),
                                contentDescription = "Contraseña",
                                tint = kilnColors.title,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            BasicTextField(
                                value = wifiPassword,
                                onValueChange = { wifiPassword = it },
                                textStyle = TextStyle(
                                    color = kilnColors.title,
                                    fontSize = 18.sp
                                ),
                                cursorBrush = SolidColor(kilnColors.title),
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                            )
                        }
                    }
                }
                    }

                    // Espacio adicional al final
                    item {
                        Spacer(modifier = Modifier.height(40.dp))
                    }
                }
            }
            
            // Espacio antes del botón
            Spacer(modifier = Modifier.height(24.dp))

            // Botón Agregar/Actualizar Horno (usando estética del botón Comenzar)
            // Usar la variable de estado explícita isEditMode
            androidx.compose.material3.Button(
                onClick = {
                    if (!isKilnFormValid()) {
                        showValidationAlert = true
                        return@Button
                    }
                    if (isSendingToKiln) return@Button
                    coroutineScope.launch {
                        try {
                            saveKiln()
                        } catch (e: Exception) {
                            isSendingToKiln = false
                            sendingPhase = ""
                            sendingDetail = ""
                            android.util.Log.e("ConfigureKilnScreen", "💥 Error al guardar: ${e.message}", e)
                            saveErrorMessage = e.message ?: "Error desconocido"
                            showSaveErrorAlert = true
                        }
                    }
                },
                enabled = true, // Siempre habilitado visualmente
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 24.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = DialogBrand.primaryButton(),
                    contentColor = kilnColors.onAccent
                    // Removidos los colores de disabled para mantener siempre el mismo aspecto
                ),
                shape = fieldShape
            ) {
                Text(
                    text = when {
                        isEditMode && isDataLogger -> "Actualizar dispositivo"
                        isEditMode -> "Actualizar horno"
                        isDataLogger -> "Agregar dispositivo"
                        else -> "Agregar Horno"
                    },
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            // Bottom spacing for navigation
            Spacer(modifier = Modifier.height(160.dp))
        }
        
        // Overlay del menú de agregar - SE SUPERPONE cuando isMenuOpen = true
        if (isMenuOpen) {
            AddMenuScreen(
                onNewKilnClick = {
                    isMenuOpen = false
                    // Volver a la pantalla de hornos para escanear
                    onBack()
                },
                onNewProgramClick = {
                    isMenuOpen = false
                    onCreateProgram()
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
                .zIndex(2f)
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
                onSettingsClick = onNavigateToSettings,
                onPlusClick = { isMenuOpen = !isMenuOpen },
                isKilnsActive = false,
                isProgramsActive = false,
                isActivityActive = false,
                isSettingsActive = false,
                showFab = true,
                isMenuOpen = isMenuOpen,
                modifier = Modifier.align(alignment = Alignment.BottomStart)
            )
        }
        
        // Home Indicator
        HomeIndicatorWhite(
            modifier = Modifier
                .align(alignment = Alignment.TopStart)
                .offset(x = 0.dp, y = 810.dp)
        )
        
        if (showValidationAlert) {
            AlertDialog(
                onDismissRequest = { showValidationAlert = false },
                title = {
                    Text(
                        text = if (isDataLogger) "Datos del dispositivo" else "Datos del horno",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                text = {
                    Text(
                        text = if (isDataLogger) {
                            "Completa el nombre y el tipo de termocupla (K o S). " +
                                "La red WiFi de casa es opcional (funciona sin internet)."
                        } else {
                            "Completa nombre, fabricante/marca y volumen. " +
                                "La red WiFi de casa es opcional (el horno funciona sin internet)."
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showValidationAlert = false }) {
                        Text("Entendido")
                    }
                }
            )
        }

        if (showSaveErrorAlert) {
            AlertDialog(
                onDismissRequest = { showSaveErrorAlert = false },
                title = {
                    Text(
                        text = "Horno no sincronizado",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                text = {
                    Text(
                        text = saveErrorMessage,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showSaveErrorAlert = false }) {
                        Text("Entendido")
                    }
                }
            )
        }
        
        // WiFi Scan Dialog
        if (showWifiScanDialog) {
            WifiScanDialog(
                onNetworkSelected = { selectedSSID ->
                    wifiSSID = selectedSSID
                    showWifiScanDialog = false
                },
                onDismiss = {
                    showWifiScanDialog = false
                }
            )
        }

        if (isSendingToKiln) {
            KilnSendingCommandDialog(
                phase = sendingPhase.ifBlank { "Enviando al horno…" },
                detail = sendingDetail.ifBlank { null }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        )
    }
}

/** Convierte MAC sin ":" (Firebase/API) a formato AA:BB:CC:DD:EE:FF */
private fun formatBleMacAddress(mac: String): String {
    if (mac.contains(":")) return mac.uppercase()
    val clean = mac.replace(":", "").uppercase()
    if (clean.length != 12) return mac
    return clean.chunked(2).joinToString(":")
}

/**
 * Envía kiln-info al ESP: ping directo o conexión automática al SoftAP.
 */
private suspend fun syncKilnInfoToEsp(
    context: android.content.Context,
    kilnMacNoColons: String,
    kilnHttpClient: KilnHttpClient,
    kilnInfoJson: String,
    deviceType: SoftApDeviceType = SoftApDeviceType.KILN,
    onDetail: (String) -> Unit
): Boolean {
    suspend fun tryUpload(): Boolean {
        onDetail("Enviando configuración al dispositivo…")
        if (!kilnHttpClient.ping()) return false
        return kilnHttpClient.uploadKilnInfo(kilnInfoJson)
            .onSuccess {
                android.util.Log.d("ConfigureKilnScreen", "kiln-info enviado por HTTP")
            }
            .onFailure {
                android.util.Log.w("ConfigureKilnScreen", "kiln-info HTTP: ${it.message}")
            }
            .isSuccess
    }

    var shouldMarkCooldown = false
    try {
        onDetail("Comprobando conexión con el dispositivo…")
        if (tryUpload()) {
            // HTTP local: el ESP puede soltar SoftAP unos segundos.
            shouldMarkCooldown = true
            return true
        }

        if (!KilnNetworkConnector.isSupported) return false
        if (!KilnNetworkConnector.hasRequiredPermissions(context)) return false

        val ssid = KilnNetworkConnector.ssidFromMac(kilnMacNoColons, deviceType) ?: return false
        val pass = KilnNetworkConnector.passphraseFromMac(kilnMacNoColons) ?: return false

        onDetail("Conectando al WiFi del dispositivo…")
        android.util.Log.d("ConfigureKilnScreen", "Auto-conexión a $ssid para kiln-info")
        if (KilnNetworkConnector.connect(context, ssid, pass) == null) {
            return false
        }
        shouldMarkCooldown = true
        delay(500)
        return tryUpload()
    } finally {
        KilnNetworkConnector.release(context, markHandoffCooldown = shouldMarkCooldown)
    }
}

@Preview(showBackground = true)
@Composable
fun ConfigureKilnScreenPreview() {
    MySmartKilnTheme {
        ConfigureKilnScreen(
            macAddress = "A4:CF:12:88:56:E8",
            deviceName = "SmartKiln - Ceramica",
            authViewModel = null as AuthViewModel? ?: return@MySmartKilnTheme
        )
    }
}