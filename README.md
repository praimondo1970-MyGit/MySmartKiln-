# MySmartKiln

App Android para control de hornos cerámicos inteligentes (WiFi SoftAP + Firebase).

| Pieza | Ubicación actual | Estado |
|-------|------------------|--------|
| App Android (este repo) | `app/` | En GitHub — path SoftAP (sin BLE) |
| Firmware ESP32 | Repo separado (pendiente) | Hoy en PlatformIO / OneDrive |
| Sensor MAX31855 | Repo o carpeta firmware (pendiente) | Hoy en PlatformIO / OneDrive |
| Backend | Firebase Realtime Database (`ia-based-kiln`) | Compartido |

## Stack

- Kotlin, Jetpack Compose, MVVM, Navigation Compose
- Room (local) + Firebase Auth / Realtime Database
- Conexión al horno por **WiFi SoftAP / HTTP local** (`KilnHttpClient`, `KilnNetworkConnector`) y sync remota por Firebase

> La versión con Bluetooth LE fue **descartada**. No hay paquete `ble/`, pantallas de escaneo BLE ni permisos Bluetooth en el manifiesto.

## Abrir el proyecto

1. Clonar este repo.
2. Abrir `MySmartKiln.code-workspace` en Cursor / Android Studio.
3. Cuando el firmware esté en Git, clonar junto a este repo y añadir la carpeta al workspace.

## Documentación

- [`AGENTS.md`](AGENTS.md) — instrucciones para agentes de Cursor
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — arquitectura viva (corta)
- [`docs/REORGANIZATION.md`](docs/REORGANIZATION.md) — plan de reorganización profesional

## Branches

- Línea SoftAP actual (sin BLE): historial en `feature/remote-firebase-commands` / PRs de limpieza
- `master` histórico puede aún contener restos de la era BLE hasta mergear esta línea

## Build

```bash
./gradlew :app:assembleDebug
```

Requiere SDK Android y `local.properties` con `sdk.dir` (no se versiona).
