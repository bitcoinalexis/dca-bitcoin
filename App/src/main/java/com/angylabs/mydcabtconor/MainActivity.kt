package com.angylabs.mydcabtconor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.angylabs.mydcabtconor.ui.screens.BackupScreen
import com.angylabs.mydcabtconor.ui.screens.CryptoScreen
import com.angylabs.mydcabtconor.ui.screens.FeesScreen
import com.angylabs.mydcabtconor.ui.screens.QrScanScreen
import com.angylabs.mydcabtconor.ui.screens.SplashScreen
import com.angylabs.mydcabtconor.ui.screens.VMenuScreen
import com.angylabs.mydcabtconor.ui.screens.WelcomeScreen
import com.angylabs.mydcabtconor.ui.theme.MyDCABTCOnorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyDCABTCOnorTheme {
                AppRoot()
            }
        }
    }
}

@Composable
fun AppRoot() {
    var screen by remember { mutableStateOf("splash") }
    when (screen) {
        "splash" -> SplashScreen(onFinished = { screen = "welcome" })
        "welcome" -> WelcomeScreen(onStart = { screen = "vmenu" })
        "btc" -> CryptoScreen(asset = "btc", onBack = { screen = "vmenu" })
        "sol" -> CryptoScreen(asset = "sol", onBack = { screen = "vmenu" })
        "fees" -> FeesScreen(onBack = { screen = "vmenu" })
        "backup" -> BackupScreen(onBack = { screen = "vmenu" }, onScanQr = { screen = "qr" })
        "qr" -> QrScanScreen(onBack = { screen = "backup" }, onImported = { screen = "vmenu" })
        else -> VMenuScreen(onNavigate = { dest ->
            screen = when (dest) {
                "btc" -> "btc"
                "sol" -> "sol"
                "fees" -> "fees"
                "historial" -> "btc"
                "resumen" -> "btc"
                "backup" -> "backup"
                else -> "vmenu"
            }
        })
    }
}
//  /model claude-fable-5 /model claude-opus-4-7