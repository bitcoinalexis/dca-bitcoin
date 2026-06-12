package com.angylabs.mydcabtconor.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.angylabs.mydcabtconor.data.FeeRecord
import com.angylabs.mydcabtconor.data.Repo
import com.angylabs.mydcabtconor.ui.components.NumberFieldWithMic
import com.angylabs.mydcabtconor.ui.components.SectionCard
import com.angylabs.mydcabtconor.ui.components.StatCard
import com.angylabs.mydcabtconor.ui.components.TextFieldStyled
import com.angylabs.mydcabtconor.ui.components.fmtCoin
import com.angylabs.mydcabtconor.ui.components.fmtMoney
import com.angylabs.mydcabtconor.ui.theme.CyanAccent
import com.angylabs.mydcabtconor.ui.theme.DangerRed
import com.angylabs.mydcabtconor.ui.theme.GoldPrimary
import com.angylabs.mydcabtconor.ui.theme.NavyCard
import com.angylabs.mydcabtconor.ui.theme.NavyDeep
import com.angylabs.mydcabtconor.ui.theme.TextDim
import com.angylabs.mydcabtconor.ui.theme.TextLight

private val tiposFee = listOf(
    "Envio a wallet fria", "Retiro de exchange", "Consolidacion de UTXOs",
    "Envio entre wallets propias", "Pago a tercero", "Otro"
)

@Composable
fun FeesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val list = remember { mutableStateListOf<FeeRecord>() }
    LaunchedEffect(Unit) { list.clear(); list.addAll(Repo.listFees(context)) }

    var btcFee by remember { mutableStateOf("") }
    var precio by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf(tiposFee.first()) }
    var notas by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf<String?>(null) }
    var menuOpen by remember { mutableStateOf(false) }

    val btcF = btcFee.toDoubleOrNull() ?: 0.0
    val pF = precio.toDoubleOrNull() ?: 0.0
    val usdFee = btcF * pF

    Box(Modifier.fillMaxSize().background(Color(0xFF0A0A08))) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null, tint = TextLight) }
                Column {
                    Text("Fees de envio", color = CyanAccent, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Comisiones en BTC", color = TextDim, fontSize = 12.sp)
                }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                SectionCard {
                    Column {
                        Text("Nuevo fee", color = TextLight, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(10.dp))
                        NumberFieldWithMic(btcFee, { btcFee = it }, "BTC pagados como fee")
                        Spacer(Modifier.height(10.dp))
                        NumberFieldWithMic(precio, { precio = it }, "Precio BTC al envio (USD)")
                        Spacer(Modifier.height(10.dp))
                        Box {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = NavyCard),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "Tipo: $tipo",
                                    color = TextLight,
                                    fontSize = 13.sp,
                                    modifier = Modifier
                                        .padding(14.dp)
                                )
                            }
                            Box(
                                Modifier
                                    .matchParentSize()
                                    .tapNoRipple { menuOpen = true }
                            )
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                tiposFee.forEach { t ->
                                    DropdownMenuItem(
                                        text = { Text(t) },
                                        onClick = { tipo = t; menuOpen = false }
                                    )
                                }
                            }
                        }
                        if (usdFee > 0) {
                            Spacer(Modifier.height(8.dp))
                            Text("Fee equivalente: ${fmtMoney(usdFee, 6)} USD", color = TextDim, fontSize = 11.sp)
                        }
                        Spacer(Modifier.height(10.dp))
                        TextFieldStyled(notas, { notas = it }, "Notas (opcional)", lines = 2)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = {
                        if (btcF <= 0 || pF <= 0) { msg = "Valores invalidos"; return@Button }
                        Repo.insertFee(context, FeeRecord(0, Repo.fechaAhora(), btcF, pF, usdFee, tipo, notas.ifBlank { null }))
                        list.clear(); list.addAll(Repo.listFees(context))
                        btcFee = ""; precio = ""; notas = ""; msg = "Fee guardado"
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = NavyDeep)
                ) { Text("Guardar fee", fontWeight = FontWeight.Bold) }
                msg?.let { Spacer(Modifier.height(8.dp)); Text(it, color = if (it.contains("guardado")) CyanAccent else DangerRed, fontSize = 13.sp) }
                Spacer(Modifier.height(20.dp))
                if (list.isNotEmpty()) {
                    Text("Totales", color = TextLight, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) { StatCard("Operaciones", list.size.toString()) }
                        Box(Modifier.weight(1f)) { StatCard("BTC total", fmtCoin(list.sumOf { it.btcFee })) }
                        Box(Modifier.weight(1f)) { StatCard("USD total", fmtMoney(list.sumOf { it.usdFee }, 4)) }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Historial", color = TextLight, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    list.forEach { r ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = NavyCard),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(r.fecha, color = TextDim, fontSize = 11.sp)
                                    Text(r.tipoMovimiento, color = TextLight, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text("${fmtCoin(r.btcFee)} BTC  |  ${fmtMoney(r.usdFee, 6)} USD",
                                        color = CyanAccent, fontSize = 12.sp)
                                    if (!r.notas.isNullOrBlank()) Text(r.notas, color = TextDim, fontSize = 11.sp)
                                }
                                IconButton(onClick = {
                                    Repo.deleteFee(context, r.id)
                                    list.clear(); list.addAll(Repo.listFees(context))
                                }) { Icon(Icons.Filled.Delete, null, tint = DangerRed) }
                            }
                        }
                    }
                    Spacer(Modifier.height(40.dp))
                }
            }
        }
    }
}

@Composable
private fun Modifier.tapNoRipple(onClick: () -> Unit): Modifier {
    val src = remember { MutableInteractionSource() }
    return this.clickable(
        interactionSource = src,
        indication = null,
        onClick = onClick
    )
}
