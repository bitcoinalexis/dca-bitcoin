package com.angylabs.mydcabtconor.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.angylabs.mydcabtconor.data.descargarDbDesdeUrl
import com.angylabs.mydcabtconor.ui.theme.CyanAccent
import com.angylabs.mydcabtconor.ui.theme.DangerRed
import com.angylabs.mydcabtconor.ui.theme.GoldBright
import com.angylabs.mydcabtconor.ui.theme.GoldPrimary
import com.angylabs.mydcabtconor.ui.theme.NavyCard
import com.angylabs.mydcabtconor.ui.theme.NavyDeep
import com.angylabs.mydcabtconor.ui.theme.TextDim
import com.angylabs.mydcabtconor.ui.theme.TextLight
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

@OptIn(ExperimentalGetImage::class)
@Composable
fun QrScanScreen(onBack: () -> Unit, onImported: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasCameraPermission = it }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permLauncher.launch(Manifest.permission.CAMERA)
    }

    var estado by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var procesando by remember { mutableStateOf(false) }
    var detectado by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyDeep)
    ) {
        if (hasCameraPermission && detectado == null) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val analysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                        val executor = Executors.newSingleThreadExecutor()
                        val scanner = BarcodeScanning.getClient()
                        analysis.setAnalyzer(executor) { imageProxy ->
                            val media = imageProxy.image
                            if (media != null) {
                                val img = InputImage.fromMediaImage(
                                    media, imageProxy.imageInfo.rotationDegrees
                                )
                                scanner.process(img)
                                    .addOnSuccessListener { codes ->
                                        codes.firstOrNull { it.valueType == Barcode.TYPE_URL || it.rawValue != null }
                                            ?.rawValue?.let { v ->
                                                if (detectado == null) detectado = v
                                            }
                                    }
                                    .addOnCompleteListener { imageProxy.close() }
                            } else {
                                imageProxy.close()
                            }
                        }
                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview, analysis
                            )
                        } catch (_: Exception) {
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                }
            )
            ScanOverlay()
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = TextLight)
                }
                Spacer(Modifier.width(8.dp))
                Text("Escanear QR de PC", color = TextLight, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Genera el QR en la app de PC y apuntalo con la camara. Ambos en la misma WiFi.",
                color = TextDim,
                fontSize = 12.sp
            )
        }

        if (!hasCameraPermission) {
            Box(Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.QrCodeScanner, null, tint = GoldBright, modifier = Modifier.size(72.dp))
                    Spacer(Modifier.height(14.dp))
                    Text("Permiso de camara requerido", color = TextLight, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("Activa la camara para escanear el QR.", color = TextDim, fontSize = 12.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = { permLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = NavyDeep),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Conceder permiso", fontWeight = FontWeight.Bold) }
                }
            }
        }

        detectado?.let { url ->
            Box(
                Modifier.fillMaxSize().background(NavyDeep.copy(alpha = 0.92f)).padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = NavyCard)
                ) {
                    Column(
                        Modifier.padding(20.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.CheckCircle, null, tint = CyanAccent, modifier = Modifier.size(54.dp))
                        Spacer(Modifier.height(10.dp))
                        Text("QR detectado", color = TextLight, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(url, color = TextDim, fontSize = 11.sp, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        if (procesando) {
                            CircularProgressIndicator(color = GoldPrimary)
                            Spacer(Modifier.height(10.dp))
                            Text(estado ?: "Descargando...", color = TextDim, fontSize = 12.sp)
                        } else if (estado != null) {
                            Text(estado!!, color = CyanAccent, fontSize = 13.sp, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(14.dp))
                            Button(
                                onClick = onImported,
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = NavyDeep),
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text("Continuar", fontWeight = FontWeight.Bold) }
                        } else if (error != null) {
                            Text(error!!, color = DangerRed, fontSize = 13.sp, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(14.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = { detectado = null; error = null },
                                    colors = ButtonDefaults.buttonColors(containerColor = NavyDeep, contentColor = TextLight),
                                    shape = RoundedCornerShape(12.dp)
                                ) { Text("Reintentar") }
                                Button(
                                    onClick = onBack,
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = NavyDeep),
                                    shape = RoundedCornerShape(12.dp)
                                ) { Text("Cancelar") }
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = { detectado = null },
                                    colors = ButtonDefaults.buttonColors(containerColor = NavyDeep, contentColor = TextLight),
                                    shape = RoundedCornerShape(12.dp)
                                ) { Text("Cancelar") }
                                Button(
                                    onClick = {
                                        procesando = true
                                        scope.launch {
                                            val r = withContext(Dispatchers.IO) {
                                                descargarDbDesdeUrl(context, url)
                                            }
                                            procesando = false
                                            if (r.isSuccess) {
                                                val s = r.getOrNull()
                                                estado = "BD importada: ${s?.btc ?: 0} compras BTC, ${s?.sol ?: 0} SOL, ${s?.fees ?: 0} fees"
                                            } else {
                                                error = "Error: ${r.exceptionOrNull()?.message}"
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = NavyDeep),
                                    shape = RoundedCornerShape(12.dp)
                                ) { Text("Descargar BD", fontWeight = FontWeight.Bold) }
                            }
                        }
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try { ProcessCameraProvider.getInstance(context).get().unbindAll() } catch (_: Exception) {}
        }
    }
}

@Composable
private fun ScanOverlay() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(260.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Transparent)
        )
    }
}
