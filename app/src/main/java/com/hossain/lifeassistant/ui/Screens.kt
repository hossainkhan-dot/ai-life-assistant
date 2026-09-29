package com.hossain.lifeassistant.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------- Sample data (পরে Room database দিয়ে বদলানো হবে) ----------
data class DiaryItem(val time: String, val text: String, val category: String)
data class TaskItem(val title: String, val time: String, val priority: String, var done: Boolean)

val sampleDiary = listOf(
    DiaryItem("07:00 AM", "ঘুম থেকে উঠেছি।", "🏃 Activity"),
    DiaryItem("08:15 AM", "নাস্তা করেছি।", "🏃 Activity"),
    DiaryItem("10:30 AM", "বাজারে গিয়েছি।", "🛒 Shopping"),
    DiaryItem("01:20 PM", "দুপুরের খাবার খেয়েছি।", "🏃 Activity"),
    DiaryItem("05:00 PM", "বন্ধুর সাথে দেখা করেছি।", "📝 Diary")
)

// ---------- Home: Tap & Talk ----------
@Composable
fun TapAndTalkButton() {
    var listening by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("TAP & TALK") }

    val pulse = rememberInfiniteTransition(label = "pulse")
    val s by pulse.animateFloat(
        1f, 1.12f,
        infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "s"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (listening) Waveform() else Spacer(Modifier.height(40.dp))
        Spacer(Modifier.height(16.dp))
        Box(
            Modifier
                .size(140.dp)
                .scale(if (listening) s else 1f)
                .clip(CircleShape)
                .background(
                    if (listening) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.primary
                )
                .pointerInput(Unit) {
                    detectTapGestures(onPress = {
                        listening = true
                        status = "Listening…"
                        tryAwaitRelease()
                        listening = false
                        status = "✓ Saved (demo)"
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
        Text(status, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
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

// ---------- Diary ----------
@Composable
fun DiaryScreen() {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("Diary", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("30 September 2026", color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(sampleDiary) { d ->
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(3.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(d.time, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(d.text, fontSize = 16.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(d.category, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
                    }
                }
            }
        }
    }
}

// ---------- Tasks ----------
@Composable
fun TasksScreen() {
    val tasks = remember {
        mutableStateListOf(
            TaskItem("বাজার করা", "10:30 AM", "Normal", true),
            TaskItem("ডাক্তারের কাছে যাওয়া", "02:00 PM", "High", false),
            TaskItem("বন্ধুর সাথে দেখা", "05:00 PM", "Normal", false),
            TaskItem("ওষুধ খাওয়া", "10:00 PM", "High", false)
        )
    }
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("Tasks", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(tasks.size) { i ->
                val t = tasks[i]
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(3.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = t.done, onCheckedChange = { tasks[i] = t.copy(done = it) })
                        Column(Modifier.weight(1f)) {
                            Text(t.title, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            Text("${t.time} • ${t.priority}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
                        }
                    }
                }
            }
        }
    }
}

// ---------- Calendar ----------
@Composable
fun CalendarScreen() {
    var selected by remember { mutableIntStateOf(30) }
    val marked = setOf(2, 5, 12, 30)
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("Calendar", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("September 2026", color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth()) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                Text(it, Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        // 1 September 2026 = মঙ্গলবার, তাই শুরুতে ১টি ফাঁকা ঘর
        val cells = List(1) { 0 } + (1..30).toList()
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                for (idx in 0 until 7) {
                    val day = week.getOrNull(idx) ?: 0
                    Box(Modifier.weight(1f).height(44.dp), contentAlignment = Alignment.Center) {
                        if (day != 0) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(if (day == selected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent)
                                        .clickable { selected = day },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "$day", fontSize = 14.sp,
                                        color = if (day == selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                if (day in marked) {
                                    Box(Modifier.size(5.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary))
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("$selected September: এই দিনের Diary, Tasks ও Reminder এখানে দেখাবে", Modifier.padding(16.dp))
        }
    }
}

// ---------- Settings ----------
@Composable
fun SettingsScreen() {
    var floating by remember { mutableStateOf(false) }
    val items = listOf("Profile", "Language", "Theme", "AI Settings", "Voice Settings",
        "Notification", "Reminder", "Privacy", "Backup", "Data Export", "About")
    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Settings", fontSize = 26.sp, fontWeight = FontWeight.Bold) }
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Floating Assistant", Modifier.weight(1f), fontSize = 16.sp)
                    Switch(checked = floating, onCheckedChange = { floating = it })
                }
            }
        }
        items(items) { name ->
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(name, Modifier.weight(1f), fontSize = 16.sp)
                    Icon(Icons.Filled.ChevronRight, null)
                }
            }
        }
    }
}
