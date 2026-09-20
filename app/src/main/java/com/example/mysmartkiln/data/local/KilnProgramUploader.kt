package com.example.mysmartkiln.data.local

import android.content.Context
import com.example.mysmartkiln.data.repository.ProgramRepository
import com.example.mysmartkiln.utils.WifiUtils
import kotlinx.coroutines.flow.first

/**
 * Envía programas al horno por HTTP local (sin BLE).
 */
object KilnProgramUploader {

    suspend fun uploadProgram(
        context: Context,
        programRepository: ProgramRepository,
        programId: String,
        httpClient: KilnHttpClient? = null
    ): Result<String> {
        val client = httpClient ?: KilnHttpClient(context.applicationContext)
        val onKilnWifi = WifiUtils.isOnSmartKilnNetwork(context)
        if (!onKilnWifi && !client.ping()) {
            return Result.failure(
                Exception(
                    "Conéctate al WiFi del horno (SmartKiln-...) para enviar el programa."
                )
            )
        }
        val program = programRepository.getProgramById(programId)
            ?: return Result.failure(Exception("Programa no encontrado"))
        val stages = programRepository.getStagesByProgram(programId).first()
        if (stages.isEmpty()) {
            return Result.failure(Exception("El programa no tiene etapas"))
        }
        val json = KilnProfileJsonBuilder.build(program.name, stages, program.updatedAt)
        return client.uploadProfile(json).fold(
            onSuccess = { resp ->
                if (!resp.ok) {
                    Result.failure(
                        Exception(resp.error ?: resp.message ?: "El horno rechazó el programa")
                    )
                } else {
                    Result.success(program.name)
                }
            },
            onFailure = { Result.failure(it) }
        )
    }
}
