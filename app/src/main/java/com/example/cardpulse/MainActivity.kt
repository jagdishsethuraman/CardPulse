package com.example.cardpulse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.cardpulse.theme.CardPulseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as CardPulseApp
        setContent {
            val darkModeState by app.userPreferences.darkModeFlow.collectAsState(initial = "system")
            val isDarkTheme = when (darkModeState) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }
            CardPulseTheme(darkTheme = isDarkTheme) {
                MainNavigation()
            }
        }
    }
}

