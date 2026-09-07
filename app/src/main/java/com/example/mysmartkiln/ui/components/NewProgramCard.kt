package com.example.mysmartkiln.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mysmartkiln.R
import com.example.mysmartkiln.ui.theme.AppColors
import com.example.mysmartkiln.ui.theme.LocalKilnColors
import com.example.mysmartkiln.ui.theme.MySmartKilnTheme

/**
 * Componente para la tarjeta "Nuevo Programa"
 * Muestra un fondo blanco con imagen bg_new_item_card2 superpuesta y el icono/texto blanco
 */
@Composable
fun NewProgramCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(83.dp) // Incrementada 10% (75dp * 1.1 = 82.5dp ≈ 83dp)
            .clip(shape = RoundedCornerShape(16.dp)) // Reducido para coincidir con las imágenes
            .background(color = LocalKilnColors.current.cardBackground) // Fondo blanco
            .clickable { onClick() }
    ) {
        // Imagen superpuesta sobre el fondo blanco - responsive al ancho de la tarjeta
        Image(
            painter = painterResource(id = R.drawable.bg_new_item_card2),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomStart) // Alineada desde extremo inferior izquierdo
                .fillMaxWidth() // Responsive: toma todo el ancho disponible
                .height(83.dp), // Altura exacta de la tarjeta
            contentScale = ContentScale.FillBounds // Deforma la imagen para llenar exactamente el área
        )
        
        // Contenedor del hexágono e icono
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 24.dp)
                .size(44.dp) // Aumentado a 44dp para coincidir con el hexágono
        ) {
            // Hexágono azul como fondo
            OrangeHexagon(
                modifier = Modifier.size(44.dp)
            )
            
            // Icono de programa - centrado automáticamente dentro del Box/hexágono
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.nav_programas),
                contentDescription = "Icono programa",
                tint = LocalKilnColors.current.onAccent,
                modifier = Modifier
                    .align(Alignment.Center) // Centrado automático dentro del Box
                    .size(24.dp) // Reducido proporcionalmente (28dp * 40/48 = ~23dp, redondeado a 24dp)
            )
        }
        
        // Texto "Nuevo Programa"
        Text(
            text = "Nuevo Programa",
            style = TextStyle(
                fontSize = 17.sp, // Reducido mínimamente a 17sp
                fontWeight = FontWeight.SemiBold,
                color = LocalKilnColors.current.muted // Azul
            ),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 80.dp)
        )
    }
}

@Preview(widthDp = 350, heightDp = 83)
@Composable
private fun NewProgramCardPreview() {
    MySmartKilnTheme {
        NewProgramCard()
    }
}

