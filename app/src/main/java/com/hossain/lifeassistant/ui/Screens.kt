@file:OptIn(ExperimentalMaterial3Api::class)

package com.hossain.lifeassistant.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hossain.lifeassistant.data.AppViewModel
import com.hossain.lifeassistant.data.DiaryEntry
import com.hossain.lifeassistant.data.TaskEntity
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

val categories = listOf(
    "📝 Diary", "🏃 Activity", "💡 Idea", "🛒 Shopping", "📌 Important", "📞 Call/Follow-up"
)

// ---------- Common ----------
@Composable
private fun EmptyHint(text: String) {
    Text(
        text,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun ChipRow(options: List<String>, selected: String, onPick: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(options) { c ->
            FilterChip(selected = c == selected, onClick = { onPick(c) }, label = { Text(c) })
        }
    }
}

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
                        status = "Voice সংযোগ পরের ধাপে"
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
private fun DiaryCard(d: DiaryEntry, onDelete: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(3.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(d.time, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(d.content, fontSize = 16.sp)
                Spacer(Modifier.height(6.dp))
                Text(d.category, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
            }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, "Delete") }
        }
    }
}

@Composable
private fun AddDiaryDialog(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var text by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf(categories[0]) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("নতুন Diary entry") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("কী হয়েছে?") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                ChipRow(categories, cat) { cat = it }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (text.isNotBlank()) onSave(text.trim(), cat) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun DiaryScreen(vm: AppViewModel = viewModel()) {
    val diary by vm.diary.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Text("Diary", fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            if (diary.isEmpty()) EmptyHint("এখনো কোনো entry নেই। নিচের + চাপ দিয়ে যোগ করুন।")
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                val grouped = diary.groupBy { it.date }
                grouped.forEach { (date, list) ->
                    item(key = "h$date") {
                        Text(date, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    items(list, key = { it.id }) { d -> DiaryCard(d) { vm.deleteDiary(d) } }
                }
            }
        }
        FloatingActionButton(
            onClick = { showAdd = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)
        ) { Icon(Icons.Filled.Add, "Add") }
    }
    if (showAdd) {
        AddDiaryDialog(
            onDismiss = { showAdd = false },
            onSave = { c, cat -> vm.addDiary(c, cat); showAdd = false }
        )
    }
}

// ---------- Tasks ----------
@Composable
private fun TaskCard(t: TaskEntity, vm: AppViewModel) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(3.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = t.done, onCheckedChange = { vm.toggleTask(t) })
            Column(Modifier.weight(1f)) {
                Text(
                    t.title, fontSize = 16.sp, fontWeight = FontWeight.Medium,
                    textDecoration = if (t.done) TextDecoration.LineThrough else TextDecoration.None
                )
                val timePart = if (t.time.isBlank()) "" else " • ${t.time}"
                Text(
                    "${t.date}$timePart • ${t.priority}", fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            }
            IconButton(onClick = { vm.deleteTask(t) }) { Icon(Icons.Filled.Delete, "Delete") }
        }
    }
}

private fun LazyListScope.taskSection(title: String, list: List<TaskEntity>, vm: AppViewModel) {
    item(key = "sec_$title") {
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
    if (list.isEmpty()) {
        item(key = "empty_$title") { EmptyHint("কিছু নেই") }
    } else {
        items(list, key = { it.id }) { t -> TaskCard(t, vm) }
    }
}

@Composable
private fun AddTaskDialog(onDismiss: () -> Unit, onSave: (String, String, String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var time by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("Normal") }
    val dateOk = runCatching { LocalDate.parse(date) }.isSuccess
    val timeOk = time.isBlank() || Regex("([01]\\d|2[0-3]):[0-5]\\d").matches(time)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("নতুন Task") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("কাজের নাম") }, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = date, onValueChange = { date = it }, isError = !dateOk,
                    singleLine = true, label = { Text("তারিখ (yyyy-MM-dd)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = time, onValueChange = { time = it }, isError = !timeOk,
                    singleLine = true, label = { Text("সময় (HH:mm, ঐচ্ছিক)") },
                    modifier = Modifier.fillMaxWidth()
                )
                ChipRow(listOf("Normal", "High"), priority) { priority = it }
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank() && dateOk && timeOk,
                onClick = { onSave(title.trim(), date, time, priority) }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun TasksScreen(vm: AppViewModel = viewModel()) {
    val tasks by vm.tasks.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    val today = LocalDate.now().toString()
    val todayList = tasks.filter { !it.done && it.date <= today }
    val upcoming = tasks.filter { !it.done && it.date > today }
    val completed = tasks.filter { it.done }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Text("Tasks", fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                taskSection("Today's Tasks", todayList, vm)
                taskSection("Upcoming Tasks", upcoming, vm)
                taskSection("Completed Tasks", completed, vm)
            }
        }
        FloatingActionButton(
            onClick = { showAdd = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)
        ) { Icon(Icons.Filled.Add, "Add") }
    }
    if (showAdd) {
        AddTaskDialog(
            onDismiss = { showAdd = false },
            onSave = { t, d, tm, p -> vm.addTask(t, d, tm, p); showAdd = false }
        )
    }
}

// ---------- Calendar ----------
@Composable
fun CalendarScreen(vm: AppViewModel = viewModel()) {
    val diary by vm.diary.collectAsState()
    val tasks by vm.tasks.collectAsState()
    var month by remember { mutableStateOf(YearMonth.now()) }
    var selected by remember { mutableStateOf(LocalDate.now()) }
    val marked = remember(diary, tasks) {
        diary.map { it.date }.toSet() + tasks.map { it.date }.toSet()
    }
    val monthName = month.month.getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text("Calendar", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { month = month.minusMonths(1) }) { Icon(Icons.Filled.ChevronLeft, "Previous") }
            Text(
                "$monthName ${month.year}", Modifier.weight(1f),
                textAlign = TextAlign.Center, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(onClick = { month = month.plusMonths(1) }) { Icon(Icons.Filled.ChevronRight, "Next") }
        }
        Row(Modifier.fillMaxWidth()) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        val offset = month.atDay(1).dayOfWeek.value - 1
        val cells = List(offset) { 0 } + (1..month.lengthOfMonth()).toList()
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                for (idx in 0 until 7) {
                    val day = week.getOrNull(idx) ?: 0
                    Box(Modifier.weight(1f).height(44.dp), contentAlignment = Alignment.Center) {
                        if (day != 0) {
                            val date = month.atDay(day)
                            val isSel = date == selected
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable { selected = date },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "$day", fontSize = 14.sp,
                                        color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                if (date.toString() in marked) {
                                    Box(Modifier.size(5.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary))
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        val sel = selected.toString()
        val dayDiary = diary.filter { it.date == sel }.sortedBy { it.time }
        val dayTasks = tasks.filter { it.date == sel }
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(sel, fontWeight = FontWeight.Bold)
                if (dayDiary.isEmpty() && dayTasks.isEmpty()) EmptyHint("এই দিনে কিছু নেই")
                dayDiary.forEach { Text("📝 ${it.time}  ${it.content}") }
                dayTasks.forEach {
                    val mark = if (it.done) "☑" else "☐"
                    Text("$mark ${it.title}")
                }
            }
        }
    }
}

// ---------- Settings ----------
@Composable
fun SettingsScreen() {
    var floating by remember { mutableStateOf(false) }
    val entries = listOf(
        "Profile", "Language", "Theme", "AI Settings", "Voice Settings",
        "Notification", "Reminder", "Privacy", "Backup", "Data Export", "About"
    )
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
        items(entries) { name ->
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
