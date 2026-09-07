# Plan de restyling UI → look Display ESP

Última actualización: 2026-09-06  
Rama de trabajo: `feature/ui-restyle-esp`  
Punto de rollback: tag `v-pre-restyle` (app funcional previa al restyling)

Mockups de referencia: `docs/mockups-ui-esp/*.jpg`

---

## Objetivo

Acercar la app Android al lenguaje visual del display del horno (fondo negro, cards `#161616`, acento naranja `#FF5722`, pills de estado, herradura tipo ESP), **sin romper** la lógica de WiFi/Firebase/comandos y con forma clara de volver al look clásico.

---

## Rollback (cómo volver atrás)

| Método | Cuándo usarlo |
|--------|----------------|
| Tag `v-pre-restyle` / rama `master` | Volver al código anterior al restyling |
| Rama `feature/ui-restyle-esp` sin merge | El restyling nunca llega a producción |
| Toggle **Claro / Oscuro (Horno)** en Ajustes | Rollback visual instantáneo en runtime (Fase A) |

```bash
# Ver tag
git checkout v-pre-restyle

# O volver a master estable
git checkout master
```

---

## Fases

### Fase A — Theme / tokens + toggle (EN CURSO)
**Impacto:** alto percibido · **Riesgo:** bajo · **Esfuerzo:** 1–3 días

1. Tokens ESP (`EspBrand` / `KilnColors` semánticos).
2. Preferencia persistida `ThemeMode`: `CLASSIC` | `ESP`.
3. `MySmartKilnTheme` + `LocalKilnColors` + `LocalThemeController`.
4. Toggle en Ajustes (reutilizar selector Claro/Oscuro).
5. Migrar pantallas clave a colores semánticos (sin cambiar layout):
   - KilnDetail / Status
   - Kilns
   - Settings
   - Bottom nav (tintes básicos)
6. ProgressArcs: track/progress desde `KilnColors` (sigue layout actual).

**No hacer en A:** rediseñar jerarquía tipo dashboard ESP.

**Done cuando:** con Oscuro la app se ve negra/naranja; con Claro idéntica (o casi) a hoy.

---

### Fase B — Pantalla Estado estilo display
**Impacto:** muy alto · **Riesgo:** medio-bajo · **Esfuerzo:** ~1 semana

1. Herradura más cercana al ESP (track oscuro, fill naranja, tip limpio).
2. Labels TEMPERATURA ACTUAL / velocidad.
3. Pill HORNEANDO / estados.
4. Barra AVANCE DEL CICLO.
5. Tiles resumen (objetivo / etapa / tiempo) adaptados a vertical.

**Done cuando:** Estado “habla” como el display; resto de app coherente en tema A.

---

### Fase C — App completa coherente
**Impacto:** coherencia de marca · **Riesgo:** medio · **Esfuerzo:** 2–3 semanas

1. Programas, Curva, Activity, Login/Splash, diálogos.
2. Iconografía tintada para fondos oscuros.
3. QA contraste + estados Idle/Pausa/Error/Remoto.

**Done cuando:** no queden pantallas “huérfanas” en azul claro si el modo es ESP.

---

## Archivos clave (Fase A)

| Archivo | Rol |
|---------|-----|
| `docs/RESTYLE_PLAN.md` | Este plan |
| `ui/theme/EspBrand.kt` | Paleta ESP cruda |
| `ui/theme/KilnColors.kt` | Colores semánticos + CompositionLocal |
| `ui/theme/ThemePreferences.kt` | SharedPreferences mode |
| `ui/theme/Theme.kt` | Schemes + provider |
| `MainActivity.kt` | Observa mode |
| `SettingsScreen.kt` | Toggle Claro/Oscuro |
| `KilnDetailScreen.kt`, `KilnStatusScreen.kt`, `KilnsScreen.kt` | Aplicar `LocalKilnColors` |

---

## Continuidad (mañana)

1. Leer este archivo: `docs/RESTYLE_PLAN.md`
2. `git checkout feature/ui-restyle-esp` && `git status`
3. Probar en dispositivo: Ajustes → Tema → **Oscuro** (ESP) / **Claro** (clásico)
4. Completar migración a `LocalKilnColors` en pantallas aún claras:
   - ProgramsScreen, ActivityScreen
   - KilnProgramScreen, KilnCurveScreen, KilnCard
   - ResponsiveBottomNavigation (barra compartida)
   - Diálogos (Sending, ApReconfig, OperateOffline, Interrupted)
5. Cuando A se vea bien → validar toggle → decidir Fase B
6. **No mergear a master** hasta validar

### Estado al 2026-09-06 (inicio)
- Plan creado
- Tag `v-pre-restyle` = último commit estable en master (`dde123a` / origin)
- Rama `feature/ui-restyle-esp` con WIP funcional + **Fase A iniciada**
- Hecho en A: tokens ESP, ThemeController, toggle Ajustes, Detail/Status/Kilns/Settings parcialmente migrados
- Pendiente A: resto de pantallas + bottom nav compartida + diálogos

---

## Fuera de alcance

- Duplicar carpeta del proyecto Android.
- Cambiar `applicationId` (salvo que más adelante se pida build side-by-side).
- Copiar layout landscape 800×480 1:1 al teléfono.
- Toclar firmware ESP en este trabajo.
