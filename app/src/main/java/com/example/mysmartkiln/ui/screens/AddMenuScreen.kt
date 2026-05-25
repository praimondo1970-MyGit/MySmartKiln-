package com.example.mysmartkiln.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mysmartkiln.ui.components.NewKilnCard
import com.example.mysmartkiln.ui.components.NewProgramCard
import com.example.mysmartkiln.ui.theme.AppColors
import com.example.mysmartkiln.ui.theme.MySmartKilnTheme

/**
 * Pantalla overlay que aparece al presionar el botón +
 * Muestra las opciones "Nuevo Horno" y "Nuevo Programa"
 */
@Composable
fun AddMenuScreen(
    onNewKilnClick: () -> Unit = {},
    onNewProgramClick: () -> Unit = {},
    onDismiss: () -> Unit = {}, // Callback para cerrar el menú al hacer click en el fondo
    modifier: Modifier = Modifier
) {
    val backgroundInteractionSource = remember { MutableInteractionSource() }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = AppColors.Primary100.copy(alpha = 0.40f)) // Fondo semi-transparente al 40% para ver el contenido debajo
            .clickable(
                interactionSource = backgroundInteractionSource,
                indication = null // Sin efecto visual
            ) { onDismiss() } // Cerrar el menú al hacer click en el fondo
    ) {
        // Contenido principal - tarjetas de selección
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            // Contenedor de las tarjetas con offset hacia abajo
            Column(
                modifier = Modifier.offset(y = 10.dp) // Mueve las tarjetas 10dp más abajo
            ) {
                // Tarjeta "Nuevo Programa" con padding horizontal
                NewProgramCard(
                    onClick = {
                        onNewProgramClick()
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Tarjeta "Nuevo Horno" con padding horizontal
                NewKilnCard(
                    onClick = {
                        onNewKilnClick()
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            
            // Espaciador para posicionar 200dp desde el límite inferior
            Spacer(modifier = Modifier.height(200.dp))
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AddMenuScreenPreview() {
    MySmartKilnTheme {
        AddMenuScreen()
    }
}

