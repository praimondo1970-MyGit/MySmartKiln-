# MySmartKiln - Documentación Completa del Sistema

## 📋 **Información General del Proyecto**

**Proyecto**: MySmartKiln - Sistema Completo de Control de Hornos Cerámicos Inteligentes  
**Tecnologías**: Android (Kotlin, Jetpack Compose), ESP32 (PlatformIO), Firebase  
**Estado**: En desarrollo activo - Sistema híbrido BLE/WiFi implementado  
**Última actualización**: Octubre 2025  
**Versión**: 1.3

---

## 🏗️ **Arquitectura General del Sistema**

### **Componentes del Sistema**
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Android App   │    │    Firebase     │    │     ESP32       │
│                 │    │                 │    │                 │
│ Room Database   │◄──►│ Firestore DB    │    │ CurvaActiva     │
│ (Local Storage) │    │ (Cloud Backup)  │    │ (Execution)     │
│                 │    │                 │    │                 │
│ BLE Client      │───►│ Sync Service    │    │ BLE Server      │
│ WiFi Client     │───►│ Realtime Data   │    │ WiFi Client     │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

### **Stack Tecnológico**

#### **Android App (Kotlin + Jetpack Compose)**
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material3)
- **Architecture**: MVVM (Model-View-ViewModel)
- **Navigation**: Navigation Compose
- **Database Local**: Room Database
- **Backend**: Firebase Realtime Database
- **Communication**: BLE (Bluetooth Low Energy) + WiFi
- **Connectivity**: HybridConnectionManager (BLE/WiFi switching)

#### **ESP32 Firmware (C++)**
- **Platform**: ESP32 (Arduino Framework)
- **Communication**: BLE + WiFi
- **Storage**: NVS (Non-Volatile Storage)
- **Backend**: Firebase Realtime Database
- **Temperature Control**: PID Control System
- **PWM**: SSR (Solid State Relay) Control

---

## 📱 **Características Principales de la App**

### **1. Sistema de Conexión Híbrido (BLE/WiFi)**

#### **HybridConnectionManager**
El sistema implementa un gestor híbrido que alterna automáticamente entre BLE y WiFi según la proximidad del usuario al horno:

**Estados de Conexión:**
```kotlin
enum class HybridConnectionState {
    BLE_CONNECTED,      // "Conectado" + icono Bluetooth
    WIFI_ONLINE,        // "Conectado" + icono WiFi (internet + Firebase OK)
    WIFI_NO_INTERNET,   // "Sin internet" + icono WiFi
    WIFI_FIREBASE_DOWN, // "Firebase no disponible" + icono WiFi
    OFFLINE             // "Desconectado" + icono offline
}
```

**Lógica de Detección:**
- **BLE**: Usa RSSI (Received Signal Strength Indicator) para detectar proximidad
- **WiFi**: Fallback cuando BLE no está disponible o el usuario está lejos
- **Timeout**: 8 segundos para cambiar de modo BLE a WiFi
- **Fallback**: Sistema de escaneo directo si ProximityManager falla

#### **Características del Sistema Híbrido:**
- ✅ **Detección automática de proximidad** (RSSI threshold)
- ✅ **Conexión asíncrona BLE** con timeouts y reintentos
- ✅ **Fallback a WiFi** cuando BLE falla
- ✅ **Detección de desconexión activa** del ESP32
- ✅ **Sistema de fallback** con BleManager directo
- ✅ **Validación de conectividad** (internet + Firebase)

### **2. Gestión de Hornos**

#### **Pantalla "Mis Hornos" (`KilnsScreen`)**
- Lista todos los hornos vinculados al usuario
- Muestra temperatura en tiempo real desde Firebase
- Indicador de conexión inteligente (BLE/WiFi/Desconectado)
- Estado del programa (flame icon + "Cargar programa" si no hay programa)
- Swipe-to-delete para eliminar hornos
- Botón "+" para agregar nuevo horno

#### **Tarjeta de Horno (`KilnCard`)**
- **Altura**: 100.dp
- **Elementos**:
  - **Superior izquierda**: Nombre del horno
  - **Superior derecha**: 
    - Icono de llama (🔥) en `Primary600` (24dp)
    - Texto "Cargar programa" (14.sp) si no hay programa activo
  - **Centro izquierda**: 
    - Icono de temperatura (35dp) con fondo `Primary100`
    - Temperatura en tiempo real (16.sp Bold)
  - **Inferior derecha**: 
    - Estado de conexión inteligente con iconos específicos
    - Texto contextual: "Conectado", "Sin internet", "Firebase no disponible", "Visto hace xxx"
    - Punto de color (verde/rojo, 8dp)

#### **Sistema de Conectividad Inteligente**
```kotlin
val displayText = when {
    connectionType == "ble" && isConnected -> "Conectado"
    connectionType == "wifi" && isConnected -> "Conectado"
    updatedConnectionState == HybridConnectionState.WIFI_NO_INTERNET -> "Sin internet"
    updatedConnectionState == HybridConnectionState.WIFI_FIREBASE_DOWN -> "Firebase no disponible"
    connectionType == "wifi" && !isConnected && lastOnlineTimestamp > 0 -> "Visto hace $timeSinceLastOnline"
    connectionType == "wifi" && !isConnected -> "Desconectado"
    connectionType == "ble" && !isConnected -> "Desconectado"
    else -> "Desconectado"
}
```

### **3. Pantalla de Detalle del Horno (`KilnDetailScreen`)**

#### **Navegación por Pestañas (Tab Navigation)**
- **Estado**: Monitoreo en tiempo real con indicadores de conexión
- **Programa**: Gestión del programa de cocción
- **Curva**: Visualización de curva de temperatura

#### **Estado de Conexión (Top Right)**
- Texto contextual según tipo de conexión
- Punto de color (verde/rojo, 10dp)
- Basado en timestamp de Firebase (timeout 30s) y conectividad real

### **4. Pantalla "Estado" (`KilnStatusScreen`)**

#### **Layout General**
- **Card**: 450.dp de altura, fondo blanco, bordes redondeados (16.dp)
- **Padding**: 24.dp

#### **Sección Superior (Programa Cargado)**
- **Izquierda**: Nombre del programa (16.sp Bold, `Primary700`)
- **Divisor vertical**: 1.dp × 40.dp (`Primary200`)
- **Derecha**: 
  - Icono de tiempo (24.dp)
  - Duración total (16.sp Bold)
  - Label "Duración" (13.sp, `Primary400`)

#### **Indicador Circular de Progreso (`ProgressArcs`)**
- **Tamaño**: 235.dp
- **Arco Total**: 270° (desde 135° hasta 405°)
- **Segmentos**: Proporcionales a la duración de cada etapa
- **Colores**:
  - Completado: `Primary700` (azul)
  - Pendiente: `DarkGray` con alpha 0.4
- **Separadores**: Líneas blancas de 3° entre etapas
- **Grosor**: 8.dp

#### **Temperatura Central**
- **Posición**: Centrada dentro del círculo de progreso
- **Offset**: -10.dp hacia arriba
- **Tamaño**: 
  - Número: 80.sp
  - Símbolo "°C": 32.sp con `baselineShift(0.6f)`
- **Color**: `Primary700`

#### **Botones de Control Adaptativos**

##### **Sin Programa Cargado**
- **Botón "Cargar Programa"**:
  - Ancho: `fillMaxWidth()` con padding horizontal 10.dp
  - Altura: 48.dp
  - Color: `Primary700`
  - Texto: 16.sp Bold, blanco
  - Action: Navega a `SelectProgramScreen`

##### **Con Programa Cargado**
- **Botón "Play"** (Izquierda):
  - Color: `Tertiary900` (naranja)
  - Icono: `icon_play` (24.dp)
  - Texto: "Play" (16.sp Bold)
  - Action: `viewModel.playKiln()`

- **Botón "Stop"** (Derecha):
  - Color: `Primary700` (azul)
  - Icono: `icon_stop` (24.dp)
  - Texto: "Stop" (16.sp Bold)
  - Action: `viewModel.stopKiln()`

### **5. Gestión de Programas**

#### **Pantalla "Programas" (`ProgramsScreen`)**
- Lista de programas guardados en Room Database
- Botón "+" para crear nuevo programa
- Opciones por programa:
  - Editar
  - Duplicar
  - Eliminar (swipe-to-delete)

#### **Selección de Programa (`SelectProgramScreen`)**
- Copia de `ProgramsScreen` adaptada para selección
- Botón "Volver" en lugar de título
- Click en tarjeta: Selecciona programa y regresa a `KilnDetailScreen`
- No muestra botón ">" en las tarjetas

#### **Crear/Editar Programa (`CreateProgramScreen`)**
- Nombre del programa
- Lista de etapas (stages)
- Por etapa:
  - Temperatura objetivo (°C)
  - Velocidad de rampa (°C/min)
  - Tiempo de sostenimiento (hold time)
- Cálculo automático de duración total y temperatura máxima

### **6. Sistema de Monitoreo en Tiempo Real**

#### **Arquitectura de Monitoreo**
```
┌─────────────────┐         ┌─────────────────┐         ┌─────────────────┐
│     ESP32       │         │    Firebase     │         │   Android App   │
│                 │         │  Realtime DB    │         │                 │
│  - Termocupla   │ ──WiFi─►│                 │◄──WiFi──│  - UI Monitor   │
│  - Estado       │  (5s)   │  realtimeData/  │  (RT)   │  - Detección    │
│  - Timestamp    │         │  - temperature  │         │  - Conexión     │
└─────────────────┘         │  - status       │         └─────────────────┘
                            │  - timestamp    │
                            └─────────────────┘
```

#### **Detección de Conexión**
```kotlin
fun isConnected(currentTimeSeconds: Long): Boolean {
    return (currentTimeSeconds - timestamp) < 30  // Desconectado si > 30 segundos sin actualización
}
```

#### **Comportamiento de la UI:**
- **Conectado** (< 30 segundos): Muestra temperatura y estado actual
- **Desconectado** (> 30 segundos): Muestra "Desconocido" y "--°C"

#### **Frecuencias de Actualización:**
- **Firebase**: Cada 5 segundos (vía WiFi)
- **BLE**: Cada 1 segundo (via notificaciones)

### **7. Firebase Integration**

#### **Estructura de Firebase Realtime Database**

```
users/
  └── {userId}/
      └── kilns/
          └── {macAddress}/
              ├── deviceInfo/
              │   ├── manufacturer: "AFRA"
              │   ├── volume: "25"
              │   ├── name: "MiHorno"
              │   └── macAddress: "A4CF128856E8"
              └── realtimeData/
                  ├── temperature: 430.5
                  ├── targetTemp: 1200
                  ├── status: "running" | "paused" | "stopped"
                  └── timestamp: 1697540000000
```

#### **Observación en Tiempo Real**

```kotlin
fun observeRealtimeData(
    userId: String, 
    macAddress: String
): Flow<Map<String, Any>?> {
    val path = "users/$userId/kilns/$macAddress/realtimeData"
    return callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.value as? Map<String, Any>)
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        database.getReference(path).addValueEventListener(listener)
        awaitClose { 
            database.getReference(path).removeEventListener(listener) 
        }
    }
}
```

### **8. Comunicación BLE**

#### **UUIDs de Servicio y Características**

```kotlin
val SERVICE_UUID = "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
val CHAR_UUID_KILN_INFO = "beb5483e-36e1-4688-b7f5-ea07361b26a8"
val CHAR_UUID_WIFI_CONFIG = "beb5483e-36e1-4688-b7f5-ea07361b26a9"
```

#### **Protocolo de Comunicación**

##### **1. KILN_INFO Request**
```json
{
  "type": "KILN_INFO"
}
```

##### **2. KILN_INFO Response**
```json
{
  "manufacturer": "AFRA",
  "volume": "25",
  "macAddress": "A4CF128856E8"
}
```

##### **3. WIFI_CONFIG Request**
```json
{
  "type": "WIFI_CONFIG",
  "ssid": "MyWiFi",
  "password": "password123",
  "userId": "user_abc123"
}
```

##### **4. WIFI_CONFIG Response**
```json
{
  "status": "success",
  "message": "WiFi configured"
}
```

---

## 🎨 **Sistema de Diseño**

### **Paleta de Colores (AppColors)**
```kotlin
// Colores Primarios
Primary100: #CCE1FF (fondo de pantallas)
Primary300: #6BA3FF (botones secundarios, hexágonos iconos)
Primary400: #3D8FFF (botón borrar)
Primary500: #1976D2 (elementos destacados)
Primary600: #0052CC (botones activos)
Primary700: #003D99 (botones principales, navegación)
Primary800: #002D73 (fondos oscuros)
Primary900: #150536 (navegación del sistema)

// Colores Terciarios
Terciary_800: #FF6B00 (botón "+", acciones principales)

// Colores Neutrales
Neutral400: #9CA3AF (botones deshabilitados)
Neutral500: #6B7280 (texto placeholder)
Neutral600: #4B5563 (texto secundario)

// Utilidad
White: #FFFFFF
Black: #000000
Error400: #EF4444 (errores)
```

### **Tipografía**
- **Fuente Principal**: Raleway (Bold para títulos)
- **Jerarquía**:
  - `titleLarge`: Títulos de pantalla (Primary700, Bold)
  - `titleMedium`: Subtítulos y botones
  - `bodyLarge`: Texto principal
  - `bodyMedium`: Texto secundario
  - `bodySmall`: Etiquetas y detalles

### **Componentes UI Personalizados**

#### **Navegación**
- `ResponsiveBottomNavigation`: Barra inferior con curva personalizada
- `Plusbtn`: Botón FAB naranja (se convierte en "X" al abrir menú)
- `CloseBtnCircle`: Botón "X" para cerrar overlay (Primary300)

#### **Tarjetas**
- `ProgramCard`: Tarjeta de programa con swipe (-80dp)
- `StageCard`: Tarjeta de etapa con swipe (-80dp)
- `ActivityCard`: Tarjeta de actividad con swipe (-80dp)
- `NewProgramCard`: Tarjeta "Nuevo Programa" con imagen de fondo
- `NewKilnCard`: Tarjeta "Nuevo Horno" con imagen de fondo
- `KilnCard`: Tarjeta de horno con sistema de conectividad inteligente

#### **Inputs y Formularios**
- `ProgramNameInput`: Campo para nombre de programa
- `SearchBar`: Barra de búsqueda con icono
- `Group20`: Grupo de 3 campos para etapas (Temperatura, Velocidad, Meseta)
- `StageInputField`: Campo genérico para parámetros de etapa

#### **Acciones Swipe**
- `ProgramSwipeActions`: Botones borrar + duplicar (azul + naranja)
- `ActivitySwipeActions`: Botón borrar (azul)

#### **Otros**
- `ScrollGradient`: Gradiente fade-out para contenido scrolleable
- `OrangeHexagon`: Hexágono PNG con color personalizable (usado en iconos)
- `HomeIndicatorWhite`: Indicador de navegación del sistema
- `ConnectivityManager`: Gestor de conectividad internet/Firebase

---

## 🧭 **Sistema de Navegación**

### **Rutas Definidas (Screen.kt)**
```kotlin
object Kilns : Screen("kilns?startScanning={startScanning}")
object Programs : Screen("programs")
object Activity : Screen("activity")
object Settings : Screen("settings")
object CreateProgram : Screen("create_program/{programId}/{mode}")
object AddStage : Screen("add_stage")
```

### **Flujo de Navegación**
```
LoginScreen → SignInScreen → SplashScreen
                                    ↓
                              KilnsScreen (inicial)
                                    ↓
    ┌───────────────────┬───────────┴───────────┬───────────────────┐
    ↓                   ↓                       ↓                   ↓
ProgramsScreen    ActivityScreen        SettingsScreen         KilnsScreen
    ↓
CreateProgramScreen
    ↓
Modal: Add/Edit Stage
```

### **Navegación Inferior**
- **Hornos**: Navega a `KilnsScreen`
- **Programas**: Navega a `ProgramsScreen`
- **Actividad**: Navega a `ActivityScreen`
- **Ajustes**: Navega a `SettingsScreen`
- **Botón "+"**: Abre `AddMenuScreen` overlay

---

## 🔧 **Funcionalidades Implementadas**

### **✅ Sistema de Persistencia (Room Database)**

#### **Entidades**
```kotlin
@Entity(tableName = "programs")
data class ProgramEntity(
    @PrimaryKey val id: String,
    val name: String,
    val maxTemperature: String,
    val totalDuration: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "stages")
data class StageEntity(
    @PrimaryKey val id: String,
    val programId: String,
    val stageNumber: Int,
    val temperature: String,
    val speed: String,
    val maintenance: String
)
```

#### **DAOs**
- `ProgramDao`: CRUD de programas
- `StageDao`: CRUD de etapas

#### **Repository**
- `ProgramRepository`: Capa de abstracción entre ViewModel y DAO

#### **ViewModels**
- `CreateProgramViewModel`: Lógica de creación/edición de programas
- `ProgramsListViewModel`: Lógica de lista de programas con búsqueda
- `ViewModelFactory`: Factory para instanciar ViewModels con contexto

### **✅ Sistema de Conexión Híbrido**

#### **HybridConnectionManager**
- Gestión automática de conexión BLE/WiFi
- Detección de proximidad por RSSI
- Fallback a WiFi cuando BLE falla
- Sistema de reintentos y timeouts
- Detección de desconexión activa

#### **ProximityManager**
- Escaneo BLE para detectar hornos cercanos
- Manejo de errores con backoff exponencial
- Sistema de fallback con BleManager directo

#### **BleManager**
- Operaciones BLE directas (conectar, leer, escribir)
- Manejo de características y servicios
- Logging detallado para debugging

#### **ConnectivityManager**
- Validación de conectividad internet
- Verificación de disponibilidad Firebase
- Monitoreo en tiempo real de conectividad

### **✅ Funcionalidades de UI**

#### **Crear/Editar/Duplicar Programas**
- **Crear**: Nuevo programa desde cero
- **Editar**: Modificar programa existente
- **Duplicar**: Copiar programa con nuevo nombre
- **Validaciones**: Nombre obligatorio + al menos 1 etapa
- **Feedback**: Snackbar "Programa guardado" / "Error al guardar"

#### **Gestión de Etapas**
- **Agregar**: Modal overlay con 3 campos
- **Editar**: Click en `StageCard` → Modal con datos pre-cargados
- **Eliminar**: Swipe left en `StageCard`
- **Validación**: Temperatura y Velocidad obligatorios, Meseta opcional (default: "0")

#### **Swipe Actions Unificadas**
- **Desplazamiento**: -80dp en todas las tarjetas
- **ProgramCard**: Borrar + Duplicar
- **StageCard**: Borrar
- **ActivityCard**: Borrar

#### **Scroll Gradient**
- **Implementación**: `ScrollGradient` component
- **Altura**: 40dp
- **Color**: `AppColors.Primary100` (fondo de pantalla)
- **Efecto**: Fade-out vertical de transparente a opaco
- **Aplicado en**:
  - ProgramsScreen (160dp del final)
  - ActivityScreen (160dp del final)
  - CreateProgramScreen (160dp del final)
  - SettingsScreen (180dp del final)

#### **Menú "Nuevo Horno" / "Nuevo Programa"**
- **Activación**: Botón "+" → Abre overlay
- **Desactivación de navegación**: Tabs inferiores deshabilitados mientras está abierto
- **"Nuevo Programa"**: Navega a `CreateProgramScreen`
- **"Nuevo Horno"**: Navega a `KilnsScreen` con `startScanning=true`

#### **Búsqueda y Filtrado**
- **ProgramsScreen**: Filtrar programas por nombre
- **ActivityScreen**: Filtrar actividades por nombre

---

## 📊 **Modelos de Datos**

### **Programa (ProgramCardData)**
```kotlin
data class ProgramCardData(
    val id: String,
    val name: String,
    val maxTemperature: String, // "1120°C"
    val duration: String, // "6h 30min"
    val stages: Int // Número de etapas
)
```

### **Etapa (StageData)**
```kotlin
data class StageData(
    val stageNumber: Int,
    val temperature: String, // "850"
    val speed: String, // "2.0"
    val maintenance: String // "30"
)
```

### **Actividad (ActivityItem)**
```kotlin
data class ActivityItem(
    val id: String,
    val kilnBrand: String, // "AFRA"
    val kilnVolume: String, // "45Lt"
    val kilnName: String, // "AFRA 45Lt"
    val executionDate: String, // "26/09/2025"
    val programName: String, // "Esmalte de Gres"
    val maxTemperature: String, // "1120"
    val duration: String, // "6h 30min"
    val timestamp: Long
)
```

### **Datos en Tiempo Real (RealtimeData)**
```kotlin
data class RealtimeData(
    val temperature: Float = 0f,
    val status: String = "",
    val timestamp: Long = 0L
) {
    fun isConnected(currentTimeSeconds: Long): Boolean
    fun getDisplayStatus(currentTimeSeconds: Long): String
    fun getFormattedTemperature(currentTimeSeconds: Long): String
}
```

---

## 🎯 **Posicionamiento y Layouts**

### **Distancias desde el Final de Pantalla**
| Pantalla | Distancia | Componente |
|----------|-----------|------------|
| **ProgramsScreen** | 160dp | Spacer antes de navegación |
| **ActivityScreen** | 160dp | Spacer antes de navegación |
| **CreateProgramScreen** | 160dp | Spacer antes de navegación |
| **SettingsScreen** | 180dp | Spacer antes de navegación |

### **Swipe Offsets**
| Componente | Desplazamiento | Rango de Detección |
|------------|---------------|-------------------|
| **ProgramCard** | -80dp | -80f a 0f |
| **StageCard** | -80dp | -80f a 0f |
| **ActivityCard** | -80dp | -80f a 0f |

### **Iconos de Acción**
| Pantalla | Icono | Posición | Color |
|----------|-------|----------|-------|
| **ProgramSwipeActions** | Borrar | -8dp desde derecha | Primary400 (azul) |
| **ProgramSwipeActions** | Duplicar | -3dp desde derecha | Terciary_800 (naranja) |
| **ActivitySwipeActions** | Borrar | Centrado en 80dp | Primary400 (azul) |

---

## 🔨 **Configuraciones Técnicas**

### **Gradle Build**
```bash
# Compilar proyecto
.\gradlew assembleDebug

# Limpiar proyecto
.\gradlew clean

# Ver estado de Gradle
.\gradlew --status
```

### **Dependencias Principales**
```kotlin
// Compose & Navigation
implementation(libs.androidx.compose.ui)
implementation(libs.androidx.compose.material3)
implementation(libs.androidx.activity.compose)
implementation("androidx.navigation:navigation-compose:2.7.7")

// Room Database
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
ksp("androidx.room:room-compiler:2.6.1")

// ViewModel
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")

// Firebase
implementation("com.google.firebase:firebase-database:20.2.0")
implementation("com.google.firebase:firebase-auth:22.1.0")

// BLE
implementation("no.nordicsemi.android:ble:2.6.0")

// Coil (SVG support)
implementation("io.coil-kt:coil-compose:2.5.0")
implementation("io.coil-kt:coil-svg:2.5.0")
```

---

## 📁 **Estructura de Archivos**

```
app/src/main/java/com/example/mysmartkiln/
├── data/
│   ├── local/
│   │   ├── entities/
│   │   │   ├── ProgramEntity.kt ✅
│   │   │   └── StageEntity.kt ✅
│   │   ├── dao/
│   │   │   ├── ProgramDao.kt ✅
│   │   │   └── StageDao.kt ✅
│   │   └── database/
│   │       └── BakingDatabase.kt ✅
│   ├── repository/
│   │   └── ProgramRepository.kt ✅
│   ├── firebase/
│   │   └── FirebaseRealtimeManager.kt ✅
│   ├── auth/
│   │   ├── FirebaseAuthManager.kt ✅
│   │   ├── AuthRepository.kt ✅
│   │   ├── AuthViewModel.kt ✅
│   │   └── AuthResult.kt ✅
│   ├── model/
│   │   └── RealtimeData.kt ✅
│   └── viewmodel/
│       └── RealtimeDataViewModel.kt ✅
├── ui/
│   ├── screens/
│   │   ├── SplashScreen.kt ✅
│   │   ├── LoginScreen.kt ✅
│   │   ├── SignInScreen.kt ✅
│   │   ├── KilnsScreen.kt ✅
│   │   ├── ProgramsScreen.kt ✅
│   │   ├── ActivityScreen.kt ✅
│   │   ├── SettingsScreen.kt ✅
│   │   ├── CreateProgramScreen.kt ✅
│   │   ├── AddMenuScreen.kt ✅
│   │   ├── MonitoringScreen.kt ✅
│   │   └── KilnDetailScreen.kt ✅
│   ├── components/
│   │   ├── ResponsiveBottomNavigation.kt ✅
│   │   ├── Plusbtn.kt ✅
│   │   ├── CloseBtnCircle.kt ✅
│   │   ├── ProgramCard.kt ✅
│   │   ├── StageCard.kt ✅
│   │   ├── ActivityCard.kt ✅
│   │   ├── ProgramSwipeActions.kt ✅
│   │   ├── NewProgramCard.kt ✅
│   │   ├── NewKilnCard.kt ✅
│   │   ├── SearchBar.kt ✅
│   │   ├── ScrollGradient.kt ✅
│   │   ├── OrangeHexagon.kt ✅
│   │   ├── KilnCard.kt ✅
│   │   ├── RealtimeDataCard.kt ✅
│   │   └── ForgotPasswordDialog.kt ✅
│   ├── viewmodels/
│   │   ├── CreateProgramViewModel.kt ✅
│   │   ├── ProgramsListViewModel.kt ✅
│   │   └── ViewModelFactory.kt ✅
│   └── theme/
│       ├── AppColors.kt ✅
│       ├── Theme.kt ✅
│       └── Type.kt ✅
├── ble/
│   ├── HybridConnectionManager.kt ✅
│   ├── HybridConnectionState.kt ✅
│   ├── ProximityManager.kt ✅
│   ├── BleManager.kt ✅
│   └── BleConnectionState.kt ✅
├── utils/
│   └── ConnectivityManager.kt ✅
├── navigation/
│   ├── AppNavigation.kt ✅
│   └── Screen.kt ✅
└── MainActivity.kt ✅
```

---

## 🆕 **Avances Más Recientes (Octubre 2025)**

### **✅ Sistema Híbrido BLE/WiFi Completo**
1. **HybridConnectionManager**: Gestión automática de conexión BLE/WiFi
2. **Detección de Proximidad**: RSSI threshold para cambio automático de modo
3. **Fallback Inteligente**: Sistema de escaneo directo si ProximityManager falla
4. **Detección de Desconexión**: Monitoreo activo del estado de conexión ESP32
5. **Validación de Conectividad**: Verificación de internet y Firebase en tiempo real

### **✅ Sistema de Conectividad Inteligente**
1. **Estados Granulares**: BLE_CONNECTED, WIFI_ONLINE, WIFI_NO_INTERNET, WIFI_FIREBASE_DOWN, OFFLINE
2. **Iconos Contextuales**: Bluetooth para BLE, WiFi para conexión remota
3. **Textos Descriptivos**: "Conectado", "Sin internet", "Firebase no disponible", "Visto hace xxx"
4. **ConnectivityManager**: Monitoreo en tiempo real de conectividad
5. **Reactivity**: UI actualizada automáticamente según cambios de estado

### **✅ Mejoras de Robustez**
1. **Exponential Backoff**: Reintentos inteligentes para errores de escaneo BLE
2. **Timeouts Configurables**: 8 segundos para cambio de modo, 30 segundos para detección de desconexión
3. **Manejo de Errores**: Logging detallado y recuperación automática
4. **Fallback Scanning**: BleManager directo cuando ProximityManager falla consistentemente

### **✅ Sistema Completo de Gestión de Programas**
1. **Persistencia con Room Database**: Programas y etapas se guardan localmente
2. **CRUD Completo**: Crear, leer, actualizar, eliminar programas
3. **Edición y Duplicación**: Modificar programas existentes o crear copias
4. **Swipe Actions**: Borrar y duplicar con gestos intuitivos
5. **Validaciones**: Campos obligatorios y retroalimentación visual

### **✅ Mejoras de UI/UX**
1. **Scroll Gradients**: Fade-out en contenido scrolleable (4 pantallas)
2. **Swipe Unificado**: -80dp en todas las tarjetas para consistencia
3. **Menú "+" Mejorado**: Overlay con "Nuevo Horno" y "Nuevo Programa"
4. **Navegación Deshabilitada**: Tabs inferiores deshabilitados durante overlay
5. **SearchBar**: Filtrado en ProgramsScreen y ActivityScreen

### **✅ Pantallas Nuevas**
1. **ActivityScreen**: Historial de ejecuciones completadas
2. **SettingsScreen**: Configuración de cuenta, idioma y unidades
3. **AddMenuScreen**: Overlay para crear nuevo horno o programa
4. **MonitoringScreen**: Monitoreo en tiempo real con detección de conexión

### **✅ Consistencia Visual**
1. **Colores**: Paleta unificada en toda la app
2. **Tipografía**: Raleway Bold para títulos
3. **Iconos**: Hexágonos PNG con color personalizable
4. **Spacing**: Distancias estandarizadas (160dp, 180dp)

---

## 🐛 **Problemas Resueltos Recientemente**

### **Sistema de Conexión**
1. ✅ **Conexión BLE Asíncrona**: Implementado sistema de espera para conexión real
2. ✅ **Timeouts y Reintentos**: Sistema robusto de manejo de errores
3. ✅ **Fallback Scanning**: BleManager directo cuando ProximityManager falla
4. ✅ **Detección de Desconexión**: Monitoreo activo del estado ESP32
5. ✅ **Estados Granulares**: WIFI_NO_INTERNET, WIFI_FIREBASE_DOWN para mejor UX

### **UI/UX**
1. ✅ **Iconos Contextuales**: Bluetooth solo para BLE, WiFi para conexión remota
2. ✅ **Textos Descriptivos**: Estados específicos según tipo de conexión
3. ✅ **Reactivity**: UI actualizada automáticamente con cambios de estado
4. ✅ **Validación de Conectividad**: Verificación real de internet y Firebase

### **Navegación**
1. ✅ CreateProgramScreen: Navegación a Activity y Settings corregida
2. ✅ Navegación deshabilitada mientras menú "+" está abierto
3. ✅ Parámetros de navegación correctamente implementados

### **Swipe Actions**
1. ✅ Desplazamiento unificado a -80dp en todas las tarjetas
2. ✅ Posición de icono "borrar" ajustada (-8dp desde derecha)
3. ✅ Rango de detección actualizado para todas las tarjetas

### **UI/UX**
1. ✅ Gradientes de scroll aplicados en 4 pantallas
2. ✅ Icono "borrar" consistente en Activity y Programs
3. ✅ Overlay "+" con opacidad correcta (40%)
4. ✅ Botón "X" con color Primary300

---

## 📝 **Próximos Pasos**

### **⏳ Alta Prioridad**
1. **Optimización del Sistema BLE**: Mejorar estabilidad de conexión
2. **Transferencia de Programas**: Envío de programas al ESP32 vía BLE/Firebase
3. **Ejecución de Programas**: Control de inicio/pausa/stop desde la app
4. **Monitoreo de Progreso**: Visualización de progreso real del programa

### **⏳ Media Prioridad**
5. **Firebase Integration**: Backup en la nube (opcional)
6. **Tests**: Unitarios e integración
7. **Optimizaciones**: Performance y manejo de errores
8. **Modo Oscuro**: Implementar tema dark

### **⏳ Baja Prioridad**
9. **Compartir programas**: Entre usuarios
10. **Importar/Exportar**: Archivos de programas
11. **Categorías**: Organización por tipo de cerámica
12. **Notificaciones**: Alertas en tiempo real

---

## 💡 **Notas Importantes**

### **Restricciones de Diseño**
- ✅ **Curva de navegación**: NO modificar la forma personalizada
- ✅ **Colores**: Mantener consistencia con paleta Primary
- ✅ **Responsividad**: App se adapta a diferentes tamaños
- ✅ **Swipe**: -80dp estandarizado en todas las tarjetas

### **Decisiones de Arquitectura**
- ✅ **MVVM**: Separación clara de responsabilidades
- ✅ **Room Database**: Persistencia local robusta
- ✅ **Compose Navigation**: Sistema de rutas type-safe
- ✅ **Repository Pattern**: Abstracción de fuentes de datos
- ✅ **Hybrid Connection**: Sistema inteligente BLE/WiFi

### **Estado del Proyecto**
- ✅ **UI**: Completa y consistente
- ✅ **Persistencia**: Funcional con Room
- ✅ **Navegación**: Sistema completo implementado
- ✅ **Sistema Híbrido**: BLE/WiFi con fallback implementado
- ✅ **Monitoreo**: Sistema de tiempo real funcional
- ⏳ **Transferencia**: Pendiente de implementación completa
- ⏳ **Control**: Pendiente de comandos de ejecución

---

## 🔄 **Flujo de Trabajo Completo**

### **1. Agregar Nuevo Horno**
```
Usuario → Click "+" → Escaneo BLE → Detecta ESP32
   ↓
App envía KILN_INFO request → ESP32 responde con datos
   ↓
App envía WIFI_CONFIG → ESP32 guarda en NVS
   ↓
ESP32 se conecta a WiFi → Envía datos a Firebase
   ↓
App guarda horno en Room Database → Navega a "Mis Hornos"
```

### **2. Configuración Inicial del Horno**
```
App → BLE Connect → ESP32
   ↓
App → Send USER_ID → ESP32 almacena en NVS
   ↓
App → Send KILN_INFO:{JSON} → ESP32 parsea y almacena
   ↓
ESP32 → Save to NVS → Update Firebase deviceInfo
   ↓
App → Send WIFI_CONFIG:{JSON} → ESP32 configura WiFi
   ↓
ESP32 → Connect WiFi → Connect Firebase → Send realtimeData
   ↓
App → Detect Firebase data → Update UI connection status
```

### **3. Monitoreo en Tiempo Real**
```
ESP32 → TaskControlHorno → Simula temperatura cada 500ms
   ↓
ESP32 → TaskComunicaciones → BLE notifications cada 1s (si conectado)
   ↓
ESP32 → TaskComunicaciones → Firebase updates cada 5s (si WiFi activo)
   ↓
App → Observa Firebase con Flow → Actualiza UI automáticamente
   ↓
App → Verifica timestamp → Actualiza estado de conexión
   ↓
HybridConnectionManager → Decide modo BLE o WiFi según proximidad
```

### **4. Gestión de Programas**
```
Usuario → Crear/Editar Programa → Room Database
   ↓
Usuario → Seleccionar Programa → Convertir ProgramEntity a JSON
   ↓
App → Send LOAD_PROFILE:{JSON} → ESP32 parsea y almacena en curvaActual
   ↓
ESP32 → Confirm PROFILE_OK → App muestra confirmación
   ↓
Usuario → Send START → ESP32 cambia estado a "CALENTANDO"
   ↓
ESP32 → TaskControlCurva → Ejecuta programa segmento por segmento
   ↓
ESP32 → Send temperatura/estado → App actualiza UI en tiempo real
   ↓
ESP32 → Programa finalizado → Estado "FINALIZADO"
```

### **5. Sistema Híbrido BLE/WiFi**
```
App cerca del horno → ProximityManager detecta RSSI alto
   ↓
HybridConnectionManager → Switch to BLE mode
   ↓
App → Connect BLE → ESP32 detecta conexión
   ↓
ESP32 → Suspend WiFi → Priorizar BLE notifications
   ↓
App lejos del horno → BLE desconecta
   ↓
ESP32 → Wait 5 seconds → Reactivate WiFi
   ↓
ESP32 → Connect WiFi → Connect Firebase → Send realtimeData
   ↓
App → Detect Firebase data → Switch to WiFi mode
```

---

## 🔧 **ESP32 Firmware - Análisis Completo**

### **Arquitectura del Firmware**

El ESP32 implementa un sistema multitarea con 3 tareas principales:

```cpp
// Tareas principales
xTaskCreatePinnedToCore(TaskControlHorno, "ControlHorno", 4096, NULL, 1, NULL, 1);
xTaskCreatePinnedToCore(TaskComunicaciones, "Comunicaciones", 6144, NULL, 1, NULL, 0);
xTaskCreatePinnedToCore(TaskControlCurva, "ControlCurva", 6144, NULL, 1, NULL, 1);
```

### **Características Principales**

1. **BLE Server**: Servidor BLE completo con múltiples características
2. **WiFi Client**: Conexión WiFi dinámica con reconexión automática
3. **Firebase Integration**: Envío de datos en tiempo real con manejo de errores
4. **Temperature Control**: Simulación de termocupla (preparado para MAX31855)
5. **Program Execution**: Ejecución de curvas de cocción recibidas por BLE
6. **NVS Storage**: Almacenamiento persistente de configuración
7. **Sistema Híbrido**: Suspensión automática de WiFi cuando BLE está activo

### **Estructura de Datos**

#### **Curva de Cocción**
```cpp
struct Segmento {
    float tempObjetivo;      // Temperatura objetivo (°C)
    float rampaCporMin;     // Velocidad de rampa (°C/min)
    int tiempoRemojoMin;    // Tiempo de mantenimiento (min)
};

struct CurvaActiva {
    char nombre[32];                    // Nombre del programa
    Segmento segmentos[10];            // Máximo 10 segmentos
    uint8_t numSegmentos;              // Número de segmentos
    bool recibida;                     // Flag de nueva curva
};
```

#### **Variables Globales Principales**
```cpp
// Estado del sistema
float temperaturaActual = 0.0;
String estadoHorno = "IDLE";
bool wifiConnected = false;
bool firebaseConnected = false;

// Información del horno
String kilnName = "Mi Horno Cerámico";
String kilnModel = "SmartKiln - Ceramica";
float kilnVolume = 0.0;
String userId = "";
String deviceMacAddress = "";

// Control BLE/WiFi híbrido
bool bleClientConnected = false;
bool wifiSuspended = false;
unsigned long lastBleDisconnectTime = 0;
const unsigned long BLE_DISCONNECT_DELAY = 5000; // 5 segundos
```

### **Sistema BLE Completo**

#### **UUIDs de Servicio y Características**
```cpp
const char* BLE_DEVICE_NAME = "SmartKiln - Ceramica";
const char* BLE_SERVICE_UUID = "00001234-0000-1000-8000-00805f9b34fb";
const char* BLE_TEMP_CHAR_UUID   = "0000abcd-0000-1000-8000-00805f9b34fb";
const char* BLE_STATUS_CHAR_UUID = "0000dcba-0000-1000-8000-00805f9b34fb";
const char* BLE_COMMAND_CHAR_UUID= "00002211-0000-1000-8000-00805f9b34fb";
const char* BLE_CURVA_CHAR_UUID= "0000a001-0000-1000-8000-00805f9b34fb";
const char* BLE_USER_ID_CHAR_UUID = "0000a002-0000-1000-8000-00805f9b34fb";
const char* BLE_WIFI_CONFIG_CHAR_UUID = "0000a003-0000-1000-8000-00805f9b34fb";
```

#### **Callbacks BLE Implementados**

##### **1. ServerCallbacks** - Gestión de Conexión
```cpp
class ServerCallbacks : public NimBLEServerCallbacks {
    void onConnect(NimBLEServer* pServer) override {
        bleClientConnected = true;
        // Suspender WiFi para priorizar BLE
    }
    
    void onDisconnect(NimBLEServer* pServer) override {
        bleClientConnected = false;
        lastBleDisconnectTime = millis();
        // Programar reactivación WiFi en 5 segundos
    }
};
```

##### **2. CommandCallback** - Comandos de Control
```cpp
class CommandCallback : public NimBLECharacteristicCallbacks {
    void onWrite(NimBLECharacteristic* pCharacteristic) override {
        String cmd = pCharacteristic->getValue();
        
        // Comandos soportados:
        // - "KILN_INFO:{JSON}" - Configuración del horno
        // - "START" - Iniciar horneado
        // - "PAUSE" - Pausar horneado
        // - "STOP" - Detener horneado
        // - "TEST" - Comando de prueba
        // - "CLEAR_WIFI" - Limpiar credenciales WiFi
        // - "DEBUG_VARS" - Mostrar variables globales
        // - "STATUS" - Estado del sistema
    }
};
```

##### **3. CurvaCallback** - Recepción de Programas
```cpp
class CurvaCallback : public NimBLECharacteristicCallbacks {
    void onWrite(NimBLECharacteristic* pCharacteristic) override {
        String payload = pCharacteristic->getValue();
        
        // Formato esperado: "LOAD_PROFILE:{JSON}"
        // JSON contiene: nombre, numSeg, seg[temp, rampa, remojo]
        
        // Parsear JSON y almacenar en curvaActual
        // Confirmar recepción con "PROFILE_OK"
    }
};
```

##### **4. UserIdCallback** - Configuración de Usuario
```cpp
class UserIdCallback : public NimBLECharacteristicCallbacks {
    void onWrite(NimBLECharacteristic* pCharacteristic) override {
        String receivedUserId = pCharacteristic->getValue();
        userId = receivedUserId;
        // Confirmar con "USER_ID_OK"
    }
};
```

##### **5. WifiConfigCallback** - Configuración WiFi
```cpp
class WifiConfigCallback : public NimBLECharacteristicCallbacks {
    void onWrite(NimBLECharacteristic* pCharacteristic) override {
        String payload = pCharacteristic->getValue();
        
        // JSON esperado: {"ssid": "...", "password": "..."}
        // Guardar en NVS y marcar para reconexión
        // Confirmar con "WIFI_CONFIG_OK"
    }
};
```

### **Sistema de Comunicaciones (TaskComunicaciones)**

#### **Lógica Híbrida BLE/WiFi**
```cpp
void TaskComunicaciones(void* pvParameters) {
    for (;;) {
        // Control de WiFi basado en estado BLE
        if (bleClientConnected && !wifiSuspended) {
            // BLE activo - suspender WiFi
            WiFi.disconnect();
            wifiConnected = false;
            firebaseConnected = false;
            wifiSuspended = true;
        } else if (!bleClientConnected && wifiSuspended) {
            // BLE inactivo - reactivar WiFi después de delay
            if (now - lastBleDisconnectTime >= BLE_DISCONNECT_DELAY) {
                // Reconectar WiFi y Firebase
            }
        }
        
        // BLE: Actualización cada 1 segundo
        if (now - lastBLEUpdate >= 1000) {
            if (pServer->getConnectedCount() > 0) {
                gTempChar->setValue(temperaturaActual);
                gTempChar->notify();
                gStatusChar->setValue(estadoHorno);
                gStatusChar->notify();
            }
        }
        
        // Firebase: Envío cada 5 segundos (solo si WiFi no suspendido)
        if (now - lastFirebaseUpdate >= 5000 && !wifiSuspended) {
            reportToFirebase(temperaturaActual, estadoHorno);
        }
    }
}
```

### **Integración con Firebase**

#### **Estructura de Datos en Firebase**
```cpp
// Path: /users/{userId}/kilns/{macAddress}/
void reportToFirebase(float temp, const String& estado) {
    String realtimePath = "/users/" + userId + "/kilns/" + deviceMacAddress + "/realtimeData";
    
    Firebase.RTDB.setFloat(&fbdo, (realtimePath + "/temperature").c_str(), temp);
    Firebase.RTDB.setString(&fbdo, (realtimePath + "/status").c_str(), estado);
    Firebase.RTDB.setTimestamp(&fbdo, (realtimePath + "/timestamp").c_str());
}

void updateDeviceInfoFirebase() {
    String deviceInfoPath = "/users/" + userId + "/kilns/" + deviceMacAddress + "/deviceInfo";
    
    Firebase.RTDB.setString(&fbdo, (deviceInfoPath + "/name").c_str(), kilnName);
    Firebase.RTDB.setString(&fbdo, (deviceInfoPath + "/model").c_str(), kilnModel);
    Firebase.RTDB.setInt(&fbdo, (deviceInfoPath + "/volume").c_str(), (int)kilnVolume);
    Firebase.RTDB.setString(&fbdo, (deviceInfoPath + "/macAddress").c_str(), deviceMacAddress);
}
```

#### **Manejo de Errores Firebase**
```cpp
// Contador de errores consecutivos
unsigned int firebaseErrorCount = 0;

// Si hay muchos errores, desactivar temporalmente
if (firebaseErrorCount > 3) {
    firebaseConnected = false;
    firebaseErrorCount = 0;
    // Reconectar en 30 segundos
}
```

### **Almacenamiento Persistente (NVS)**

#### **Configuración del Horno**
```cpp
void saveKilnInfo() {
    preferences.begin("kiln_config", false);
    preferences.putString("name", kilnName);
    preferences.putString("model", kilnModel);
    preferences.putFloat("volume", kilnVolume);
    preferences.putString("userId", userId);
    preferences.end();
}

void loadKilnInfo() {
    preferences.begin("kiln_config", false);
    kilnName = preferences.getString("name", "Mi Kiln Cerámico");
    kilnModel = preferences.getString("model", "SmartKiln - Ceramica");
    kilnVolume = preferences.getFloat("volume", 0.0);
    userId = preferences.getString("userId", "");
    preferences.end();
}
```

#### **Configuración WiFi**
```cpp
void saveWifiConfig() {
    preferences.begin("wifi_config", false);
    preferences.putString("ssid", wifiSSID);
    preferences.putString("password", wifiPassword);
    preferences.end();
}

void loadWifiConfig() {
    preferences.begin("wifi_config", false);
    wifiSSID = preferences.getString("ssid", "");
    wifiPassword = preferences.getString("password", "");
    preferences.end();
}
```

### **Control de Curvas (TaskControlCurva)**

#### **Ejecución de Programas**
```cpp
void TaskControlCurva(void* pvParameters) {
    for (;;) {
        // Esperar curva recibida y horno en modo calentando
        if (estadoHorno == "CALENTANDO" && curvaActual.recibida) {
            
            for (int i = 0; i < curvaActual.numSegmentos; i++) {
                Segmento seg = curvaActual.segmentos[i];
                
                // Rampa de temperatura
                float rampaPorSegundo = seg.rampaCporMin / 60.0;
                while (temperaturaActual < seg.tempObjetivo && estadoHorno == "CALENTANDO") {
                    temperaturaActual += rampaPorSegundo;
                    vTaskDelay(pdMS_TO_TICKS(1000));
                }
                
                // Tiempo de mantenimiento
                unsigned long inicioRemojo = millis();
                while ((millis() - inicioRemojo) < (unsigned long)seg.tiempoRemojoMin * 60000UL
                       && estadoHorno == "CALENTANDO") {
                    vTaskDelay(pdMS_TO_TICKS(1000));
                }
            }
            
            // Finalizar programa
            estadoHorno = "FINALIZADO";
            curvaActual.recibida = false;
        }
    }
}
```

### **Simulación de Temperatura**

```cpp
float simularTermocupla() {
    static unsigned long lastUpdate = 0;
    static float temp = 25.0;
    
    if (millis() - lastUpdate > 2000) {
        lastUpdate = millis();
        temp += 5.0;
        if (temp > 1200.0) temp = 1200.0;
    }
    return temp;
}
```

### **Configuración Firebase**

```cpp
const char* FIREBASE_API_KEY = "AIzaSyBv3VknyBVxY_bMpgKgyhlVc_ImE7ejgwk";
const char* FIREBASE_DATABASE_URL = "https://ia-based-kiln-default-rtdb.firebaseio.com";

bool initFirebase() {
    config.api_key = FIREBASE_API_KEY;
    config.database_url = FIREBASE_DATABASE_URL;
    config.timeout.serverResponse = 3000;
    config.timeout.socketConnection = 3000;
    config.timeout.rtdbKeepAlive = 30000;
    config.timeout.rtdbStreamReconnect = 1000;
    
    if (Firebase.signUp(&config, &auth, "", "")) {
        Firebase.begin(&config, &auth);
        Firebase.reconnectWiFi(true);
        firebaseConnected = true;
        return true;
    }
    return false;
}
```

### **Protocolo de Comunicación Completo**

#### **Comandos BLE Soportados**

| Comando | Formato | Descripción | Respuesta |
|---------|---------|-------------|-----------|
| **KILN_INFO** | `KILN_INFO:{JSON}` | Configurar datos del horno | `[BLE] KILN_INFO_OK` |
| **LOAD_PROFILE** | `LOAD_PROFILE:{JSON}` | Cargar programa de cocción | `PROFILE_OK` |
| **WIFI_CONFIG** | `{JSON}` | Configurar credenciales WiFi | `[BLE] WIFI_CONFIG_OK` |
| **USER_ID** | `{userId}` | Establecer ID de usuario | `[BLE] USER_ID_OK` |
| **START** | `START` | Iniciar horneado | `[BLE] START\|OK` |
| **PAUSE** | `PAUSE` | Pausar horneado | `[BLE] PAUSE\|OK` |
| **STOP** | `STOP` | Detener horneado | `[BLE] STOP\|OK` |
| **TEST** | `TEST` | Comando de prueba | `[BLE] TEST_OK` |
| **STATUS** | `STATUS` | Estado del sistema | `STATUS\|{data}` |
| **DEBUG_VARS** | `DEBUG_VARS` | Variables globales | `DEBUG\|{data}` |
| **CLEAR_WIFI** | `CLEAR_WIFI` | Limpiar WiFi | `[BLE] WIFI_CLEARED` |

#### **Formato JSON para KILN_INFO**
```json
{
  "name": "Mi Horno Cerámico",
  "model": "SmartKiln - Ceramica",
  "volume": 50.0,
  "userId": "user_abc123"
}
```

#### **Formato JSON para LOAD_PROFILE**
```json
{
  "nombre": "Esmalte para Gres",
  "numSeg": 2,
  "seg": [
    {
      "temp": 850,
      "rampa": 2.0,
      "remojo": 30
    },
    {
      "temp": 1000,
      "rampa": 1.5,
      "remojo": 60
    }
  ]
}
```

#### **Formato JSON para WIFI_CONFIG**
```json
{
  "ssid": "MiWiFi",
  "password": "password123"
}
```

### **Estados del Sistema**

#### **Estados del Horno**
- `IDLE` - Horno en reposo
- `CALENTANDO` - Ejecutando programa de cocción
- `PAUSADO` - Programa pausado
- `DETENIDO` - Programa detenido
- `FINALIZADO` - Programa completado

#### **Estados de Conexión**
- `wifiConnected` - WiFi conectado
- `firebaseConnected` - Firebase conectado
- `bleClientConnected` - Cliente BLE conectado
- `wifiSuspended` - WiFi suspendido por BLE activo

### **Frecuencias de Actualización**

| Componente | Frecuencia | Condición |
|------------|------------|-----------|
| **BLE Notifications** | 1 segundo | Solo si hay cliente conectado |
| **Firebase Updates** | 5 segundos | Solo si WiFi no suspendido |
| **WiFi Check** | 5 segundos | Verificación de conexión |
| **Advertising Check** | 10 segundos | Verificación de advertising |
| **Firebase Reconnect** | 30 segundos | Si hay errores de conexión |

---

## 🧪 **Testing y Debugging**

### **Logs del Sistema**
- **HybridManager**: Gestión de conexión híbrida
- **ProximityManager**: Escaneo BLE y detección de proximidad
- **BleManager**: Operaciones BLE directas
- **KilnCard**: Actualizaciones de UI y conectividad
- **ConnectivityManager**: Validación de conectividad

### **Comandos de Debug**
```bash
# Monitorear logs del sistema híbrido
adb logcat -s HybridManager ProximityManager BleManager

# Monitorear conectividad
adb logcat -s ConnectivityManager KilnCard HybridManager

# Compilar e instalar
.\gradlew assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

### **Estados de Prueba**
1. **BLE Conectado**: Proximidad cercana, conexión estable
2. **WiFi Online**: Lejos del horno, conectado vía WiFi
3. **Sin Internet**: Sin conexión a internet
4. **Firebase Down**: Internet OK pero Firebase no disponible
5. **Desconectado**: ESP32 apagado o fuera de alcance

---

## 📚 **Dependencias Principales**

### **Android**
```gradle
// Jetpack Compose
implementation("androidx.compose.ui:ui:1.5.0")
implementation("androidx.compose.material3:material3:1.1.0")

// Navigation
implementation("androidx.navigation:navigation-compose:2.7.0")

// Room Database
implementation("androidx.room:room-runtime:2.5.0")
kapt("androidx.room:room-compiler:2.5.0")

// Firebase
implementation("com.google.firebase:firebase-database:20.2.0")
implementation("com.google.firebase:firebase-auth:22.1.0")

// BLE
implementation("no.nordicsemi.android:ble:2.6.0")

// Coil (SVG support)
implementation("io.coil-kt:coil-compose:2.5.0")
implementation("io.coil-kt:coil-svg:2.5.0")
```

### **ESP32**
```ini
[platformio]
platform = espressif32
framework = arduino

lib_deps =
  bblanchon/ArduinoJson@^6.21.3
  firebase-arduino/Firebase-ESP32@^4.3.0
  h2zero/NimBLE-Arduino@^1.4.0
  adafruit/Adafruit MAX31855 library@^1.4.0

monitor_speed = 115200
upload_speed = 921600
```

#### **Librerías Principales Utilizadas**
- **ArduinoJson**: Parsing de JSON para comandos BLE
- **Firebase-ESP32**: Integración con Firebase Realtime Database
- **NimBLE-Arduino**: Implementación BLE (más eficiente que BluetoothSerial)
- **Adafruit MAX31855**: Lectura de termocupla (preparado para implementación futura)
- **Preferences**: Almacenamiento persistente en NVS
- **WiFi**: Conexión WiFi del ESP32
- **Wire**: Comunicación I2C (para sensores futuros)

---

## 👥 **Equipo y Contacto**

- **Developer**: Pablo Raimondo
- **Platform**: Android (Kotlin) + ESP32 (C++)
- **Backend**: Firebase Realtime Database
- **Email**: praimondo1970@hotmail.com

---

## 📄 **Licencia**

[Especificar licencia del proyecto]

---

**Última actualización**: Octubre 2025  
**Versión**: 1.4  
**Estado**: Sistema híbrido BLE/WiFi funcional + ESP32 completamente documentado ✅  
**Próximo hito**: Transferencia completa de programas y control de ejecución  
**Compilación**: `.\gradlew assembleDebug` → ✅ BUILD SUCCESSFUL

