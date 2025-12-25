package dev.pranav.reef.util

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat


fun Activity.setDarkStatusBar() {
    val windowInsetsController =
        WindowCompat.getInsetsController(window, window.decorView)
    windowInsetsController.isAppearanceLightStatusBars = false
}

fun ComponentActivity.applyDefaults() {
    setDarkStatusBar()
    enableEdgeToEdge()
}

