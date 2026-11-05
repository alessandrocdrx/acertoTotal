package io.github.alessandrocdrx.acertototal.ui

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.alessandrocdrx.acertototal.BuildConfig
import io.github.alessandrocdrx.acertototal.dados.Banco
import io.github.alessandrocdrx.acertototal.dados.Conta
import io.github.alessandrocdrx.acertototal.dados.ImportacaoReg
import io.github.alessandrocdrx.acertototal.dados.Repositorio
import io.github.alessandrocdrx.acertototal.dados.novoId
import io.github.alessandrocdrx.acertototal.planilha.AbaAnalisada
import io.github.alessandrocdrx.acertototal.planilha.AbaXlsx
import io.github.alessandrocdrx.acertototal.planilha.CampoPlanilha
import io.github.alessandrocdrx.acertototal.planilha.Campos
import io.github.alessandrocdrx.acertototal.planilha.Gravacao
import io.github.alessandrocdrx.acertototal.planilha.Importacao
import io.github.alessandrocdrx.acertototal.planilha.LeitorArquivo
import io.github.alessandrocdrx.acertototal.planilha.Leitura
import io.github.alessandrocdrx.acertototal.planilha.Modelos
import io.github.alessandrocdrx.acertototal.planilha.Plano
import io.github.alessandrocdrx.acertototal.planilha.ResumoImp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.LocalDateTime

/** Em que passo da importação a pessoa está: 1 Arquivo, 2 Colunas, 3 Conferir. Nada é gravado antes de confirmar. */
sealed interface ImpEstado {
    data object Escolher : ImpEstado
    data class Lendo(val arquivo: String) : ImpEstado
    data class Erro(val arquivo: String, val titulo: String, val texto: String, val modelo: Boolean) : ImpEstado

    /** A aba escolhida e o mapa de colunas ficam nos dois passos seguintes. */
    sealed interface ComAba : ImpEstado {
        val arquivo: String
        val abas: List<AbaAnalisada>
        val aba: Int
        val mapa: Map<CampoPlanilha, Int>
        val totalAbas: Int
    }

    data class Colunas(
        override val arquivo: String, override val abas: List<AbaAnalisada>, override val aba: Int,
        override val mapa: Map<CampoPlanilha, Int>, override val totalAbas: Int,
        /** Erro de escolha de coluna, mostrado embaixo do campo (R8). */
        val erro: Pair<CampoPlanilha, String>? = null,
    ) : ComAba

    data class Previa(
        override val arquivo: String, override val abas: List<AbaAnalisada>, override val aba: Int,
        override val mapa: Map<CampoPlanilha, Int>, override val totalAbas: Int,
        val plano: Plano, val contas: List<Conta>,
    ) : ComAba

    data class Concluida(val resumo: ResumoImp, val registroId: Long, val previa: Previa) : ImpEstado
}

class ImportacaoViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repositorio(Banco.obter(app))
    private val hoje = LocalDate.now()
    private val _estado = MutableStateFlow<ImpEstado>(ImpEstado.Escolher)
    val estado: StateFlow<ImpEstado> = _estado

    /** As 10 últimas importações, da mais recente para a mais antiga. */
    val registros: StateFlow<List<ImportacaoReg>> =
        repo.importacoes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun reiniciar() { _estado.value = ImpEstado.Escolher }

    // ---------------------------------------------------------------- passo 1: arquivo

    fun lerUri(uri: Uri) {
        viewModelScope.launch {
            val (nome, bytes) = withContext(Dispatchers.IO) { nomeEBytes(uri) }
            _estado.value = ImpEstado.Lendo(nome)
            if (bytes == null) {
                _estado.value = ImpEstado.Erro(
                    nome, "Não deu para abrir o arquivo",
                    "Escolha de novo. Se o arquivo estiver na nuvem (Drive, OneDrive), baixe para o celular antes.", false,
                )
                return@launch
            }
            abrir(nome, bytes)
        }
    }

    /** Lê o arquivo escolhido, sem passar de 10 MB: um arquivo maior é recusado antes de ocupar a memória. */
    private fun nomeEBytes(uri: Uri): Pair<String, ByteArray?> {
        val resolver = getApplication<Application>().contentResolver
        val nome = runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull() ?: "planilha"
        val bytes = runCatching {
            resolver.openInputStream(uri)?.use { entrada ->
                val saida = ByteArrayOutputStream()
                val buffer = ByteArray(64 * 1024)
                while (saida.size() <= LeitorArquivo.LIMITE_BYTES) {
                    val n = entrada.read(buffer)
                    if (n < 0) break
                    saida.write(buffer, 0, n)
                }
                saida.toByteArray()
            }
        }.getOrNull()
        return nome to bytes
    }

    private suspend fun abrir(nome: String, bytes: ByteArray) {
        val leitura = withContext(Dispatchers.Default) { LeitorArquivo.ler(bytes) }
        when (leitura) {
            is Leitura.Erro -> _estado.value = ImpEstado.Erro(nome, leitura.titulo, leitura.texto, leitura.modelo)
            is Leitura.Ok -> seguirComAbas(nome, leitura.abas)
        }
    }

    /** "Testar com a planilha de exemplo": só no app de teste. */
    fun usarExemplo() { viewModelScope.launch { seguirComAbas("Planilha de exemplo", listOf(Modelos.exemplo(hoje))) } }

    private suspend fun seguirComAbas(nome: String, abasLidas: List<AbaXlsx>) {
        val abas = withContext(Dispatchers.Default) {
            abasLidas.mapNotNull { Campos.analisarAba(it.nome, it.linhas) }
                .sortedWith(compareByDescending<AbaAnalisada> { it.score }.thenByDescending { it.linhas.isNotEmpty() })
        }
        if (abas.isEmpty()) {
            _estado.value = ImpEstado.Erro(
                nome, "Não achei os títulos das colunas",
                "A primeira linha da planilha precisa ter os nomes das colunas, como CPF/CNPJ, Nome, Valor e Vencimento. Baixe a planilha modelo para ver como fica.",
                true,
            )
            return
        }
        seguir(ImpEstado.Colunas(nome, abas, 0, Campos.autoMapa(abas[0].cols), abasLidas.size))
    }

    /** Com tudo reconhecido vai direto para a prévia; se falta uma coluna, pede para a pessoa escolher. */
    private suspend fun seguir(c: ImpEstado.Colunas) {
        val a = c.abas[c.aba]
        when {
            a.linhas.isEmpty() -> _estado.value = ImpEstado.Erro(
                c.arquivo, "A planilha está sem dados",
                "Achei os títulos na linha ${a.linhaCab}" + (if (c.abas.size > 1) " da aba “${a.nome}”" else "") + ", mas nenhuma linha preenchida embaixo deles.", true,
            )
            a.linhas.size > LeitorArquivo.LIMITE_LINHAS -> _estado.value = ImpEstado.Erro(
                c.arquivo, "Planilha grande demais",
                "São ${"%,d".format(java.util.Locale("pt", "BR"), a.linhas.size)} linhas. Importe no máximo 5.000 de cada vez: divida a planilha em partes.", false,
            )
            Campos.faltaColuna(c.mapa).isNotEmpty() -> _estado.value = c
            else -> gerarPrevia(c)
        }
    }

    // ---------------------------------------------------------------- passo 2: colunas

    fun escolherAba(i: Int) {
        val c = _estado.value as? ImpEstado.ComAba ?: return
        _estado.value = ImpEstado.Colunas(c.arquivo, c.abas, i, Campos.autoMapa(c.abas[i].cols), c.totalAbas)
    }

    /** Liga um campo a uma coluna (ou a nenhuma, com [j] nulo). A mesma coluna não serve a dois campos. */
    fun mudarColuna(campo: CampoPlanilha, j: Int?) {
        val c = _estado.value as? ImpEstado.Colunas ?: return
        val dono = j?.let { col -> c.mapa.entries.firstOrNull { it.value == col && it.key != campo }?.key }
        if (dono != null) {
            _estado.value = c.copy(erro = campo to "Essa coluna já está em “${dono.rotL}”. Escolha outra ou “Não usar”.")
            return
        }
        val novo = c.mapa.toMutableMap()
        if (j == null) novo.remove(campo) else novo[campo] = j
        _estado.value = c.copy(mapa = novo, erro = null)
    }

    fun confirmarColunas() {
        val c = _estado.value as? ImpEstado.Colunas ?: return
        val problema = Campos.validarMapa(c.mapa)
        if (problema != null) { _estado.value = c.copy(erro = problema); return }
        viewModelScope.launch { gerarPrevia(c) }
    }

    /** "Ajustar colunas", na prévia, volta ao passo 2 com o mapa que está valendo. */
    fun ajustarColunas() {
        val p = _estado.value as? ImpEstado.Previa ?: return
        _estado.value = ImpEstado.Colunas(p.arquivo, p.abas, p.aba, p.mapa, p.totalAbas)
    }

    // ---------------------------------------------------------------- passo 3: conferir e gravar

    private suspend fun gerarPrevia(c: ImpEstado.ComAba) {
        val clientes = repo.clientesAgora()
        val contas = repo.contasAgora()
        val plano = withContext(Dispatchers.Default) {
            Importacao.planejar(
                Campos.linhasDoMapa(c.abas[c.aba], c.mapa), c.arquivo, Campos.soClientes(c.mapa),
                clientes, contas, hoje, BuildConfig.COBRANCA_REAL, ::novoId,
            )
        }
        _estado.value = ImpEstado.Previa(c.arquivo, c.abas, c.aba, c.mapa, c.totalAbas, plano, contas)
    }

    /** Grava o que a pessoa marcou. Tudo ou nada, numa transação só. */
    fun aplicar(novos: Boolean, atualizar: Boolean) {
        val p = _estado.value as? ImpEstado.Previa ?: return
        viewModelScope.launch {
            val ap = Gravacao.aplicar(p.plano, novos, atualizar, repo.contasAgora(), ::novoId)
            val id = repo.gravarImportacao(ap, p.arquivo, LocalDateTime.now().toString())
            _estado.value = ImpEstado.Concluida(ap.resumo, id, p)
        }
    }

    /** Devolve quantos itens ficaram por já terem sido usados; null se esta importação não pode mais ser desfeita. */
    fun desfazer(id: Long, aoTerminar: (Int?) -> Unit) {
        viewModelScope.launch { aoTerminar(repo.desfazerImportacao(id, LocalDateTime.now().toString())) }
    }
}
