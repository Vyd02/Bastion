package com.vyd.bastion.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CrashScreen(trace: String, onContinue: () -> Unit) {
    val ctx = LocalContext.current
    Column(Modifier.fillMaxSize().background(SheetBg).systemBarsPadding().padding(20.dp)) {
        Text("Bastion hit a crash", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("Tap Copy, then paste it to Claude so it can be fixed.", color = Mint, fontSize = 14.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 14.dp))
        Box(
            Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CardBg)
                .verticalScroll(rememberScrollState()).horizontalScroll(rememberScrollState()).padding(12.dp)
        ) {
            Text(trace, color = Color(0xFFE6E1F5), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
        }
        Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = {
                    ctx.getSystemService(ClipboardManager::class.java)
                        .setPrimaryClip(ClipData.newPlainText("Bastion crash", trace))
                    Toast.makeText(ctx, "Copied", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Violet), modifier = Modifier.weight(1f),
            ) { Text("Copy") }
            Button(
                onClick = onContinue,
                colors = ButtonDefaults.buttonColors(containerColor = CardBg), modifier = Modifier.weight(1f),
            ) { Text("Restart launcher") }
        }
    }
}
