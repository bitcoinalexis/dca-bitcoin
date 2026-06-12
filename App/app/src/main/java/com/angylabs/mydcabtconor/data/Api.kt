package com.angylabs.mydcabtconor.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private suspend fun getJson(url: String): JSONObject? = withContext(Dispatchers.IO) {
    runCatching {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 6000
            readTimeout = 6000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
        }
        conn.connect()
        if (conn.responseCode !in 200..299) {
            conn.disconnect()
            return@runCatching null
        }
        val text = conn.inputStream.bufferedReader().use { it.readText() }
        conn.disconnect()
        JSONObject(text)
    }.getOrNull()
}

suspend fun fetchUsdMxn(): Double? {
    val urls = listOf(
        "https://open.er-api.com/v6/latest/USD",
        "https://api.exchangerate-api.com/v4/latest/USD"
    )
    for (u in urls) {
        val j = getJson(u) ?: continue
        val rates = j.optJSONObject("rates") ?: continue
        if (rates.has("MXN")) return rates.optDouble("MXN").takeIf { !it.isNaN() }
    }
    return null
}

suspend fun fetchPrice(id: String): Double? {
    val j = getJson("https://api.coingecko.com/api/v3/simple/price?ids=$id&vs_currencies=usd") ?: return null
    return j.optJSONObject(id)?.optDouble("usd")?.takeIf { !it.isNaN() }
}

suspend fun fetchBtcUsd(): Double? = fetchPrice("bitcoin")
suspend fun fetchSolUsd(): Double? = fetchPrice("solana")
