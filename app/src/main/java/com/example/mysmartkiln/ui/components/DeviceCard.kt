package com.example.mysmartkiln.ui.components

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mysmartkiln.ui.theme.AppColors

/**
 * Tarjeta para mostrar un dispositivo BLE encontrado
 * Basada en el diseño de ActivityCard
 */
@SuppressLint("MissingPermission")
@Composable
fun DeviceCard(
    device: BluetoothDevice,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(55.dp)
            .clickable(onClick = onClick)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            // Icono de Bluetooth a la izquierda (más pequeño)
            androidx.compose.material3.Icon(
                painter = androidx.compose.ui.res.painterResource(id = android.R.drawable.stat_sys_data_bluetooth),
                contentDescription = "Bluetooth",
                tint = AppColors.Primary700,
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Información del dispositivo
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Bottom
            ) {
                // Nombre del dispositivo
                Text(
                    text = device.name ?: "Dispositivo Desconocido",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = AppColors.Primary800
                    )
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                // MAC Address
                Text(
                    text = "MAC: ${device.address}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = AppColors.Primary600,
                        fontSize = 12.sp
                    )
                )
            }
        }
    }
}

