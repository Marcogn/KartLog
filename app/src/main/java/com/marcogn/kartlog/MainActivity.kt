package com.marcogn.kartlog

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.marcogn.kartlog.domain.model.AppLocale
import com.marcogn.kartlog.domain.model.ThemeMode
import com.marcogn.kartlog.ui.navigation.KartLogNavGraph
import com.marcogn.kartlog.ui.theme.KartLogTheme
import com.marcogn.kartlog.ui.theme.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * `AppCompatActivity` e non `ComponentActivity`: `AppCompatDelegate.setApplicationLocales()`
 * (selettore della lingua in Impostazioni) richiede questa classe base, altrimenti il cambio di
 * lingua viene ignorato in silenzio. Stessa scelta di ThePatientGamerHelper; la UI resta tutta
 * Compose (`setContent {}`).
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // La configurazione dell'activity tiene già conto della lingua scelta nell'app (anche sotto
        // Android 13, dove il backport AppCompat non tocca Locale.getDefault()): da qui la leggono i
        // nomi di gioco localizzati (vedi AppLocale).
        AppLocale.update(resources.configuration.locales[0])
        enableEdgeToEdge()
        setContent {
            KartLogApp()
        }
    }
}

@Composable
private fun KartLogApp(themeViewModel: ThemeViewModel = hiltViewModel()) {
    val themeMode by themeViewModel.themeMode.collectAsState()
    val darkTheme = when (themeMode) {
        ThemeMode.SISTEMA -> isSystemInDarkTheme()
        ThemeMode.CHIARO -> false
        ThemeMode.SCURO -> true
    }
    KartLogTheme(darkTheme = darkTheme) {
        Surface(modifier = Modifier.fillMaxSize()) {
            KartLogNavGraph()
        }
    }
}
