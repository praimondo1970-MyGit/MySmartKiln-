package com.example.mysmartkiln.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mysmartkiln.R
import com.example.mysmartkiln.ui.theme.LocalKilnColors
import com.example.mysmartkiln.ui.theme.RalewayFontFamily

/**
 * Marca MiHornitoPiola para pantallas principales en tema ESP
 * (logo + nombre bicolor + leyenda).
 */
@Composable
fun EspBrandHeader(
    modifier: Modifier = Modifier,
    tagline: String = "Control de Horno IA"
) {
    val kilnColors = LocalKilnColors.current
    // "MiHornito" naranja + "Piola" blanco
    val brandName = buildAnnotatedString {
        withStyle(
            SpanStyle(
                color = kilnColors.accent,
                fontFamily = RalewayFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        ) {
            append("MiHornito")
        }
        withStyle(
            SpanStyle(
                color = kilnColors.title,
                fontFamily = RalewayFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        ) {
            append("Piola")
        }
    }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.logoicon),
            contentDescription = "MiHornitoPiola",
            modifier = Modifier.size(28.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            BasicText(text = brandName)
            BasicText(
                text = tagline,
                style = TextStyle(
                    color = kilnColors.muted,
                    fontFamily = RalewayFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.sp,
                    lineHeight = 12.sp
                )
            )
        }
    }
}
