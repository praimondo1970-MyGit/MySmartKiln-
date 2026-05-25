package com.example.mysmartkiln.navigation

/**
 * Enum que define todas las rutas de navegación de la aplicación
 */
sealed class Screen(val route: String) {
    object Kilns : Screen("kilns?startScanning={startScanning}") {
        const val BASE_ROUTE = "kilns"
        fun createRoute(startScanning: Boolean = false): String {
            return "kilns?startScanning=$startScanning"
        }
    }
    object Programs : Screen("programs")
    object Activity : Screen("activity")
    object Settings : Screen("settings")
    object CreateProgram : Screen("create_program/{programId}/{mode}") {
        const val BASE_ROUTE = "create_program"
        fun createRoute(programId: String? = null, mode: String = "new"): String {
            return if (programId != null) {
                "create_program/$programId/$mode"
            } else {
                "create_program/new/create"
            }
        }
    }
    object AddStage : Screen("add_stage")
}


