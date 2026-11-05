package io.github.alessandrocdrx.acertototal.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.alessandrocdrx.acertototal.dados.Banco
import io.github.alessandrocdrx.acertototal.dados.Repositorio
import io.github.alessandrocdrx.acertototal.dominio.Estatisticas
import io.github.alessandrocdrx.acertototal.dominio.calcularEstatisticas
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class NumerosUi(
    val periodo: Int = 1,
    val est: Estatisticas? = null,
    val temContas: Boolean = false,
)

class NumerosViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repositorio(Banco.obter(app))
    private val hoje = LocalDate.now()
    private val periodo = MutableStateFlow(1)

    val ui: StateFlow<NumerosUi> = combine(repo.contas, periodo) { contas, p ->
        NumerosUi(p, calcularEstatisticas(contas, p, hoje), contas.isNotEmpty())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NumerosUi())

    fun escolher(meses: Int) { periodo.value = meses }
}
