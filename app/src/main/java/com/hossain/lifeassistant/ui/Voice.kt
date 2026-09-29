package com.hossain.lifeassistant.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.hossain.lifeassistant.data.AiItem
import com.hossain.lifeassistant.data.AppViewModel
import com.hossain.lifeassistant.data.VoiceState
import kotlinx.coroutines.delay

@Composable
fun TapAndTalkButton(vm: AppViewModel) {
    val ctx = LocalContext.current
    val state by vm.voice.collectAsState()
    val recording = state is VoiceState.Recording
    val busy = state is VoiceState.Processing

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) vm.showMessage("Mic permission ছাড়া voice কাজ করবে না", false)
    }

    LaunchedEffect(state) {
        if (state is VoiceState.Message) {
            delay(4000)
            vm.clearMessage()
        }
    }

    val pulse = rememberInfiniteTransition(label = "pulse")
    val s by pulse.animateFloat(
        1f, 1.12f,
        infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "s"
    )

    val label = when (val st = state) {
        is VoiceState.Recording -> "Listening…"
        is VoiceState.Processing -> st.stage
        is VoiceState.Message -> st.text
        is VoiceState.Confirm -> "নিশ্চিত করুন"
        else -> "TAP & TALK"
    }
    val isIdle = state is VoiceState.Idle

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.height(40.dp), contentAlignment = Alignment.Center) {
            if (recording) Waveform()
            else if (busy) CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
        }
        Spacer(Modifier.height(16.dp))
        Box(
            Modifier
                .size(140.dp)
                .scale(if (recording) s else 1f)
                .clip(CircleShape)
                .background(
                    if (recording) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.primary
                )
                .pointerInput(Unit) {
                    detectTapGestures(onPress = {
                        val cur = vm.voice.value
                        if (cur is VoiceState.Processing || cur is VoiceState.Confirm) {
                            // প্রসেসিং চলছে, এখন নতুন রেকর্ড নয়
                        } else if (ContextCompat.checkSelfPermission(
                                ctx, Manifest.permission.RECORD_AUDIO
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            permLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            vm.startRecording()
                            tryAwaitRelease()
                            vm.stopAndProcess()
                        }
                    })
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Mic, "Tap and Talk",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(64.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            label,
            fontWeight = FontWeight.Bold,
            letterSpacing = if (isIdle) 2.sp else 0.sp,
            fontSize = if (isIdle) 14.sp else 15.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        if (isIdle) {
            Text(
                "বাটন ধরে কথা বলুন, ছাড়লে সংরক্ষণ হবে",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
        }
    }
}

@Composable
fun Waveform() {
    val t = rememberInfiniteTransition(label = "wave")
    Row(
        Modifier.height(40.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(9) { i ->
            val h by t.animateFloat(
                8f, 40f,
                infiniteRepeatable(tween(400 + i * 60), RepeatMode.Reverse), label = "b$i"
            )
            Box(
                Modifier
                    .width(5.dp)
                    .height(h.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

// ---------- ভুল/অস্পষ্ট তথ্য থাকলে নিশ্চিতকরণ ----------
@Composable
private fun ItemRow(item: AiItem) {
    val emoji = when (item.type) {
        "task" -> "✅"
        "reminder" -> "⏰"
        "event" -> "📅"
        "birthday" -> "🎂"
        "idea" -> "💡"
        "shopping" -> "🛒"
        "important" -> "📌"
        "activity" -> "🏃"
        "call" -> "📞"
        else -> "📝"
    }
    val main = when (item.type) {
        "task", "reminder", "event", "birthday", "call" -> item.title
        else -> item.content
    }
    val meta = listOfNotNull(item.date, item.time).joinToString(" • ")
    Column(Modifier.fillMaxWidth()) {
        Text("$emoji $main", fontSize = 15.sp, fontWeight = FontWeight.Medium)
        if (meta.isNotEmpty()) {
            Text(meta, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
        }
        if (item.needsConfirm) {
            Text(
                "⚠ " + item.note.ifBlank { "তথ্য পরিষ্কার নয়, দেখে নিন" },
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun VoiceConfirmDialog(vm: AppViewModel) {
    val state by vm.voice.collectAsState()
    val c = state as? VoiceState.Confirm ?: return
    AlertDialog(
        onDismissRequest = { vm.discard() },
        title = { Text("নিশ্চিত করুন") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "\"${c.transcript}\"",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                c.items.forEach { ItemRow(it) }
                Text(
                    "ভুল থাকলে Discard করে আবার বলুন।",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        },
        confirmButton = { TextButton(onClick = { vm.confirmSave() }) { Text("Save all") } },
        dismissButton = { TextButton(onClick = { vm.discard() }) { Text("Discard") } }
    )
}

// ---------- Settings: OpenAI API key ----------
@Composable
fun ApiKeyCard(vm: AppViewModel) {
    val saved by vm.apiKey.collectAsState()
    var text by remember { mutableStateOf("") }
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("OpenAI API key", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(
                if (saved.isBlank()) "এখনো key দেওয়া হয়নি"
                else "Key সংরক্ষিত আছে (••••${saved.takeLast(4)})",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                label = { Text("sk-... পেস্ট করুন") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    enabled = text.isNotBlank(),
                    onClick = { vm.saveKey(text); text = "" }
                ) { Text("Save") }
                if (saved.isNotBlank()) {
                    TextButton(onClick = { vm.saveKey("") }) { Text("Remove") }
                }
            }
            Text(
                "Voice ও লেখা বিশ্লেষণের জন্য আপনার কথা OpenAI-র সার্ভারে পাঠানো হয়। Key শুধু এই ফোনে থাকে, কোডে বা GitHub-এ নয়।",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
        }
    }
}
