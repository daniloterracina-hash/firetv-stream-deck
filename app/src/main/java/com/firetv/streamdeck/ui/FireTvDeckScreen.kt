package com.firetv.streamdeck.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.firetv.streamdeck.data.FireTvDeckRepository
import com.firetv.streamdeck.data.FireTvKeycodes
import com.firetv.streamdeck.data.VideoPreset4K
import kotlinx.coroutines.launch

val DarkBackground = Color(0xFF070B14)
val DarkSurface = Color(0xFF0F172A)
val AmberAccent = Color(0xFFF59E0B)
val SkyAccent = Color(0xFF38BDF8)
val EmeraldAccent = Color(0xFF10B981)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FireTvDeckScreen() {
    val repository = remember { FireTvDeckRepository() }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    var host by remember { mutableStateOf("192.168.1.100") }
    var port by remember { mutableStateOf("5555") }
    var isConnected by remember { mutableStateOf(false) }
    var latencyMs by remember { mutableStateOf<Long?>(null) }
    var isExecutingMacro by remember { mutableStateOf(false) }
    var macroRemainingMs by remember { mutableStateOf<Long?>(null) }
    var activePreset by remember { mutableStateOf(VideoPreset4K.UHD_60HZ) }
    var lastLog by remember { mutableStateOf("In attesa...") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fire TV Stream Deck", fontWeight = FontWeight.Bold, color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = DarkSurface), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Configurazione di Rete (ADB TCP/IP)", fontWeight = FontWeight.Bold, color = Color.White)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = host,
                            onValueChange = { host = it },
                            label = { Text("IP Fire TV") },
                            modifier = Modifier.weight(2f),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                        OutlinedTextField(
                            value = port,
                            onValueChange = { port = it },
                            label = { Text("Porta") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isConnected) "Connesso (ms)" else "Non connesso",
                            color = if (isConnected) EmeraldAccent else Color.Gray,
                            fontSize = 12.sp
                        )
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                scope.launch {
                                    repository.pingSocket(host, port.toIntOrNull() ?: 5555)
                                        .onSuccess { isConnected = true; latencyMs = it }
                                        .onFailure { isConnected = false }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AmberAccent)
                        ) {
                            Text("Salva & Connetti", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = DarkSurface), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Sezione 1: Macro Diagnostica Stream", fontWeight = FontWeight.Bold, color = Color.White)
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            isExecutingMacro = true
                            scope.launch {
                                repository.executeStreamInfoMacro(host, port.toIntOrNull() ?: 5555) { remaining ->
                                    macroRemainingMs = remaining
                                }.onSuccess { lastLog = "Macro completata!" }
                                isExecutingMacro = false
                                macroRemainingMs = null
                            }
                        },
                        enabled = !isExecutingMacro,
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SkyAccent)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (isExecutingMacro) "Esecuzione Macro..." else "Info Stream (Debug Overlay)", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 16.sp)
                            Text(if (isExecutingMacro) "Pausa: ms" else "DPAD 23+20 -> 1.2s -> MENU 82", fontSize = 10.sp, color = Color.DarkGray)
                        }
                    }
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = DarkSurface), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Sezione 2: Preset Risoluzione & Frame Rate (4K)", fontWeight = FontWeight.Bold, color = Color.White)
                    VideoPreset4K.entries.forEach { preset ->
                        val isSelected = activePreset == preset
                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                activePreset = preset
                                scope.launch {
                                    repository.applyVideoPreset(host, port.toIntOrNull() ?: 5555, preset)
                                        .onSuccess { lastLog = "Impostato: " }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, if (isSelected) AmberAccent else Color.DarkGray)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(preset.label, fontWeight = FontWeight.Bold, color = Color.White)
                                if (isSelected) Text("ATTIVO", color = AmberAccent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = DarkSurface), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Telecomando Rapido KeyEvents", fontWeight = FontWeight.Bold, color = Color.White)
                    Box(modifier = Modifier.size(180.dp).clip(CircleShape).background(DarkBackground).border(1.dp, Color.DarkGray, CircleShape), contentAlignment = Alignment.Center) {
                        IconButton(onClick = { scope.launch { repository.sendKey(host, port.toIntOrNull() ?: 5555, FireTvKeycodes.KEYCODE_DPAD_UP) } }, modifier = Modifier.align(Alignment.TopCenter)) { Icon(Icons.Default.KeyboardArrowUp, null, tint = Color.White) }
                        IconButton(onClick = { scope.launch { repository.sendKey(host, port.toIntOrNull() ?: 5555, FireTvKeycodes.KEYCODE_DPAD_DOWN) } }, modifier = Modifier.align(Alignment.BottomCenter)) { Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.White) }
                        IconButton(onClick = { scope.launch { repository.sendKey(host, port.toIntOrNull() ?: 5555, FireTvKeycodes.KEYCODE_DPAD_LEFT) } }, modifier = Modifier.align(Alignment.CenterStart)) { Icon(Icons.Default.KeyboardArrowLeft, null, tint = Color.White) }
                        IconButton(onClick = { scope.launch { repository.sendKey(host, port.toIntOrNull() ?: 5555, FireTvKeycodes.KEYCODE_DPAD_RIGHT) } }, modifier = Modifier.align(Alignment.CenterEnd)) { Icon(Icons.Default.KeyboardArrowRight, null, tint = Color.White) }
                        Box(modifier = Modifier.size(60.dp).clip(CircleShape).background(AmberAccent).clickable { scope.launch { repository.sendKey(host, port.toIntOrNull() ?: 5555, FireTvKeycodes.KEYCODE_DPAD_CENTER) } }, contentAlignment = Alignment.Center) {
                            Text("OK", color = Color.Black, fontWeight = FontWeight.Black)
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        IconButton(onClick = { scope.launch { repository.sendKey(host, port.toIntOrNull() ?: 5555, FireTvKeycodes.KEYCODE_BACK) } }) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) }
                        IconButton(onClick = { scope.launch { repository.sendKey(host, port.toIntOrNull() ?: 5555, FireTvKeycodes.KEYCODE_HOME) } }) { Icon(Icons.Default.Home, null, tint = AmberAccent) }
                        IconButton(onClick = { scope.launch { repository.sendKey(host, port.toIntOrNull() ?: 5555, FireTvKeycodes.KEYCODE_MENU) } }) { Icon(Icons.Default.Menu, null, tint = Color.White) }
                    }
                }
            }

            Text("Log: ", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
        }
    }
}
