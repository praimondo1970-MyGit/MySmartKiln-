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
import com.example.mysmartkiln.ui.theme.AppColors
import com.example.mysmartkiln.ui.theme.LocalKilnColors
import com.example.mysmartkiln.utils.KilnAccessPoint
import com.example.mysmartkiln.utils.KilnNetworkConnector
import com.example.mysmartkiln.utils.WifiUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Diálogo de alta de horno por descubrimiento WiFi.
 *
 * Reemplaza el flujo viejo de "abrí Ajustes y seleccioná la red SmartKiln":
 *  1. Pide al usuario el permiso runtime que requiere `WifiNetworkSpecifier`
 *     (NEARBY_WIFI_DEVICES en Android 13+, ACCESS_FINE_LOCATION antes).
 *  2. Escanea redes WiFi y filtra las que arrancan con `SmartKiln-`.
 *  3. Lista los hornos detectados; al tap, deriva la passphrase WPA2 desde
 *     el BSSID y pide a Android conectarse vía WifiNetworkSpecifier.
 *  4. Cuando la asociación es aceptada, hace `GET /api/info` por la red
 *     bound y devuelve los datos del horno al caller.
 *
 * Si todo va bien, el caller recibe en [onKilnSelected] un [KilnInfoDto]
 * con name/model/volume/userId/macAddress prellenados desde el firmware,
 * y se ocupa de navegar a `ConfigureKilnScreen` para que el usuario
 * confirme/edite los datos antes de guardar.
 */
@Composable
fun KilnDiscoveryDialog(
    onDismiss: () -> Unit,
    onKilnSelected: (KilnInfoDto) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val httpClient = remember { KilnHttpClient(context.applicationContext) }

    var hasPermission by remember {
        mutableStateOf(
            !KilnNetworkConnector.isSupported ||
                KilnNetworkConnector.hasRequiredPermissions(context)
        )
    }
    var isScanning by remember { mutableStateOf(false) }
    var accessPoints by remember { mutableStateOf<List<KilnAccessPoint>>(emptyList()) }
    var connectingTo by remember { mutableStateOf<KilnAccessPoint?>(null) }
    var connectingPhase by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var hasScanned by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted ||
            ContextCompat.checkSelfPermission(
                context,
                KilnNetworkConnector.requiredRuntimePermission()
            ) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun runScan() {
        isScanning = true
        errorMessage = null
        // Damos un breve respiro para que el HW de WiFi entregue resultados
        // frescos antes de leerlos.
        accessPoints = WifiUtils.getSmartKilnAccessPoints(context)
        delay(1500)
        accessPoints = WifiUtils.getSmartKilnAccessPoints(context)
        hasScanned = true
        isScanning = false
    }

    LaunchedEffect(hasPermission) {
        if (!hasPermission) {
            permissionLauncher.launch(KilnNetworkConnector.requiredRuntimePermission())
        } else {
            runScan()
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (connectingTo == null) onDismiss()
        },
        title = {
            Text(
                text = "Buscar horno",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = LocalKilnColors.current.body
                )
            )
        },
        text = {
            Column {
                if (!hasPermission) {
                    Text(
                        text = "Para descubrir hornos cercanos necesitamos permiso para ver redes WiFi.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = LocalKilnColors.current.body
                        )
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            permissionLauncher.launch(
                                KilnNetworkConnector.requiredRuntimePermission()
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LocalKilnColors.current.accent,
                            contentColor = LocalKilnColors.current.onAccent
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
                    text = "Encendé el horno y verificá que esté en modo de configuración. Vamos a buscar las redes SmartKiln cercanas.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = LocalKilnColors.current.body
                    )
                )
                Spacer(Modifier.height(12.dp))

                when {
                    isScanning && accessPoints.isEmpty() -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = LocalKilnColors.current.body
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = "Buscando hornos…",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = LocalKilnColors.current.body
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
                            items(accessPoints, key = { it.bssid }) { ap ->
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
                            text = "No detectamos ningún horno SmartKiln. Asegurate de que esté encendido y a pocos metros del teléfono.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = LocalKilnColors.current.muted
                            )
                        )
                    }
                }

                errorMessage?.let { msg ->
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = LocalKilnColors.current.danger.copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = LocalKilnColors.current.danger,
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
                        color = LocalKilnColors.current.body,
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
                        color = LocalKilnColors.current.muted
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
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = LocalKilnColors.current.cardBackground,
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
                        color = LocalKilnColors.current.body
                    )
                )
                Text(
                    text = "Señal: ${accessPoint.signalLevel} dBm",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = LocalKilnColors.current.muted
                    )
                )
            }
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(
                        color = signalColor(
                            accessPoint.signalLevel,
                            LocalKilnColors.current.success,
                            LocalKilnColors.current.warning,
                            LocalKilnColors.current.danger
                        ),
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
            )
        }
    }
}

@Composable
private fun ConnectingBlock(ssid: String, phase: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(56.dp),
            color = LocalKilnColors.current.body,
            strokeWidth = 4.dp
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = "Conectando a $ssid",
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                color = LocalKilnColors.current.body
            )
        )
        if (phase.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = phase,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = LocalKilnColors.current.muted
                )
            )
        }
    }
}

private fun signalColor(
    dbm: Int,
    success: Color,
    warning: Color,
    danger: Color
): Color = when {
    dbm >= -55 -> success
    dbm >= -70 -> warning
    else -> danger
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
        val passphrase = KilnNetworkConnector.passphraseFromBssid(accessPoint.bssid)
        if (passphrase.isNullOrBlank()) {
            onError("No se pudo derivar la contraseña del horno desde el BSSID.")
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

        onPhase("Pidiendo datos al horno…")
        // El AP recién aceptó la asociación; un pequeño margen evita timeouts
        // del primer ping.
        delay(500)

        val infoResult = httpClient.getInfo()
        infoResult.onSuccess { info ->
            if (info.macAddress.isBlank()) {
                onError("El horno respondió sin dirección MAC. Probá de nuevo.")
                KilnNetworkConnector.release(context)
                return@onSuccess
            }
            onSuccess(info)
        }.onFailure { err ->
            onError("No se pudo leer la información del horno (${err.message ?: "error desconocido"}).")
            KilnNetworkConnector.release(context)
        }
    } finally {
        onConnectingFinished()
    }
}
