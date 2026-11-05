package io.github.alessandrocdrx.acertototal

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.alessandrocdrx.acertototal.ui.App
import io.github.alessandrocdrx.acertototal.ui.TemaAcerto

/** Casca do app nativo (Jetpack Compose). A interface vem de `ui/`; os dados, de `dados/`. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // O tema claro é o padrão (R18); o escuro só entra quando a pessoa escolhe.
        val escuro = getSharedPreferences("ajustes", MODE_PRIVATE).getBoolean("tema_escuro", false)
        enableEdgeToEdge(
            statusBarStyle = if (escuro) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = if (escuro) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent { TemaAcerto(escuro = escuro) { App() } }
    }
}
