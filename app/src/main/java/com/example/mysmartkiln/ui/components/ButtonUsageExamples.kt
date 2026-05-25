package com.example.mysmartkiln.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * BUTTON USAGE EXAMPLES AND GUIDELINES
 * 
 * This file provides comprehensive examples of how to use the standardized
 * button components in the MySmartKiln application. These examples serve as
 * a reference for developers and ensure consistent implementation across the project.
 * 
 * ============================================================================
 * BASIC BUTTON USAGE
 * ============================================================================
 */

@Composable
fun ButtonUsageExamples() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        
        // ========================================================================
        // PRIMARY BUTTONS
        // ========================================================================
        
        Text(
            text = "Primary Buttons",
            style = MaterialTheme.typography.headlineSmall
        )
        
        // Basic primary button
        AppPrimaryButton(
            text = "Comenzar",
            onClick = { /* Handle click */ }
        )
        
        // Full width primary button
        AppPrimaryButton(
            text = "Guardar Cambios",
            onClick = { /* Handle click */ },
            modifier = Modifier.fillMaxWidth()
        )
        
        // Primary button with custom corner radius
        AppPrimaryButton(
            text = "Continuar",
            onClick = { /* Handle click */ },
            cornerRadius = 16f
        )
        
        // Disabled primary button
        AppPrimaryButton(
            text = "Procesando...",
            onClick = { /* Handle click */ },
            enabled = false
        )
        
        // Loading primary button
        AppPrimaryButton(
            text = "Cargando",
            onClick = { /* Handle click */ },
            loading = true
        )
        
        // ========================================================================
        // SECONDARY BUTTONS
        // ========================================================================
        
        Text(
            text = "Secondary Buttons",
            style = MaterialTheme.typography.headlineSmall
        )
        
        // Basic secondary button
        AppSecondaryButton(
            text = "Cancelar",
            onClick = { /* Handle click */ }
        )
        
        // Full width secondary button
        AppSecondaryButton(
            text = "Volver",
            onClick = { /* Handle click */ },
            modifier = Modifier.fillMaxWidth()
        )
        
        // Disabled secondary button
        AppSecondaryButton(
            text = "No disponible",
            onClick = { /* Handle click */ },
            enabled = false
        )
        
        // ========================================================================
        // TERTIARY BUTTONS
        // ========================================================================
        
        Text(
            text = "Tertiary Buttons",
            style = MaterialTheme.typography.headlineSmall
        )
        
        // Basic tertiary button
        AppTertiaryButton(
            text = "Omitir",
            onClick = { /* Handle click */ }
        )
        
        // Full width tertiary button
        AppTertiaryButton(
            text = "Más información",
            onClick = { /* Handle click */ },
            modifier = Modifier.fillMaxWidth()
        )
        
        // Disabled tertiary button
        AppTertiaryButton(
            text = "No disponible",
            onClick = { /* Handle click */ },
            enabled = false
        )
        
        // ========================================================================
        // ICON BUTTONS
        // ========================================================================
        
        Text(
            text = "Icon Buttons",
            style = MaterialTheme.typography.headlineSmall
        )
        
        // Primary icon button
        AppIconButton(
            icon = { Icon(Icons.Default.Add, contentDescription = "Agregar") },
            onClick = { /* Handle click */ }
        )
        
        // Secondary icon button
        AppIconButton(
            icon = { Icon(Icons.Default.Edit, contentDescription = "Editar") },
            onClick = { /* Handle click */ },
            buttonType = AppButtonType.Secondary
        )
        
        // Tertiary icon button
        AppIconButton(
            icon = { Icon(Icons.Default.Delete, contentDescription = "Eliminar") },
            onClick = { /* Handle click */ },
            buttonType = AppButtonType.Tertiary
        )
        
        // Disabled icon button
        AppIconButton(
            icon = { Icon(Icons.Default.Edit, contentDescription = "Editar") },
            onClick = { /* Handle click */ },
            enabled = false
        )
        
        // ========================================================================
        // BUTTONS WITH ICONS AND TEXT
        // ========================================================================
        
        Text(
            text = "Buttons with Icons and Text",
            style = MaterialTheme.typography.headlineSmall
        )
        
        // Primary button with icon
        AppButtonWithIcon(
            text = "Crear Nuevo",
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            onClick = { /* Handle click */ }
        )
        
        // Secondary button with icon
        AppButtonWithIcon(
            text = "Editar Perfil",
            icon = { Icon(Icons.Default.Edit, contentDescription = null) },
            onClick = { /* Handle click */ },
            buttonType = AppButtonType.Secondary
        )
        
        // Full width button with icon
        AppButtonWithIcon(
            text = "Continuar al Siguiente Paso",
            icon = { Icon(Icons.Default.ArrowForward, contentDescription = null) },
            onClick = { /* Handle click */ },
            modifier = Modifier.fillMaxWidth()
        )
        
        // ========================================================================
        // FLOATING ACTION BUTTONS
        // ========================================================================
        
        Text(
            text = "Floating Action Buttons",
            style = MaterialTheme.typography.headlineSmall
        )
        
        // Basic floating action button
        AppFloatingActionButton(
            icon = { Icon(Icons.Default.Add, contentDescription = "Agregar") },
            onClick = { /* Handle click */ }
        )
        
        // Disabled floating action button
        AppFloatingActionButton(
            icon = { Icon(Icons.Default.Add, contentDescription = "Agregar") },
            onClick = { /* Handle click */ },
            enabled = false
        )
    }
}

/**
 * REAL-WORLD USAGE EXAMPLES
 * 
 * These examples show how buttons are typically used in common UI patterns
 * within the MySmartKiln application.
 */

@Composable
fun RealWorldButtonExamples() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        
        // ========================================================================
        // FORM SUBMISSION PATTERN
        // ========================================================================
        
        Text(
            text = "Form Submission Pattern",
            style = MaterialTheme.typography.headlineSmall
        )
        
        // Primary action (submit) and secondary action (cancel)
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppPrimaryButton(
                text = "Guardar Configuración",
                onClick = { /* Save form */ },
                modifier = Modifier.fillMaxWidth()
            )
            
            AppSecondaryButton(
                text = "Cancelar",
                onClick = { /* Cancel form */ },
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        // ========================================================================
        // CONFIRMATION DIALOG PATTERN
        // ========================================================================
        
        Text(
            text = "Confirmation Dialog Pattern",
            style = MaterialTheme.typography.headlineSmall
        )
        
        // Destructive action (delete) and cancel action
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppSecondaryButton(
                text = "Eliminar Horno",
                onClick = { /* Delete kiln */ },
                modifier = Modifier.fillMaxWidth()
            )
            
            AppTertiaryButton(
                text = "Cancelar",
                onClick = { /* Cancel deletion */ },
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        // ========================================================================
        // LOADING STATE PATTERN
        // ========================================================================
        
        Text(
            text = "Loading State Pattern",
            style = MaterialTheme.typography.headlineSmall
        )
        
        var isLoading by remember { mutableStateOf(false) }
        
        AppPrimaryButton(
            text = if (isLoading) "Conectando..." else "Conectar Horno",
            onClick = { 
                isLoading = true
                // Simulate async operation
            },
            loading = isLoading,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        )
        
        // ========================================================================
        // NAVIGATION PATTERN
        // ========================================================================
        
        Text(
            text = "Navigation Pattern",
            style = MaterialTheme.typography.headlineSmall
        )
        
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppPrimaryButton(
                text = "Continuar",
                onClick = { /* Navigate to next screen */ },
                modifier = Modifier.fillMaxWidth()
            )
            
            AppTertiaryButton(
                text = "Omitir este paso",
                onClick = { /* Skip to next section */ },
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        // ========================================================================
        // TOOLBAR ACTIONS PATTERN
        // ========================================================================
        
        Text(
            text = "Toolbar Actions Pattern",
            style = MaterialTheme.typography.headlineSmall
        )
        
        // Row of icon buttons for toolbar actions
        androidx.compose.foundation.layout.Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AppIconButton(
                icon = { Icon(Icons.Default.Edit, contentDescription = "Editar") },
                onClick = { /* Edit action */ }
            )
            
            AppIconButton(
                icon = { Icon(Icons.Default.Delete, contentDescription = "Eliminar") },
                onClick = { /* Delete action */ },
                buttonType = AppButtonType.Secondary
            )
            
            AppIconButton(
                icon = { Icon(Icons.Default.Add, contentDescription = "Agregar") },
                onClick = { /* Add action */ },
                buttonType = AppButtonType.Tertiary
            )
        }
    }
}

/**
 * ACCESSIBILITY EXAMPLES
 * 
 * These examples demonstrate proper accessibility practices for buttons.
 */

@Composable
fun AccessibilityButtonExamples() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        
        Text(
            text = "Accessibility Best Practices",
            style = MaterialTheme.typography.headlineSmall
        )
        
        // Good: Descriptive button text
        AppPrimaryButton(
            text = "Guardar configuración del horno",
            onClick = { /* Save kiln configuration */ }
        )
        
        // Good: Icon button with proper content description
        AppIconButton(
            icon = { 
                Icon(
                    Icons.Default.Add, 
                    contentDescription = "Agregar nuevo horno a la lista"
                ) 
            },
            onClick = { /* Add new kiln */ }
        )
        
        // Good: Button with icon and descriptive text
        AppButtonWithIcon(
            text = "Eliminar horno seleccionado",
            icon = { 
                Icon(
                    Icons.Default.Delete, 
                    contentDescription = null // Text already describes the action
                ) 
            },
            onClick = { /* Delete selected kiln */ },
            buttonType = AppButtonType.Secondary
        )
        
        // Good: Disabled state with clear indication
        AppPrimaryButton(
            text = "Conectar (Horno no disponible)",
            onClick = { /* Connect to kiln */ },
            enabled = false
        )
    }
}

/**
 * RESPONSIVE DESIGN EXAMPLES
 * 
 * These examples show how buttons adapt to different screen sizes and orientations.
 */

@Composable
fun ResponsiveButtonExamples() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        
        Text(
            text = "Responsive Design Examples",
            style = MaterialTheme.typography.headlineSmall
        )
        
        // Mobile-first: Full width buttons for important actions
        AppPrimaryButton(
            text = "Comenzar Escaneo",
            onClick = { /* Start scanning */ },
            modifier = Modifier.fillMaxWidth()
        )
        
        // Tablet/Desktop: Fixed width buttons with proper spacing
        androidx.compose.foundation.layout.Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppPrimaryButton(
                text = "Guardar",
                onClick = { /* Save action */ }
            )
            
            AppSecondaryButton(
                text = "Cancelar",
                onClick = { /* Cancel action */ }
            )
        }
        
        // Adaptive sizing based on content
        AppPrimaryButton(
            text = "Configuración Avanzada",
            onClick = { /* Open advanced settings */ },
            cornerRadius = 16f // Larger corner radius for more prominent actions
        )
    }
}

/**
 * PERFORMANCE OPTIMIZATION EXAMPLES
 * 
 * These examples demonstrate best practices for button performance.
 */

@Composable
fun PerformanceOptimizedButtonExamples() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        
        Text(
            text = "Performance Optimization Examples",
            style = MaterialTheme.typography.headlineSmall
        )
        
        // Good: Stable click handler with remember
        val stableClickHandler = remember {
            { /* Handle click with stable reference */ }
        }
        
        AppPrimaryButton(
            text = "Optimized Button",
            onClick = stableClickHandler
        )
        
        // Good: Conditional rendering to avoid unnecessary recompositions
        var showAdvancedButton by remember { mutableStateOf(false) }
        
        AppPrimaryButton(
            text = "Toggle Advanced Options",
            onClick = { showAdvancedButton = !showAdvancedButton }
        )
        
        if (showAdvancedButton) {
            AppSecondaryButton(
                text = "Advanced Configuration",
                onClick = { /* Advanced action */ }
            )
        }
        
        // Good: Efficient loading state management
        var isLoading by remember { mutableStateOf(false) }
        
        AppPrimaryButton(
            text = "Process Data",
            onClick = { 
                isLoading = true
                // Simulate async operation
            },
            loading = isLoading,
            enabled = !isLoading
        )
    }
}
