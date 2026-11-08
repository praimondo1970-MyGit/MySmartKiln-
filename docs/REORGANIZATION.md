# Plan de reorganización profesional — MySmartKiln

Objetivo: un solo producto con repos claros, docs mínimas, branches honestas y un flujo Cursor predecible.

## 1. Principios

1. **Código > documentos largos.** Docs solo para arquitectura, onboarding y decisiones.
2. **Cada pieza versionada en GitHub.** Nada crítico solo en OneDrive.
3. **Una verdad por línea de producto.** `master` (o `main`) refleja lo desplegable; features van en PRs.
4. **Cursor:** 1 Project de producto · 1 chat = 1 misión · rules/`AGENTS.md` como memoria.
5. **Local vs cloud:** hardware en Desktop; docs/refactors/PRs pueden ir a Cloud Agents.

## 2. Estructura objetivo (recomendada: multi-repo)

```
GitHub
├── MySmartKiln-              # app Android (este repo)
├── MySmartKiln-firmware      # ESP32 PlatformIO (hoy: "IA Kiln")
└── (opcional) MySmartKiln-max31855   # o subcarpeta del firmware

Local (Desktop)
~/dev/mysmartkiln/
├── android/     → clone MySmartKiln-
├── firmware/    → clone MySmartKiln-firmware
└── MySmartKiln.code-workspace
```

**Por qué multi-repo primero:** la app ya está en GitHub; el firmware no. Es el camino más corto y profesional sin migrar historial Android.

Monorepo (`/android` + `/firmware` + `/docs`) queda como opción B si más adelante siempre cambiás ambas piezas juntas.

## 3. Documentación

### Eliminado en este PR

| Archivo | Motivo |
|---------|--------|
| `MySmartKiln_DOCUMENTACION_COMPLETA.md` (~1500 líneas) | Desactualizado (oct 2025), monolítico, duplicaba el código |
| `ALTERNATIVAS_RESOLUCION_PROGRAMA_BLE.md` | Nota de incidente ya implementada en `BleManager`; no es doc de producto |

### Queda (lean)

| Archivo | Rol |
|---------|-----|
| `README.md` | Entrada humana |
| `AGENTS.md` | Entrada para agentes Cursor |
| `docs/ARCHITECTURE.md` | Mapa corto del sistema |
| `docs/REORGANIZATION.md` | Este plan (se puede archivar cuando esté cumplido) |

**Regla nueva:** no volver a crear “documentación completa del sistema” en la raíz. Preferí PRs + issues + `docs/` cortos.

## 4. Git / branches

| Acción | Detalle |
|--------|---------|
| Decidir destino de `feature/remote-firebase-commands` | Merge a `master` vía PR, o renombrar a `develop` si será la línea activa |
| Borrar `cursor/cloud-agent-*` huérfana | Si no aporta commits útiles |
| PR por feature | Evitar commits sueltos “checkpoint” en la rama publicada |
| Tags opcionales | `v0.x` cuando haya build estable |

## 5. Cursor — organización del trabajo

| Nivel | Nombre | Uso |
|-------|--------|-----|
| Project | `MySmartKiln` | Producto entero (cuando firmware esté en Git + Cloud Agents) |
| Chat | Misión concreta | Ej. “SoftAP handoff”, “Fix PROFILE_OK timeout” |
| Side chat | `/btw` | Preguntas laterales |
| Rules | `AGENTS.md` (+ `.cursor/rules` si hace falta) | Memoria estable |

Hasta tener firmware en GitHub: **no** dependas de Cloud Agents para features BLE/SoftAP reales.

## 6. Fases

### Fase A — Hecho en este cambio (repo app)

- [x] Borrar docs viejas/redundantes
- [x] Añadir `README.md`, `AGENTS.md`, `docs/*`
- [x] Workspace sin paths OneDrive rotos

### Fase B — Vos en el PC (bloqueante)

- [ ] Crear repo GitHub del firmware desde `PlatformIO/Projects/IA Kiln`
- [ ] Decidir si MAX31855 va solo o dentro del firmware
- [ ] Clonar app + firmware bajo una carpeta `~/dev/mysmartkiln/`
- [ ] Actualizar el `.code-workspace` local con esas rutas reales
- [ ] Decidir destino de `feature/remote-firebase-commands`

### Fase C — Higiene de producto

- [ ] Abrir PR de la feature branch (o merge planificado)
- [ ] Revisar si `google-services.json` debe seguir versionado o ir a secretos CI
- [ ] Plan de renombrar package `com.example.mysmartkiln` → id real
- [ ] Crear Project “MySmartKiln” en Agents Window (multi-repo)

### Fase D — Operación continua

- [ ] Chats cortos por misión; archivar los eternos
- [ ] Actualizar `docs/ARCHITECTURE.md` solo cuando cambie el diseño
- [ ] Cloud Agents para trabajo sin dispositivo; Desktop para horno real

## 7. Ayuda que necesito de vos

Sin esto no puedo completar la reorg de punta a punta:

1. **Firmware**  
   - ¿Podés crear el repo en GitHub y dar acceso, o preferís que te pase comandos exactos para correr en tu PC?  
   - Desde este Cloud Agent **no** tengo tus carpetas OneDrive.

2. **Decisión de layout**  
   - Confirmá **multi-repo (A)** o monorepo (B).

3. **Branch de producto**  
   - ¿`feature/remote-firebase-commands` es la verdad actual? ¿Mergeamos a `master`?

4. **MAX31855**  
   - ¿Repo aparte o carpeta del firmware?

5. **Identidad de la app**  
   - Package/id deseado (reemplazo de `com.example.mysmartkiln`) y si Firebase/`google-services.json` se quedan públicos en el repo.

6. **Chats locales**  
   - Yo no veo el historial Desktop. Si hay hilos importantes, listá títulos o exportá notas a issues.

Con (1)+(2)+(3) puedo seguir en el siguiente turno: guion de publish del firmware, PR de la feature, o Project Cursor.
