package com.example.mysmartkiln.data.local

import java.util.concurrent.ConcurrentHashMap

/**
 * Caché en memoria del último estado/telemetría por MAC.
 *
 * Evita el flash HORNEANDO → DESCONECTADO → PAUSA en la lista de hornos
 * cuando se vuelve desde el detalle: la tarjeta se remonta con remember
 * vacío y Firebase tarda un instante (o entrega un snapshot viejo).
 */
object KilnTelemetryCache {

    data class Entry(
        val status: String,
        val temperature: Float?,
        /** Timestamp del ESP en realtimeData (epoch ms). */
        val timestampMs: Long,
        val programName: String?,
        /** Reloj del teléfono al actualizar la entrada. */
        val updatedAtWallMs: Long,
        /** true si viene de SoftAP/comando local (prioridad breve sobre RTDB). */
        val fromLocalCommand: Boolean = false
    )

    private val byMac = ConcurrentHashMap<String, Entry>()

    fun normalizeMac(mac: String): String =
        mac.replace(":", "").replace("-", "").uppercase()

    fun get(mac: String): Entry? = byMac[normalizeMac(mac)]

    fun putFromFirebase(
        mac: String,
        status: String,
        temperature: Float?,
        timestampMs: Long,
        programName: String?
    ) {
        val key = normalizeMac(mac)
        val now = System.currentTimeMillis()
        val existing = byMac[key]
        // Tras PAUSE/START local, no dejar que un RTDB atrasado pise ~12 s.
        if (existing != null &&
            existing.fromLocalCommand &&
            now - existing.updatedAtWallMs < LOCAL_HOLD_MS
        ) {
            val remote = status.trim().uppercase()
            val held = existing.status.trim().uppercase()
            if (remote.isNotEmpty() && remote != held) {
                byMac[key] = existing.copy(
                    temperature = temperature ?: existing.temperature,
                    timestampMs = timestampMs.takeIf { it > 0L } ?: existing.timestampMs,
                    programName = programName?.takeIf { it.isNotBlank() } ?: existing.programName
                )
                return
            }
        }
        byMac[key] = Entry(
            status = status,
            temperature = temperature,
            timestampMs = timestampMs,
            programName = programName,
            updatedAtWallMs = now,
            fromLocalCommand = false
        )
    }

    fun putFromLocal(
        mac: String,
        status: String,
        temperature: Float? = null,
        programName: String? = null
    ) {
        val key = normalizeMac(mac)
        val existing = byMac[key]
        val now = System.currentTimeMillis()
        byMac[key] = Entry(
            status = status,
            temperature = temperature ?: existing?.temperature,
            timestampMs = maxOf(existing?.timestampMs ?: 0L, now),
            programName = programName?.takeIf { it.isNotBlank() } ?: existing?.programName,
            updatedAtWallMs = now,
            fromLocalCommand = true
        )
    }

    fun isWallFresh(mac: String, maxAgeMs: Long = SEED_FRESH_MS): Boolean {
        val e = get(mac) ?: return false
        return System.currentTimeMillis() - e.updatedAtWallMs < maxAgeMs
    }

    private const val LOCAL_HOLD_MS = 12_000L
    const val SEED_FRESH_MS = 60_000L
}
