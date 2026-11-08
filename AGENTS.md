# AGENTS.md — MySmartKiln (Android)

Instrucciones para agentes de Cursor que trabajan en **este** repositorio (app Android).

## Qué es este repo

App Kotlin / Jetpack Compose que controla hornos ESP32 vía:

1. **WiFi SoftAP / HTTP local** — red del horno (`data/local/KilnHttpClient`, `utils/KilnNetworkConnector`, `utils/WifiUtils`)
2. **Firebase Realtime Database** — sync remota cuando hay internet (`data/firebase/`)
3. **Modos de operación** — local vs online (`utils/OperationModePreferences`, sync ESP)

El firmware ESP32 **no** está en este repo. No inventes rutas PlatformIO aquí; pedí el repo de firmware si hace falta.

## BLE descartado

No reintroducir Bluetooth LE. No existe (ni debe existir) el paquete `ble/`, ni pantallas `ScanKiln` / `DevicesFound` / `BleMonitoring` / `ConnectKiln` basadas en GATT, ni permisos `BLUETOOTH*` en el manifiesto.

## Arquitectura de la app

```
app/src/main/java/com/example/mysmartkiln/
  data/
    auth/        # Firebase Auth
    firebase/    # Realtime DB
    local/       # HTTP al ESP, Room DAOs/entities, SoftAP helpers
  ui/            # Compose screens, components, viewmodels, theme
  navigation/
  utils/         # WiFi SoftAP, network binder, operation mode, etc.
```

- UI: Compose + Material3, patrón MVVM
- Persistencia local: Room
- Package actual: `com.example.mysmartkiln` (placeholder; no renombrar sin tarea explícita)

## Reglas de trabajo

1. **Un chat = una misión** verificable (bug, feature o refactor acotado).
2. Preferí leer el código y los tests antes que docs largas. La fuente de verdad es el código.
3. No agregues markdown monolítico en la raíz. Si hace falta documentar, actualizá `docs/ARCHITECTURE.md` o un archivo corto en `docs/`.
4. No commits de `local.properties`, `.apk`, ni secrets. `google-services.json` ya está en el repo; no lo dupliques ni lo pegues en chats.
5. Hardware real (SoftAP / horno): preferí agente **local** en Desktop. Cloud Agents sirven para refactors, docs, PRs y cambios que no requieran dispositivo.

## Cómo verificar

- Compilar: `./gradlew :app:assembleDebug`
- Unit tests: `./gradlew :app:test`
- SoftAP / envío de programa / telemetría local: requiere dispositivo o horno; no simular éxito sin evidencia

## Contexto útil (no exhaustivo)

- Discovery de SoftAP: SSIDs `SmartKiln-` / `SmartLink-` (`WifiUtils`)
- HTTP local al ESP vía red vinculada (`KilnNetworkConnector`, `KilnWifiNetworkBinder`)
- Firebase project id: `ia-based-kiln` (ver `app/google-services.json`)
