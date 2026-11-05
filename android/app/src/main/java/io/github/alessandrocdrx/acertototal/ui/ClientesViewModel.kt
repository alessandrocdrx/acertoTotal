package io.github.alessandrocdrx.acertototal.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.alessandrocdrx.acertototal.BuildConfig
import io.github.alessandrocdrx.acertototal.dados.Banco
import io.github.alessandrocdrx.acertototal.dados.Cliente
import io.github.alessandrocdrx.acertototal.dados.ContaComCliente
import io.github.alessandrocdrx.acertototal.dados.Repositorio
import io.github.alessandrocdrx.acertototal.dados.novoId
import io.github.alessandrocdrx.acertototal.dominio.Documento
import io.github.alessandrocdrx.acertototal.planilha.Campos
import io.github.alessandrocdrx.acertototal.dominio.ErroForm
import io.github.alessandrocdrx.acertototal.dominio.Validacao
import io.github.alessandrocdrx.acertototal.dominio.diasEntre
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Uma linha da lista de clientes: mostra primeiro o que importa, quanto o cliente deve (R7). */
data class LinhaCliente(
    val cliente: Cliente,
    val nContas: Int,
    val aberto: Long,
    val atrasadas: Int,
    /** Dias da conta atrasada mais antiga (0 se não houver). */
    val maisAntiga: Int,
)

data class ClientesUi(
    val linhas: List<LinhaCliente> = emptyList(),
    val todos: List<Cliente> = emptyList(),
    val busca: String = "",
)

class ClientesViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repositorio(Banco.obter(app))
    private val hoje = LocalDate.now()
    private val busca = MutableStateFlow("")

    val ui: StateFlow<ClientesUi> = combine(repo.clientes, repo.contas, busca) { clientes, contas, q ->
        montar(clientes, contas, q, hoje)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ClientesUi())

    fun buscar(q: String) { busca.value = q }

    /** Resultado do salvar: erro de campo, ou o cliente gravado (com aviso quando o documento só vale no teste). */
    sealed interface Resultado {
        data class Erro(val erro: ErroForm) : Resultado
        data class Salvo(val cliente: Cliente, val documentoValido: Boolean, val novo: Boolean) : Resultado
    }

    fun salvar(
        id: String?, nome: String, docTxt: String, telefone: String, email: String, obs: String,
        aoTerminar: (Resultado) -> Unit,
    ) = viewModelScope.launch {
        val d = Documento.soDigitos(docTxt)
        val dono = if (d.isNotEmpty()) repo.donoDoDoc(d, id)?.nome else null
        val erro = Validacao.cliente(nome, docTxt, dono, producao = BuildConfig.COBRANCA_REAL)
        if (erro != null) return@launch aoTerminar(Resultado.Erro(erro))
        val c = Cliente(id ?: novoId(), nome.trim(), d, telefone.trim(), email.trim(), obs.trim())
        repo.salvarCliente(c)
        aoTerminar(Resultado.Salvo(c, Documento.valido(d), novo = id == null))
    }

    /** Devolve o cliente excluído (para o "Desfazer"), ou null se ele tem contas. */
    fun excluir(id: String, aoTerminar: (Cliente?) -> Unit) = viewModelScope.launch {
        aoTerminar(repo.excluirCliente(id))
    }

    fun restaurar(c: Cliente) = viewModelScope.launch { repo.salvarCliente(c) }

    companion object {
        fun montar(clientes: List<Cliente>, contas: List<ContaComCliente>, busca: String, hoje: LocalDate): ClientesUi {
            // Busca só por nome ou telefone: o nome sem acento nem maiúscula, o telefone só pelos números.
            val q = Campos.norm(busca)
            val qd = Documento.soDigitos(busca)
            val porCliente = contas.groupBy { it.clienteId }
            val linhas = clientes
                .filter { c ->
                    q.isEmpty() || Campos.norm(c.nome).contains(q) ||
                        (qd.isNotEmpty() && Documento.soDigitos(c.telefone).contains(qd))
                }
                .sortedBy { it.nome.lowercase() }
                .map { c ->
                    val cs = porCliente[c.id].orEmpty()
                    val atrasadas = cs.filter { it.pagoEm == null && it.venc < hoje }
                    LinhaCliente(
                        cliente = c,
                        nContas = cs.size,
                        aberto = cs.filter { it.pagoEm == null }.sumOf { it.valor },
                        atrasadas = atrasadas.size,
                        maisAntiga = atrasadas.maxOfOrNull { diasEntre(it.venc, hoje) } ?: 0,
                    )
                }
            return ClientesUi(linhas, clientes, busca)
        }
    }
}
