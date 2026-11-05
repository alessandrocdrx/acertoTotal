package io.github.alessandrocdrx.acertototal.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import io.github.alessandrocdrx.acertototal.Assinatura
import io.github.alessandrocdrx.acertototal.BuildConfig
import io.github.alessandrocdrx.acertototal.EstadoAssinatura
import io.github.alessandrocdrx.acertototal.dominio.Avaliacao
import io.github.alessandrocdrx.acertototal.dominio.DadosAvaliacao
import io.github.alessandrocdrx.acertototal.dominio.Teste
import kotlinx.coroutines.flow.StateFlow

/** Teste grátis + assinatura. O contador vive em arquivo à parte dos dados (restaurar uma cópia não o reinicia). */
class LicencaViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("avaliacao", Context.MODE_PRIVATE)
    val assinatura = Assinatura(app)
    val estado: StateFlow<EstadoAssinatura> = assinatura.estado
    val avaliacao: Avaliacao

    init {
        val salvo = if (prefs.contains("inicio")) {
            DadosAvaliacao(prefs.getLong("inicio", 0), prefs.getLong("visto", 0), prefs.getBoolean("relogio", false))
        } else null
        val (novo, av) = Teste.conferir(salvo, System.currentTimeMillis(), BuildConfig.LIMITE_DIAS)
        prefs.edit().putLong("inicio", novo.inicio).putLong("visto", novo.visto).putBoolean("relogio", novo.relogioAtrasado).apply()
        avaliacao = av
        assinatura.iniciar()
    }

    override fun onCleared() {
        assinatura.fechar()
    }
}
