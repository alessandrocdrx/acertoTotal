package io.github.alessandrocdrx.acertototal

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.alessandrocdrx.acertototal.ui.App
import io.github.alessandrocdrx.acertototal.ui.Preferencias
import io.github.alessandrocdrx.acertototal.ui.Tema
import io.github.alessandrocdrx.acertototal.ui.TemaAcerto

/** Casca do app nativo (Jetpack Compose). A interface vem de `ui/`; os dados, de `dados/`. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var tema by remember { mutableStateOf(Preferencias.tema(this@MainActivity)) }
            val escuro = when (tema) {
                Tema.AUTO -> isSystemInDarkTheme()
                Tema.CLARO -> false
                Tema.ESCURO -> true
            }
            // Barras do sistema acompanham o tema: ícones escuros no claro, claros no escuro.
            DisposableEffect(escuro) {
                val estilo = if (escuro) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = estilo, navigationBarStyle = estilo)
                onDispose { }
            }
            TemaAcerto(escuro = escuro) {
                App(tema = tema, aoMudarTema = { tema = it; Preferencias.salvarTema(this@MainActivity, it) })
            }
        }
    }
}
