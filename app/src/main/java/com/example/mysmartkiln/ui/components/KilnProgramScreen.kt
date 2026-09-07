package com.example.mysmartkiln.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.mysmartkiln.ui.theme.LocalKilnColors
import com.example.mysmartkiln.ui.viewmodels.KilnDetailViewModel
import com.example.mysmartkiln.ui.viewmodels.ProgramStage
import java.util.Locale
import kotlinx.coroutines.launch

private fun displayOrDash(value: String): String = value.trim().ifEmpty { "—" }

private fun ProgramStage.toStageData(): StageData {
    val tempText = if (targetTemp == targetTemp.toLong().toFloat()) {
        targetTemp.toLong().toString()
    } else {
        String.format(Locale.getDefault(), "%.0f", targetTemp)
    }
    val speedText = if (rampRate == rampRate.toLong().toFloat()) {
        rampRate.toLong().toString()
    } else {
        String.format(Locale.getDefault(), "%.1f", rampRate).trimEnd('0').trimEnd('.')
    }
    return StageData(
        stageNumber = stageNumber,
        temperature = tempText,
        speed = speedText,
        maintenance = maintenanceMinutes.toString()
    )
}

@Composable
fun KilnProgramScreen(
    kilnMacAddress: String,
    viewModel: KilnDetailViewModel,
    onChangeProgram: () -> Unit = {
},
    onEditCurrentProgram: (programId: String) -> Unit = {}
) {
    val kilnProgram by viewModel.kilnProgram.collectAsState()
    val kilnStatus by viewModel.kilnStatus.collectAsState()
    val loadedProgramId by viewModel.loadedProgramId.collectAsState()
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(kilnMacAddress) {
        viewModel.loadKilnData(kilnMacAddress)
    }

    LaunchedEffect(kilnMacAddress, kilnProgram.name) {
        viewModel.ensureProgramStagesLoaded()
    }

    val subtitle = when {
        kilnProgram.name.isBlank() -> "Sin programa cargado"
        kilnProgram.stages.isEmpty() -> "Cargando etapas…"
        kilnStatus.isRunning || kilnStatus.isPaused -> "Horneado en curso"
        kilnStatus.espStatus == "INTERRUMPIDO" -> "Programa interrumpido"
        else -> "${kilnProgram.stages.size} etapas · listo para iniciar"
    }

    val currentStageNumber = kilnStatus.currentStage.coerceAtLeast(0)
    val isActiveRun = kilnStatus.isRunning || kilnStatus.isPaused ||
        kilnStatus.espStatus in setOf("CALENTANDO", "PAUSADO", "INTERRUMPIDO")

    fun openEditCurrentProgram() {
        scope.launch {
            val programId = loadedProgramId ?: viewModel.resolveCurrentProgramIdForEdit()
            if (programId != null) {
                onEditCurrentProgram(programId)
            } else {
                // Sin programa resoluble en Room → elegir uno
                onChangeProgram()
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(450.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LocalKilnColors.current.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayOrDash(kilnProgram.name),
                        color = LocalKilnColors.current.body,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = subtitle,
                        color = LocalKilnColors.current.muted,
                        fontSize = 12.sp
                    )
                    if (kilnProgram.duration.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Duración total: ${kilnProgram.duration}",
                            color = LocalKilnColors.current.body,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                FloatingActionButton(
                    onClick = {
                        if (kilnProgram.name.isBlank()) {
                            onChangeProgram()
                        } else {
                            openEditCurrentProgram()
                        }
                    },
                    modifier = Modifier.size(40.dp),
                    containerColor = LocalKilnColors.current.fab,
                    contentColor = LocalKilnColors.current.onFab
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.icon_edit),
                        contentDescription = if (kilnProgram.name.isBlank()) {
                            "Seleccionar programa"
                        } else {
                            "Editar programa actual"
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (kilnProgram.stages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (kilnProgram.name.isBlank()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No hay programa seleccionado",
                                color = LocalKilnColors.current.body,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Pulsa el lápiz o «Seleccionar programa»",
                                color = LocalKilnColors.current.muted,
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = LocalKilnColors.current.body,
                            strokeWidth = 3.dp
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    kilnProgram.stages.forEach { stage ->
                        val isCurrent = isActiveRun && currentStageNumber > 0 &&
                            stage.stageNumber == currentStageNumber
                        val isCompleted = isActiveRun && currentStageNumber > 0 &&
                            stage.stageNumber < currentStageNumber

                        ReadOnlyStageCard(
                            stageData = stage.toStageData(),
                            isCurrentStage = isCurrent,
                            isCompleted = isCompleted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onChangeProgram,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LocalKilnColors.current.accent
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (kilnProgram.name.isBlank()) "Seleccionar programa" else "Cambiar programa",
                    color = LocalKilnColors.current.onAccent,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
