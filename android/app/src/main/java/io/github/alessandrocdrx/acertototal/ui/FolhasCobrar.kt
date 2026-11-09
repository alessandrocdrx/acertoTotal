package io.github.alessandrocdrx.acertototal.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.alessandrocdrx.acertototal.dados.Cliente
import io.github.alessandrocdrx.acertototal.dados.ContaComCliente
import io.github.alessandrocdrx.acertototal.dominio.Cobranca
import io.github.alessandrocdrx.acertototal.dominio.ContaParaCobrar
import io.github.alessandrocdrx.acertototal.dominio.ChavePix
import io.github.alessandrocdrx.acertototal.dominio.Negocio
import io.github.alessandrocdrx.acertototal.dominio.brl
import io.github.alessandrocdrx.acertototal.dominio.plural
import java.time.LocalDate

/**
 * Cobrar no WhatsApp (R81). A mensagem sai pronta, a pessoa confere e pode mudar o texto; o app só abre o WhatsApp
 * quando ela toca e não envia nada sozinho. Sem telefone válido não há link: a tela explica e oferece
 * "Editar dados" e "Copiar mensagem".
 */
@Composable
fun FolhaCobrar(
    cliente: Cliente,
    contas: List<ContaComCliente>,
    negocio: Negocio,
    hoje: LocalDate,
    aoEditarCliente: () -> Unit,
    aoMeuNegocio: () -> Unit,
    aoFechar: () -> Unit,
) {
    val ctx = LocalContext.current
    val tel = Cobranca.telefoneWhats(cliente.telefone)
    val total = contas.sumOf { it.valor }
    var texto by remember {
        mutableStateOf(Cobranca.mensagem(cliente.nome, contas.map { ContaParaCobrar(it.descricao, it.valor, it.venc) }, negocio, hoje))
    }
    var retorno by remember { mutableStateOf<Retorno?>(null) }

    FolhaComRodape(
        titulo = "Cobrar ${cliente.nome}",
        subtitulo = "${plural(contas.size, "conta")} · ${brl(total)}. Confira a mensagem e, se quiser, mude o texto.",
        aoFechar = aoFechar,
        rodape = {
            retorno?.let {
                Text(
                    it.texto, fontWeight = FontWeight.SemiBold,
                    color = if (it.erro) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }
            if (tel.isNotEmpty()) BotaoBloco("Abrir no WhatsApp", principal = true, aoClicar = {
                val ok = try {
                    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Cobranca.linkWhats(tel, texto))).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    true
                } catch (_: Exception) {
                    false
                }
                if (!ok) retorno = Retorno("Não encontrei o WhatsApp neste celular. Use “Copiar mensagem”.", erro = true)
            })
            BotaoBloco("Copiar mensagem", principal = tel.isEmpty(), aoClicar = {
                val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("Mensagem de cobrança", texto))
                retorno = Retorno("Mensagem copiada. Cole no WhatsApp.")
            })
            BotaoBloco("Fechar", aoClicar = aoFechar)
        },
    ) {
        if (tel.isEmpty()) {
            Alerta("Falta o telefone com DDD deste cliente. Cadastre para abrir direto no WhatsApp, ou copie a mensagem.") {
                OutlinedButton(onClick = aoEditarCliente, modifier = Modifier.heightIn(min = 48.dp)) { Text("Editar dados") }
            }
        }
        if (negocio.pix.isBlank()) {
            Alerta("Dica: cadastre a sua chave Pix para ela ir na mensagem.") {
                OutlinedButton(onClick = aoMeuNegocio, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cadastrar agora") }
            }
        }
        OutlinedTextField(
            value = texto, onValueChange = { texto = it; retorno = null },
            label = { Text("Mensagem") }, minLines = 8, modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Nome e chave Pix que vão na mensagem. Ficam só no aparelho (e não saem em "Apagar tudo"). */
@Composable
fun FolhaNegocio(inicial: Negocio, aoSalvar: (Negocio) -> Unit, aoFechar: () -> Unit) {
    var nome by remember { mutableStateOf(inicial.nome) }
    var pix by remember { mutableStateOf(inicial.pix) }
    Folha(titulo = "Meu negócio", subtitulo = "Vão na mensagem de cobrança. Ficam só no seu aparelho.", aoFechar = aoFechar) {
        CampoForm("Seu nome ou do negócio", nome, { nome = it }, dica = "Aparece no fim da mensagem. Ex.: Padaria Bom Pão")
        CampoForm(
            "Sua chave Pix", pix, { pix = it },
            dica = "CPF, CNPJ, telefone, e-mail ou chave aleatória. CPF, CNPJ e telefone ganham a pontuação ao salvar.",
        )
        BotaoBloco("Salvar", principal = true, aoClicar = { aoSalvar(Negocio(nome.trim(), ChavePix.formatar(pix))) })
        BotaoBloco("Cancelar", aoClicar = aoFechar)
    }
}
