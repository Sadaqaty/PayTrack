package studio.fixare.paytrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import studio.fixare.paytrack.ui.PayTrackApp
import studio.fixare.paytrack.ui.theme.PayTrackTheme

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
