# MySmartKiln

App Android para control de hornos cerámicos inteligentes (BLE + WiFi SoftAP + Firebase).

| Pieza | Ubicación actual | Estado |
|-------|------------------|--------|
| App Android (este repo) | `app/` | En GitHub |
| Firmware ESP32 | Repo separado (pendiente) | Hoy en PlatformIO / OneDrive |
| Sensor MAX31855 | Repo o carpeta firmware (pendiente) | Hoy en PlatformIO / OneDrive |
| Backend | Firebase Realtime Database (`ia-based-kiln`) | Compartido |

## Stack

- Kotlin, Jetpack Compose, MVVM, Navigation Compose
- Room (local) + Firebase Auth / Realtime Database
- BLE (`BleManager`, `HybridConnectionManager`) y WiFi SoftAP (`KilnHttpClient`, `KilnNetworkConnector`)

## Abrir el proyecto

1. Clonar este repo.
2. Abrir `MySmartKiln.code-workspace` en Cursor / Android Studio.
3. Cuando el firmware esté en Git, clonar junto a este repo y descomentar la carpeta en el workspace (ver comentarios en el `.code-workspace`).

## Documentación

- [`AGENTS.md`](AGENTS.md) — instrucciones para agentes de Cursor
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — arquitectura viva (corta)
- [`docs/REORGANIZATION.md`](docs/REORGANIZATION.md) — plan de reorganización profesional

La documentación histórica monolítica en la raíz fue eliminada por estar desactualizada y redundante con el código.

## Branches relevantes

- `master` — línea principal publicada
- `feature/remote-firebase-commands` — trabajo sustancial (SoftAP / modos / UI) aún no mergeado; decidir destino en el plan de reorg

## Build

Abrir en Android Studio o:

```bash
./gradlew :app:assembleDebug
```

Requiere SDK Android y `local.properties` con `sdk.dir` (no se versiona).
