# MySmartKiln Button System

This document provides a comprehensive guide to the standardized button system used throughout the MySmartKiln application.

## Overview

The MySmartKiln button system provides a consistent, accessible, and maintainable approach to button implementation across the entire application. All buttons follow Material Design 3 principles and are built with Jetpack Compose.

## Quick Start

```kotlin
import com.example.mysmartkiln.ui.components.AppPrimaryButton

@Composable
fun MyScreen() {
    AppPrimaryButton(
        text = "Comenzar",
        onClick = { /* Handle click */ }
    )
}
```

## Button Types

### 1. Primary Buttons (`AppPrimaryButton`)
- **Use for**: Main actions (Submit, Save, Continue, etc.)
- **Appearance**: Filled button with primary color
- **Guideline**: Only one primary button per screen/section

```kotlin
AppPrimaryButton(
    text = "Guardar Configuración",
    onClick = { /* Save action */ },
    modifier = Modifier.fillMaxWidth()
)
```

### 2. Secondary Buttons (`AppSecondaryButton`)
- **Use for**: Secondary actions (Cancel, Back, Alternative options)
- **Appearance**: Outlined button with border
- **Guideline**: Less prominent than primary buttons

```kotlin
AppSecondaryButton(
    text = "Cancelar",
    onClick = { /* Cancel action */ },
    modifier = Modifier.fillMaxWidth()
)
```

### 3. Tertiary Buttons (`AppTertiaryButton`)
- **Use for**: Less important actions (Skip, Learn More, etc.)
- **Appearance**: Text-only button
- **Guideline**: Least prominent button type

```kotlin
AppTertiaryButton(
    text = "Omitir",
    onClick = { /* Skip action */ }
)
```

### 4. Icon Buttons (`AppIconButton`)
- **Use for**: Actions represented by icons
- **Appearance**: Compact button with icon only
- **Guideline**: Perfect for toolbars and headers

```kotlin
AppIconButton(
    icon = { Icon(Icons.Default.Add, contentDescription = "Agregar") },
    onClick = { /* Add action */ }
)
```

### 5. Buttons with Icons (`AppButtonWithIcon`)
- **Use for**: Actions that benefit from both icon and text
- **Appearance**: Button with icon and text side by side
- **Guideline**: Great for complex actions

```kotlin
AppButtonWithIcon(
    text = "Crear Nuevo",
    icon = { Icon(Icons.Default.Add, contentDescription = null) },
    onClick = { /* Create action */ }
)
```

### 6. Floating Action Buttons (`AppFloatingActionButton`)
- **Use for**: Primary floating actions
- **Appearance**: Circular floating button
- **Guideline**: Typically for "Add" or "Create" actions

```kotlin
AppFloatingActionButton(
    icon = { Icon(Icons.Default.Add, contentDescription = "Agregar") },
    onClick = { /* Add action */ }
)
```

## Button States

### Enabled/Disabled States
All buttons automatically handle enabled/disabled states with proper visual feedback:

```kotlin
AppPrimaryButton(
    text = "Procesando...",
    onClick = { /* Action */ },
    enabled = false // Automatically applies disabled styling
)
```

### Loading States
Primary, secondary, and tertiary buttons support loading states:

```kotlin
AppPrimaryButton(
    text = "Conectando...",
    onClick = { /* Action */ },
    loading = true // Shows loading indicator
)
```

## Styling Options

### Corner Radius
Customize button corner radius for different visual styles:

```kotlin
AppPrimaryButton(
    text = "Continuar",
    onClick = { /* Action */ },
    cornerRadius = 16f // Larger radius for more prominent actions
)
```

### Full Width
Make buttons span the full width of their container:

```kotlin
AppPrimaryButton(
    text = "Guardar",
    onClick = { /* Action */ },
    modifier = Modifier.fillMaxWidth()
)
```

### Custom Text Styles
Override default text styling when needed:

```kotlin
AppPrimaryButton(
    text = "Título Especial",
    onClick = { /* Action */ },
    textStyle = MaterialTheme.typography.titleMedium
)
```

## Color System

The button system uses a standardized color palette:

- **Primary**: Blue (#0066FF) - Main actions
- **Secondary**: Red (#FF3043) - Secondary/destructive actions  
- **Tertiary**: Yellow (#FBE741) - Tertiary actions
- **Disabled**: 50% opacity of base color

### Disabled Button Colors
Disabled buttons automatically use:
- Container: `MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)`
- Content: `MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)`

## Common Patterns

### Form Submission
```kotlin
Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    AppPrimaryButton(
        text = "Guardar",
        onClick = { /* Save form */ },
        modifier = Modifier.fillMaxWidth()
    )
    
    AppSecondaryButton(
        text = "Cancelar",
        onClick = { /* Cancel form */ },
        modifier = Modifier.fillMaxWidth()
    )
}
```

### Confirmation Dialog
```kotlin
Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    AppPrimaryButton(
        text = "Eliminar",
        onClick = { /* Delete action */ },
        buttonType = AppButtonType.Secondary, // Use secondary for destructive actions
        modifier = Modifier.fillMaxWidth()
    )
    
    AppTertiaryButton(
        text = "Cancelar",
        onClick = { /* Cancel deletion */ },
        modifier = Modifier.fillMaxWidth()
    )
}
```

### Loading State
```kotlin
var isLoading by remember { mutableStateOf(false) }

AppPrimaryButton(
    text = if (isLoading) "Procesando..." else "Procesar",
    onClick = { 
        isLoading = true
        // Handle async operation
    },
    loading = isLoading,
    enabled = !isLoading,
    modifier = Modifier.fillMaxWidth()
)
```

### Toolbar Actions
```kotlin
Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    AppIconButton(
        icon = { Icon(Icons.Default.Edit, contentDescription = "Editar") },
        onClick = { /* Edit action */ }
    )
    
    AppIconButton(
        icon = { Icon(Icons.Default.Delete, contentDescription = "Eliminar") },
        onClick = { /* Delete action */ },
        buttonType = AppButtonType.Secondary
    )
}
```

## Accessibility Guidelines

### Content Descriptions
Always provide meaningful content descriptions for icon buttons:

```kotlin
// Good
AppIconButton(
    icon = { Icon(Icons.Default.Add, contentDescription = "Agregar nuevo horno") },
    onClick = { /* Add kiln */ }
)

// Avoid
AppIconButton(
    icon = { Icon(Icons.Default.Add, contentDescription = "Button") },
    onClick = { /* Add kiln */ }
)
```

### Button Text
Use descriptive button text that clearly indicates the action:

```kotlin
// Good
AppPrimaryButton(
    text = "Guardar configuración del horno",
    onClick = { /* Save kiln config */ }
)

// Avoid
AppPrimaryButton(
    text = "OK",
    onClick = { /* Save kiln config */ }
)
```

### Touch Targets
All buttons meet the minimum 48dp touch target requirement for accessibility.

## Performance Best Practices

### Stable Click Handlers
Use `remember` for click handlers to avoid unnecessary recompositions:

```kotlin
val stableClickHandler = remember {
    { /* Handle click with stable reference */ }
}

AppPrimaryButton(
    text = "Optimized Button",
    onClick = stableClickHandler
)
```

### Conditional Rendering
Use conditional rendering to avoid unnecessary button creation:

```kotlin
var showAdvancedButton by remember { mutableStateOf(false) }

if (showAdvancedButton) {
    AppSecondaryButton(
        text = "Advanced Options",
        onClick = { /* Advanced action */ }
    )
}
```

## Migration Guide

When updating existing buttons to use the new system:

1. **Replace custom Button composables** with `AppPrimaryButton`, `AppSecondaryButton`, etc.
2. **Update color references** to use the standardized color system
3. **Standardize corner radius values** (8dp, 12dp, 16dp)
4. **Ensure consistent text styling** with `MaterialTheme.typography`
5. **Add proper enabled/disabled state handling**
6. **Test all button states** (active, disabled, loading, pressed)

### Before (Custom Button)
```kotlin
Button(
    onClick = { /* Action */ },
    colors = ButtonDefaults.buttonColors(
        containerColor = Color.Blue,
        contentColor = Color.White
    )
) {
    Text("Custom Button")
}
```

### After (Standardized Button)
```kotlin
AppPrimaryButton(
    text = "Standardized Button",
    onClick = { /* Action */ }
)
```

## Troubleshooting

### Common Issues

1. **Button not responding to clicks**
   - Check if `enabled` parameter is set to `false`
   - Verify `onClick` handler is properly defined

2. **Inconsistent button colors**
   - Ensure you're using the standardized button components
   - Check if custom colors are overriding the theme

3. **Accessibility issues**
   - Verify content descriptions are provided for icon buttons
   - Ensure button text is descriptive and meaningful

4. **Performance issues**
   - Use `remember` for click handlers
   - Avoid creating buttons in conditional blocks unnecessarily

## Examples

For comprehensive examples of button usage, see:
- `ButtonUsageExamples.kt` - Basic usage examples
- `ButtonStandards.kt` - Technical documentation
- `EmptyKilnsScreen.kt` - Real-world implementation

## Support

For questions or issues with the button system:
1. Check this documentation first
2. Review the examples in `ButtonUsageExamples.kt`
3. Consult the technical standards in `ButtonStandards.kt`
4. Contact the development team for additional support



