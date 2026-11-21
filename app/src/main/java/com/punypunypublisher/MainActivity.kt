package com.punypunypublisher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.punypunypublisher.ui.AppScaffold
import com.punypunypublisher.ui.theme.PunyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            PunyTheme {
                AppScaffold()
            }
        }
    }
}
