package com.example.mysmartkiln.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mysmartkiln.R
import com.example.mysmartkiln.ui.theme.AppColors
import com.example.mysmartkiln.ui.viewmodels.KilnDetailViewModel

@Composable
fun KilnCurveScreen(
    kilnMacAddress: String,
    viewModel: KilnDetailViewModel
) {
    val kilnStatus by viewModel.kilnStatus.collectAsState()

    LaunchedEffect(kilnMacAddress) {
        viewModel.loadKilnData(kilnMacAddress)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(450.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Title
            Text(
                text = "Temperatura y Programa",
                color = AppColors.Primary700,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Program line legend
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(20.dp)
                            .height(3.dp)
                            .background(AppColors.Primary700)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Programa",
                        color = AppColors.Primary700,
                        fontSize = 14.sp
                    )
                }

                // Real line legend
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(20.dp)
                            .height(3.dp)
                            .background(Color(0xFFFF6F00))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Real",
                        color = Color(0xFFFF6F00),
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Temperature Curve Chart
            TemperatureCurveChart(
                currentTemperature = kilnStatus.currentTemperature,
                targetTemperature = kilnStatus.targetTemperature
            )
        }
    }
}

@Composable
fun TemperatureCurveChart(
    currentTemperature: Float,
    targetTemperature: Float
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .background(
                Color.LightGray.copy(alpha = 0.1f),
                RoundedCornerShape(8.dp)
            )
    ) {
        // Chart placeholder - in a real implementation, you would use a chart library
        // For now, we'll show a simple representation
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.icon_chart),
                contentDescription = "Chart",
                tint = AppColors.Primary400,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Gráfico de Temperatura",
                color = AppColors.Primary700,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = "Actual: ${currentTemperature.toInt()}°C | Objetivo: ${targetTemperature.toInt()}°C",
                color = AppColors.Primary400,
                fontSize = 12.sp
            )
        }
    }
}
