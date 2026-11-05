package io.github.alessandrocdrx.acertototal.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** No rodapé, só destinos de trabalho (R4). Assinatura, tema, ajuda e "Apagar tudo" ficam no Menu. */
enum class Aba(val rotulo: String, val icone: ImageVector) {
    CONTAS("Contas", Icons.Filled.ReceiptLong),
    CLIENTES("Clientes", Icons.Filled.People),
    NUMEROS("Números", Icons.Filled.BarChart),
    PLANILHA("Planilha", Icons.Filled.TableChart),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(contasVm: ContasViewModel = viewModel()) {
    var aba by rememberSaveable { mutableStateOf(Aba.CONTAS) }
    var noMenu by rememberSaveable { mutableStateOf(false) }
    val ui by contasVm.ui.collectAsStateWithLifecycle()
    val aviso = remember { SnackbarHostState() }
    val escopo = rememberCoroutineScope()
    val listaContas = rememberLazyListState()

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
                            Icon(Icons.Filled.Menu, contentDescription = "Menu")
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
        snackbarHost = { SnackbarHost(aviso) },
    ) { margens ->
        val corpo = Modifier.fillMaxSize().padding(margens)
        when {
            noMenu -> EmConstrucao("Menu", corpo)
            aba == Aba.CONTAS -> TelaContas(
                ui = ui,
                aoFiltrar = contasVm::alternarFiltro,
                aoBuscar = contasVm::buscar,
                aoVerTudo = contasVm::verTudo,
                aoDarBaixa = { c ->
                    contasVm.darBaixa(c.id)
                    avisar("Baixa registrada para ${c.clienteNome}.") { contasVm.desfazerBaixa(c.id) }
                },
                aoAbrirConta = { },
                modifier = corpo,
                estado = listaContas,
            )
            else -> EmConstrucao(aba.rotulo, corpo)
        }
    }
}

/** Telas que ainda não foram portadas do protótipo. Some conforme cada uma fica pronta. */
@Composable
private fun EmConstrucao(nome: String, modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Text("$nome", style = MaterialTheme.typography.titleMedium)
            Text(
                "Esta tela ainda será refeita em Compose.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
