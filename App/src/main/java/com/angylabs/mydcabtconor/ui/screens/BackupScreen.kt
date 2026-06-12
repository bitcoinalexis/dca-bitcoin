package com.angylabs.mydcabtconor.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.angylabs.mydcabtconor.data.Repo
import com.angylabs.mydcabtconor.ui.components.SectionCard
import com.angylabs.mydcabtconor.ui.theme.BlueGlow
import com.angylabs.mydcabtconor.ui.theme.CyanAccent
import com.angylabs.mydcabtconor.ui.theme.DangerRed
import com.angylabs.mydcabtconor.ui.theme.GoldPrimary
import com.angylabs.mydcabtconor.ui.theme.NavyDeep
import com.angylabs.mydcabtconor.ui.theme.TextDim
import com.angylabs.mydcabtconor.ui.theme.TextLight
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupScreen(onBack: () -> Unit, onScanQr: () -> Unit) {
    val context = LocalContext.current
    var msg by remember { mutableStateOf<String?>(null) }
    var msgIsError by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            runCatching {
                val data = Repo.bytes(context)
                context.contentResolver.openOutputStream(uri)?.use { it.write(data) }
            }.fold(
                onSuccess = { msg = "BD exportada correctamente"; msgIsError = false },
                onFailure = { msg = "Error: ${it.message}"; msgIsError = true }
            )
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            runCatching {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw RuntimeException("No se pudo leer")
                Repo.replaceFromBytes(context, bytes).getOrThrow()
            }.fold(
                onSuccess = { s -> msg = "BD importada: ${s.btc} compras BTC, ${s.sol} SOL, ${s.fees} fees"; msgIsError = false },
                onFailure = { msg = "Error: ${it.message}"; msgIsError = true }
            )
        }
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF0A0A08))) {
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null, tint = TextLight) }
                Column {
                    Text("Backup / Restaurar", color = GoldPrimary, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Mismo formato .db que la app PC", color = TextDim, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
            SectionCard {
                Column {
                    Text("Exportar BD a archivo", color = TextLight, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("Genera un .db que puedes abrir en PC o transferir.", color = TextDim, fontSize = 12.sp)
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                            exportLauncher.launch("dca_bitcoin_$stamp.db")
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = NavyDeep)
                    ) {
                        Icon(Icons.Filled.FileDownload, null); Spacer(Modifier.size(8.dp))
                        Text("Exportar .db", fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            SectionCard {
                Column {
                    Text("Importar BD desde archivo", color = TextLight, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("Selecciona un .db (se valida que tenga las tablas de DCA).", color = TextDim, fontSize = 12.sp)
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { importLauncher.launch("*/*") },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BlueGlow, contentColor = NavyDeep)
                    ) {
                        Icon(Icons.Filled.FileUpload, null); Spacer(Modifier.size(8.dp))
                        Text("Importar .db", fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            SectionCard {
                Column {
                    Text("Recibir desde PC por WiFi (QR)", color = TextLight, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("En la app PC genera el QR de transferencia. Aqui escanealo.", color = TextDim, fontSize = 12.sp)
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onScanQr,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = NavyDeep)
                    ) {
                        Icon(Icons.Filled.QrCodeScanner, null); Spacer(Modifier.size(8.dp))
                        Text("Escanear QR", fontWeight = FontWeight.Bold)
                    }
                }
            }
            msg?.let {
                Spacer(Modifier.height(16.dp))
                Text(it, color = if (msgIsError) DangerRed else CyanAccent, fontSize = 13.sp)
            }
        }
    }
}
