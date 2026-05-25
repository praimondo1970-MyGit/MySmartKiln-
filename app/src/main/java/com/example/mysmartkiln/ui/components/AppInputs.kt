package com.example.mysmartkiln.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
// Using custom icons instead of Material icons
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mysmartkiln.ui.theme.AppColors

/**
 * Custom input components for MySmartKiln app
 */

/**
 * Primary text field with app styling
 */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    enabled: Boolean = true,
    error: String? = null,
    supportingText: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = if (enabled) AppColors.Neutral900 else AppColors.Neutral500
            )
        }
        
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            placeholder = placeholder?.let { 
                { 
                    Text(
                        text = it,
                        color = AppColors.Neutral500
                    ) 
                } 
            },
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            isError = error != null,
            singleLine = singleLine,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppColors.Primary500,
                unfocusedBorderColor = AppColors.Neutral300,
                errorBorderColor = AppColors.Error500,
                disabledBorderColor = AppColors.Neutral200,
                focusedTextColor = AppColors.Neutral900,
                unfocusedTextColor = AppColors.Neutral900,
                errorTextColor = AppColors.Neutral900,
                disabledTextColor = AppColors.Neutral500,
                focusedLabelColor = AppColors.Primary500,
                unfocusedLabelColor = AppColors.Neutral600,
                errorLabelColor = AppColors.Error500,
                disabledLabelColor = AppColors.Neutral400
            ),
            shape = RoundedCornerShape(8.dp)
        )
        
        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.Error500
            )
        } else if (supportingText != null) {
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.Neutral600
            )
        }
    }
}

/**
 * Password field with visibility toggle
 */
@Composable
fun AppPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    enabled: Boolean = true,
    error: String? = null,
    supportingText: String? = null
) {
    var passwordVisible by remember { mutableStateOf(false) }
    
    AppTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label,
        placeholder = placeholder,
        enabled = enabled,
        error = error,
        supportingText = supportingText,
        trailingIcon = {
            IconButton(
                onClick = { passwordVisible = !passwordVisible }
            ) {
                Text(
                    text = if (passwordVisible) "👁️" else "🙈",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        },
        keyboardType = KeyboardType.Password
    )
}

/**
 * Search field with search icon
 */
@Composable
fun AppSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Buscar...",
    enabled: Boolean = true
) {
    AppTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = placeholder,
        enabled = enabled,
        leadingIcon = {
            Text(
                text = "🔍",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    )
}

/**
 * Filled text field variant
 */
@Composable
fun AppFilledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    enabled: Boolean = true,
    error: String? = null,
    supportingText: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = if (enabled) AppColors.Neutral900 else AppColors.Neutral500
            )
        }
        
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            placeholder = placeholder?.let { 
                { 
                    Text(
                        text = it,
                        color = AppColors.Neutral500
                    ) 
                } 
            },
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            isError = error != null,
            singleLine = singleLine,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = AppColors.Neutral50,
                unfocusedContainerColor = AppColors.Neutral100,
                errorContainerColor = AppColors.Error50,
                disabledContainerColor = AppColors.Neutral200,
                focusedIndicatorColor = AppColors.Primary500,
                unfocusedIndicatorColor = AppColors.Neutral300,
                errorIndicatorColor = AppColors.Error500,
                disabledIndicatorColor = AppColors.Neutral200,
                focusedTextColor = AppColors.Neutral900,
                unfocusedTextColor = AppColors.Neutral900,
                errorTextColor = AppColors.Neutral900,
                disabledTextColor = AppColors.Neutral500,
                focusedLabelColor = AppColors.Primary500,
                unfocusedLabelColor = AppColors.Neutral600,
                errorLabelColor = AppColors.Error500,
                disabledLabelColor = AppColors.Neutral400
            ),
            shape = RoundedCornerShape(8.dp)
        )
        
        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.Error500
            )
        } else if (supportingText != null) {
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.Neutral600
            )
        }
    }
}

/**
 * Input states for demonstration
 */
@Composable
fun InputStatesDemo(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Normal state
        AppTextField(
            value = "Texto normal",
            onValueChange = { },
            label = "Campo Normal",
            placeholder = "Ingresa texto aquí"
        )
        
        // Error state
        AppTextField(
            value = "Texto con error",
            onValueChange = { },
            label = "Campo con Error",
            error = "Este campo es requerido"
        )
        
        // Disabled state
        AppTextField(
            value = "Texto deshabilitado",
            onValueChange = { },
            label = "Campo Deshabilitado",
            enabled = false
        )
        
        // Password field
        AppPasswordField(
            value = "password123",
            onValueChange = { },
            label = "Contraseña",
            placeholder = "Ingresa tu contraseña"
        )
        
        // Search field
        AppSearchField(
            value = "búsqueda",
            onValueChange = { },
            placeholder = "Buscar hornos..."
        )
    }
}
