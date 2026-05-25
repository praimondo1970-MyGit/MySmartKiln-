package com.example.mysmartkiln.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mysmartkiln.ui.components.BackIcon
import com.example.mysmartkiln.ui.components.CrearUnPrograma
import com.example.mysmartkiln.ui.components.Etapas
import com.example.mysmartkiln.ui.components.HomeIndicatorWhite
import com.example.mysmartkiln.ui.components.Plusbtn
import com.example.mysmartkiln.ui.components.TextFieldWithLabel
import com.example.mysmartkiln.ui.theme.AppColors
import com.example.mysmartkiln.ui.theme.MySmartKilnTheme

@Composable
fun CreateProgramScreen_Backup(
    onNavigateBack: () -> Unit = {},
    onNavigateToAddStage: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var programName by remember { mutableStateOf("") }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = AppColors.Primary100)
    ) {
        // Icono de regreso
        BackIcon(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 25.dp, top = 70.dp)
        )
        
        // Título "Crear un programa"
        CrearUnPrograma(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 25.dp, top = 125.dp)
        )
        
        // Campo de entrada "Nombre"
        TextFieldWithLabel(
            label = "Nombre",
            value = programName,
            onValueChange = { programName = it },
            placeholder = "Ingresa el nombre del programa",
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 25.dp, top = 180.dp, end = 25.dp)
        )
        
        // Botón + alineado con el texto "Etapas"
        Plusbtn(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 25.dp, top = 290.dp),
            onClick = { onNavigateToAddStage() }
        )
        
        // Título "Etapas"
        Etapas(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 25.dp, top = 295.dp)
        )
        
        // Botón "Crear" al pie de la pantalla
        Button(
            onClick = { /* TODO: Implementar lógica para crear programa */ },
            enabled = true, // Siempre activo
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 25.dp, vertical = 90.dp)
                .height(48.dp),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = AppColors.Primary700, // Azul cuando está activo
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Crear",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        }
        
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
fun CreateProgramScreen_BackupPreview() {
    MySmartKilnTheme {
        CreateProgramScreen_Backup()
    }
}
