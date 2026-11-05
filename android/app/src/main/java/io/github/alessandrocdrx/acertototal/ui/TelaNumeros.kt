package io.github.alessandrocdrx.acertototal.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.alessandrocdrx.acertototal.dominio.Estatisticas
import io.github.alessandrocdrx.acertototal.dominio.brl
import io.github.alessandrocdrx.acertototal.dominio.brlInteiro
import io.github.alessandrocdrx.acertototal.dominio.plural
import kotlin.math.abs

private val PERIODOS = mapOf(1 to "Este mês", 3 to "Últimos 3 meses", 12 to "Últimos 12 meses")
private val CURTO = mapOf(1 to "Este mês", 3 to "3 meses", 12 to "12 meses")

@Composable
fun TelaNumeros(
    ui: NumerosUi,
    aoEscolherPeriodo: (Int) -> Unit,
    aoAbrirCliente: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.fillMaxWidth().semantics { contentDescription = "Período" },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(1, 3, 12).forEach { p ->
                FilterChip(
                    selected = ui.periodo == p,
                    onClick = { aoEscolherPeriodo(p) },
                    label = { Text(CURTO.getValue(p)) },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).semantics { contentDescription = PERIODOS.getValue(p) },
                )
            }
        }
        val est = ui.est
        if (!ui.temContas || est == null) {
            Text(
                "Cadastre contas para ver seus números aqui.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 24.dp),
            )
            return@Column
        }
        val cores = MaterialTheme.acerto

        Cartao {
            Text("Recebido · ${PERIODOS.getValue(ui.periodo).lowercase()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(brl(est.recebido), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = cores.ok)
        }

        Titulo("Taxa de recebimento")
        Cartao { TaxaDeRecebimento(est, ui.periodo) }

        Titulo("Recebido por mês")
        val maxMes = maxOf(est.meses.maxOf { it.total }, 1L)
        est.meses.forEachIndexed { i, m ->
            val hoje = i == est.meses.lastIndex
            LinhaBarra(
                rotulo = m.rotulo + if (hoje) " (hoje)" else "",
                fracao = m.total.toFloat() / maxMes,
                valor = brlInteiro(m.total),
                cor = cores.ok,
                larguraRotulo = 78.dp,
            )
        }

        Titulo("Atrasos por tempo")
        if (est.atrasadas == 0) {
            Text("Nenhuma conta atrasada.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            val maxFaixa = maxOf(est.faixas.maxOf { it.total }, 1L)
            est.faixas.forEach { f ->
                LinhaBarra(f.rotulo, f.total.toFloat() / maxFaixa, "${brlInteiro(f.total)} · ${f.quantas}", cores.ruim, 112.dp, larguraValor = 120.dp)
            }
        }

        Titulo("Quem mais deve")
        if (est.devedores.isEmpty()) {
            Text("Ninguém em atraso.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            est.devedores.forEach { d ->
                Card(
                    onClick = { aoAbrirCliente(d.clienteId) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(d.nome, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${plural(d.quantas, "conta")} · a mais antiga há ${plural(d.maisAntiga, "dia")}",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(brl(d.total), fontWeight = FontWeight.Bold, color = cores.ruim)
                    }
                }
            }
        }

        Titulo("Hábitos de pagamento")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val dias = est.prazoMedioDias
            val prazo = when {
                dias == null -> "—"
                dias == 0 -> "No dia"
                else -> plural(abs(dias), "dia")
            }
            val legenda = when {
                dias == null -> "sem pagamentos no período"
                dias == 0 -> "do vencimento"
                dias > 0 -> "depois do vencimento"
                else -> "antes do vencimento"
            }
            Bloco("Prazo médio", prazo, legenda, Modifier.weight(1f))
            Bloco("Vencem em 7 dias", brl(est.proximasTotal), plural(est.proximasQuantas, "conta"), Modifier.weight(1f))
        }
    }
}

@Composable
private fun Titulo(texto: String) {
    Text(texto, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp))
}

@Composable
private fun Cartao(conteudo: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { conteudo() } }
}

@Composable
private fun Bloco(rotulo: String, valor: String, legenda: String, modifier: Modifier) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier,
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(rotulo, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(valor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(legenda, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TaxaDeRecebimento(est: Estatisticas, periodo: Int) {
    val cores = MaterialTheme.acerto
    val pct = est.percentualRecebido
    when {
        periodo == 1 && est.temVencidas && est.vencidoPago == 0L -> Text(
            "Nenhuma conta vencida neste mês foi paga ainda. Veja quem deve em “Quem mais deve”, mais abaixo.",
        )
        pct != null -> {
            val nota = if (pct >= 90) " Muito bom." else if (pct < 70) " Vale cobrar os atrasados." else ""
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("$pct%", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Das contas que venceram no período (${brl(est.vencido)}), você já recebeu ${brl(est.vencidoPago)}.$nota", Modifier.weight(1f))
            }
            // Medidor: verde = recebido, vermelho = ainda não pago. O texto acima diz o mesmo (R3).
            Row(
                Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(50)).semantics { contentDescription = "$pct% recebido" },
            ) {
                if (pct > 0) Box(Modifier.weight(pct.toFloat()).fillMaxHeight().background(cores.ok))
                if (pct < 100) Box(Modifier.weight((100 - pct).toFloat()).fillMaxHeight().background(cores.ruim))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Legenda(cores.ok, "Recebido ${brl(est.vencidoPago)}")
                Legenda(cores.ruim, "Ainda não pago ${brl(est.vencido - est.vencidoPago)}")
            }
        }
        else -> Text("Ainda não há contas vencidas neste período.")
    }
}

@Composable
private fun Legenda(cor: Color, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(cor))
        Text(texto, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 6.dp))
    }
}

@Composable
private fun LinhaBarra(rotulo: String, fracao: Float, valor: String, cor: Color, larguraRotulo: Dp, larguraValor: Dp = 76.dp) {
    Row(Modifier.fillMaxWidth().heightIn(min = 28.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(rotulo, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(larguraRotulo))
        Box(Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surfaceVariant)) {
            Box(Modifier.fillMaxWidth(fracao.coerceIn(0f, 1f)).fillMaxHeight().background(cor))
        }
        // Largura fixa e alinhado à direita: todas as barras terminam no mesmo ponto.
        Text(
            valor, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            modifier = Modifier.width(larguraValor).padding(start = 8.dp),
        )
    }
}
