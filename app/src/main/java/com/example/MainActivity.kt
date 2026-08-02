package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AlarmEntity
import com.example.ui.screens.AddEditAlarmScreen
import com.example.ui.screens.AlarmHomeScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AlarmViewModel

sealed class Screen {
    object Home : Screen()
    data class AddEdit(val alarm: AlarmEntity? = null) : Screen()
}

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Permission granted handler
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        checkNotificationPermission()

        setContent {
            val viewModel: AlarmViewModel = viewModel()
            val isDarkModeState by viewModel.isDarkMode.collectAsStateWithLifecycle()
            val useDarkTheme = isDarkModeState ?: isSystemInDarkTheme()

            var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

            MyApplicationTheme(darkTheme = useDarkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    when (val screen = currentScreen) {
                        is Screen.Home -> {
                            AlarmHomeScreen(
                                viewModel = viewModel,
                                onAddAlarm = {
                                    currentScreen = Screen.AddEdit(alarm = null)
                                },
                                onEditAlarm = { alarm ->
                                    currentScreen = Screen.AddEdit(alarm = alarm)
                                }
                            )
                        }
                        is Screen.AddEdit -> {
                            val is24Hour by viewModel.is24HourFormat.collectAsStateWithLifecycle()
                            AddEditAlarmScreen(
                                existingAlarm = screen.alarm,
                                is24Hour = is24Hour,
                                onBack = {
                                    currentScreen = Screen.Home
                                },
                                onSave = { alarm ->
                                    viewModel.saveAlarm(alarm)
                                    currentScreen = Screen.Home
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
