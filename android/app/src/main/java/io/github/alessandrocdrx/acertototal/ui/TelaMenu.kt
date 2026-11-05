package io.github.alessandrocdrx.acertototal.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

const val AUTOR = "alessandrocdrx"
private const val PERFIL = "https://github.com/alessandrocdrx"
private const val REPO = "https://github.com/alessandrocdrx/acertoTotal"
private const val LINK_LOJA = "https://play.google.com/store/apps/details?id=io.github.alessandrocdrx.acertototal"

/** Símbolo da marca desenhado em código, nas medidas do SVG em marca/simbolo.svg (108×108). */
@Composable
fun Simbolo(tamanho: Dp, modifier: Modifier = Modifier) {
    val acento = MaterialTheme.colorScheme.primary
    val fundo = MaterialTheme.colorScheme.onPrimary
    val sol = MaterialTheme.acerto.sol
    Canvas(modifier.size(tamanho)) {
        val k = size.width / 108f
        drawRoundRect(acento, cornerRadius = CornerRadius(24f * k))
        drawArc(
            fundo, startAngle = -15.4f, sweepAngle = 293.8f, useCenter = false,
            topLeft = Offset(33f * k, 33f * k), size = Size(42f * k, 42f * k),
            style = Stroke(7.5f * k, cap = StrokeCap.Round),
        )
        val visto = Path().apply {
            moveTo(41.5f * k, 55.5f * k); lineTo(50.5f * k, 64.5f * k); lineTo(76.5f * k, 35.5f * k)
        }
        drawPath(visto, sol, style = Stroke(7.5f * k, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** "acerto" em peso normal e "Total" na cor da marca. */
@Composable
fun Marca(modifier: Modifier = Modifier) {
    Text(
        buildAnnotatedString {
            append("acerto")
            withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)) { append("Total") }
        },
        modifier = modifier,
        style = MaterialTheme.typography.titleLarge,
    )
}

@Composable
private fun Grupo(titulo: String) {
    Text(titulo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp))
}

/** Linha tocável do Menu: ícone, título, explicação e seta (R84: o que se toca parece tocável). */
@Composable
fun Opcao(
    icone: ImageVector,
    titulo: String,
    texto: String,
    aoClicar: () -> Unit,
    externo: Boolean = false,
    extra: (@Composable () -> Unit)? = null,
) {
    Card(
        onClick = aoClicar,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(titulo, fontWeight = FontWeight.SemiBold)
                Text(texto, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (extra != null) Box(Modifier.padding(top = 6.dp)) { extra() }
            }
            Icon(
                if (externo) Icons.AutoMirrored.Filled.OpenInNew else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = if (externo) "Abre fora do app" else null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun TelaMenu(
    versao: String,
    mostrarApagar: Boolean,
    backup: io.github.alessandrocdrx.acertototal.dominio.InfoBackup,
    aoBackup: () -> Unit,
    aoRestaurar: () -> Unit,
    aoSobre: () -> Unit,
    aoTema: () -> Unit,
    aoFaq: () -> Unit,
    aoApagar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val links = LocalUriHandler.current
    Column(modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Card(
            onClick = aoSobre,
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp),
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Simbolo(52.dp)
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Marca()
                    Text("Criado por @$AUTOR · versão $versao", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Grupo("Aparência")
        Opcao(Icons.Filled.Palette, "Tema", "Claro, escuro ou igual ao celular", aoTema)
        Grupo("Seus dados")
        Opcao(
            Icons.Filled.Backup, "Cópia de segurança", "Tudo numa planilha, para guardar ou trocar de celular", aoBackup,
            extra = { SeloSituacao(backup.tom, backup.texto) },
        )
        Opcao(Icons.Filled.Restore, "Restaurar uma cópia", "Traga de volta os dados de uma cópia de segurança", aoRestaurar)
        Grupo("Ajuda")
        Opcao(Icons.Filled.Star, "Avaliar o acertoTotal", "Na Play Store · ajuda muito o app a crescer", { links.openUri(LINK_LOJA) }, externo = true)
        Opcao(Icons.Filled.Feedback, "Relatar problema ou sugestão", "Abre a página do projeto", { links.openUri("$REPO/issues/new") }, externo = true)
        Opcao(Icons.Filled.PrivacyTip, "Política de privacidade", "Seus dados ficam só no aparelho", { links.openUri("$REPO/blob/main/PRIVACIDADE.md") }, externo = true)
        Opcao(Icons.Filled.Info, "Perguntas frequentes", "Como dar baixa, trocar de celular e mais", aoFaq)
        if (mostrarApagar) {
            Grupo("Versão de teste")
            Text(
                "Para recomeçar do zero: apaga todos os clientes, contas, comprovantes e importações deste aparelho.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            BotaoBloco("Apagar tudo…", perigo = true, aoClicar = aoApagar)
        }
    }
}

@Composable
fun FolhaSobre(versao: String, aoFechar: () -> Unit) {
    val links = LocalUriHandler.current
    Folha(titulo = "Sobre", aoFechar = aoFechar) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Simbolo(88.dp)
            Marca()
            Text("Contas a receber, acertadas.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Versão $versao", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Criado por ")
            Text(
                "@$AUTOR", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.heightIn(min = 48.dp).clickable { links.openUri(PERFIL) }.padding(vertical = 12.dp),
            )
        }
        Text("Funciona sem internet. Os seus clientes, contas e comprovantes ficam só no seu aparelho.")
        Text("Da mesma família: geoTotal e calcTotal.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        BotaoBloco("Fechar", aoClicar = aoFechar)
    }
}

@Composable
fun FolhaTema(atual: Tema, aoEscolher: (Tema) -> Unit, aoFechar: () -> Unit) {
    Folha(titulo = "Tema", subtitulo = "Escolha como o app aparece. Automático segue o tema do celular.", aoFechar = aoFechar) {
        Tema.entries.forEach { t ->
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(selected = atual == t, onClick = { aoEscolher(t) }, role = Role.RadioButton),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = atual == t, onClick = null)
                Text(t.rotulo, modifier = Modifier.padding(start = 12.dp))
            }
        }
        BotaoBloco("Fechar", aoClicar = aoFechar)
    }
}

private val FAQ = listOf(
    "Como dar baixa numa conta?" to "Na lista de Contas, toque em “Dar baixa”, confira a data do pagamento e toque em “Marcar como paga”. Errou? Toque em “Desfazer” no aviso.",
    "Como mudar o valor ou o vencimento?" to "Toque na conta e depois em “Editar conta”. Dá para mudar o cliente, a descrição, o valor e o vencimento.",
    "Como ver ou mandar um comprovante?" to "Toque numa conta paga. O cartão do comprovante tem “Ver” (tela cheia), “Compartilhar” (WhatsApp, e-mail) e “Salvar na pasta Downloads”.",
    "Como trocar de celular sem perder nada?" to "No celular antigo: Menu › Cópia de segurança, e guarde o arquivo Excel (mande para o seu e-mail ou Drive). No novo: Menu › Restaurar uma cópia. Os comprovantes não vão na cópia: salve os que quiser guardar.",
    "Como cadastrar muitos clientes de uma vez?" to "Na aba Planilha, toque em “Importar planilha”. Cada linha é uma conta e o CPF/CNPJ diz de quem ela é. O app mostra tudo antes de gravar.",
    "O que acontece quando o teste grátis termina?" to "O app continua abrindo para consulta. Para cadastrar, dar baixa, importar e exportar de novo, assine em Menu › Assinatura.",
)

@Composable
fun FolhaFaq(aoFechar: () -> Unit) {
    Folha(titulo = "Perguntas frequentes", aoFechar = aoFechar) {
        FAQ.forEach { (pergunta, resposta) ->
            var aberta by remember { mutableStateOf(false) }
            Column(Modifier.fillMaxWidth()) {
                // Sanfona sempre com setinha: para baixo fechada, para cima aberta (R84).
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { aberta = !aberta },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(pergunta, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Icon(
                        if (aberta) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (aberta) "Fechar resposta" else "Abrir resposta",
                    )
                }
                if (aberta) Text(resposta, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
            }
        }
        BotaoBloco("Fechar", aoClicar = aoFechar)
    }
}

/** Só na versão de teste. Diz o que vai ser apagado e só ativa o botão quando a pessoa digita APAGAR (R60). */
@Composable
fun FolhaApagar(clientes: Int, contas: Int, comprovantes: Int, aoApagar: () -> Unit, aoFechar: () -> Unit) {
    var texto by remember { mutableStateOf("") }
    val confere = io.github.alessandrocdrx.acertototal.dominio.confereApagar(texto)
    Folha(titulo = "Apagar tudo", subtitulo = "Isto não pode ser desfeito. Serão apagados:", aoFechar = aoFechar) {
        Column {
            Text("• ${io.github.alessandrocdrx.acertototal.dominio.plural(clientes, "cliente")}")
            Text("• ${io.github.alessandrocdrx.acertototal.dominio.plural(contas, "conta")}")
            Text("• ${io.github.alessandrocdrx.acertototal.dominio.plural(comprovantes, "comprovante")}")
        }
        CampoForm(
            "Para confirmar, digite ${io.github.alessandrocdrx.acertototal.dominio.PALAVRA_APAGAR}", texto, { texto = it },
            dica = "Pode ser em letras maiúsculas ou minúsculas.",
        )
        BotaoBloco("Apagar tudo", perigo = true, habilitado = confere, aoClicar = aoApagar)
        BotaoBloco("Cancelar", aoClicar = aoFechar)
    }
}
