# Arquitectura — MySmartKiln

Documento vivo y corto. Si diverge del código, gana el código.

## Sistema

```
[ Android app ]  <--BLE / SoftAP HTTP-->  [ ESP32 firmware ]
       |                                        |
       +------------- Firebase RTDB ------------+
```

| Componente | Responsabilidad |
|------------|-----------------|
| App Android (este repo) | UI, programas, pairing, sync, comandos al horno |
| ESP32 | Ejecución de curva, PID/SSR, BLE server, SoftAP, telemetría |
| Firebase | Auth de usuario, backup/sync, estado remoto |

## App — capas

| Capa | Paquete / zona | Notas |
|------|----------------|-------|
| UI | `ui/screens`, `ui/components`, `ui/viewmodels` | Compose + MVVM |
| Dominio de conexión | `ble/`, `utils/KilnNetwork*` | BLE vs WiFi local |
| Datos remotos | `data/firebase`, `data/auth` | RTDB + Auth |
| Datos locales | `data/local` | Room + HTTP al ESP |
| Navegación | `navigation/` | Navigation Compose |

## Conectividad (visión actual en `master`)

1. **BLE** cerca del horno → comandos/programas por GATT.
2. **WiFi SoftAP** → HTTP local al ESP cuando conviene red del horno.
3. **Firebase** → cuando hay internet / modo remoto.

La branch `feature/remote-firebase-commands` evoluciona modos local/online y SoftAP; documentar ahí solo después de merge o al trabajar sobre esa branch.

## Fuera de este repo

- Firmware PlatformIO (“IA Kiln”)
- Proyecto sensor MAX31855

Hasta que estén en Git, no hay arquitectura versionada de firmware en este repositorio.
