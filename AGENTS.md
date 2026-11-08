# AGENTS.md — MySmartKiln (Android)

Instrucciones para agentes de Cursor que trabajan en **este** repositorio (app Android).

## Qué es este repo

App Kotlin / Jetpack Compose que controla hornos ESP32 vía:

1. **BLE** — cercano al horno (`ble/BleManager`, `HybridConnectionManager`)
2. **WiFi SoftAP / HTTP local** — red del horno (`data/local/KilnHttpClient`, `utils/KilnNetworkConnector`)
3. **Firebase Realtime Database** — sync remota cuando hay internet (`data/firebase/`)

El firmware ESP32 **no** está en este repo. No inventes rutas PlatformIO aquí; pedí el repo de firmware si hace falta.

## Arquitectura de la app

```
app/src/main/java/com/example/mysmartkiln/
  ble/           # BLE + hybrid connection
  data/
    auth/        # Firebase Auth
    firebase/    # Realtime DB
    local/       # HTTP al ESP, Room DAOs/entities
  ui/            # Compose screens, components, viewmodels, theme
  navigation/
  utils/
```

- UI: Compose + Material3, patrón MVVM
- Persistencia local: Room (`AppDatabase` / entidades kiln, program, stage, sync)
- Package actual: `com.example.mysmartkiln` (placeholder; no renombrar sin tarea explícita)

## Reglas de trabajo

1. **Un chat = una misión** verificable (bug, feature o refactor acotado).
2. Preferí leer el código y los tests antes que docs largas. La fuente de verdad es el código.
3. No agregues markdown de diseño “completo del sistema” en la raíz. Si hace falta documentar, actualizá `docs/ARCHITECTURE.md` o un archivo corto en `docs/`.
4. No commits de `local.properties`, `.apk`, ni secrets. `google-services.json` ya está en el repo; no lo dupliques ni lo pegues en chats.
5. Hardware real (BLE / SoftAP): preferí agente **local** en Desktop. Cloud Agents sirven para refactors, docs, PRs y cambios que no requieran dispositivo.
6. Branch activa de producto divergente: `feature/remote-firebase-commands` puede estar más adelante que `master`. Antes de basar trabajo, confirmá en qué branch está la verdad operativa.

## Cómo verificar

- Compilar: `./gradlew :app:assembleDebug`
- Unit tests: `./gradlew :app:test`
- BLE / envío de programa / SoftAP: requiere dispositivo o horno; no simular éxito sin evidencia

## Contexto útil (no exhaustivo)

- Envío de programa BLE espera `PROFILE_OK` / `PROFILE_ERROR`; hay fallback si hay timeout tras write exitoso (`BleManager`).
- Estados híbridos: `HybridConnectionState` (`BLE_CONNECTED`, `WIFI_*`, `OFFLINE`).
- Firebase project id: `ia-based-kiln` (ver `app/google-services.json`).
