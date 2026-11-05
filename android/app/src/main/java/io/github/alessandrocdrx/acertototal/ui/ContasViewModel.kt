package io.github.alessandrocdrx.acertototal.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.alessandrocdrx.acertototal.dados.Banco
import io.github.alessandrocdrx.acertototal.dados.Conta
import io.github.alessandrocdrx.acertototal.dados.ContaComCliente
import io.github.alessandrocdrx.acertototal.dados.Exemplo
import io.github.alessandrocdrx.acertototal.dados.Repositorio
import io.github.alessandrocdrx.acertototal.dados.novoId
import io.github.alessandrocdrx.acertototal.dominio.Situacao
import io.github.alessandrocdrx.acertototal.dominio.situacaoDe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Os 3 cartões do topo são os filtros da lista (R77). Sem cartão marcado, mostra tudo o que falta receber. */
enum class FiltroContas(val titulo: String) {
    ABERTAS("Para receber"),
    AVENCER("A vencer"),
    ATRASADAS("Atrasadas"),
    PAGAS("Pagas"),
}

data class Total(val centavos: Long, val quantas: Int)

data class ContasUi(
    val hoje: LocalDate,
    val filtro: FiltroContas = FiltroContas.ABERTAS,
    val busca: String = "",
    val aVencer: Total = Total(0, 0),
    val atrasadas: Total = Total(0, 0),
    val pagasNoMes: Total = Total(0, 0),
    val lista: List<ContaComCliente> = emptyList(),
    /** Todas as contas, sem filtro: o detalhe e a tela do cliente procuram aqui. */
    val todas: List<ContaComCliente> = emptyList(),
    val temContas: Boolean = false,
    val carregado: Boolean = false,
)

class ContasViewModel(app: Application) : AndroidViewModel(app) {
    private val banco = Banco.obter(app)
    private val repo = Repositorio(banco)
    private val hoje = LocalDate.now()
    private val filtro = MutableStateFlow(FiltroContas.ABERTAS)
    private val busca = MutableStateFlow("")

    init {
        // Os dados de exemplo entram uma vez só: depois de "Apagar tudo", o app não volta a se encher.
        val prefs = app.getSharedPreferences("ajustes", Context.MODE_PRIVATE)
        viewModelScope.launch {
            if (!prefs.getBoolean("exemplo_semeado", false)) {
                Exemplo.semearSeVazio(banco, hoje)
                prefs.edit().putBoolean("exemplo_semeado", true).apply()
            }
        }
    }

    fun apagarTudo(aoTerminar: () -> Unit) = viewModelScope.launch {
        repo.apagarTudo()
        aoTerminar()
    }

    val ui: StateFlow<ContasUi> = combine(repo.contas, filtro, busca) { contas, f, q ->
        montar(contas, f, q, hoje)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ContasUi(hoje))

    /** Tocar de novo no cartão marcado tira o filtro (R77). */
    fun alternarFiltro(f: FiltroContas) = filtro.update { if (it == f) FiltroContas.ABERTAS else f }
    fun verTudo() { filtro.value = FiltroContas.ABERTAS }
    fun buscar(q: String) { busca.value = q }

    // ---- Escrita. Toda ação que apaga ou muda devolve o estado de antes, para o "Desfazer" (R5). ----

    /** Marca como paga. Devolve a conta como estava antes. */
    fun darBaixa(id: String, data: LocalDate, aoGravar: (antes: Conta) -> Unit = {}) = viewModelScope.launch {
        val c = repo.conta(id) ?: return@launch
        repo.salvarConta(c.copy(pagoEm = data))
        aoGravar(c)
    }

    fun restaurarConta(c: Conta) = viewModelScope.launch { repo.salvarConta(c) }

    fun desfazerBaixa(id: String, aoGravar: (antes: Conta) -> Unit = {}) = viewModelScope.launch {
        val c = repo.conta(id) ?: return@launch
        repo.salvarConta(c.copy(pagoEm = null))
        aoGravar(c)
    }

    fun salvarNovaConta(clienteId: String, descricao: String, valor: Long, venc: LocalDate) = viewModelScope.launch {
        repo.salvarConta(
            Conta(id = novoId(), clienteId = clienteId, descricao = descricao.trim().ifEmpty { "Conta" }, valor = valor, venc = venc),
        )
    }

    fun editarConta(
        id: String, clienteId: String, descricao: String, valor: Long, venc: LocalDate,
        aoGravar: (antes: Conta) -> Unit,
    ) = viewModelScope.launch {
        val c = repo.conta(id) ?: return@launch
        repo.salvarConta(c.copy(clienteId = clienteId, descricao = descricao.trim().ifEmpty { "Conta" }, valor = valor, venc = venc))
        aoGravar(c)
    }

    fun excluirConta(id: String, aoExcluir: (Conta) -> Unit) = viewModelScope.launch {
        repo.excluirConta(id)?.let(aoExcluir)
    }

    companion object {
        /** Pura e testável: monta o que a tela mostra a partir das contas e dos filtros. */
        fun montar(contas: List<ContaComCliente>, f: FiltroContas, q: String, hoje: LocalDate): ContasUi {
            fun sit(c: ContaComCliente) = situacaoDe(c.venc, c.pagoEm, hoje)
            fun total(l: List<ContaComCliente>) = Total(l.sumOf { it.valor }, l.size)
            val abertas = contas.filter { it.pagoEm == null }
            val mes = contas.filter { it.pagoEm != null && it.pagoEm.year == hoje.year && it.pagoEm.month == hoje.month }
            val termo = q.trim().lowercase()
            val lista = contas.filter { c ->
                when {
                    // Com busca, procura em todas as contas (não existe o filtro "Todas").
                    termo.isNotEmpty() -> "${c.clienteNome} ${c.descricao}".lowercase().contains(termo)
                    f == FiltroContas.ABERTAS -> sit(c) != Situacao.PAGA
                    f == FiltroContas.AVENCER -> sit(c) == Situacao.PENDENTE
                    f == FiltroContas.ATRASADAS -> sit(c) == Situacao.ATRASADA
                    else -> sit(c) == Situacao.PAGA
                }
            }.sortedWith { a, b ->
                // Pagas: a mais recente primeiro. O resto: vence antes primeiro, pagas no fim.
                if (a.pagoEm != null && b.pagoEm != null) b.pagoEm.compareTo(a.pagoEm)
                else compareValuesBy(a, b, { it.pagoEm != null }, { it.venc })
            }
            return ContasUi(
                hoje = hoje, filtro = f, busca = q,
                aVencer = total(abertas.filter { it.venc >= hoje }),
                atrasadas = total(abertas.filter { it.venc < hoje }),
                pagasNoMes = total(mes),
                lista = lista, todas = contas, temContas = contas.isNotEmpty(), carregado = true,
            )
        }
    }
}
