package com.hossain.lifeassistant.data

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.get(app)
    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")
    private val recorder = VoiceRecorder(app)
    private val prefs = app.getSharedPreferences("settings", Context.MODE_PRIVATE)

    val diary = db.diaryDao().all()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val tasks = db.taskDao().all()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val reminders = db.reminderDao().all()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ---------- API key ----------
    private val _apiKey = MutableStateFlow(prefs.getString("openai_key", "") ?: "")
    val apiKey: StateFlow<String> = _apiKey

    private fun currentKey(): String = prefs.getString("openai_key", "") ?: ""

    fun saveKey(k: String) {
        val v = k.trim()
        prefs.edit().putString("openai_key", v).apply()
        _apiKey.value = v
    }

    // ---------- Voice pipeline ----------
    private val _voice = MutableStateFlow<VoiceState>(VoiceState.Idle)
    val voice: StateFlow<VoiceState> = _voice

    fun showMessage(text: String, ok: Boolean) {
        _voice.value = VoiceState.Message(text, ok)
    }

    fun clearMessage() {
        if (_voice.value is VoiceState.Message) _voice.value = VoiceState.Idle
    }

    fun startRecording() {
        if (currentKey().isBlank()) {
            showMessage("আগে Settings-এ OpenAI API key দিন", false)
            return
        }
        try {
            recorder.start()
            _voice.value = VoiceState.Recording
        } catch (e: Exception) {
            showMessage("রেকর্ড শুরু করা যায়নি", false)
        }
    }

    fun stopAndProcess() {
        if (_voice.value !is VoiceState.Recording) return
        val file = recorder.stop()
        if (file == null) {
            showMessage("খুব ছোট রেকর্ড। বাটন ধরে রেখে কথা বলুন", false)
            return
        }
        val key = currentKey()
        viewModelScope.launch {
            try {
                _voice.value = VoiceState.Processing("Transcribing…")
                val text = withContext(Dispatchers.IO) { OpenAi.transcribe(key, file) }
                if (text.isBlank()) {
                    showMessage("কিছু শোনা যায়নি", false)
                } else {
                    _voice.value = VoiceState.Processing("Analyzing…")
                    val items = withContext(Dispatchers.IO) {
                        OpenAi.analyze(key, text, LocalDate.now(), LocalTime.now())
                    }
                    if (items.isEmpty()) {
                        showMessage("সংরক্ষণ করার মতো কিছু পাওয়া যায়নি", false)
                    } else if (items.any { x -> x.needsConfirm }) {
                        _voice.value = VoiceState.Confirm(items, text)
                    } else {
                        val summary = saveItems(items)
                        showMessage("✓ Saved  $summary", true)
                    }
                }
            } catch (e: ApiException) {
                showMessage(e.message ?: "API error", false)
            } catch (e: IOException) {
                showMessage("AI processing requires internet connection", false)
            } catch (e: Exception) {
                showMessage("Error: ${e.message}", false)
            } finally {
                file.delete()
            }
        }
    }

    fun confirmSave() {
        val s = _voice.value as? VoiceState.Confirm ?: return
        _voice.value = VoiceState.Processing("Saving…")
        viewModelScope.launch {
            val summary = saveItems(s.items)
            showMessage("✓ Saved  $summary", true)
        }
    }

    fun discard() {
        if (_voice.value is VoiceState.Confirm) _voice.value = VoiceState.Idle
    }

    private fun categoryFor(type: String) = when (type) {
        "activity" -> "🏃 Activity"
        "idea" -> "💡 Idea"
        "shopping" -> "🛒 Shopping"
        "important" -> "📌 Important"
        else -> "📝 Diary"
    }

    private fun prefixFor(type: String) = when (type) {
        "event" -> "📅 "
        "birthday" -> "🎂 "
        "call" -> "📞 "
        else -> "⏰ "
    }

    private suspend fun saveItems(items: List<AiItem>): String {
        val today = LocalDate.now().toString()
        val nowT = LocalTime.now().format(timeFmt)
        var nDiary = 0
        var nTask = 0
        var nRem = 0
        for (item in items) {
            when (item.type) {
                "task" -> {
                    db.taskDao().insert(
                        TaskEntity(
                            title = item.title,
                            date = item.date ?: today,
                            time = item.time ?: "",
                            priority = item.priority
                        )
                    )
                    nTask++
                }
                "reminder", "event", "birthday", "call" -> {
                    db.reminderDao().insert(
                        ReminderEntity(
                            title = prefixFor(item.type) + item.title,
                            date = item.date ?: today,
                            time = item.time ?: ""
                        )
                    )
                    nRem++
                }
                else -> {
                    db.diaryDao().insert(
                        DiaryEntry(
                            date = item.date ?: today,
                            time = item.time ?: nowT,
                            content = item.content,
                            category = categoryFor(item.type)
                        )
                    )
                    nDiary++
                }
            }
        }
        return listOfNotNull(
            if (nDiary > 0) "📝$nDiary" else null,
            if (nTask > 0) "✅$nTask" else null,
            if (nRem > 0) "⏰$nRem" else null
        ).joinToString(" ")
    }

    // ---------- Manual add / delete ----------
    fun addDiary(content: String, category: String) {
        viewModelScope.launch {
            db.diaryDao().insert(
                DiaryEntry(
                    date = LocalDate.now().toString(),
                    time = LocalTime.now().format(timeFmt),
                    content = content,
                    category = category
                )
            )
        }
    }

    fun deleteDiary(e: DiaryEntry) {
        viewModelScope.launch { db.diaryDao().delete(e) }
    }

    fun addTask(title: String, date: String, time: String, priority: String) {
        viewModelScope.launch {
            db.taskDao().insert(TaskEntity(title = title, date = date, time = time, priority = priority))
        }
    }

    fun toggleTask(t: TaskEntity) {
        viewModelScope.launch { db.taskDao().update(t.copy(done = !t.done)) }
    }

    fun deleteTask(t: TaskEntity) {
        viewModelScope.launch { db.taskDao().delete(t) }
    }

    fun deleteReminder(r: ReminderEntity) {
        viewModelScope.launch { db.reminderDao().delete(r) }
    }
}
