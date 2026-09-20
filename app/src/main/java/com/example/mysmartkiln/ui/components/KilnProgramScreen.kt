package com.example.mysmartkiln.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mysmartkiln.R
import com.example.mysmartkiln.ui.theme.DialogBrand
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
    onChangeProgram: () -> Unit = {},
    onEditCurrentProgram: (programId: String) -> Unit = {}
) {
    val kilnColors = LocalKilnColors.current
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
                onChangeProgram()
            }
        }
    }

    // Misma lógica de clearance que Estado (INICIAR/STOP): resto FAB/nav.
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        val fabClearance = 112.dp
        val layoutHeight = (maxHeight - fabClearance).coerceAtLeast(280.dp)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(layoutHeight)
                .padding(horizontal = 2.dp)
        ) {
            // Cabecera fuera del marco gris: nombre, subtítulo, duración, editar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayOrDash(kilnProgram.name),
                        color = kilnColors.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = subtitle,
                        color = kilnColors.muted,
                        fontSize = 12.sp
                    )
                    if (kilnProgram.duration.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Duración total: ${kilnProgram.duration}",
                            color = kilnColors.body,
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
                    containerColor = kilnColors.fab,
                    contentColor = kilnColors.onFab
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

            Spacer(modifier = Modifier.height(10.dp))

            // Marco gris solo para las etapas
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = kilnColors.cardBackground),
                border = BorderStroke(1.dp, kilnColors.cardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                if (kilnProgram.stages.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (kilnProgram.name.isBlank()) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "No hay programa seleccionado",
                                    color = kilnColors.body,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Pulsa el lápiz o «Seleccionar programa»",
                                    color = kilnColors.muted,
                                    fontSize = 12.sp
                                )
                            }
                        } else {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = kilnColors.accent,
                                strokeWidth = 3.dp
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp, vertical = 12.dp)
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
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Misma altura visual que INICIAR/STOP en Estado
            Button(
                onClick = onChangeProgram,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .offset(y = (-10).dp)
                    .padding(horizontal = 2.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DialogBrand.primaryButton(),
                    contentColor = kilnColors.onAccent
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (kilnProgram.name.isBlank()) {
                        "Seleccionar programa"
                    } else {
                        "Cambiar programa"
                    },
                    color = kilnColors.onAccent,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
