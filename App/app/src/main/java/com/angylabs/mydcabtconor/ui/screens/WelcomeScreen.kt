package com.angylabs.mydcabtconor.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.angylabs.mydcabtconor.R
import com.angylabs.mydcabtconor.ui.theme.BlueGlow
import com.angylabs.mydcabtconor.ui.theme.GoldBright
import com.angylabs.mydcabtconor.ui.theme.GoldPrimary
import com.angylabs.mydcabtconor.ui.theme.NavyCard
import com.angylabs.mydcabtconor.ui.theme.NavyDeep
import com.angylabs.mydcabtconor.ui.theme.TextDim
import com.angylabs.mydcabtconor.ui.theme.TextLight

@Composable
fun WelcomeScreen(onStart: () -> Unit) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { onStart() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0A0A08), Color(0xFF14120C), Color(0xFF0A0A08))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_dca),
                contentDescription = "Logo DCA Onor",
                modifier = Modifier.size(190.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Bienvenido a",
                color = TextDim,
                fontSize = 16.sp
            )
            Text(
                text = "DCA ONOR",
                color = GoldPrimary,
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 3.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tu tracker de acumulacion de Bitcoin y Solana.\nOpen source, sin cuentas y tus datos solo tuyos.",
                color = TextDim,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp
            )
            Spacer(modifier = Modifier.height(32.dp))
            FeatureRow(Icons.Filled.Mic, "Registra precios con tu voz")
            Spacer(modifier = Modifier.height(12.dp))
            FeatureRow(Icons.Filled.CloudSync, "Precios en vivo de BTC y SOL")
            Spacer(modifier = Modifier.height(12.dp))
            FeatureRow(Icons.Filled.Security, "Base de datos local y exportable")
            Spacer(modifier = Modifier.height(40.dp))
            Button(
                onClick = {
                    val granted = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                    if (granted) onStart()
                    else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldPrimary,
                    contentColor = NavyDeep
                )
            ) {
                Text(
                    text = "COMENZAR",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Se pedira permiso de microfono para el dictado por voz",
                color = TextDim,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, label: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = NavyCard)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (label.contains("voz")) GoldBright else BlueGlow,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(text = label, color = TextLight, fontSize = 14.sp)
        }
    }
}
