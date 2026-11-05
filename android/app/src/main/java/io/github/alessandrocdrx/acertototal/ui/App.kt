package io.github.alessandrocdrx.acertototal.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.alessandrocdrx.acertototal.dominio.Documento
import io.github.alessandrocdrx.acertototal.dominio.backupPendente
import io.github.alessandrocdrx.acertototal.dominio.brl
import io.github.alessandrocdrx.acertototal.dominio.infoBackup
import io.github.alessandrocdrx.acertototal.planilha.Exportacao
import io.github.alessandrocdrx.acertototal.planilha.FiltrosExport
import io.github.alessandrocdrx.acertototal.planilha.PlanilhaPronta
import io.github.alessandrocdrx.acertototal.planilha.Xlsx
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** No rodapé, só destinos de trabalho (R4). Assinatura, tema, ajuda e "Apagar tudo" ficam no Menu. */
enum class Aba(val rotulo: String, val icone: ImageVector) {
    CONTAS("Contas", Icons.AutoMirrored.Filled.ReceiptLong),
    CLIENTES("Clientes", Icons.Filled.People),
    NUMEROS("Números", Icons.Filled.BarChart),
    PLANILHA("Planilha", Icons.Filled.TableChart),
}

/** A janela aberta por cima da tela, se houver. */
sealed interface Janela {
    data class Conta(val id: String) : Janela
    data class FormConta(val editarId: String?, val clientePre: String?) : Janela
    data class Cliente(val id: String) : Janela
    /** [fila]: "Completar agora" abre um cliente sem CPF/CNPJ por vez (R59). */
    data class FormCliente(val id: String?, val fila: Boolean = false) : Janela
    data object Sobre : Janela
    data object Tema : Janela
    data object Faq : Janela
    data object Apagar : Janela
    data object ExportarFiltros : Janela
    /** [backup]: a cópia de segurança não tem filtros nem tabela, só o resumo. */
    data class ExportarPrevia(val backup: Boolean) : Janela
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(
    tema: io.github.alessandrocdrx.acertototal.ui.Tema,
    aoMudarTema: (io.github.alessandrocdrx.acertototal.ui.Tema) -> Unit,
    contasVm: ContasViewModel = viewModel(),
    clientesVm: ClientesViewModel = viewModel(),
    numerosVm: NumerosViewModel = viewModel(),
) {
    var aba by rememberSaveable { mutableStateOf(Aba.CONTAS) }
    var noMenu by rememberSaveable { mutableStateOf(false) }
    var janela by remember { mutableStateOf<Janela?>(null) }
    val ui by contasVm.ui.collectAsStateWithLifecycle()
    val uiClientes by clientesVm.ui.collectAsStateWithLifecycle()
    val uiNumeros by numerosVm.ui.collectAsStateWithLifecycle()
    val aviso = remember { SnackbarHostState() }
    val escopo = rememberCoroutineScope()
    val listaContas = rememberLazyListState()
    val listaClientes = rememberLazyListState()
    val contexto = androidx.compose.ui.platform.LocalContext.current
    val area = androidx.compose.ui.platform.LocalClipboardManager.current
    val completas by contasVm.completas.collectAsStateWithLifecycle()
    var filtros by remember { mutableStateOf(FiltrosExport()) }
    var ultimoBackup by remember { mutableStateOf(Preferencias.ultimoBackup(contexto)) }
    var retorno by remember { mutableStateOf<Retorno?>(null) }
    // Cada janela começa sem a resposta da anterior.
    androidx.compose.runtime.LaunchedEffect(janela) { retorno = null }
    val infoBackup = infoBackup(ultimoBackup, ui.hoje)
    val pendente = backupPendente(ultimoBackup, ui.hoje)

    // No Menu, o Voltar do Android devolve à aba de onde a pessoa veio.
    BackHandler(enabled = noMenu) { noMenu = false }

    /** Só um aviso na tela por vez (R21); com "Desfazer", dura 6 segundos (R5). */
    fun avisar(texto: String, desfazer: (() -> Unit)? = null) {
        aviso.currentSnackbarData?.dismiss()
        escopo.launch {
            if (desfazer == null) {
                aviso.showSnackbar(texto, duration = SnackbarDuration.Short)
            } else {
                val r = withTimeoutOrNull(6_000) {
                    aviso.showSnackbar(texto, actionLabel = "Desfazer", duration = SnackbarDuration.Indefinite)
                }
                if (r == SnackbarResult.ActionPerformed) desfazer()
            }
        }
    }

    fun novaConta(clientePre: String? = null) {
        when {
            uiClientes.todos.isEmpty() -> avisar("Cadastre um cliente primeiro")
            uiClientes.todos.none { it.doc.isNotEmpty() } -> avisar("Complete o CPF ou CNPJ de um cliente primeiro")
            else -> janela = Janela.FormConta(null, clientePre)
        }
    }

    /** A planilha que a janela de prévia mostra agora, pronta para gravar. */
    fun planilhaAtual(backup: Boolean): PlanilhaPronta =
        if (backup) Exportacao.copiaDeSeguranca(uiClientes.todos, completas)
        else Exportacao.relatorioOuEditar(uiClientes.todos, completas, filtros, ui.hoje)

    fun nomeDoArquivo(p: PlanilhaPronta) = "${p.arquivo}-${ui.hoje}.xlsx"

    /** Falha ao salvar nunca é silenciosa (R28). A data da cópia só muda se o arquivo foi gravado. */
    fun baixar(backup: Boolean) {
        val p = planilhaAtual(backup)
        escopo.launch {
            val nome = nomeDoArquivo(p)
            val ok = withContext(Dispatchers.IO) {
                runCatching { Arquivos.salvarEmDownloads(contexto, nome, Xlsx.escrever(p.abas())) }.getOrDefault(false)
            }
            if (ok) {
                if (backup) {
                    Preferencias.salvarUltimoBackup(contexto, ui.hoje)
                    ultimoBackup = ui.hoje
                }
                retorno = Retorno("Arquivo Excel salvo em Downloads: $nome")
            } else {
                retorno = Retorno("Não foi possível salvar o arquivo. Confira se há espaço no aparelho.", erro = true)
            }
        }
    }

    fun compartilhar(backup: Boolean) {
        val p = planilhaAtual(backup)
        escopo.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching { Arquivos.compartilhar(contexto, nomeDoArquivo(p), Xlsx.escrever(p.abas())) }.getOrDefault(false)
            }
            if (!ok) retorno = Retorno("Não encontrei um app para enviar o arquivo. Use “Baixar Excel” e abra em Downloads.", erro = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (noMenu) "Menu" else aba.rotulo) }, // o título é o rótulo da aba tocada (R72)
                navigationIcon = {
                    if (noMenu) {
                        IconButton(onClick = { noMenu = false }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                        }
                    }
                },
                actions = {
                    if (!noMenu) {
                        IconButton(onClick = { noMenu = true }) {
                            // Cópia de segurança pendente: ponto vermelho no ☰ (R78), dito também por escrito.
                            BadgedBox(badge = { if (pendente) Badge() }) {
                                Icon(Icons.Filled.Menu, contentDescription = if (pendente) "Menu, cópia de segurança pendente" else "Menu")
                            }
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (!noMenu) {
                NavigationBar {
                    Aba.entries.forEach { a ->
                        NavigationBarItem(
                            selected = aba == a,
                            onClick = { aba = a },
                            icon = { Icon(a.icone, contentDescription = null) },
                            label = { Text(a.rotulo) },
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (!noMenu) when (aba) {
                Aba.CONTAS -> BotaoNovo("Nova conta", listaContas) { novaConta() }
                Aba.CLIENTES -> BotaoNovo("Novo cliente", listaClientes) { janela = Janela.FormCliente(null) }
                else -> Unit
            }
        },
        snackbarHost = { SnackbarHost(aviso) },
    ) { margens ->
        val corpo = Modifier.fillMaxSize().padding(margens)
        when {
            noMenu -> TelaMenu(
                versao = io.github.alessandrocdrx.acertototal.BuildConfig.VERSION_NAME,
                mostrarApagar = !io.github.alessandrocdrx.acertototal.BuildConfig.COBRANCA_REAL,
                backup = infoBackup,
                aoBackup = { janela = Janela.ExportarPrevia(backup = true) },
                aoSobre = { janela = Janela.Sobre },
                aoTema = { janela = Janela.Tema },
                aoFaq = { janela = Janela.Faq },
                aoApagar = { janela = Janela.Apagar },
                modifier = corpo,
            )
            aba == Aba.CONTAS -> TelaContas(
                ui = ui,
                aoFiltrar = contasVm::alternarFiltro,
                aoBuscar = contasVm::buscar,
                aoVerTudo = contasVm::verTudo,
                aoDarBaixa = { c -> janela = Janela.Conta(c.id) },
                aoAbrirConta = { c -> janela = Janela.Conta(c.id) },
                modifier = corpo,
                estado = listaContas,
            )
            aba == Aba.CLIENTES -> TelaClientes(
                ui = uiClientes,
                aoBuscar = clientesVm::buscar,
                aoAbrir = { c -> janela = Janela.Cliente(c.id) },
                aoCompletarDocs = {
                    uiClientes.todos.firstOrNull { it.doc.isEmpty() }?.let { janela = Janela.FormCliente(it.id, fila = true) }
                },
                modifier = corpo,
                estado = listaClientes,
            )
            aba == Aba.PLANILHA -> TelaPlanilha(
                backup = infoBackup,
                // Cada tarefa abre com os filtros limpos: nada herdado de outra tarefa (R56).
                aoExportar = { filtros = FiltrosExport(); janela = Janela.ExportarFiltros },
                aoBackup = { janela = Janela.ExportarPrevia(backup = true) },
                modifier = corpo,
            )
            aba == Aba.NUMEROS -> TelaNumeros(
                ui = uiNumeros,
                aoEscolherPeriodo = numerosVm::escolher,
                aoAbrirCliente = { id -> janela = Janela.Cliente(id) },
                modifier = corpo,
            )
            else -> EmConstrucao(aba.rotulo, corpo)
        }

        when (val j = janela) {
            null -> Unit
            Janela.ExportarFiltros -> FolhaFiltrosExport(
                filtros = filtros, clientes = uiClientes.todos, contas = completas, hoje = ui.hoje,
                aoMudar = { filtros = it },
                aoVerPrevia = { janela = Janela.ExportarPrevia(backup = false) },
                aoFechar = { janela = null },
            )
            is Janela.ExportarPrevia -> {
                val p = planilhaAtual(j.backup)
                FolhaPreviaExport(
                    planilha = p,
                    backup = if (j.backup) ResumoBackup(
                        clientes = uiClientes.todos.size, contas = completas.size,
                        pagas = completas.count { it.pagoEm != null }, comprovantes = completas.count { it.compArquivo != null },
                    ) else null,
                    retorno = retorno,
                    aoBaixar = { baixar(j.backup) },
                    aoCompartilhar = { compartilhar(j.backup) },
                    aoCopiar = {
                        area.setText(androidx.compose.ui.text.AnnotatedString(p.tsv()))
                        retorno = Retorno("Copiado. Cole numa planilha do Excel.")
                    },
                    aoVoltar = if (j.backup) null else ({ janela = Janela.ExportarFiltros }),
                    aoFechar = { janela = null },
                )
            }
            Janela.Sobre -> FolhaSobre(io.github.alessandrocdrx.acertototal.BuildConfig.VERSION_NAME) { janela = null }
            Janela.Tema -> FolhaTema(tema, aoEscolher = aoMudarTema, aoFechar = { janela = null })
            Janela.Faq -> FolhaFaq { janela = null }
            Janela.Apagar -> FolhaApagar(
                clientes = uiClientes.todos.size,
                contas = ui.todas.size,
                comprovantes = ui.todas.count { it.temComprovante },
                aoApagar = {
                    contasVm.apagarTudo {
                        Preferencias.limparUltimoBackup(contexto)
                        ultimoBackup = null
                        janela = null
                        noMenu = false
                        aba = Aba.CONTAS
                        avisar("Tudo apagado. O app está vazio.")
                    }
                },
                aoFechar = { janela = null },
            )
            is Janela.Conta -> ui.todas.firstOrNull { it.id == j.id }?.let { c ->
                FolhaConta(
                    conta = c, hoje = ui.hoje,
                    aoFechar = { janela = null },
                    aoBaixar = { data ->
                        contasVm.darBaixa(c.id, data) { antes ->
                            avisar("Baixa registrada: ${brl(c.valor)}") {
                                contasVm.restaurarConta(antes)
                                avisar("Baixa desfeita")
                            }
                        }
                        janela = null
                    },
                    aoDesfazerBaixa = {
                        contasVm.desfazerBaixa(c.id) { antes -> avisar("Baixa desfeita") { contasVm.restaurarConta(antes) } }
                        janela = null
                    },
                    aoEditar = { janela = Janela.FormConta(c.id, null) },
                    aoExcluir = {
                        contasVm.excluirConta(c.id) { removida -> avisar("Conta excluída") { contasVm.restaurarConta(removida) } }
                        janela = null
                    },
                )
            }
            is Janela.FormConta -> {
                val inicial = j.editarId?.let { id -> ui.todas.firstOrNull { it.id == id } }
                FolhaFormConta(
                    inicial = inicial, clientes = uiClientes.todos, clientePre = j.clientePre, hoje = ui.hoje,
                    aoFechar = { janela = inicial?.let { Janela.Conta(it.id) } },
                    aoSalvar = { clienteId, descricao, valor, venc ->
                        if (inicial == null) {
                            contasVm.salvarNovaConta(clienteId, descricao, valor, venc)
                            avisar("Conta salva")
                        } else {
                            contasVm.editarConta(inicial.id, clienteId, descricao, valor, venc) { antes ->
                                avisar("Conta atualizada") {
                                    contasVm.restaurarConta(antes)
                                    avisar("Alteração desfeita")
                                }
                            }
                        }
                        janela = null
                    },
                )
            }
            is Janela.Cliente -> uiClientes.todos.firstOrNull { it.id == j.id }?.let { cl ->
                FolhaCliente(
                    cliente = cl,
                    contas = ui.todas.filter { it.clienteId == cl.id },
                    hoje = ui.hoje,
                    aoFechar = { janela = null },
                    aoNovaConta = { novaConta(cl.id) },
                    aoEditar = { janela = Janela.FormCliente(cl.id) },
                    aoExcluir = {
                        clientesVm.excluir(cl.id) { removido ->
                            if (removido == null) avisar("Este cliente tem contas e não pode ser excluído.")
                            else {
                                avisar("Cliente excluído") { clientesVm.restaurar(removido) }
                                janela = null
                            }
                        }
                    },
                    aoAbrirConta = { c -> janela = Janela.Conta(c.id) },
                )
            }
            is Janela.FormCliente -> {
                val inicial = j.id?.let { id -> uiClientes.todos.firstOrNull { it.id == id } }
                FolhaFormCliente(
                    inicial = inicial,
                    aoFechar = { janela = inicial?.let { if (j.fila) null else Janela.Cliente(it.id) } },
                    aoSalvar = { nome, doc, tel, email, obs, aoErro ->
                        clientesVm.salvar(j.id, nome, doc, tel, email, obs) { r ->
                            when (r) {
                                is ClientesViewModel.Resultado.Erro -> aoErro(r.erro)
                                is ClientesViewModel.Resultado.Salvo -> {
                                    val base = if (r.novo) "Cliente cadastrado" else "Dados atualizados"
                                    val atencao = if (r.documentoValido) "" else
                                        ". Atenção: o ${Documento.tipo(r.cliente.doc)} não é válido (aceito só na versão de teste)."
                                    // Na fila de "Completar agora", segue para o próximo cliente sem CPF/CNPJ.
                                    val proximo = if (j.fila) uiClientes.todos.firstOrNull { it.doc.isEmpty() && it.id != r.cliente.id } else null
                                    janela = proximo?.let { Janela.FormCliente(it.id, fila = true) }
                                    if (proximo == null) avisar(base + atencao)
                                }
                            }
                        }
                    },
                )
            }
        }
    }
}

/** Telas que ainda não foram portadas do protótipo. Some conforme cada uma fica pronta. */
@Composable
private fun EmConstrucao(nome: String, modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Text(nome, style = MaterialTheme.typography.titleMedium)
            Text(
                "Esta tela ainda será refeita em Compose.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
