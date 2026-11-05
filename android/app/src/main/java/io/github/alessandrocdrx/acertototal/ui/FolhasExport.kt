package io.github.alessandrocdrx.acertototal.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.alessandrocdrx.acertototal.dados.Cliente
import io.github.alessandrocdrx.acertototal.dados.Conta
import io.github.alessandrocdrx.acertototal.dominio.brl
import io.github.alessandrocdrx.acertototal.dominio.plural
import io.github.alessandrocdrx.acertototal.planilha.Exportacao
import io.github.alessandrocdrx.acertototal.planilha.FiltrosExport
import io.github.alessandrocdrx.acertototal.planilha.FormatoExport
import io.github.alessandrocdrx.acertototal.planilha.PlanilhaPronta
import io.github.alessandrocdrx.acertototal.planilha.StatusExport
import java.time.LocalDate

/** Escolha o que exportar. A contagem muda ao vivo e o botão diz quantas linhas vão sair (R34). */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FolhaFiltrosExport(
    filtros: FiltrosExport,
    clientes: List<Cliente>,
    contas: List<Conta>,
    hoje: LocalDate,
    aoMudar: (FiltrosExport) -> Unit,
    aoVerPrevia: () -> Unit,
    aoFechar: () -> Unit,
) {
    val editar = filtros.formato == FormatoExport.EDITAR
    val lista = Exportacao.contasFiltradas(contas, clientes, filtros, hoje)
    val total = lista.sumOf { it.valor }
    Folha(
        titulo = if (editar) "Planilha para editar" else "Exportar relatório",
        subtitulo = if (editar) "Para mudar contas no Excel e enviar de volta. Tem o código de cada conta e o CPF/CNPJ do cliente."
        else "Para ler, imprimir ou mandar a alguém. Tem a situação e os dias de atraso.",
        aoFechar = aoFechar,
    ) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().semantics { contentDescription = "Tipo de planilha" }) {
            listOf(FormatoExport.RELATORIO to "Relatório", FormatoExport.EDITAR to "Para editar").forEachIndexed { i, (formato, rotulo) ->
                SegmentedButton(
                    selected = filtros.formato == formato,
                    onClick = { aoMudar(filtros.copy(formato = formato)) },
                    shape = SegmentedButtonDefaults.itemShape(i, 2),
                    modifier = Modifier.heightIn(min = 48.dp),
                ) { Text(rotulo) }
            }
        }
        Text("Situação", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusExport.entries.forEach { s ->
                FilterChip(
                    selected = filtros.status == s, onClick = { aoMudar(filtros.copy(status = s)) },
                    label = { Text(s.rotulo) }, modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }
        FiltroCliente(clientes, filtros.clienteId) { aoMudar(filtros.copy(clienteId = it)) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CampoData(
                "Vencimento de", filtros.de, { aoMudar(filtros.copy(de = it)) }, Modifier.weight(1f),
                aoLimpar = { aoMudar(filtros.copy(de = null)) },
            )
            CampoData(
                "Até", filtros.ate, { aoMudar(filtros.copy(ate = it)) }, Modifier.weight(1f),
                erro = if (filtros.periodoInvertido) "A data final é antes da inicial" else null,
                aoLimpar = { aoMudar(filtros.copy(ate = null)) },
            )
        }
        Text("Deixe as datas em branco para incluir todas.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            when {
                filtros.periodoInvertido -> "Corrija o período para exportar."
                lista.isEmpty() -> "Nenhuma conta com esses filtros."
                else -> "${plural(lista.size, "conta")} · ${brl(total)}"
            },
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        val pode = lista.isNotEmpty() && !filtros.periodoInvertido
        BotaoBloco(if (pode) "Ver prévia (${lista.size})" else "Ver prévia", principal = true, habilitado = pode, aoClicar = aoVerPrevia)
        if (!filtros.padrao) BotaoBloco("Limpar filtros", aoClicar = { aoMudar(FiltrosExport(formato = filtros.formato)) })
        BotaoBloco("Cancelar", aoClicar = aoFechar)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FiltroCliente(clientes: List<Cliente>, selecionado: String?, aoEscolher: (String?) -> Unit) {
    var aberto by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        Text("Cliente", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        ExposedDropdownMenuBox(expanded = aberto, onExpandedChange = { aberto = it }) {
            OutlinedTextField(
                value = clientes.firstOrNull { it.id == selecionado }?.nome ?: "Todos os clientes",
                onValueChange = {}, readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = aberto) },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp).menuAnchor(MenuAnchorType.PrimaryNotEditable),
                colors = camposAcerto(),
            )
            ExposedDropdownMenu(expanded = aberto, onDismissRequest = { aberto = false }) {
                DropdownMenuItem(text = { Text("Todos os clientes") }, onClick = { aoEscolher(null); aberto = false })
                clientes.sortedBy { it.nome.lowercase() }.forEach { c ->
                    DropdownMenuItem(text = { Text(c.nome) }, onClick = { aoEscolher(c.id); aberto = false })
                }
            }
        }
    }
}

/**
 * Resposta de uma ação feita dentro da janela (salvar, copiar). O aviso flutuante do app fica escondido
 * atrás da janela aberta, então o resultado aparece aqui, ao lado dos botões (R28).
 */
data class Retorno(val texto: String, val erro: Boolean = false)

/** O que vai dentro da cópia de segurança: só um resumo, ninguém confere uma tabela de códigos. */
data class ResumoBackup(val clientes: Int, val contas: Int, val pagas: Int, val comprovantes: Int)

/**
 * Prévia da planilha. Os botões vêm antes da tabela, para a ação principal aparecer sem rolar (R70).
 */
@Composable
fun FolhaPreviaExport(
    planilha: PlanilhaPronta,
    backup: ResumoBackup?,
    retorno: Retorno?,
    aoBaixar: () -> Unit,
    aoCompartilhar: () -> Unit,
    aoCopiar: () -> Unit,
    aoVoltar: (() -> Unit)?,
    aoFechar: () -> Unit,
) {
    val n = planilha.linhas.size
    val paraEditar = backup == null && planilha.extras.isNotEmpty()
    Folha(
        titulo = planilha.titulo,
        subtitulo = (if (planilha.filtros.isNotEmpty()) "Filtros: ${planilha.filtros}. " else "") +
            "${plural(n, "linha")}. Baixe como Excel (.xlsx) ou copie e cole numa planilha.",
        aoFechar = aoFechar,
    ) {
        if (paraEditar) {
            Aviso(
                "Depois de baixar: mude no Excel o valor, o vencimento, a descrição ou o “Pago em”. " +
                    "Não mexa na coluna “Código da conta”.",
            )
        }
        if (backup != null) {
            Aviso(
                "Fotos e PDFs de comprovante não vão na planilha. Para guardar um, abra a conta paga e use " +
                    "Compartilhar ou Salvar na pasta Downloads.",
            )
        }
        if (n > 0) {
            BotaoBloco("Baixar Excel (.xlsx)", principal = true, aoClicar = aoBaixar)
            BotaoBloco("Compartilhar", aoClicar = aoCompartilhar)
            BotaoBloco("Copiar para colar no Excel", aoClicar = aoCopiar)
        }
        if (retorno != null) {
            val cores = MaterialTheme.acerto
            Surface(
                color = if (retorno.erro) cores.ruimFundo else cores.okFundo,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
            ) {
                Text(
                    retorno.texto, modifier = Modifier.padding(12.dp), fontWeight = FontWeight.SemiBold,
                    color = if (retorno.erro) cores.ruim else cores.ok,
                )
            }
        }
        if (backup != null) {
            Text("O que vai na cópia", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Column {
                Text("• ${plural(backup.clientes, "cliente")}")
                Text("• ${plural(backup.contas, "conta")} (${plural(backup.pagas, "paga")})")
                Text("• Telefone, e-mail, vencimentos e datas de pagamento")
            }
            Text(
                "Cada conta leva um código na planilha. Ele é o que permite restaurar uma cópia sem duplicar nada. " +
                    "Não mexa na coluna “Código da conta”." +
                    if (backup.comprovantes > 0) " Os ${plural(backup.comprovantes, "comprovante")} não vão na planilha." else "",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text("Prévia", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (n == 0) Text("Nenhuma conta com esses filtros.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            else Tabela(planilha.cabecalho, planilha.linhas)
        }
        if (aoVoltar != null) BotaoBloco("Voltar aos filtros", aoClicar = aoVoltar)
        BotaoBloco("Fechar", aoClicar = aoFechar)
    }
}

@Composable
private fun Aviso(texto: String) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Text(texto, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

/** Prévia em tabela que rola para o lado. Mostra só as primeiras linhas: o arquivo leva todas. */
@Composable
private fun Tabela(cabecalho: List<String>, linhas: List<List<String>>) {
    val limite = 30
    val larguras = cabecalho.indices.map { c -> if (c == 0 && cabecalho[0].startsWith("Código")) 110.dp else 150.dp }
    Surface(
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Prévia da planilha, role para o lado" },
    ) {
        Column(Modifier.horizontalScroll(rememberScrollState())) {
            Row(Modifier.padding(vertical = 8.dp)) {
                cabecalho.forEachIndexed { i, h ->
                    Text(h, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(larguras[i]).padding(horizontal = 8.dp))
                }
            }
            HorizontalDivider()
            linhas.take(limite).forEach { l ->
                Row(Modifier.padding(vertical = 6.dp)) {
                    cabecalho.indices.forEach { i ->
                        Text(l.getOrNull(i).orEmpty(), style = MaterialTheme.typography.bodySmall, maxLines = 2, modifier = Modifier.width(larguras[i]).padding(horizontal = 8.dp))
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            if (linhas.size > limite) {
                Text(
                    "Mostrando as primeiras $limite de ${linhas.size}. O arquivo leva todas.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(8.dp),
                )
            }
        }
    }
}
