package com.example.mysmartkiln.data.local



import android.content.Context

import com.example.mysmartkiln.utils.KilnWifiNetworkBinder

import com.google.gson.Gson

import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.withContext

import java.io.BufferedReader

import java.io.InputStreamReader

import java.io.OutputStreamWriter

import java.net.HttpURLConnection

import java.net.URL



class KilnHttpClient(

    private val appContext: Context? = null,

    private val baseUrl: String = KilnApiDefaults.BASE_URL,

    private val gson: Gson = Gson()

) {



    suspend fun ping(): Boolean = withContext(Dispatchers.IO) {

        try {

            val connection = openConnection("/api/status", "GET").apply {

                connectTimeout = 1500

                readTimeout = 1500

            }

            val ok = connection.responseCode in 200..299

            connection.disconnect()

            ok

        } catch (e: Exception) {

            false

        }

    }



    suspend fun getStatus(): Result<KilnStatusDto> = withContext(Dispatchers.IO) {

        requestGet("/api/status") { body ->

            gson.fromJson(body, KilnStatusDto::class.java)

        }

    }



    suspend fun getApInfo(): Result<KilnApInfoDto> = withContext(Dispatchers.IO) {

        requestGet("/api/ap-info") { body ->

            gson.fromJson(body, KilnApInfoDto::class.java)

        }

    }



    suspend fun getInfo(): Result<KilnInfoDto> = withContext(Dispatchers.IO) {

        requestGet("/api/info") { body ->

            gson.fromJson(body, KilnInfoDto::class.java)

        }

    }



    suspend fun sendCommand(cmd: String): Result<KilnCommandResponseDto> = withContext(Dispatchers.IO) {

        val jsonBody = gson.toJson(mapOf("cmd" to cmd))

        requestPost("/api/command", jsonBody) { body ->

            gson.fromJson(body, KilnCommandResponseDto::class.java)

        }

    }



    suspend fun uploadProfile(profileJson: String): Result<KilnProfileResponseDto> =

        withContext(Dispatchers.IO) {

            requestPost(
                path = "/api/profile",
                jsonBody = profileJson,
                connectTimeoutMs = 5000,
                readTimeoutMs = 8000
            ) { body ->

                gson.fromJson(body, KilnProfileResponseDto::class.java)

            }

        }



    suspend fun uploadKilnInfo(kilnInfoJson: String): Result<KilnInfoUploadResponseDto> =

        withContext(Dispatchers.IO) {

            requestPost(
                path = "/api/kiln-info",
                jsonBody = kilnInfoJson,
                connectTimeoutMs = 4000,
                readTimeoutMs = 4000
            ) { body ->

                gson.fromJson(body, KilnInfoUploadResponseDto::class.java)

            }

        }



    private fun openConnection(path: String, method: String): HttpURLConnection {

        val url = URL("$baseUrl$path")

        val ctx = appContext

        return if (ctx != null) {

            KilnWifiNetworkBinder.openHttpConnection(ctx, url)

        } else {

            url.openConnection() as HttpURLConnection

        }.apply {

            requestMethod = method

        }

    }



    private inline fun <T> requestGet(

        path: String,

        parse: (String) -> T

    ): Result<T> {

        return try {

            val connection = openConnection(path, "GET").apply {

                connectTimeout = 5000

                readTimeout = 5000

            }

            val code = connection.responseCode

            val body = readBody(connection, code in 200..299)

            connection.disconnect()

            if (code in 200..299) {

                Result.success(parse(body))

            } else {

                Result.failure(Exception("HTTP $code: $body"))

            }

        } catch (e: Exception) {

            Result.failure(e)

        }

    }



    private inline fun <T> requestPost(

        path: String,

        jsonBody: String,

        connectTimeoutMs: Int = 8000,

        readTimeoutMs: Int = 8000,

        parse: (String) -> T

    ): Result<T> {

        return try {

            val connection = openConnection(path, "POST").apply {

                connectTimeout = connectTimeoutMs

                readTimeout = readTimeoutMs

                doOutput = true

                setRequestProperty("Content-Type", "application/json; charset=utf-8")

            }

            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->

                writer.write(jsonBody)

                writer.flush()

            }

            val code = connection.responseCode

            val body = readBody(connection, code in 200..299)

            connection.disconnect()

            if (code in 200..299) {

                Result.success(parse(body))

            } else {

                Result.failure(Exception("HTTP $code: $body"))

            }

        } catch (e: Exception) {

            Result.failure(e)

        }

    }



    private fun readBody(connection: HttpURLConnection, success: Boolean): String {

        val stream = if (success) connection.inputStream else connection.errorStream

        return BufferedReader(InputStreamReader(stream ?: return "", Charsets.UTF_8)).use { reader ->

            reader.readText()

        }

    }

}

