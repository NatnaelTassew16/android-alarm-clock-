package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AlarmDatabase
import com.example.data.AlarmEntity
import com.example.data.AlarmRepository
import com.example.data.BackupRestoreManager
import com.example.scheduler.AlarmScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SortOption {
    BY_TIME,
    BY_LABEL,
    BY_ENABLED
}

class AlarmViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AlarmRepository
    private val scheduler: AlarmScheduler

    val searchQuery = MutableStateFlow("")
    val sortOption = MutableStateFlow(SortOption.BY_TIME)
    val is24HourFormat = MutableStateFlow(false)
    val isDarkMode = MutableStateFlow<Boolean?>(null) // null = system

    val allAlarms: StateFlow<List<AlarmEntity>>

    val filteredAlarms: StateFlow<List<AlarmEntity>>

    val nextUpcomingAlarm: StateFlow<AlarmEntity?>

    init {
        val alarmDao = AlarmDatabase.getDatabase(application).alarmDao()
        repository = AlarmRepository(alarmDao)
        scheduler = AlarmScheduler(application)

        allAlarms = repository.allAlarms
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        filteredAlarms = combine(allAlarms, searchQuery, sortOption) { list, query, sort ->
            val filtered = if (query.isBlank()) {
                list
            } else {
                list.filter { alarm ->
                    alarm.label.contains(query, ignoreCase = true) ||
                    alarm.getFormattedTime(is24Hour = false).contains(query) ||
                    alarm.getFormattedTime(is24Hour = true).contains(query)
                }
            }

            when (sort) {
                SortOption.BY_TIME -> filtered.sortedWith(compareBy({ it.hour }, { it.minute }))
                SortOption.BY_LABEL -> filtered.sortedBy { it.label.lowercase() }
                SortOption.BY_ENABLED -> filtered.sortedWith(compareByDescending<AlarmEntity> { it.isEnabled }.thenBy { it.hour }.thenBy { it.minute })
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        nextUpcomingAlarm = allAlarms.map { alarms ->
            val now = System.currentTimeMillis()
            alarms.filter { it.isEnabled }
                .minByOrNull { it.calculateNextTriggerMillis(now) }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    }

    fun saveAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            if (alarm.id == 0L) {
                val newId = repository.insertAlarm(alarm)
                val savedAlarm = alarm.copy(id = newId)
                scheduler.schedule(savedAlarm)
            } else {
                repository.updateAlarm(alarm)
                scheduler.schedule(alarm)
            }
        }
    }

    fun toggleAlarmEnabled(alarm: AlarmEntity, isEnabled: Boolean) {
        viewModelScope.launch {
            val updated = alarm.copy(isEnabled = isEnabled, isSnoozed = false, snoozeUntilMillis = 0L)
            repository.updateAlarm(updated)
            if (isEnabled) {
                scheduler.schedule(updated)
            } else {
                scheduler.cancel(updated)
            }
        }
    }

    fun deleteAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            scheduler.cancel(alarm)
            repository.deleteAlarm(alarm)
        }
    }

    fun exportBackupJson(): String {
        return BackupRestoreManager.exportAlarmsToJson(allAlarms.value)
    }

    fun importBackupJson(jsonString: String, onSuccess: (Int) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val imported = BackupRestoreManager.importAlarmsFromJson(jsonString)
                if (imported.isNotEmpty()) {
                    repository.insertAll(imported)
                    // Schedule new enabled alarms
                    for (alarm in imported) {
                        if (alarm.isEnabled) {
                            scheduler.schedule(alarm)
                        }
                    }
                    onSuccess(imported.size)
                } else {
                    onError("No valid alarms found in backup JSON.")
                }
            } catch (e: Exception) {
                onError("Failed to parse JSON: ${e.localizedMessage}")
            }
        }
    }
}
