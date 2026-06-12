package com.angylabs.mydcabtconor.ui.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.angylabs.mydcabtconor.ui.theme.DangerRed
import com.angylabs.mydcabtconor.ui.theme.GoldBright
import com.angylabs.mydcabtconor.ui.theme.NavyDeep

@Composable
fun MicButton(onResult: (String) -> Unit) {
    val context = LocalContext.current
    var listening by remember { mutableStateOf(false) }
    val recognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context))
            SpeechRecognizer.createSpeechRecognizer(context) else null
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startListening(context, recognizer, onResult) { listening = it }
    }

    DisposableEffect(Unit) { onDispose { recognizer?.destroy() } }

    Icon(
        imageVector = if (listening) Icons.Filled.Mic else Icons.Filled.MicNone,
        contentDescription = "Dictar por voz",
        tint = if (listening) DangerRed else NavyDeep,
        modifier = Modifier
            .size(36.dp)
            .background(GoldBright, CircleShape)
            .clickable {
                val granted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
                if (!granted) permLauncher.launch(Manifest.permission.RECORD_AUDIO)
                else if (!listening) startListening(context, recognizer, onResult) { listening = it }
                else { recognizer?.stopListening(); listening = false }
            }
            .padding(7.dp)
    )
}

private fun startListening(
    ctx: Context,
    recognizer: SpeechRecognizer?,
    onResult: (String) -> Unit,
    setListening: (Boolean) -> Unit
) {
    if (recognizer == null) return
    setListening(true)
    recognizer.setRecognitionListener(object : RecognitionListener {
        override fun onReadyForSpeech(p0: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(p0: Float) {}
        override fun onBufferReceived(p0: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onError(p0: Int) { setListening(false) }
        override fun onPartialResults(p0: Bundle?) {}
        override fun onEvent(p0: Int, p1: Bundle?) {}
        override fun onResults(results: Bundle?) {
            setListening(false)
            val list = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = list?.firstOrNull() ?: return
            val parsed = parseNumber(text) ?: return
            onResult(parsed)
        }
    })
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-MX")
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "es-MX")
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, ctx.packageName)
    }
    recognizer.startListening(intent)
}

private fun parseNumber(text: String): String? {
    val cleaned = text.replace(",", ".").replace(" ", "")
    val direct = Regex("""-?\d+(?:\.\d+)?""").find(cleaned)?.value
    if (direct != null) return direct
    val words = text.lowercase().split(" ", " ", "-")
    val total = wordsToNumber(words)
    return total?.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() }
}

private val unidades = mapOf(
    "cero" to 0, "uno" to 1, "una" to 1, "un" to 1, "dos" to 2, "tres" to 3,
    "cuatro" to 4, "cinco" to 5, "seis" to 6, "siete" to 7, "ocho" to 8, "nueve" to 9,
    "diez" to 10, "once" to 11, "doce" to 12, "trece" to 13, "catorce" to 14,
    "quince" to 15, "dieciseis" to 16, "diecisiete" to 17, "dieciocho" to 18,
    "diecinueve" to 19, "veinte" to 20, "veintiuno" to 21, "veintidos" to 22,
    "veintitres" to 23, "veinticuatro" to 24, "veinticinco" to 25, "veintiseis" to 26,
    "veintisiete" to 27, "veintiocho" to 28, "veintinueve" to 29,
    "treinta" to 30, "cuarenta" to 40, "cincuenta" to 50, "sesenta" to 60,
    "setenta" to 70, "ochenta" to 80, "noventa" to 90,
    "cien" to 100, "ciento" to 100, "doscientos" to 200, "trescientos" to 300,
    "cuatrocientos" to 400, "quinientos" to 500, "seiscientos" to 600,
    "setecientos" to 700, "ochocientos" to 800, "novecientos" to 900
)

private fun wordsToNumber(words: List<String>): Double? {
    var total = 0L
    var current = 0L
    var any = false
    for (w in words) {
        val k = w.replace("á", "a").replace("é", "e").replace("í", "i")
            .replace("ó", "o").replace("ú", "u")
        when {
            k == "y" || k.isBlank() -> {}
            k == "mil" -> { current = if (current == 0L) 1000L else current * 1000L; any = true }
            k == "millon" || k == "millones" -> {
                current = if (current == 0L) 1_000_000L else current * 1_000_000L
                total += current; current = 0; any = true
            }
            unidades.containsKey(k) -> { current += unidades.getValue(k); any = true }
            else -> return null
        }
    }
    if (!any) return null
    return (total + current).toDouble()
}
