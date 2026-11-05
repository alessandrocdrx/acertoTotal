package io.github.alessandrocdrx.acertototal.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.alessandrocdrx.acertototal.EstadoAssinatura
import io.github.alessandrocdrx.acertototal.MotivoAssinatura
import io.github.alessandrocdrx.acertototal.dominio.Avaliacao
import io.github.alessandrocdrx.acertototal.dominio.Faixa
import io.github.alessandrocdrx.acertototal.dominio.MSG_SOMENTE_CONSULTA
import io.github.alessandrocdrx.acertototal.dominio.Plano
import io.github.alessandrocdrx.acertototal.dominio.Planos
import io.github.alessandrocdrx.acertototal.dominio.Teste
import io.github.alessandrocdrx.acertototal.dominio.brl

/** Faixa do topo: ambiente de teste, dias do teste grátis ou "só para consulta". Diz por escrito, nunca só por cor. */
@Composable
fun FaixaDoTopo(faixa: Faixa, aoAcao: () -> Unit) {
    if (faixa.textos.isEmpty()) return
    Surface(
        color = if (faixa.encerrado) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                faixa.textos.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
            if (faixa.acao != null) {
                OutlinedButton(onClick = aoAcao, modifier = Modifier.padding(start = 8.dp).heightIn(min = 48.dp)) { Text(faixa.acao) }
            }
        }
    }
}

private fun precoDoPlano(p: Plano, e: EstadoAssinatura): String = e.precos[p.id]?.takeIf { e.real } ?: brl(p.precoCentavos)

private fun avisoSemPlanos(e: EstadoAssinatura): String = when (e.motivo) {
    MotivoAssinatura.SEM_PLANOS -> "Os planos ainda não estão à venda na Google Play. Tente de novo mais tarde."
    MotivoAssinatura.PLAY -> "Não consegui falar com a Google Play. Confira a internet e tente de novo."
    MotivoAssinatura.NENHUM -> "A Google Play ainda está carregando os planos. Tente de novo em instantes."
}

/** Janela da assinatura: escolher o plano, ou ver/gerenciar a assinatura ativa. */
@Composable
fun FolhaPlanos(
    estado: EstadoAssinatura,
    avaliacao: Avaliacao,
    agora: Long,
    aoAssinar: (plano: String, aoFalhar: (String) -> Unit) -> Unit,
    aoRestaurar: () -> Unit,
    aoCancelarSimulada: () -> Unit,
    aoFechar: () -> Unit,
) {
    val ctx = LocalContext.current
    var plano by remember { mutableStateOf(Planos.PADRAO) }
    var erro by remember { mutableStateOf<String?>(null) }
    val sim = !estado.real
    val encerrado = Teste.somenteConsulta(avaliacao, false, agora)
    val fim = avaliacao.fimTexto()

    if (estado.ativa) {
        Folha(titulo = "Sua assinatura", subtitulo = "Assinatura ativa. O app está liberado: obrigado por assinar!", aoFechar = aoFechar) {
            if (sim) Alerta("Versão de teste: esta assinatura é simulada e ninguém é cobrado.")
            Text(
                "Para trocar de plano ou cancelar, use a Google Play › Pagamentos e assinaturas › Assinaturas.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            BotaoBloco(if (sim) "Gerenciar (simulação)" else "Gerenciar na Google Play", principal = true, aoClicar = {
                if (!sim) runCatching {
                    ctx.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/account/subscriptions?package=${ctx.packageName}"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }
            })
            if (sim) BotaoBloco("Cancelar a assinatura simulada", aoClicar = aoCancelarSimulada)
            BotaoBloco("Fechar", aoClicar = aoFechar)
        }
        return
    }

    val sel = Planos.porId(plano)
    FolhaComRodape(
        titulo = "Assinar acertoTotal",
        subtitulo = if (encerrado) "Seu teste grátis terminou. Assine para voltar a cadastrar, dar baixa, importar e exportar. Seus dados continuam guardados."
        else if (avaliacao.limiteDias > 0) "Seu teste grátis vai até $fim. Você pode assinar agora ou depois: escolha o plano."
        else "Escolha o plano que preferir.",
        aoFechar = aoFechar,
        rodape = {
            erro?.let { Text(it, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold) }
            BotaoBloco("Assinar o plano ${sel.nome.lowercase()} por ${precoDoPlano(sel, estado)}", principal = true, aoClicar = {
                erro = null
                if (!sim && !estado.pronto) erro = avisoSemPlanos(estado)
                else aoAssinar(plano) { erro = it }
            })
            if (!sim) BotaoBloco("Já assinei: restaurar", aoClicar = aoRestaurar)
            BotaoBloco("Agora não", aoClicar = aoFechar)
        },
    ) {
        if (!sim && estado.motivo != MotivoAssinatura.NENHUM) {
            val continua = if (encerrado) "" else " Seu teste grátis continua valendo até $fim."
            Alerta(
                (if (estado.motivo == MotivoAssinatura.SEM_PLANOS) "Os planos ainda não estão à venda na Google Play."
                else "Não consegui falar com a Google Play agora. Confira a internet.") + continua +
                    if (estado.motivo == MotivoAssinatura.SEM_PLANOS) " Tente de novo mais tarde." else "",
            )
        }
        if (sim) Alerta("Versão de teste: a assinatura é simulada e ninguém é cobrado.")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Planos.todos.forEach { p -> CartaoPlano(p, selecionado = p.id == plano, preco = precoDoPlano(p, estado), aoEscolher = { plano = p.id }) }
        }
        Text(
            "A cobrança é feita pela Google Play e renova sozinha. Cancele quando quiser, em Google Play › Pagamentos e assinaturas › Assinaturas.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CartaoPlano(p: Plano, selecionado: Boolean, preco: String, aoEscolher: () -> Unit) {
    val cor = if (selecionado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp)
            .selectable(selected = selecionado, onClick = aoEscolher, role = Role.RadioButton)
            .border(BorderStroke(if (selecionado) 2.dp else 1.dp, cor), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.material3.RadioButton(selected = selecionado, onClick = null)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                p.nome + if (p.economia > 0) " · ${p.economia}% mais barato" else "",
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "${p.ciclo} · ${brl(p.porMesCentavos)} por mês",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(preco, fontWeight = FontWeight.Bold)
    }
}

/** Aparece no lugar de qualquer janela de escrita quando o teste acabou (R41). */
@Composable
fun FolhaSomenteConsulta(aoVerPlanos: () -> Unit, aoFechar: () -> Unit) {
    Folha(titulo = "App só para consulta", subtitulo = MSG_SOMENTE_CONSULTA, aoFechar = aoFechar) {
        Text("Seus dados continuam guardados. Dá para ver contas, clientes e números, mas não cadastrar, dar baixa, importar ou exportar.")
        BotaoBloco("Ver planos", principal = true, aoClicar = aoVerPlanos)
        BotaoBloco("Fechar", aoClicar = aoFechar)
    }
}
