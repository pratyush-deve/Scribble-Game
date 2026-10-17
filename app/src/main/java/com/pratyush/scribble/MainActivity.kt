package com.pratyush.scribble

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.pratyush.scribble.core.designsystem.theme.DarkBackground
import com.pratyush.scribble.core.designsystem.theme.ScribbleTheme
import com.pratyush.scribble.navigation.ScribbleNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ScribbleTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    val navController = rememberNavController()
                    ScribbleNavHost(navController = navController)
                }
            }
        }
    }
}