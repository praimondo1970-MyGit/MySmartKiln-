package com.example.mysmartkiln.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * Custom icons from MySmartKiln project
 */
object AppIcons {
    // SVG Icons from assets
    val Settings = "file:///android_asset/icons/ic_settings.svg"
    val Search = "file:///android_asset/icons/ic_search.svg"
    val Add = "file:///android_asset/icons/ic_add.svg"
    val Close = "file:///android_asset/icons/ic_close.svg"
    val Edit = "file:///android_asset/icons/ic_edit.svg"
    val Delete = "file:///android_asset/icons/ic_delete.svg"
    val Send = "file:///android_asset/icons/ic_send.svg"
    val AppLogo = "file:///android_asset/icons/ic_app_logo.svg"
    
    // Button icons
    val ButtonIcon = "file:///android_asset/icons/ic_button_icon.svg"
    val PlusButton = "file:///android_asset/icons/ic_plus_button.svg"
    
    // Splash screen
    val SplashGetStarted = "file:///android_asset/icons/splash_get_started.svg"
}

/**
 * Composable function to display custom SVG icons
 */
@Composable
fun AppIcon(
    iconPath: String,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    size: Dp = 24.dp
) {
    val context = LocalContext.current
    
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(iconPath)
            .build(),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        colorFilter = if (tint != Color.Unspecified) ColorFilter.tint(tint) else null,
        contentScale = ContentScale.Fit
    )
}

/**
 * Icon button with custom SVG icon
 */
@Composable
fun AppIconButton(
    iconPath: String,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
    ) {
        AppIcon(
            iconPath = iconPath,
            contentDescription = contentDescription,
            tint = tint
        )
    }
}

/**
 * Predefined icon composables for common actions
 */
@Composable
fun SettingsIcon(
    contentDescription: String? = "Settings",
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    onClick: (() -> Unit)? = null
) {
    if (onClick != null) {
        AppIconButton(
            iconPath = AppIcons.Settings,
            contentDescription = contentDescription,
            modifier = modifier,
            tint = tint,
            onClick = onClick
        )
    } else {
        AppIcon(
            iconPath = AppIcons.Settings,
            contentDescription = contentDescription,
            modifier = modifier,
            tint = tint
        )
    }
}

@Composable
fun SearchIcon(
    contentDescription: String? = "Search",
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    onClick: (() -> Unit)? = null
) {
    if (onClick != null) {
        AppIconButton(
            iconPath = AppIcons.Search,
            contentDescription = contentDescription,
            modifier = modifier,
            tint = tint,
            onClick = onClick
        )
    } else {
        AppIcon(
            iconPath = AppIcons.Search,
            contentDescription = contentDescription,
            modifier = modifier,
            tint = tint
        )
    }
}

@Composable
fun AddIcon(
    contentDescription: String? = "Add",
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    onClick: (() -> Unit)? = null
) {
    if (onClick != null) {
        AppIconButton(
            iconPath = AppIcons.Add,
            contentDescription = contentDescription,
            modifier = modifier,
            tint = tint,
            onClick = onClick
        )
    } else {
        AppIcon(
            iconPath = AppIcons.Add,
            contentDescription = contentDescription,
            modifier = modifier,
            tint = tint
        )
    }
}

@Composable
fun CloseIcon(
    contentDescription: String? = "Close",
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    onClick: (() -> Unit)? = null
) {
    if (onClick != null) {
        AppIconButton(
            iconPath = AppIcons.Close,
            contentDescription = contentDescription,
            modifier = modifier,
            tint = tint,
            onClick = onClick
        )
    } else {
        AppIcon(
            iconPath = AppIcons.Close,
            contentDescription = contentDescription,
            modifier = modifier,
            tint = tint
        )
    }
}

@Composable
fun EditIcon(
    contentDescription: String? = "Edit",
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    onClick: (() -> Unit)? = null
) {
    if (onClick != null) {
        AppIconButton(
            iconPath = AppIcons.Edit,
            contentDescription = contentDescription,
            modifier = modifier,
            tint = tint,
            onClick = onClick
        )
    } else {
        AppIcon(
            iconPath = AppIcons.Edit,
            contentDescription = contentDescription,
            modifier = modifier,
            tint = tint
        )
    }
}

@Composable
fun DeleteIcon(
    contentDescription: String? = "Delete",
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    onClick: (() -> Unit)? = null
) {
    if (onClick != null) {
        AppIconButton(
            iconPath = AppIcons.Delete,
            contentDescription = contentDescription,
            modifier = modifier,
            tint = tint,
            onClick = onClick
        )
    } else {
        AppIcon(
            iconPath = AppIcons.Delete,
            contentDescription = contentDescription,
            modifier = modifier,
            tint = tint
        )
    }
}

@Composable
fun SendIcon(
    contentDescription: String? = "Send",
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    onClick: (() -> Unit)? = null
) {
    if (onClick != null) {
        AppIconButton(
            iconPath = AppIcons.Send,
            contentDescription = contentDescription,
            modifier = modifier,
            tint = tint,
            onClick = onClick
        )
    } else {
        AppIcon(
            iconPath = AppIcons.Send,
            contentDescription = contentDescription,
            modifier = modifier,
            tint = tint
        )
    }
}

@Composable
fun AppLogoIcon(
    contentDescription: String? = "MySmartKiln Logo",
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    size: Dp = 48.dp
) {
    AppIcon(
        iconPath = AppIcons.AppLogo,
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint,
        size = size
    )
}

@Composable
fun ButtonIcon(
    contentDescription: String? = "Button Icon",
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    onClick: (() -> Unit)? = null
) {
    if (onClick != null) {
        AppIconButton(
            iconPath = AppIcons.ButtonIcon,
            contentDescription = contentDescription,
            modifier = modifier,
            tint = tint,
            onClick = onClick
        )
    } else {
        AppIcon(
            iconPath = AppIcons.ButtonIcon,
            contentDescription = contentDescription,
            modifier = modifier,
            tint = tint
        )
    }
}

@Composable
fun PlusButtonIcon(
    contentDescription: String? = "Plus Button",
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    onClick: (() -> Unit)? = null
) {
    if (onClick != null) {
        AppIconButton(
            iconPath = AppIcons.PlusButton,
            contentDescription = contentDescription,
            modifier = modifier,
            tint = tint,
            onClick = onClick
        )
    } else {
        AppIcon(
            iconPath = AppIcons.PlusButton,
            contentDescription = contentDescription,
            modifier = modifier,
            tint = tint
        )
    }
}
