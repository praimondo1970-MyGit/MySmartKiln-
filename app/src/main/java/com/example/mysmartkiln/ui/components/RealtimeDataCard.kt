package com.example.mysmartkiln.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mysmartkiln.R
import com.example.mysmartkiln.data.model.RealtimeData
import com.example.mysmartkiln.ui.theme.AppColors

/**
 * Tarjeta que muestra los datos en tiempo real del horno
 * Incluye temperatura, estado e indicador de conexión
 */
@Composable
fun RealtimeDataCard(
    realtimeData: RealtimeData?,
    isConnected: Boolean,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Título y estado de conexión
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Estado del Horno",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.Primary800
                )
                
                ConnectionIndicator(
                    isConnected = isConnected,
                    isLoading = isLoading
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Content: Temperatura y Estado
            if (isLoading && realtimeData == null) {
                // Estado de carga inicial
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = AppColors.Primary600
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Temperatura
                    DataItem(
                        icon = R.drawable.icon_termometro,
                        label = "Temperatura",
                        value = realtimeData?.getFormattedTemperature() ?: "--°C",
                        isConnected = isConnected
                    )
                    
                    // Divisor vertical
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(60.dp)
                            .background(AppColors.Primary200)
                    )
                    
                    // Estado
                    DataItem(
                        icon = R.drawable.icon_estado,
                        label = "Estado",
                        value = realtimeData?.getDisplayStatus() ?: "Desconocido",
                        isConnected = isConnected
                    )
                }
            }
        }
    }
}

/**
 * Indicador visual del estado de conexión
 */
@Composable
private fun ConnectionIndicator(
    isConnected: Boolean,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(12.dp),
                strokeWidth = 2.dp,
                color = AppColors.Primary600
            )
        } else {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(
                        color = if (isConnected) Color(0xFF4CAF50) else Color(0xFFFF5252),
                        shape = CircleShape
                    )
            )
        }
        
        Text(
            text = when {
                isLoading -> "Conectando..."
                isConnected -> "En línea"
                else -> "Desconectado"
            },
            style = MaterialTheme.typography.bodySmall,
            color = when {
                isLoading -> AppColors.Primary600
                isConnected -> Color(0xFF4CAF50)
                else -> Color(0xFFFF5252)
            },
            fontSize = 12.sp
        )
    }
}

/**
 * Item individual de datos (temperatura o estado)
 */
@Composable
private fun DataItem(
    icon: Int,
    label: String,
    value: String,
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = label,
            tint = if (isConnected) AppColors.Primary600 else AppColors.Primary300,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.Primary500,
            fontSize = 11.sp
        )
        
        Spacer(modifier = Modifier.height(2.dp))
        
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = if (isConnected) AppColors.Primary800 else AppColors.Primary400,
            fontSize = 18.sp
        )
    }
}













































