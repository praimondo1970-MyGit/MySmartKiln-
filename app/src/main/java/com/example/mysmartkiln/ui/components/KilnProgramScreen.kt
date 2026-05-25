package com.example.mysmartkiln.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mysmartkiln.R
import com.example.mysmartkiln.ui.theme.AppColors
import com.example.mysmartkiln.ui.viewmodels.KilnDetailViewModel

@Composable
fun KilnProgramScreen(
    kilnMacAddress: String,
    viewModel: KilnDetailViewModel,
    onChangeProgram: () -> Unit = {}
) {
    val kilnProgram by viewModel.kilnProgram.collectAsState()

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
            // Header with program name and edit button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = kilnProgram.name,
                        color = AppColors.Primary700,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "Last update was 15 min ago",
                        color = AppColors.Primary400,
                        fontSize = 12.sp
                    )
                }

                // Edit button
                FloatingActionButton(
                    onClick = onChangeProgram,
                    modifier = Modifier.size(40.dp),
                    containerColor = Color(0xFFFF6F00),
                    contentColor = Color.White
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.icon_edit),
                        contentDescription = "Edit",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Program parameters grid
            ProgramParametersGrid(kilnProgram = kilnProgram)

            Spacer(modifier = Modifier.height(24.dp))

            // Modify program button
            Button(
                onClick = { viewModel.modifyProgram() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.Primary700
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Modificar programa",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ProgramParametersGrid(kilnProgram: com.example.mysmartkiln.ui.viewmodels.KilnProgram) {
    val parameters = listOf(
        ProgramParameter(
            icon = R.drawable.icon_calendar,
            value = kilnProgram.startDate,
            label = "Start date"
        ),
        ProgramParameter(
            icon = R.drawable.icon_clock,
            value = kilnProgram.startTime,
            label = "Start time"
        ),
        ProgramParameter(
            icon = R.drawable.icon_duration,
            value = kilnProgram.duration,
            label = "Duration"
        ),
        ProgramParameter(
            icon = R.drawable.icon_remaining,
            value = kilnProgram.remaining,
            label = "Remaining"
        ),
        ProgramParameter(
            icon = R.drawable.icon_elapsed,
            value = kilnProgram.elapsed,
            label = "Elapse"
        ),
        ProgramParameter(
            icon = R.drawable.icon_stage,
            value = kilnProgram.stage,
            label = "Stage"
        )
    )

    // 2 columns grid
    for (i in parameters.indices step 2) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // First column
            ProgramParameterCard(
                parameter = parameters[i],
                modifier = Modifier.weight(1f)
            )
            
            // Second column (if exists)
            if (i + 1 < parameters.size) {
                ProgramParameterCard(
                    parameter = parameters[i + 1],
                    modifier = Modifier.weight(1f)
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
        
        if (i + 2 < parameters.size) {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ProgramParameterCard(
    parameter: ProgramParameter,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.Primary50),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(id = parameter.icon),
                contentDescription = parameter.label,
                tint = AppColors.Primary700,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = parameter.value,
                color = AppColors.Primary700,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                text = parameter.label,
                color = AppColors.Primary400,
                fontSize = 10.sp
            )
        }
    }
}

data class ProgramParameter(
    val icon: Int,
    val value: String,
    val label: String
)
