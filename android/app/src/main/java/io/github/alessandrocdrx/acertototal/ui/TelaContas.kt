package io.github.alessandrocdrx.acertototal.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.alessandrocdrx.acertototal.dados.ContaComCliente
import io.github.alessandrocdrx.acertototal.dominio.MESES_LONGO
import io.github.alessandrocdrx.acertototal.dominio.Situacao
import io.github.alessandrocdrx.acertototal.dominio.brl
import io.github.alessandrocdrx.acertototal.dominio.brlInteiro
import io.github.alessandrocdrx.acertototal.dominio.dataCurta
import io.github.alessandrocdrx.acertototal.dominio.infoPrazo
import io.github.alessandrocdrx.acertototal.dominio.plural
import io.github.alessandrocdrx.acertototal.dominio.situacaoDe
import java.time.LocalDate

@Composable
fun TelaContas(
    ui: ContasUi,
    aoFiltrar: (FiltroContas) -> Unit,
    aoBuscar: (String) -> Unit,
    aoVerTudo: () -> Unit,
    aoDarBaixa: (ContaComCliente) -> Unit,
    aoAbrirConta: (ContaComCliente) -> Unit,
    modifier: Modifier = Modifier,
    estado: androidx.compose.foundation.lazy.LazyListState = rememberLazyListState(),
) {
    val busca = ui.busca.trim()
    LazyColumn(
        modifier = modifier,
        state = estado,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item("resumo") { ResumoContas(ui, aoFiltrar) }
        item("busca") {
            OutlinedTextField(
                value = ui.busca,
                onValueChange = aoBuscar,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Buscar cliente ou descrição") },
                singleLine = true,
                colors = camposAcerto(),
            )
        }
        item("barra") {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    (if (busca.isNotEmpty()) "Resultado da busca" else ui.filtro.titulo) + " · ${ui.lista.size}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                if (busca.isEmpty() && ui.filtro != FiltroContas.ABERTAS) {
                    TextButton(onClick = aoVerTudo, modifier = Modifier.heightIn(min = 48.dp)) { Text("Ver tudo") }
                }
            }
        }
        if (ui.lista.isEmpty() && ui.carregado) {
            item("vazio") { ContasVazias(ui, aoVerTudo) { aoBuscar("") } }
        }
        val agruparPorMes = ui.filtro == FiltroContas.PAGAS && busca.isEmpty()
        var mesAnterior: Pair<Int, Int>? = null
        ui.lista.forEach { c ->
            if (agruparPorMes && c.pagoEm != null) {
                val mes = c.pagoEm.year to c.pagoEm.monthValue
                if (mes != mesAnterior) {
                    mesAnterior = mes
                    item("mes-${mes.first}-${mes.second}") {
                        Text(
                            MESES_LONGO[mes.second - 1] + if (mes.first == ui.hoje.year) "" else " de ${mes.first}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }
            item(c.id) { LinhaConta(c, ui.hoje, aoAbrirConta, aoDarBaixa) }
        }
    }
}

@Composable
fun camposAcerto() = OutlinedTextFieldDefaults.colors(
    unfocusedBorderColor = MaterialTheme.acerto.campoBorda,
    unfocusedContainerColor = MaterialTheme.acerto.campoFundo,
    focusedContainerColor = MaterialTheme.acerto.campoFundo,
)

@Composable
private fun ResumoContas(ui: ContasUi, aoFiltrar: (FiltroContas) -> Unit) {
    Row(
        Modifier.fillMaxWidth().semantics { contentDescription = "Filtrar contas (toque num cartão)" },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val c = MaterialTheme.acerto
        CartaoFiltro("A vencer", ui.aVencer, MaterialTheme.colorScheme.onSurface, ui.filtro == FiltroContas.AVENCER, Modifier.weight(1f)) { aoFiltrar(FiltroContas.AVENCER) }
        CartaoFiltro("Atrasadas", ui.atrasadas, c.ruim, ui.filtro == FiltroContas.ATRASADAS, Modifier.weight(1f)) { aoFiltrar(FiltroContas.ATRASADAS) }
        CartaoFiltro("Pagas no mês", ui.pagasNoMes, c.ok, ui.filtro == FiltroContas.PAGAS, Modifier.weight(1f)) { aoFiltrar(FiltroContas.PAGAS) }
    }
}

/** Cartão que filtra mostra ícone de filtro, e "x" quando está marcado (R82). */
@Composable
private fun CartaoFiltro(
    rotulo: String,
    total: Total,
    cor: Color,
    marcado: Boolean,
    modifier: Modifier,
    aoTocar: () -> Unit,
) {
    Card(
        onClick = aoTocar,
        modifier = modifier.heightIn(min = 48.dp).semantics { selected = marcado },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (marcado) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(if (marcado) 2.dp else 1.dp, if (marcado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(rotulo, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(
                    if (marcado) Icons.Filled.Close else Icons.Filled.FilterList,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(brlInteiro(total.centavos), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = cor)
            Text(plural(total.quantas, "conta"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LinhaConta(
    c: ContaComCliente,
    hoje: LocalDate,
    aoAbrir: (ContaComCliente) -> Unit,
    aoDarBaixa: (ContaComCliente) -> Unit,
) {
    val sit = situacaoDe(c.venc, c.pagoEm, hoje)
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).clickable { aoAbrir(c) }.heightIn(min = 48.dp)) {
                Text(c.clienteNome, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "${c.descricao} · venc. ${dataCurta(c.venc, hoje)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.size(6.dp))
                SeloSituacao(sit, (if (sit == Situacao.PAGA) "✓ " else "") + infoPrazo(c.venc, c.pagoEm, hoje))
            }
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 8.dp)) {
                Text(brl(c.valor), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                if (sit != Situacao.PAGA) {
                    Spacer(Modifier.size(6.dp))
                    Button(
                        onClick = { aoDarBaixa(c) },
                        modifier = Modifier.heightIn(min = 48.dp).widthIn(min = 48.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp),
                    ) { Text("Dar baixa") }
                }
            }
        }
    }
}

@Composable
fun SeloSituacao(sit: Situacao, texto: String) {
    val c = MaterialTheme.acerto
    val (fg, bg) = when (sit) {
        Situacao.PAGA -> c.ok to c.okFundo
        Situacao.ATRASADA -> c.ruim to c.ruimFundo
        Situacao.PENDENTE -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.primaryContainer
    }
    Surface(color = bg, shape = RoundedCornerShape(50)) {
        Text(
            texto,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = fg,
        )
    }
}

/** Tela vazia sempre oferece uma saída (R6). */
@Composable
private fun ContasVazias(ui: ContasUi, aoVerTudo: () -> Unit, aoLimparBusca: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        val busca = ui.busca.trim()
        when {
            busca.isNotEmpty() -> {
                Text("Nenhuma conta encontrada para “$busca”.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = aoLimparBusca, modifier = Modifier.heightIn(min = 48.dp)) { Text("Limpar busca") }
            }
            !ui.temContas -> Text("Nenhuma conta cadastrada ainda. Toque em “Nova conta”.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            else -> {
                val que = when (ui.filtro) {
                    FiltroContas.ABERTAS -> "para receber"
                    FiltroContas.AVENCER -> "a vencer"
                    FiltroContas.ATRASADAS -> "atrasada"
                    FiltroContas.PAGAS -> "paga"
                }
                Text("Nenhuma conta $que.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (ui.filtro != FiltroContas.ABERTAS) {
                    OutlinedButton(onClick = aoVerTudo, modifier = Modifier.heightIn(min = 48.dp)) { Text("Ver o que falta receber") }
                }
            }
        }
    }
}
