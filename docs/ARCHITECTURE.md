# Arquitectura — MySmartKiln

Documento vivo y corto. Si diverge del código, gana el código.

## Sistema

```
[ Android app ]  <-- SoftAP HTTP local -->  [ ESP32 firmware ]
       |                                        |
       +------------- Firebase RTDB ------------+
```

| Componente | Responsabilidad |
|------------|-----------------|
| App Android (este repo) | UI, programas, pairing SoftAP, sync, comandos al horno |
| ESP32 | Ejecución de curva, PID/SSR, SoftAP, telemetría |
| Firebase | Auth de usuario, backup/sync, estado remoto |

**Bluetooth LE fue descartado.** La app no usa GATT ni el paquete `ble/`.

## App — capas

| Capa | Paquete / zona | Notas |
|------|----------------|-------|
| UI | `ui/screens`, `ui/components`, `ui/viewmodels` | Compose + MVVM |
| Conexión local | `utils/KilnNetwork*`, `utils/WifiUtils`, `data/local/KilnHttp*` | SoftAP / HTTP |
| Datos remotos | `data/firebase`, `data/auth` | RTDB + Auth |
| Datos locales | `data/local` | Room + HTTP al ESP |
| Navegación | `navigation/` | Navigation Compose |

## Conectividad

1. **WiFi SoftAP** → HTTP local al ESP en la red del horno.
2. **Firebase** → cuando hay internet / modo remoto.
3. Preferencia de modo local vs online según `OperationModePreferences` y estado del dispositivo.

## Fuera de este repo

- Firmware PlatformIO (“IA Kiln”)
- Proyecto sensor MAX31855

Hasta que estén en Git, no hay arquitectura versionada de firmware en este repositorio.
