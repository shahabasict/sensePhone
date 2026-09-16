package com.sensephone.app.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sensephone.app.presentation.navigation.SenseAppNavHost
import com.sensephone.app.presentation.theme.SensePhoneTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SensePhoneTheme {
                SenseAppNavHost()
            }
        }
    }
}