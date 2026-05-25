package com.example.mysmartkiln.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

/**
 * BUTTON DESIGN STANDARDS FOR MYSMARTKILN PROJECT
 * 
 * This file establishes the standardized button formatting and color system
 * for the entire MySmartKiln application. All developers should follow these
 * guidelines to ensure consistency across the project.
 * 
 * ============================================================================
 * BUTTON TYPES AND USAGE
 * ============================================================================
 * 
 * 1. PRIMARY BUTTONS (AppPrimaryButton)
 *    - Use for main actions (Submit, Save, Continue, etc.)
 *    - Should be the most prominent button on the screen
 *    - Only one primary button per screen/section
 * 
 * 2. SECONDARY BUTTONS (AppSecondaryButton)
 *    - Use for secondary actions (Cancel, Back, Alternative options)
 *    - Outlined style with border
 *    - Less prominent than primary buttons
 * 
 * 3. TERTIARY BUTTONS (AppTertiaryButton)
 *    - Use for less important actions (Skip, Learn More, etc.)
 *    - Text-only style
 *    - Least prominent button type
 * 
 * 4. ICON BUTTONS (AppIconButton)
 *    - Use for actions that can be represented by icons
 *    - Compact design for toolbars, headers, etc.
 * 
 * 5. FLOATING ACTION BUTTONS (AppFloatingActionButton)
 *    - Use for primary actions that float over content
 *    - Typically for "Add" or "Create" actions
 * 
 * ============================================================================
 * COLOR STANDARDS
 * ============================================================================
 * 
 * ACTIVE STATES:
 * - Primary: AppColors.Primary500 (Blue #0066FF)
 * - Secondary: AppColors.Secondary500 (Red #FF3043)
 * - Tertiary: AppColors.Tertiary500 (Yellow #FBE741)
 * 
 * DISABLED STATES:
 * - Container: Primary color with 50% opacity (alpha = 0.5f)
 * - Content: OnPrimary color with 50% opacity (alpha = 0.5f)
 * - Example: MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
 * 
 * HOVER/PRESSED STATES:
 * - Slightly darker shade of the base color
 * - Handled automatically by Material3 elevation system
 * 
 * ============================================================================
 * SIZING STANDARDS
 * ============================================================================
 * 
 * HEIGHT:
 * - Default: 48.dp (Material3 standard)
 * - Large: 56.dp (for important actions)
 * - Small: 40.dp (for compact layouts)
 * 
 * CORNER RADIUS:
 * - Default: 12.dp (Material3 standard)
 * - Large: 16.dp (for prominent buttons)
 * - Small: 8.dp (for compact buttons)
 * 
 * PADDING:
 * - Horizontal: 16.dp (default)
 * - Vertical: 12.dp (default)
 * - Icon spacing: 8.dp
 * 
 * ============================================================================
 * TYPOGRAPHY STANDARDS
 * ============================================================================
 * 
 * TEXT STYLES:
 * - Primary/Secondary: MaterialTheme.typography.labelLarge
 * - Tertiary: MaterialTheme.typography.labelMedium
 * - Custom: Use AppTypes.type_Button_Large for consistency
 * 
 * FONT WEIGHT:
 * - Default: FontWeight.Medium
 * - Important actions: FontWeight.SemiBold
 * 
 * ============================================================================
 * ACCESSIBILITY STANDARDS
 * ============================================================================
 * 
 * - Minimum touch target: 48.dp x 48.dp
 * - High contrast ratios for text on backgrounds
 * - Clear visual feedback for all states
 * - Descriptive content descriptions for icon buttons
 * 
 * ============================================================================
 * IMPLEMENTATION EXAMPLES
 * ============================================================================
 * 
 * // Primary Button
 * AppPrimaryButton(
 *     text = "Comenzar",
 *     onClick = { /* action */ },
 *     modifier = Modifier.fillMaxWidth(),
 *     enabled = true,
 *     cornerRadius = 16f
 * )
 * 
 * // Secondary Button
 * AppSecondaryButton(
 *     text = "Cancelar",
 *     onClick = { /* action */ },
 *     modifier = Modifier.fillMaxWidth()
 * )
 * 
 * // Disabled Button
 * AppPrimaryButton(
 *     text = "Procesando...",
 *     onClick = { /* action */ },
 *     enabled = false // Automatically applies disabled styling
 * )
 * 
 * ============================================================================
 * STATE MANAGEMENT
 * ============================================================================
 * 
 * LOADING STATES:
 * - Use loading parameter for async operations
 * - Shows CircularProgressIndicator with proper spacing
 * - Automatically disables button during loading
 * 
 * ENABLED/DISABLED STATES:
 * - Use enabled parameter to control interactivity
 * - Disabled buttons automatically get reduced opacity
 * - Always provide visual feedback for state changes
 * 
 * ============================================================================
 * BEST PRACTICES
 * ============================================================================
 * 
 * 1. CONSISTENCY:
 *    - Always use the provided button components
 *    - Don't create custom button styles unless absolutely necessary
 *    - Follow the established color and sizing standards
 * 
 * 2. HIERARCHY:
 *    - Use primary buttons sparingly (max 1 per screen)
 *    - Establish clear visual hierarchy with button types
 *    - Group related actions together
 * 
 * 3. ACCESSIBILITY:
 *    - Provide meaningful button text
 *    - Use appropriate content descriptions for icons
 *    - Ensure sufficient color contrast
 * 
 * 4. RESPONSIVENESS:
 *    - Use fullWidth for important actions on mobile
 *    - Consider different screen sizes and orientations
 *    - Test on various devices and accessibility tools
 * 
 * 5. PERFORMANCE:
 *    - Use remember for expensive computations in button content
 *    - Avoid unnecessary recompositions
 *    - Optimize button click handlers
 * 
 * ============================================================================
 * MIGRATION GUIDE
 * ============================================================================
 * 
 * When updating existing buttons to use these standards:
 * 
 * 1. Replace custom Button composables with AppPrimaryButton, etc.
 * 2. Update color references to use AppColors constants
 * 3. Standardize corner radius values (8dp, 12dp, 16dp)
 * 4. Ensure consistent text styling with MaterialTheme.typography
 * 5. Add proper enabled/disabled state handling
 * 6. Test all button states (active, disabled, loading, pressed)
 * 
 * ============================================================================
 * FUTURE ENHANCEMENTS
 * ============================================================================
 * 
 * - Dark mode support (already implemented in theme)
 * - Animation support for state transitions
 * - Custom button variants for specific use cases
 * - Accessibility improvements based on user feedback
 * - Performance optimizations for large button lists
 */

/**
 * Standard button colors following the design system
 */
object ButtonColors {
    /**
     * Get the standard button colors for a given type and state
     */
    @Composable
    fun getButtonColors(
        buttonType: AppButtonType,
        enabled: Boolean = true
    ): androidx.compose.material3.ButtonColors {
        val baseColors = when (buttonType) {
            AppButtonType.Primary -> Pair(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.onPrimary
            )
            AppButtonType.Secondary -> Pair(
                MaterialTheme.colorScheme.secondary,
                MaterialTheme.colorScheme.onSecondary
            )
            AppButtonType.Tertiary -> Pair(
                MaterialTheme.colorScheme.tertiary,
                MaterialTheme.colorScheme.onTertiary
            )
        }
        
        return ButtonDefaults.buttonColors(
            containerColor = if (enabled) baseColors.first else baseColors.first.copy(alpha = 0.5f),
            contentColor = if (enabled) baseColors.second else baseColors.second.copy(alpha = 0.5f),
            disabledContainerColor = baseColors.first.copy(alpha = 0.5f),
            disabledContentColor = baseColors.second.copy(alpha = 0.5f)
        )
    }
    
    /**
     * Get the standard outlined button colors for secondary buttons
     */
    @Composable
    fun getOutlinedButtonColors(
        enabled: Boolean = true
    ): androidx.compose.material3.ButtonColors {
        val baseColor = MaterialTheme.colorScheme.secondary
        return ButtonDefaults.outlinedButtonColors(
            contentColor = if (enabled) baseColor else baseColor.copy(alpha = 0.5f),
            disabledContentColor = baseColor.copy(alpha = 0.5f)
        )
    }
    
    /**
     * Get the standard text button colors for tertiary buttons
     */
    @Composable
    fun getTextButtonColors(
        enabled: Boolean = true
    ): androidx.compose.material3.ButtonColors {
        val baseColor = MaterialTheme.colorScheme.tertiary
        return ButtonDefaults.textButtonColors(
            contentColor = if (enabled) baseColor else baseColor.copy(alpha = 0.5f),
            disabledContentColor = baseColor.copy(alpha = 0.5f)
        )
    }
}

/**
 * Standard button dimensions following the design system
 */
object ButtonDimensions {
    val DefaultHeight = 48.dp
    val LargeHeight = 56.dp
    val SmallHeight = 40.dp
    
    val DefaultCornerRadius = 12.dp
    val LargeCornerRadius = 16.dp
    val SmallCornerRadius = 8.dp
    
    val DefaultPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    val IconSpacing = 8.dp
}

/**
 * Standard button text styles following the design system
 */
object ButtonTextStyles {
    @Composable
    fun getPrimaryTextStyle(): TextStyle = MaterialTheme.typography.labelLarge
    
    @Composable
    fun getSecondaryTextStyle(): TextStyle = MaterialTheme.typography.labelLarge
    
    @Composable
    fun getTertiaryTextStyle(): TextStyle = MaterialTheme.typography.labelMedium
    
    @Composable
    fun getCustomTextStyle(): TextStyle = com.example.mysmartkiln.ui.theme.AppTypes.type_Button_Large
}

/**
 * Standard button shapes following the design system
 */
object ButtonShapes {
    val Default = RoundedCornerShape(ButtonDimensions.DefaultCornerRadius)
    val Large = RoundedCornerShape(ButtonDimensions.LargeCornerRadius)
    val Small = RoundedCornerShape(ButtonDimensions.SmallCornerRadius)
}

/**
 * Utility functions for button styling
 */
object ButtonUtils {
    /**
     * Get the appropriate modifier for button sizing
     */
    fun getSizeModifier(
        size: ButtonSize = ButtonSize.Default,
        fullWidth: Boolean = false
    ): Modifier {
        return Modifier
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
            .height(
                when (size) {
                    ButtonSize.Small -> ButtonDimensions.SmallHeight
                    ButtonSize.Default -> ButtonDimensions.DefaultHeight
                    ButtonSize.Large -> ButtonDimensions.LargeHeight
                }
            )
    }
    
    /**
     * Get the appropriate corner radius for button shapes
     */
    fun getCornerRadius(size: ButtonSize = ButtonSize.Default): Float {
        return when (size) {
            ButtonSize.Small -> ButtonDimensions.SmallCornerRadius.value
            ButtonSize.Default -> ButtonDimensions.DefaultCornerRadius.value
            ButtonSize.Large -> ButtonDimensions.LargeCornerRadius.value
        }
    }
}

/**
 * Button size variants
 */
enum class ButtonSize {
    Small,
    Default,
    Large
}
