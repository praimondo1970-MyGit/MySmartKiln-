package com.example.mysmartkiln.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import com.example.mysmartkiln.R
import com.example.mysmartkiln.ui.theme.AppColors
import com.example.mysmartkiln.ui.theme.AppTypes

/**
 * Custom button components for MySmartKiln app
 */

/**
 * Primary button with app styling following design system standards
 */
@Composable
fun AppPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    fullWidth: Boolean = false,
    cornerRadius: Float = 12f,
    textStyle: TextStyle? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = if (fullWidth) modifier.fillMaxWidth() else modifier,
        colors = ButtonColors.getButtonColors(AppButtonType.Primary, enabled && !loading),
        shape = RoundedCornerShape(cornerRadius.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        )
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = textStyle ?: ButtonTextStyles.getPrimaryTextStyle()
        )
    }
}

/**
 * Secondary button with app styling following design system standards
 */
@Composable
fun AppSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    fullWidth: Boolean = false,
    cornerRadius: Float = 12f
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = if (fullWidth) modifier.fillMaxWidth() else modifier,
        colors = ButtonColors.getOutlinedButtonColors(enabled && !loading),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (enabled && !loading) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(cornerRadius.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = MaterialTheme.colorScheme.secondary,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = ButtonTextStyles.getSecondaryTextStyle()
        )
    }
}

/**
 * Tertiary button with app styling following design system standards
 */
@Composable
fun AppTertiaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    fullWidth: Boolean = false,
    cornerRadius: Float = 12f
) {
    TextButton(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = if (fullWidth) modifier.fillMaxWidth() else modifier,
        colors = ButtonColors.getTextButtonColors(enabled && !loading),
        shape = RoundedCornerShape(cornerRadius.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = MaterialTheme.colorScheme.tertiary,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = ButtonTextStyles.getTertiaryTextStyle()
        )
    }
}

/**
 * Icon button with custom styling following design system standards
 */
@Composable
fun AppIconButton(
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    buttonType: AppButtonType = AppButtonType.Primary,
    cornerRadius: Float = 12f
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        colors = ButtonColors.getButtonColors(buttonType, enabled),
        shape = RoundedCornerShape(cornerRadius.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        )
    ) {
        icon()
    }
}

/**
 * Floating action button with app styling
 */
@Composable
fun AppFloatingActionButton(
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    androidx.compose.material3.FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = AppColors.Primary500,
        contentColor = AppColors.White,
        elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(
            defaultElevation = 6.dp,
            pressedElevation = 8.dp
        )
    ) {
        icon()
    }
}

/**
 * Button with icon and text following design system standards
 */
@Composable
fun AppButtonWithIcon(
    text: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    buttonType: AppButtonType = AppButtonType.Primary,
    fullWidth: Boolean = false,
    cornerRadius: Float = 12f
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = if (fullWidth) modifier.fillMaxWidth() else modifier,
        colors = ButtonColors.getButtonColors(buttonType, enabled),
        shape = RoundedCornerShape(cornerRadius.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            Spacer(modifier = Modifier.width(ButtonDimensions.IconSpacing))
            Text(
                text = text,
                style = ButtonTextStyles.getPrimaryTextStyle()
            )
        }
    }
}

/**
 * Custom button components for splash screen based on provided code
 */

@Composable
fun TypePrimarySizeDefaultStateActiveIconNewValue(
    modifier: Modifier = Modifier,
    iconL5048: Boolean = false,
    iconR50421: Boolean = false,
    haslabel50434: Boolean = true,
    label50447: String = "Comenzar ahora",
    onClick: () -> Unit = {}
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .requiredWidth(width = 332.dp)
            .clip(shape = RoundedCornerShape(12.dp))
            .background(color = AppColors.color_Primary_400)
            .padding(all = 16.dp)
            .clickable { onClick() }
    ) {
        TypeduplicateSize16ColorBlack(iconR50421 = iconL5048)
        if (haslabel50434) {
            Text(
                text = label50447,
                color = AppColors.color_Primary_800,
                lineHeight = 1.21.em,
                style = AppTypes.type_Button_Large
            )
        }
        TypeduplicateSize16ColorBlack(iconR50421 = iconR50421)
    }
}

@Composable
fun TypeduplicateSize16ColorBlack(
    modifier: Modifier = Modifier,
    iconR50421: Boolean = false
) {
    if (iconR50421) {
        Image(
            painter = painterResource(id = R.drawable.plusicon),
            contentDescription = "Icon",
            modifier = modifier
                .requiredSize(size = 16.dp)
        )
    }
}

@Composable
fun TypeTerciarySizeDefaultStateActiveIconNewValue(
    modifier: Modifier = Modifier,
    iconL5048: Boolean = false,
    label50447: String = "Ya tengo una cuenta",
    iconR50421: Boolean = false,
    haslabel50434: Boolean = true,
    onClick: () -> Unit = {}
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .requiredWidth(width = 332.dp)
            .clip(shape = RoundedCornerShape(12.dp))
            .background(color = AppColors.color_Terciary_800)
            .padding(all = 16.dp)
            .clickable { onClick() }
    ) {
        TypeduplicateSize16ColorBlack(iconR50421 = iconL5048)
        if (haslabel50434) {
            Text(
                text = label50447,
                color = AppColors.color_Primary_800,
                lineHeight = 1.21.em,
                style = AppTypes.type_Button_Large
            )
        }
        TypeduplicateSize16ColorBlack(iconR50421 = iconR50421)
    }
}

@Composable
fun HomeIndicatorWhite(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .requiredWidth(width = 390.dp)
            .requiredHeight(height = 34.dp)
    ) {
        Box(
            modifier = Modifier
                .align(alignment = Alignment.BottomCenter)
                .offset(x = 0.dp, y = (-8).dp)
                .requiredWidth(width = 134.dp)
                .requiredHeight(height = 5.dp)
                .clip(shape = RoundedCornerShape(100.dp))
                .background(color = AppColors.color_Primary_700)
        )
    }
}

/**
 * Button types available in the design system
 */
enum class AppButtonType {
    Primary,
    Secondary,
    Tertiary
}
