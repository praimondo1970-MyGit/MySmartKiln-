package com.example.mysmartkiln.ui.components

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.mysmartkiln.data.local.KilnHttpClient
import com.example.mysmartkiln.data.local.KilnInfoDto
import com.example.mysmartkiln.data.local.SoftApDeviceType
import com.example.mysmartkiln.ui.theme.DialogBrand
import com.example.mysmartkiln.ui.theme.LocalKilnColors
import com.example.mysmartkiln.utils.KilnAccessPoint
import com.example.mysmartkiln.utils.KilnNetworkConnector
import com.example.mysmartkiln.utils.WifiUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Diálogo de alta de horno por descubrimiento WiFi.
 */
@Composable
fun KilnDiscoveryDialog(
    onDismiss: () -> Unit,
    onKilnSelected: (KilnInfoDto) -> Unit
) {
    val kilnColors = LocalKilnColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val httpClient = remember { KilnHttpClient(context.applicationContext) }

    fun discoveryPermissionsGranted(): Boolean {
        val nearbyOrLegacy = !KilnNetworkConnector.isSupported ||
            KilnNetworkConnector.hasRequiredPermissions(context)
        val locationOk = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return nearbyOrLegacy && locationOk
    }

    var hasPermission by remember { mutableStateOf(discoveryPermissionsGranted()) }
    var isScanning by remember { mutableStateOf(false) }
    var accessPoints by remember { mutableStateOf<List<KilnAccessPoint>>(emptyList()) }
    var connectingTo by remember { mutableStateOf<KilnAccessPoint?>(null) }
    var connectingPhase by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var hasScanned by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasPermission = discoveryPermissionsGranted()
    }

    fun requestDiscoveryPermissions() {
        val needed = buildList {
            add(android.Manifest.permission.ACCESS_FINE_LOCATION)
            if (KilnNetworkConnector.isSupported) {
                add(KilnNetworkConnector.requiredRuntimePermission())
            }
        }.distinct().toTypedArray()
        permissionLauncher.launch(needed)
    }

    suspend fun runScan() {
        isScanning = true
        errorMessage = null
        accessPoints = WifiUtils.getSmartKilnAccessPoints(context)
        delay(1200)
        var found = WifiUtils.getSmartKilnAccessPoints(context)

        // SoftAP conectado pero SSID oculto (<unknown ssid>) / ausente en scan.
        if (found.isEmpty()) {
            try {
                com.example.mysmartkiln.utils.KilnWifiNetworkBinder.bindToWifi(context)
                if (httpClient.ping()) {
                    val apInfo = httpClient.getApInfo().getOrNull()
                    val ssid = apInfo?.ssid?.takeIf { it.isNotBlank() }
                        ?: "SmartKiln (conectado)"
                    val type = WifiUtils.deviceTypeFromSsid(ssid).let { t ->
                        if (t == SoftApDeviceType.UNKNOWN) SoftApDeviceType.KILN else t
                    }
                    found = listOf(
                        KilnAccessPoint(
                            ssid = ssid,
                            bssid = "",
                            signalLevel = 0,
                            deviceType = type,
                            alreadyConnected = true
                        )
                    )
                }
            } finally {
                // No dejar el proceso atado al SoftAP (rompe Firebase en la lista).
                com.example.mysmartkiln.utils.KilnWifiNetworkBinder.unbind(context)
            }
        }

        accessPoints = found
        hasScanned = true
        isScanning = false
    }

    LaunchedEffect(hasPermission) {
        if (!hasPermission) {
            requestDiscoveryPermissions()
        } else {
            runScan()
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (connectingTo == null) onDismiss()
        },
        containerColor = kilnColors.cardBackground,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = "Buscar horno",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = kilnColors.title
                )
            )
        },
        text = {
            Column {
                if (!hasPermission) {
                    Text(
                        text = "Para descubrir hornos necesitamos permiso de ubicación (leer el nombre de la WiFi) y de dispositivos cercanos.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = kilnColors.body
                        )
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { requestDiscoveryPermissions() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DialogBrand.primaryButton(),
                            contentColor = kilnColors.onAccent
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Permitir")
                    }
                    return@Column
                }

                connectingTo?.let { ap ->
                    ConnectingBlock(ssid = ap.ssid, phase = connectingPhase)
                    return@Column
                }

                Text(
                    text = "Encendé el horno y, si hace falta, conectá el teléfono a su WiFi SmartKiln. Buscamos redes cercanas y también la conexión actual.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = kilnColors.body
                    )
                )
                Spacer(Modifier.height(12.dp))

                when {
                    isScanning && accessPoints.isEmpty() -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = DialogBrand.primaryButton()
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = "Buscando hornos…",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = kilnColors.title
                                )
                            )
                        }
                    }

                    accessPoints.isNotEmpty() -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 220.dp)
                        ) {
                            items(
                                accessPoints,
                                key = { "${it.ssid}|${it.bssid}|${it.alreadyConnected}" }
                            ) { ap ->
                                AccessPointRow(
                                    accessPoint = ap,
                                    enabled = connectingTo == null,
                                    onClick = {
                                        scope.launch {
                                            connectToKiln(
                                                accessPoint = ap,
                                                httpClient = httpClient,
                                                context = context,
                                                onPhase = { connectingPhase = it },
                                                onConnectingStarted = {
                                                    connectingTo = ap
                                                    errorMessage = null
                                                },
                                                onConnectingFinished = { connectingTo = null },
                                                onSuccess = onKilnSelected,
                                                onError = { msg ->
                                                    errorMessage = msg
                                                    connectingTo = null
                                                    KilnNetworkConnector.release(context)
                                                }
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }

                    hasScanned -> {
                        Text(
                            text = "No detectamos ningún horno. Si ya estás en la WiFi SmartKiln, tocá «Buscar de nuevo» y concedé ubicación si Android lo pide.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = kilnColors.muted
                            )
                        )
                    }
                }

                errorMessage?.let { msg ->
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = kilnColors.danger.copy(alpha = 0.18f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = kilnColors.danger,
                                fontSize = 13.sp
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (connectingTo == null && hasPermission) {
                TextButton(
                    onClick = {
                        scope.launch { runScan() }
                    },
                    enabled = !isScanning
                ) {
                    Text(
                        text = if (isScanning) "Buscando…" else "Buscar de nuevo",
                        color = DialogBrand.primaryButton(),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        dismissButton = {
            if (connectingTo == null) {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = "Cancelar",
                        color = kilnColors.muted
                    )
                }
            }
        }
    )
}

@Composable
private fun AccessPointRow(
    accessPoint: KilnAccessPoint,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val kilnColors = LocalKilnColors.current
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (kilnColors.isEsp) kilnColors.track else Color(0xFFF1F5F9),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .then(
                if (enabled) Modifier.clickable(onClick = onClick) else Modifier
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = accessPoint.ssid,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = kilnColors.title
                    )
                )
                Text(
                    text = if (accessPoint.alreadyConnected) {
                        "Ya conectado · Señal: ${accessPoint.signalLevel} dBm"
                    } else {
                        "Señal: ${accessPoint.signalLevel} dBm"
                    },
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (accessPoint.alreadyConnected) {
                            kilnColors.success
                        } else {
                            kilnColors.muted
                        }
                    )
                )
            }
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(
                        color = signalColor(accessPoint.signalLevel),
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
            )
        }
    }
}

@Composable
private fun ConnectingBlock(ssid: String, phase: String) {
    val kilnColors = LocalKilnColors.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(56.dp),
            color = DialogBrand.primaryButton(),
            strokeWidth = 4.dp
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = "Conectando a $ssid",
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                color = kilnColors.title
            )
        )
        if (phase.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = phase,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = kilnColors.muted
                )
            )
        }
    }
}

private fun signalColor(dbm: Int): Color = when {
    dbm >= -55 -> Color(0xFF16A34A)
    dbm >= -70 -> Color(0xFFCA8A04)
    else -> Color(0xFFDC2626)
}

/**
 * Coordina la conexión completa a un AP detectado: WifiNetworkSpecifier +
 * GET /api/info. La función es separada del componable para mantener el
 * cuerpo del diálogo legible y poder testearla en aislamiento eventualmente.
 */
private suspend fun connectToKiln(
    accessPoint: KilnAccessPoint,
    httpClient: KilnHttpClient,
    context: android.content.Context,
    onPhase: (String) -> Unit,
    onConnectingStarted: () -> Unit,
    onConnectingFinished: () -> Unit,
    onSuccess: (KilnInfoDto) -> Unit,
    onError: (String) -> Unit
) {
    onConnectingStarted()
    try {
        val alreadyOnThisAp = accessPoint.alreadyConnected ||
            WifiUtils.getCurrentWifiSSID(context)
                ?.equals(accessPoint.ssid, ignoreCase = true) == true

        if (alreadyOnThisAp) {
            onPhase("Usando la red WiFi actual…")
        } else {
            val passphrase = KilnNetworkConnector.resolvePassphrase(accessPoint)
            if (passphrase.isNullOrBlank()) {
                onError("No se pudo derivar la contraseña del horno (BSSID/SSID).")
                return
            }
            onPhase("Solicitando autorización de Android…")
            val network = KilnNetworkConnector.connect(
                context = context,
                ssid = accessPoint.ssid,
                passphrase = passphrase
            )
            if (network == null) {
                onError("No fue posible conectarse a ${accessPoint.ssid}. ¿Aceptaste el diálogo del sistema?")
                return
            }
        }

        onPhase("Pidiendo datos al horno…")
        delay(500)

        val infoResult = httpClient.getInfo()
        infoResult.onSuccess { info ->
            if (info.macAddress.isBlank()) {
                onError("El horno respondió sin dirección MAC. Probá de nuevo.")
                KilnNetworkConnector.release(context)
                return@onSuccess
            }
            val typeFromAp = when (accessPoint.deviceType) {
                SoftApDeviceType.DATALOGGER -> "DATALOGGER"
                SoftApDeviceType.KILN -> "KILN"
                SoftApDeviceType.UNKNOWN -> ""
            }
            val merged = if (info.type.isBlank() && typeFromAp.isNotBlank()) {
                info.copy(type = typeFromAp)
            } else if (info.type.isBlank() &&
                info.model.equals("DATALOGGER", ignoreCase = true)
            ) {
                info.copy(type = "DATALOGGER")
            } else {
                info
            }
            onSuccess(merged)
        }.onFailure { err ->
            onError("No se pudo leer la información del horno (${err.message ?: "error desconocido"}).")
            KilnNetworkConnector.release(context)
        }
    } finally {
        onConnectingFinished()
    }
}
