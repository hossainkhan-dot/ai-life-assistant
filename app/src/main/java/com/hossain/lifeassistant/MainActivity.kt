package com.hossain.lifeassistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hossain.lifeassistant.data.AppViewModel
import com.hossain.lifeassistant.ui.*
import com.hossain.lifeassistant.ui.theme.LifeAssistantTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LifeAssistantTheme { AppRoot() }
        }
    }
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("home", "Home", Icons.Filled.Home),
    Tab("diary", "Diary", Icons.Filled.MenuBook),
    Tab("tasks", "Tasks", Icons.Filled.CheckCircle),
    Tab("calendar", "Calendar", Icons.Filled.CalendarMonth),
    Tab("settings", "Settings", Icons.Filled.Settings)
)

@Composable
fun AppRoot() {
    val nav = rememberNavController()
    val current = nav.currentBackStackEntryAsState().value?.destination?.route
    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { t ->
                    NavigationBarItem(
                        selected = current == t.route,
                        onClick = {
                            nav.navigate(t.route) {
                                popUpTo("home")
                                launchSingleTop = true
                            }
                        },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label) }
                    )
                }
            }
        }
    ) { pad ->
        NavHost(nav, startDestination = "home", modifier = Modifier.padding(pad)) {
            composable("home") { HomeScreen() }
            composable("diary") { DiaryScreen() }
            composable("tasks") { TasksScreen() }
            composable("calendar") { CalendarScreen() }
            composable("settings") { SettingsScreen() }
        }
    }
}

@Composable
fun HomeScreen(vm: AppViewModel = viewModel()) {
    val tasks by vm.tasks.collectAsState()
    val diary by vm.diary.collectAsState()
    val reminders by vm.reminders.collectAsState()
    val now = LocalTime.now()
    val today = LocalDate.now().toString()
    val nowStr = String.format(Locale.ENGLISH, "%02d:%02d", now.hour, now.minute)
    val greeting = when {
        now.hour < 12 -> "Good Morning"
        now.hour < 17 -> "Good Afternoon"
        else -> "Good Evening"
    }
    val dateText = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH))

    val completed = tasks.count { it.done && it.date == today }
    val pending = tasks.count { !it.done && it.date <= today }
    val todayDiary = diary.filter { it.date == today }.sortedBy { it.time }
    val next = tasks
        .filter { !it.done && it.date == today && it.time.isNotBlank() && it.time >= nowStr }
        .map { it.time }
        .plus(reminders.filter { it.date == today && it.time.isNotBlank() && it.time >= nowStr }.map { it.time })
        .minOrNull() ?: "—"

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(Modifier.fillMaxWidth()) {
            Text("$greeting, HOSSAIN 👋", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(dateText, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(20.dp))
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                Modifier.padding(20.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Stat(completed.toString(), "Completed")
                Stat(pending.toString(), "Pending")
                Stat(todayDiary.size.toString(), "Activities")
                Stat(next, "Next")
            }
        }
        if (todayDiary.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Today's Timeline", fontWeight = FontWeight.Bold)
                    todayDiary.takeLast(3).forEach { e ->
                        Row {
                            Text(e.time, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(10.dp))
                            Text(e.content, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        VoiceConfirmDialog(vm)
        TapAndTalkButton(vm)
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    }
}
