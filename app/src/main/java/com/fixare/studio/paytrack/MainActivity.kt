package com.fixare.studio.paytrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.fixare.studio.paytrack.ui.PayTrackApp
import com.fixare.studio.paytrack.ui.theme.PayTrackTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PayTrackTheme {
                PayTrackApp()
            }
        }
    }
}
