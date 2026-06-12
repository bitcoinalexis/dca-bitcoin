package com.angylabs.mydcabtconor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.angylabs.mydcabtconor.ui.theme.GoldPrimary
import com.angylabs.mydcabtconor.ui.theme.NavyBorder
import com.angylabs.mydcabtconor.ui.theme.NavyCard
import com.angylabs.mydcabtconor.ui.theme.NavySurface
import com.angylabs.mydcabtconor.ui.theme.TextDim
import com.angylabs.mydcabtconor.ui.theme.TextLight
import com.angylabs.mydcabtconor.ui.voice.MicButton

@Composable
fun SectionCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NavyCard)
    ) { Box(Modifier.padding(16.dp)) { content() } }
}

@Composable
fun StatCard(label: String, value: String, accent: androidx.compose.ui.graphics.Color = GoldPrimary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = NavySurface)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(label, color = TextDim, fontSize = 11.sp)
            Spacer(Modifier.height(4.dp))
            Text(value, color = accent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun NumberFieldWithMic(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    voice: Boolean = true,
    decimal: Boolean = true
) {
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        OutlinedTextField(
            value = value,
            onValueChange = { v -> if (v.matches(Regex("""-?\d*[.,]?\d*"""))) onValueChange(v.replace(',', '.')) },
            label = { Text(label) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number
            ),
            modifier = Modifier.weight(1f),
            textStyle = TextStyle(color = TextLight, fontSize = 14.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextLight,
                unfocusedTextColor = TextLight,
                focusedBorderColor = GoldPrimary,
                unfocusedBorderColor = NavyBorder,
                focusedLabelColor = GoldPrimary,
                unfocusedLabelColor = TextDim,
                cursorColor = GoldPrimary
            )
        )
        if (voice) {
            Spacer(Modifier.width(8.dp))
            MicButton(onResult = onValueChange)
        }
    }
}

@Composable
fun TextFieldStyled(value: String, onValueChange: (String) -> Unit, label: String, lines: Int = 1) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        minLines = lines,
        maxLines = lines.coerceAtLeast(1) + 2,
        modifier = Modifier.fillMaxWidth(),
        textStyle = TextStyle(color = TextLight, fontSize = 14.sp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextLight,
            unfocusedTextColor = TextLight,
            focusedBorderColor = GoldPrimary,
            unfocusedBorderColor = NavyBorder,
            focusedLabelColor = GoldPrimary,
            unfocusedLabelColor = TextDim,
            cursorColor = GoldPrimary
        )
    )
}

fun fmtMoney(v: Double, dec: Int = 2): String =
    "$" + String.format("%,.${dec}f", v)

fun fmtCoin(v: Double, dec: Int = 8): String = String.format("%.${dec}f", v)
