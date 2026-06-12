package com.angylabs.mydcabtconor.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.Toll
import androidx.compose.material.icons.filled.Token
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.angylabs.mydcabtconor.ui.theme.BlueElectric
import com.angylabs.mydcabtconor.ui.theme.BlueGlow
import com.angylabs.mydcabtconor.ui.theme.CyanAccent
import com.angylabs.mydcabtconor.ui.theme.GoldBright
import com.angylabs.mydcabtconor.ui.theme.GoldDeep
import com.angylabs.mydcabtconor.ui.theme.GoldPrimary
import com.angylabs.mydcabtconor.ui.theme.NavyCard
import com.angylabs.mydcabtconor.ui.theme.NavyDeep
import com.angylabs.mydcabtconor.ui.theme.TextDim
import com.angylabs.mydcabtconor.ui.theme.TextLight

data class VMenuItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accent: Color
)

private val menuItems = listOf(
    VMenuItem("btc", "Bitcoin", "Registrar compras BTC", Icons.Filled.CurrencyBitcoin, GoldPrimary),
    VMenuItem("sol", "Solana", "Registrar compras SOL", Icons.Filled.Token, BlueGlow),
    VMenuItem("fees", "Fees", "Comisiones de envio", Icons.Filled.Toll, CyanAccent),
    VMenuItem("historial", "Historial", "Compras y movimientos", Icons.Filled.History, GoldBright),
    VMenuItem("resumen", "Resumen", "Metricas y graficas", Icons.Filled.BarChart, BlueElectric),
    VMenuItem("backup", "Backup", "Exportar / importar BD", Icons.Filled.SettingsBackupRestore, TextDim)
)

@Composable
fun VMenuScreen(onNavigate: (String) -> Unit = {}) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var totalBtc by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(0.0) }
    var totalSol by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(0.0) }
    var totalUsd by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(0.0) }
    var valorUsd by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<Double?>(null) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        val btc = com.angylabs.mydcabtconor.data.Repo.listBtc(context)
        val sol = com.angylabs.mydcabtconor.data.Repo.listSol(context)
        totalBtc = btc.sumOf { it.btcAdquirido }
        totalSol = sol.sumOf { it.solAdquirido }
        totalUsd = btc.sumOf { it.usdEquivalente } + sol.sumOf { it.usdEquivalente }
        val pBtc = com.angylabs.mydcabtconor.data.fetchBtcUsd()
        val pSol = com.angylabs.mydcabtconor.data.fetchSolUsd()
        if (pBtc != null || pSol != null) {
            valorUsd = totalBtc * (pBtc ?: 0.0) + totalSol * (pSol ?: 0.0)
        }
    }
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
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "DCA ONOR",
                        color = GoldPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "VMenu principal",
                        color = TextDim,
                        fontSize = 12.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(NavyCard, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Notifications,
                        contentDescription = "Notificaciones",
                        tint = GoldBright,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(GoldDeep, GoldPrimary, GoldBright)
                            )
                        )
                        .padding(20.dp)
                ) {
                    Text(
                        text = if (valorUsd != null) "Portafolio actual" else "Total invertido",
                        color = NavyDeep.copy(alpha = 0.75f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "$ " + String.format("%,.2f", valorUsd ?: totalUsd) + " USD",
                        color = NavyDeep,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "BTC " + String.format("%.8f", totalBtc) + "   |   SOL " + String.format("%.6f", totalSol),
                        color = NavyDeep.copy(alpha = 0.75f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Secciones",
                color = TextLight,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(menuItems) { item ->
                    MenuCard(item) { onNavigate(item.id) }
                }
            }
        }
    }
}

@Composable
private fun MenuCard(item: VMenuItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.15f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = NavyCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(item.accent.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = item.accent,
                    modifier = Modifier.size(26.dp)
                )
            }
            Column {
                Text(
                    text = item.title,
                    color = TextLight,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = item.subtitle,
                    color = TextDim,
                    fontSize = 11.sp
                )
            }
        }
    }
}
