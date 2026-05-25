package com.example.mysmartkiln.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mysmartkiln.ui.components.AgregarEtapa
import com.example.mysmartkiln.ui.components.CircularTerciary
import com.example.mysmartkiln.ui.components.HomeIndicatorWhite
import com.example.mysmartkiln.ui.components.MaintenanceInput
import com.example.mysmartkiln.ui.components.SimpleNumericKeyboard
import com.example.mysmartkiln.ui.components.SpeedInput
import com.example.mysmartkiln.ui.components.TemperatureInput
import com.example.mysmartkiln.ui.theme.AppColors
import com.example.mysmartkiln.ui.theme.MySmartKilnTheme

@Composable
fun AddStageScreen_Backup(
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Estados para los campos de entrada
    var temperature by remember { mutableStateOf("") }
    var speed by remember { mutableStateOf("") }
    var maintenance by remember { mutableStateOf("") }
    
    // Estado para controlar qué campo está activo
    var activeField by remember { mutableStateOf("") }
    
    // Función para manejar las teclas del teclado numérico
    fun handleKeyPress(key: String) {
        when (key) {
            "delete" -> {
                when (activeField) {
                    "temperature" -> temperature = temperature.dropLast(1)
                    "speed" -> speed = speed.dropLast(1)
                    "maintenance" -> maintenance = maintenance.dropLast(1)
                }
            }
            else -> {
                when (activeField) {
                    "temperature" -> temperature += key
                    "speed" -> speed += key
                    "maintenance" -> maintenance += key
                }
            }
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = AppColors.Primary800)
    ) {
        // Modal Add Stage - placeholder removed
        
        // Campos de entrada en una fila
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .offset(y = (-360).dp) // 15dp más abajo (375dp - 15dp = 360dp)
                .padding(horizontal = 25.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Temperature Input
            TemperatureInput(
                modifier = Modifier.width(90.dp),
                value = temperature,
                onValueChange = { temperature = it },
                onClick = { activeField = "temperature" },
                isActive = activeField == "temperature"
            )
            
            // Speed Input
            SpeedInput(
                modifier = Modifier.width(90.dp),
                value = speed,
                onValueChange = { speed = it },
                onClick = { activeField = "speed" },
                isActive = activeField == "speed"
            )
            
            // Maintenance Input
            MaintenanceInput(
                modifier = Modifier.width(90.dp),
                value = maintenance,
                onValueChange = { maintenance = it },
                onClick = { activeField = "maintenance" },
                isActive = activeField == "maintenance"
            )
        }
        
        // Título "Agregar etapa" a la altura del icono X sobre el margen izquierdo
        AgregarEtapa(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 25.dp, y = (-480).dp) // Misma altura que el icono X, margen izquierdo
        )
        
        // Icono CircularTerciary sobre el texto Meseta
        CircularTerciary(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(x = 120.dp, y = (-480).dp) // 10dp a la derecha y 10dp hacia arriba
        )
        
        // Botón Guardar
        Button(
            onClick = { /* TODO: Implementar lógica para guardar etapa */ },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(48.dp)
                .offset(y = (-255).dp) // Distancia de 255dp desde la parte inferior
                .padding(horizontal = 25.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColors.Primary700, // Color Primary700 (#003D99)
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Agregar",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        }
        
        // Teclado numérico simplificado
        SimpleNumericKeyboard(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-50).dp), // Offset de 50dp desde la parte inferior
            onKeyPress = { key -> handleKeyPress(key) }
        )
        
        // Home Indicator
        HomeIndicatorWhite(
            modifier = Modifier
                .align(alignment = Alignment.TopStart)
                .offset(x = 0.dp, y = 810.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AddStageScreen_BackupPreview() {
    MySmartKilnTheme {
        AddStageScreen_Backup()
    }
}
