package com.marcogn.kartlog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.marcogn.kartlog.ui.navigation.KartLogNavGraph
import com.marcogn.kartlog.ui.theme.KartLogTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KartLogTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    KartLogNavGraph()
                }
            }
        }
    }
}
