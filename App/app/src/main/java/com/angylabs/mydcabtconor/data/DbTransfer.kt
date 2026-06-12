package com.angylabs.mydcabtconor.data

import android.content.Context
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

const val DB_FILE_NAME = "dca_bitcoin.db"

fun dbFile(context: Context): File = context.getDatabasePath(DB_FILE_NAME)

fun descargarDbDesdeUrl(context: Context, url: String): Result<ImportStats> = runCatching {
    val conn = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 8000
        readTimeout = 15000
        requestMethod = "GET"
    }
    conn.connect()
    if (conn.responseCode !in 200..299) {
        conn.disconnect()
        throw RuntimeException("HTTP ${conn.responseCode}")
    }
    val buffer = ByteArrayOutputStream()
    try {
        conn.inputStream.use { input ->
            val buf = ByteArray(8192)
            while (true) {
                val n = input.read(buf)
                if (n <= 0) break
                buffer.write(buf, 0, n)
            }
        }
    } finally {
        conn.disconnect()
    }
    val bytes = buffer.toByteArray()
    if (bytes.isEmpty()) throw RuntimeException("Archivo vacio")
    Repo.replaceFromBytes(context, bytes).getOrThrow()
}
