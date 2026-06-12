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
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.angylabs.mydcabtconor.data.BtcRecord
import com.angylabs.mydcabtconor.data.Repo
import com.angylabs.mydcabtconor.data.SolRecord
import com.angylabs.mydcabtconor.data.fetchBtcUsd
import com.angylabs.mydcabtconor.data.fetchSolUsd
import com.angylabs.mydcabtconor.data.fetchUsdMxn
import com.angylabs.mydcabtconor.ui.components.BarChart
import com.angylabs.mydcabtconor.ui.components.LineAreaChart
import com.angylabs.mydcabtconor.ui.components.NumberFieldWithMic
import com.angylabs.mydcabtconor.ui.components.SectionCard
import com.angylabs.mydcabtconor.ui.components.StatCard
import com.angylabs.mydcabtconor.ui.components.TextFieldStyled
import com.angylabs.mydcabtconor.ui.components.fmtCoin
import com.angylabs.mydcabtconor.ui.components.fmtMoney
import com.angylabs.mydcabtconor.ui.theme.BlueGlow
import com.angylabs.mydcabtconor.ui.theme.CyanAccent
import com.angylabs.mydcabtconor.ui.theme.DangerRed
import com.angylabs.mydcabtconor.ui.theme.GoldBright
import com.angylabs.mydcabtconor.ui.theme.GoldPrimary
import com.angylabs.mydcabtconor.ui.theme.NavyCard
import com.angylabs.mydcabtconor.ui.theme.NavyDeep
import com.angylabs.mydcabtconor.ui.theme.TextDim
import com.angylabs.mydcabtconor.ui.theme.TextLight
import kotlinx.coroutines.launch

@Composable
fun CryptoScreen(asset: String, onBack: () -> Unit) {
    val isBtc = asset == "btc"
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf(0) }

    var fx by remember { mutableStateOf<Double?>(null) }
    var priceLive by remember { mutableStateOf<Double?>(null) }

    LaunchedEffect(asset) {
        fx = fetchUsdMxn()
        priceLive = if (isBtc) fetchBtcUsd() else fetchSolUsd()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A08))
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, null, tint = TextLight)
                }
                Column {
                    Text(
                        if (isBtc) "Bitcoin (BTC)" else "Solana (SOL)",
                        color = if (isBtc) GoldPrimary else BlueGlow,
                        fontSize = 20.sp, fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        "Tracker DCA",
                        color = TextDim, fontSize = 12.sp
                    )
                }
            }
            TabRow(
                selectedTabIndex = tab,
                containerColor = NavyCard,
                contentColor = GoldPrimary
            ) {
                listOf("Registrar", "Historial", "Resumen").forEachIndexed { i, t ->
                    Tab(selected = tab == i, onClick = { tab = i }) {
                        Text(t, modifier = Modifier.padding(12.dp), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            when (tab) {
                0 -> RegistrarTab(isBtc, fx, priceLive)
                1 -> HistorialTab(isBtc)
                else -> ResumenTab(isBtc, fx, priceLive)
            }
        }
    }
}

@Composable
private fun RegistrarTab(isBtc: Boolean, fxApi: Double?, precioLive: Double?) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var tcStr by remember { mutableStateOf((fxApi ?: 17.5).toString()) }
    var monedaGastoMxn by remember { mutableStateOf(true) }
    var mxnStr by remember { mutableStateOf("") }
    var usdStr by remember { mutableStateOf("") }
    var cantidadStr by remember { mutableStateOf("") }
    var precioMxnMode by remember { mutableStateOf(false) }
    var precioMxn by remember { mutableStateOf("") }
    var precioUsd by remember { mutableStateOf(precioLive?.toString() ?: "") }
    var notas by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(fxApi) { fxApi?.let { tcStr = it.toString() } }
    LaunchedEffect(precioLive) { precioLive?.let { if (precioUsd.isBlank()) precioUsd = it.toString() } }

    val tc = tcStr.toDoubleOrNull() ?: 0.0
    val mxn = mxnStr.toDoubleOrNull() ?: 0.0
    val usdInp = usdStr.toDoubleOrNull() ?: 0.0
    val cantidad = cantidadStr.toDoubleOrNull() ?: 0.0
    val priceUsd = if (precioMxnMode) {
        val v = precioMxn.toDoubleOrNull() ?: 0.0
        if (tc > 0) v / tc else 0.0
    } else precioUsd.toDoubleOrNull() ?: 0.0

    val usdEquiv = if (monedaGastoMxn) (if (tc > 0) mxn / tc else 0.0) else usdInp
    val mxnEquiv = if (monedaGastoMxn) mxn else usdInp * tc

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        if (fxApi != null) {
            Text("1 USD = ${"%.4f".format(fxApi)} MXN", color = CyanAccent, fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))
        }
        if (precioLive != null) {
            Text(
                "Precio ${if (isBtc) "BTC" else "SOL"} live: $${"%.2f".format(precioLive)} USD",
                color = if (isBtc) GoldBright else BlueGlow, fontSize = 12.sp
            )
            Spacer(Modifier.height(10.dp))
        }
        SectionCard {
            Column {
                Text("Datos del gasto", color = TextLight, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                NumberFieldWithMic(tcStr, { tcStr = it }, "Tipo de cambio (MXN por USD)")
                Spacer(Modifier.height(10.dp))
                Row {
                    PillToggle("MXN", monedaGastoMxn) { monedaGastoMxn = true }
                    Spacer(Modifier.size(8.dp))
                    PillToggle("USD (USDC/USDT)", !monedaGastoMxn) { monedaGastoMxn = false }
                }
                Spacer(Modifier.height(10.dp))
                if (monedaGastoMxn) {
                    NumberFieldWithMic(mxnStr, { mxnStr = it }, "MXN gastados")
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "USD equivalente: $${"%.4f".format(usdEquiv)}",
                        color = TextDim, fontSize = 11.sp
                    )
                } else {
                    NumberFieldWithMic(usdStr, { usdStr = it }, "USD gastados")
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "MXN equivalente: $${"%.2f".format(mxnEquiv)}",
                        color = TextDim, fontSize = 11.sp
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        SectionCard {
            Column {
                Text(if (isBtc) "Datos Bitcoin" else "Datos Solana", color = TextLight, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                NumberFieldWithMic(cantidadStr, { cantidadStr = it }, if (isBtc) "BTC adquiridos" else "SOL adquiridos")
                Spacer(Modifier.height(10.dp))
                Row {
                    PillToggle("Precio USD", !precioMxnMode) { precioMxnMode = false }
                    Spacer(Modifier.size(8.dp))
                    PillToggle("Precio MXN", precioMxnMode) { precioMxnMode = true }
                }
                Spacer(Modifier.height(10.dp))
                if (precioMxnMode) {
                    NumberFieldWithMic(precioMxn, { precioMxn = it }, "Precio compra (MXN)")
                    Spacer(Modifier.height(6.dp))
                    Text("Equivalente USD: $${"%.2f".format(priceUsd)}", color = TextDim, fontSize = 11.sp)
                } else {
                    NumberFieldWithMic(precioUsd, { precioUsd = it }, "Precio compra (USD)")
                    Spacer(Modifier.height(6.dp))
                    Text("Equivalente MXN: $${"%.2f".format(priceUsd * tc)}", color = TextDim, fontSize = 11.sp)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        TextFieldStyled(notas, { notas = it }, "Notas (opcional)", lines = 2)
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                val errs = mutableListOf<String>()
                if (usdEquiv <= 0) errs += "Monto invalido"
                if (cantidad <= 0) errs += "Cantidad invalida"
                if (priceUsd <= 0) errs += "Precio invalido"
                if (errs.isNotEmpty()) { msg = errs.joinToString(" - "); return@Button }
                val fecha = Repo.fechaAhora()
                if (isBtc) {
                    Repo.insertBtc(context, BtcRecord(0, fecha, mxnEquiv, tc, usdEquiv, cantidad, priceUsd,
                        if (monedaGastoMxn) "MXN" else "USD", notas.ifBlank { null }))
                } else {
                    Repo.insertSol(context, SolRecord(0, fecha, mxnEquiv, tc, usdEquiv, cantidad, priceUsd,
                        if (monedaGastoMxn) "MXN" else "USD", notas.ifBlank { null }))
                }
                mxnStr = ""; usdStr = ""; cantidadStr = ""; precioMxn = ""; notas = ""
                msg = "Registro guardado"
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = NavyDeep)
        ) {
            Text("Guardar registro", fontWeight = FontWeight.Bold)
        }
        msg?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = if (it.contains("guardado")) CyanAccent else DangerRed, fontSize = 13.sp)
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun PillToggle(label: String, selected: Boolean, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) GoldPrimary else NavyCard
        ),
        modifier = Modifier.clickableWithoutRipple(onClick)
    ) {
        Text(
            label,
            color = if (selected) NavyDeep else TextLight,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun Modifier.clickableWithoutRipple(onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return this.clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick
    )
}

@Composable
private fun HistorialTab(isBtc: Boolean) {
    val context = LocalContext.current
    val btcList = remember { mutableStateListOf<BtcRecord>() }
    val solList = remember { mutableStateListOf<SolRecord>() }

    LaunchedEffect(isBtc) {
        if (isBtc) { btcList.clear(); btcList.addAll(Repo.listBtc(context)) }
        else { solList.clear(); solList.addAll(Repo.listSol(context)) }
    }

    if (isBtc) {
        if (btcList.isEmpty()) EmptyState("Sin registros BTC aun")
        else LazyColumn(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(btcList, key = { it.id }) { r ->
                HistorialItem(
                    fecha = r.fecha,
                    line1 = "${fmtMoney(r.mxnGastado)} MXN  |  ${fmtMoney(r.usdEquivalente, 4)} USD",
                    line2 = "${fmtCoin(r.btcAdquirido)} BTC @ ${fmtMoney(r.precioBtcUsd)}",
                    notas = r.notas,
                    onDelete = { Repo.deleteBtc(context, r.id); btcList.remove(r) }
                )
            }
            item { Spacer(Modifier.height(40.dp)) }
        }
    } else {
        if (solList.isEmpty()) EmptyState("Sin registros SOL aun")
        else LazyColumn(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(solList, key = { it.id }) { r ->
                HistorialItem(
                    fecha = r.fecha,
                    line1 = "${fmtMoney(r.mxnGastado)} MXN  |  ${fmtMoney(r.usdEquivalente, 4)} USD",
                    line2 = "${fmtCoin(r.solAdquirido, 6)} SOL @ ${fmtMoney(r.precioSolUsd)}",
                    notas = r.notas,
                    onDelete = { Repo.deleteSol(context, r.id); solList.remove(r) }
                )
            }
            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

@Composable
private fun HistorialItem(fecha: String, line1: String, line2: String, notas: String?, onDelete: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = NavyCard),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(fecha, color = TextDim, fontSize = 11.sp)
                Spacer(Modifier.height(2.dp))
                Text(line1, color = TextLight, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(line2, color = GoldBright, fontSize = 12.sp)
                if (!notas.isNullOrBlank()) {
                    Text(notas, color = TextDim, fontSize = 11.sp)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, "Eliminar", tint = DangerRed)
            }
        }
    }
}

@Composable
private fun EmptyState(msg: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(msg, color = TextDim)
    }
}

@Composable
private fun ResumenTab(isBtc: Boolean, fxApi: Double?, precioLive: Double?) {
    val context = LocalContext.current
    val btcList = remember { mutableStateListOf<BtcRecord>() }
    val solList = remember { mutableStateListOf<SolRecord>() }
    LaunchedEffect(isBtc) {
        if (isBtc) { btcList.clear(); btcList.addAll(Repo.listBtc(context).reversed()) }
        else { solList.clear(); solList.addAll(Repo.listSol(context).reversed()) }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        if (isBtc) {
            if (btcList.isEmpty()) { Text("Sin datos aun", color = TextDim); return@Column }
            val totalMxn = btcList.sumOf { it.mxnGastado }
            val totalUsd = btcList.sumOf { it.usdEquivalente }
            val totalCoin = btcList.sumOf { it.btcAdquirido }
            val precioProm = if (totalCoin > 0) btcList.sumOf { it.precioBtcUsd * it.btcAdquirido } / totalCoin else 0.0
            ResumenMetricas(totalMxn, totalUsd, totalCoin, precioProm, isBtc = true, precioLive, fxApi, btcList.size)
            Spacer(Modifier.height(14.dp))
            LineAreaChart("Cantidad BTC acumulada", btcList.runningFold(0.0) { acc, r -> acc + r.btcAdquirido }.drop(1))
            Spacer(Modifier.height(10.dp))
            BarChart("Inversion por compra (MXN)", btcList.map { it.mxnGastado })
            Spacer(Modifier.height(10.dp))
            LineAreaChart("Precio BTC en cada compra (USD)", btcList.map { it.precioBtcUsd }, GoldBright)
        } else {
            if (solList.isEmpty()) { Text("Sin datos aun", color = TextDim); return@Column }
            val totalMxn = solList.sumOf { it.mxnGastado }
            val totalUsd = solList.sumOf { it.usdEquivalente }
            val totalCoin = solList.sumOf { it.solAdquirido }
            val precioProm = if (totalCoin > 0) solList.sumOf { it.precioSolUsd * it.solAdquirido } / totalCoin else 0.0
            ResumenMetricas(totalMxn, totalUsd, totalCoin, precioProm, isBtc = false, precioLive, fxApi, solList.size)
            Spacer(Modifier.height(14.dp))
            LineAreaChart("Cantidad SOL acumulada", solList.runningFold(0.0) { acc, r -> acc + r.solAdquirido }.drop(1), BlueGlow)
            Spacer(Modifier.height(10.dp))
            BarChart("Inversion por compra (MXN)", solList.map { it.mxnGastado }, BlueGlow)
            Spacer(Modifier.height(10.dp))
            LineAreaChart("Precio SOL en cada compra (USD)", solList.map { it.precioSolUsd }, BlueGlow)
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun ResumenMetricas(
    totalMxn: Double, totalUsd: Double, totalCoin: Double, precioProm: Double,
    isBtc: Boolean, precioLive: Double?, fxApi: Double?, n: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) { StatCard("Total MXN", fmtMoney(totalMxn)) }
            Box(Modifier.weight(1f)) { StatCard("Total USD", fmtMoney(totalUsd)) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) {
                StatCard(if (isBtc) "BTC acumulados" else "SOL acumulados",
                    fmtCoin(totalCoin, if (isBtc) 8 else 6))
            }
            Box(Modifier.weight(1f)) { StatCard("Precio prom (USD)", fmtMoney(precioProm)) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) { StatCard("Compras", n.toString()) }
            if (precioLive != null) {
                val valor = totalCoin * precioLive
                val ganancia = valor - totalUsd
                val pct = if (totalUsd > 0) ganancia / totalUsd * 100 else 0.0
                val color = if (ganancia >= 0) CyanAccent else DangerRed
                Box(Modifier.weight(1f)) { StatCard("Rendimiento", "%+.2f%%".format(pct), color) }
            }
        }
        if (precioLive != null) {
            val valor = totalCoin * precioLive
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) { StatCard("Valor actual (USD)", fmtMoney(valor)) }
                if (fxApi != null) {
                    Box(Modifier.weight(1f)) { StatCard("Valor actual (MXN)", fmtMoney(valor * fxApi)) }
                }
            }
        }
    }
}
