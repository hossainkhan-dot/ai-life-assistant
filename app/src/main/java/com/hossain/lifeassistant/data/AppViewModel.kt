package com.hossain.lifeassistant.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.get(app)
    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")

    val diary = db.diaryDao().all()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val tasks = db.taskDao().all()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val reminders = db.reminderDao().all()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

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
}
