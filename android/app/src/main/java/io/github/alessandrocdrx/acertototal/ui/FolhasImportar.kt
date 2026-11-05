package io.github.alessandrocdrx.acertototal.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import io.github.alessandrocdrx.acertototal.dados.ImportacaoReg
import io.github.alessandrocdrx.acertototal.dominio.InfoBackup
import io.github.alessandrocdrx.acertototal.dominio.dataCurta
import io.github.alessandrocdrx.acertototal.dominio.plural
import io.github.alessandrocdrx.acertototal.planilha.CampoPlanilha
import io.github.alessandrocdrx.acertototal.planilha.Campos
import io.github.alessandrocdrx.acertototal.planilha.Coluna
import io.github.alessandrocdrx.acertototal.planilha.Importacao
import io.github.alessandrocdrx.acertototal.planilha.ItemPlano
import io.github.alessandrocdrx.acertototal.planilha.LinhaItem
import io.github.alessandrocdrx.acertototal.planilha.RegistroJson
import io.github.alessandrocdrx.acertototal.planilha.Req
import io.github.alessandrocdrx.acertototal.planilha.TipoItem
import io.github.alessandrocdrx.acertototal.planilha.mantidosTexto
import io.github.alessandrocdrx.acertototal.planilha.quandoTexto
import io.github.alessandrocdrx.acertototal.planilha.resumoDoDesfazer
import java.time.LocalDate

/** Etapas da importação, escritas e numeradas, com a atual marcada (R46). */
@Composable
fun Passos(atual: Int) {
    Row(Modifier.fillMaxWidth().semantics { contentDescription = "Etapa $atual de 3" }, horizontalArrangement = Arrangement.SpaceBetween) {
        listOf("Arquivo", "Colunas", "Conferir").forEachIndexed { i, rotulo ->
            val n = i + 1
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(24.dp).background(
                        if (n <= atual) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, CircleShape,
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (n < atual) Icon(Icons.Filled.Check, contentDescription = "feito", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                    else Text(
                        "$n", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
                        color = if (n == atual) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    rotulo, modifier = Modifier.padding(start = 6.dp), style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (n == atual) FontWeight.Bold else FontWeight.Normal,
                    color = if (n <= atual) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Caixa de aviso. [erro] = vermelho (algo ficou de fora); senão, informativa. */
@Composable
fun Alerta(texto: String, erro: Boolean = false, conteudo: (@Composable () -> Unit)? = null) {
    val cores = MaterialTheme.acerto
    Surface(
        color = if (erro) cores.ruimFundo else MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(texto, color = if (erro) cores.ruim else MaterialTheme.colorScheme.onSurface)
            conteudo?.invoke()
        }
    }
}

@Composable
private fun SanfonaSimples(titulo: String, conteudo: @Composable ColumnScope.() -> Unit) {
    var aberta by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { aberta = !aberta }, verticalAlignment = Alignment.CenterVertically) {
            Text(titulo, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Icon(
                if (aberta) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = if (aberta) "Fechar" else "Abrir",
            )
        }
        if (aberta) Column(verticalArrangement = Arrangement.spacedBy(8.dp), content = conteudo)
    }
}

// ---------------------------------------------------------------------------------------- passo 1

@Composable
fun PassoEscolher(
    backup: InfoBackup,
    mostrarExemplo: Boolean,
    aoEscolherArquivo: () -> Unit,
    aoUsarExemplo: () -> Unit,
    aoBaixarModelo: () -> Unit,
    aoFazerBackup: () -> Unit,
    aoFechar: () -> Unit,
) {
    FolhaComRodape(
        titulo = "Importar planilha",
        subtitulo = "Escolha a planilha do Excel. Antes de gravar, você confere tudo.",
        aoFechar = aoFechar,
        rodape = {
            BotaoBloco("Escolher arquivo", principal = true, aoClicar = aoEscolherArquivo)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = aoBaixarModelo, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("Baixar modelo") }
                OutlinedButton(onClick = aoFechar, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("Fechar") }
            }
        },
    ) {
        Passos(1)
        SanfonaSimples("Como montar a planilha?") {
            Text("Cada linha da planilha é uma conta, e o CPF/CNPJ diz de quem ela é: um cliente com 8 contas ocupa 8 linhas com o mesmo CPF/CNPJ.")
            Text("Atualizar contas que já existem: baixe a planilha para editar (Planilha › Atualizar pelo Excel), mude o que quiser sem mexer no código e envie aqui. Você vê o antes e o depois de cada conta.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Só cadastrar clientes: preencha CPF/CNPJ, nome e contato, e deixe valor e vencimento vazios.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Colunas da planilha", fontWeight = FontWeight.SemiBold)
            CampoPlanilha.entries.forEach { c ->
                Column(Modifier.fillMaxWidth()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(c.rotL, fontWeight = FontWeight.SemiBold)
                        c.req.texto?.let { Text("($it)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
                    }
                    Text(
                        "Ex.: ${c.exemplo}" + (c.dica?.let { " · $it" } ?: ""),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                "Os títulos podem variar: “Cliente” em vez de “Nome”, “Data de vencimento” em vez de “Vencimento”. Se o app não reconhecer alguma coluna, você escolhe no passo seguinte. Aceita Excel (.xlsx) e CSV.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (backup.tom != io.github.alessandrocdrx.acertototal.dominio.Situacao.PAGA) {
            Alerta("Dica: faça uma cópia de segurança antes de importar.") {
                OutlinedButton(onClick = aoFazerBackup, modifier = Modifier.heightIn(min = 48.dp)) { Text("Fazer cópia agora") }
            }
        }
        if (mostrarExemplo) BotaoBloco("Testar com a planilha de exemplo", aoClicar = aoUsarExemplo)
    }
}

@Composable
fun PassoLendo(arquivo: String, aoFechar: () -> Unit) {
    Folha(titulo = "Lendo a planilha…", subtitulo = arquivo, aoFechar = aoFechar) { Passos(1) }
}

@Composable
fun PassoErro(e: ImpEstado.Erro, aoEscolherOutro: () -> Unit, aoBaixarModelo: () -> Unit, aoFechar: () -> Unit) {
    Folha(titulo = e.titulo, subtitulo = e.arquivo, aoFechar = aoFechar) {
        Passos(1)
        Alerta(e.texto, erro = true)
        BotaoBloco("Escolher outro arquivo", principal = true, aoClicar = aoEscolherOutro)
        if (e.modelo) BotaoBloco("Baixar planilha modelo", aoClicar = aoBaixarModelo)
        BotaoBloco("Fechar", aoClicar = aoFechar)
    }
}

// ---------------------------------------------------------------------------------------- passo 2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeletorColuna(rotulo: String, tag: String?, cols: List<Coluna>, selecionada: Int?, erro: String?, aoEscolher: (Int?) -> Unit) {
    var aberto by remember { mutableStateOf(false) }
    fun texto(c: Coluna) = "${c.letra} · ${c.titulo.ifEmpty { "(sem título)" }}" + (if (c.exemplo.isNotEmpty()) " — ex.: ${c.exemplo.take(24)}" else "")
    Column(Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(rotulo, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            if (tag != null) Text("($tag)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
        ExposedDropdownMenuBox(expanded = aberto, onExpandedChange = { aberto = it }) {
            OutlinedTextField(
                value = cols.firstOrNull { it.j == selecionada }?.let(::texto) ?: "Não usar",
                onValueChange = {}, readOnly = true, singleLine = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = aberto) },
                isError = erro != null, supportingText = erro?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp).menuAnchor(MenuAnchorType.PrimaryNotEditable),
                colors = camposAcerto(),
            )
            ExposedDropdownMenu(expanded = aberto, onDismissRequest = { aberto = false }) {
                DropdownMenuItem(text = { Text("Não usar") }, onClick = { aoEscolher(null); aberto = false })
                cols.forEach { c -> DropdownMenuItem(text = { Text(texto(c)) }, onClick = { aoEscolher(c.j); aberto = false }) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassoColunas(
    c: ImpEstado.Colunas,
    aoEscolherAba: (Int) -> Unit,
    aoMudarColuna: (CampoPlanilha, Int?) -> Unit,
    aoContinuar: () -> Unit,
    aoVoltar: () -> Unit,
    aoFechar: () -> Unit,
) {
    val a = c.abas[c.aba]
    val falta = Campos.faltaColuna(c.mapa)
    FolhaComRodape(
        titulo = "Conferir colunas",
        subtitulo = "${c.arquivo}. Diga em qual coluna da planilha está cada informação.",
        aoFechar = aoFechar,
        rodape = {
            BotaoBloco("Continuar", principal = true, aoClicar = aoContinuar)
            BotaoBloco("Voltar", aoClicar = aoVoltar)
        },
    ) {
        Passos(2)
        if (falta.isNotEmpty()) Alerta(falta, erro = true)
        if (c.abas.size > 1) {
            var aberto by remember { mutableStateOf(false) }
            Column(Modifier.fillMaxWidth()) {
                Text("Aba da planilha", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                ExposedDropdownMenuBox(expanded = aberto, onExpandedChange = { aberto = it }) {
                    OutlinedTextField(
                        value = "${a.nome} (${plural(a.linhas.size, "linha")})", onValueChange = {}, readOnly = true, singleLine = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = aberto) },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp).menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        colors = camposAcerto(),
                    )
                    ExposedDropdownMenu(expanded = aberto, onDismissRequest = { aberto = false }) {
                        c.abas.forEachIndexed { i, x ->
                            DropdownMenuItem(text = { Text("${x.nome} (${plural(x.linhas.size, "linha")})") }, onClick = { aoEscolherAba(i); aberto = false })
                        }
                    }
                }
            }
        }
        Text(
            "Títulos na linha ${a.linhaCab} · ${plural(a.linhas.size, "linha")} com dados.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        CampoPlanilha.entries.forEach { campo ->
            SeletorColuna(
                rotulo = campo.rotL, tag = campo.req.texto, cols = a.cols, selecionada = c.mapa[campo],
                erro = c.erro?.takeIf { it.first == campo }?.second, aoEscolher = { aoMudarColuna(campo, it) },
            )
        }
        val usados = c.mapa.values.toSet()
        val ignoradas = a.cols.filter { it.j !in usados }
        if (ignoradas.isNotEmpty()) {
            Text(
                "Colunas que ficam de fora: " + ignoradas.joinToString(", ") { "${it.letra} ${it.titulo.ifEmpty { "(sem título)" }}" } + ".",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ---------------------------------------------------------------------------------------- passo 3

private fun itemTexto(l: LinhaItem): AnnotatedString = when (l) {
    is LinhaItem.Texto -> AnnotatedString(l.t)
    is LinhaItem.Sub -> AnnotatedString(l.t)
    // Riscado é como está; em negrito é como vai ficar.
    is LinhaItem.Mudanca -> buildAnnotatedString {
        append(l.rot + ": ")
        withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) { append(l.de) }
        append(" → ")
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(l.para) }
    }
    is LinhaItem.Prob -> buildAnnotatedString {
        if (l.col.isNotEmpty()) { withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(l.col) }; append(" ") }
        append(l.txt)
    }
}

@Composable
private fun CartaoItem(x: ItemPlano) {
    val cores = MaterialTheme.acerto
    val borda = when (x.tipo) {
        TipoItem.ERRO -> cores.ruim
        TipoItem.AVISO -> cores.aviso
        TipoItem.NOVO -> cores.ok
        TipoItem.ATUALIZADO -> MaterialTheme.colorScheme.primary
    }
    Surface(shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), modifier = Modifier.fillMaxWidth()) {
        // Faixa colorida à esquerda reforça o tipo (R3: o texto diz, a cor só reforça).
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(4.dp).fillMaxHeight().background(borda))
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(x.titulo, fontWeight = FontWeight.SemiBold)
                x.linhas.forEach { l ->
                    Text(
                        itemTexto(l), style = MaterialTheme.typography.bodySmall,
                        color = if (l is LinhaItem.Sub) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        modifier = if (l is LinhaItem.Sub) Modifier.padding(top = 4.dp) else Modifier,
                    )
                }
            }
        }
    }
}

/** Listas longas: mostra as 20 primeiras e recolhe o resto. */
@Composable
fun ListaItens(itens: List<ItemPlano>, tipo: TipoItem) {
    val l = itens.filter { it.tipo == tipo }
    val max = 20
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        l.take(max).forEach { CartaoItem(it) }
        if (l.size > max) SanfonaSimples("Mostrar mais ${l.size - max}") { l.drop(max).forEach { CartaoItem(it) } }
    }
}

@Composable
private fun Alternador(rotulo: String, marcado: Boolean, aoMudar: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(rotulo, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
        Switch(checked = marcado, onCheckedChange = aoMudar)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PassoPrevia(
    p: ImpEstado.Previa,
    hoje: LocalDate,
    aoAjustarColunas: () -> Unit,
    aoBaixarErros: () -> Unit,
    aoIrAtualizar: () -> Unit,
    aoImportar: (novos: Boolean, atualizar: Boolean) -> Unit,
    aoFechar: () -> Unit,
) {
    val pl = p.plano
    val nAt = pl.clientesAtualizados.size + pl.contasAtualizadas.size
    val nNv = pl.clientesNovos.size + pl.contasNovas.size
    val ne = pl.erros.size
    val itens = remember(pl) { Importacao.itens(pl, true, true) }
    var atualizar by remember(pl) { mutableStateOf(true) }
    var novos by remember(pl) { mutableStateOf(true) }
    val a = p.abas[p.aba]
    val letra = a.cols.associate { it.j to it.letra }
    val usadas = CampoPlanilha.entries.filter { it in p.mapa }.joinToString(", ") { "${it.rot} (${letra[p.mapa[it]]})" }
    val cores = MaterialTheme.acerto

    FolhaComRodape(
        titulo = "Prévia da importação",
        subtitulo = "${p.arquivo}. Nada muda até você confirmar.",
        aoFechar = aoFechar,
        rodape = {
            val usarNovos = if (nAt > 0 && nNv > 0) novos else true
            val usarAtualizar = if (nAt > 0) atualizar else true
            val n = Importacao.totalMudancas(pl, usarNovos, usarAtualizar)
            BotaoBloco(
                if (n > 0) "Importar " + Importacao.textoMudancas(pl, usarNovos, usarAtualizar) else "Nada para importar",
                principal = true, habilitado = n > 0, aoClicar = { aoImportar(usarNovos, usarAtualizar) },
            )
            BotaoBloco("Cancelar", aoClicar = aoFechar)
        },
    ) {
        Passos(3)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                "Clientes novos" to pl.clientesNovos.size, "Contas novas" to pl.contasNovas.size, "Atualizações" to nAt,
                "Sem mudança" to pl.semMudanca.size, "Com problema" to ne,
            ).forEach { (rotulo, n) ->
                Surface(
                    shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.widthIn(min = 104.dp),
                ) {
                    Column(Modifier.padding(8.dp)) {
                        Text("$n", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = if (rotulo == "Com problema" && n > 0) cores.ruim else Color.Unspecified)
                        Text(rotulo, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        if (ne > 0) {
            Alerta("${plural(ne, "linha")} com problema ${if (ne > 1) "ficam" else "fica"} de fora. Você pode importar o resto agora e corrigir essas depois.", erro = true) {
                OutlinedButton(onClick = aoBaixarErros, modifier = Modifier.heightIn(min = 48.dp)) { Text("Baixar linhas com problema") }
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                (if (p.totalAbas > 1) "Aba “${a.nome}” · " else "") + "Títulos na linha ${a.linhaCab}. Colunas usadas: $usadas.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f),
            )
            TextButton(onClick = aoAjustarColunas, modifier = Modifier.heightIn(min = 48.dp)) { Text("Ajustar colunas") }
        }
        if (pl.parecemAtualizacao.isNotEmpty()) {
            val pa = pl.parecemAtualizacao
            val ex = pa[0]
            val ant = Importacao.contaAnterior(ex, p.contas)
            Alerta(
                "${plural(pa.size, "conta")} ${if (pa.size > 1) "novas parecem mudanças de contas" else "nova parece mudança de conta"} que o app já tem. " +
                    "Exemplo: ${ex.clienteNome} já tem “${ant?.descricao.orEmpty()}” em aberto (venc. ${ant?.venc?.let { dataCurta(it, hoje) }.orEmpty()}) e a planilha traz outra com vencimento ${dataCurta(ex.venc, hoje)}. " +
                    "Se a ideia era atualizar contas, não continue: use “Atualizar pelo Excel”, que mantém a conta e muda só o que você alterou. Se são contas novas de verdade, siga em frente.",
            ) { OutlinedButton(onClick = aoIrAtualizar, modifier = Modifier.heightIn(min = 48.dp)) { Text("Ir para Atualizar pelo Excel") } }
        }
        if (pl.soClientes) Alerta("A planilha não tem colunas de valor e vencimento: só os clientes serão importados.")
        if (nAt > 0) {
            Text("O que vai mudar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Dados que já existem no app e estão diferentes na planilha, por cliente. Riscado é como está; em negrito é como vai ficar.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ListaItens(itens, TipoItem.ATUALIZADO)
            Alternador("Atualizar estes dados", atualizar) { atualizar = it }
        }
        if (nNv > 0) {
            Text("O que vai entrar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            ListaItens(itens, TipoItem.NOVO)
            if (nAt > 0) Alternador("Cadastrar o que é novo", novos) { novos = it }
        }
        if (ne > 0) {
            Text("Linhas com problema", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Não serão importadas. Corrija na planilha e importe de novo: o que já entrou não se repete.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ListaItens(itens, TipoItem.ERRO)
        }
        if (pl.avisos.isNotEmpty()) {
            Text("Para conferir", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Estas linhas entram, mas vale olhar.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ListaItens(itens, TipoItem.AVISO)
        }
        if (pl.semMudanca.isNotEmpty()) {
            SanfonaSimples("${pl.semMudanca.size} ${if (pl.semMudanca.size > 1) "contas já estão iguais" else "conta já está igual"} no app") {
                pl.semMudanca.forEach { Text("${it.clienteNome} · ${it.conta.descricao} · vence ${io.github.alessandrocdrx.acertototal.dominio.dataCompleta(it.conta.venc)}", style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

@Composable
fun PassoConcluida(
    c: ImpEstado.Concluida,
    aoVer: () -> Unit,
    aoBaixarErros: () -> Unit,
    aoDesfazer: () -> Unit,
    aoFechar: () -> Unit,
) {
    val temContas = c.resumo.contasNovas + c.resumo.contasAtualizadas > 0
    val erros = c.previa.plano.erros.size
    Folha(titulo = "Importação concluída", subtitulo = "Gravado: ${c.resumo.copy(erros = 0).texto()}.", aoFechar = aoFechar) {
        if (erros > 0) {
            Alerta("${plural(erros, "linha")} ${if (erros > 1) "ficaram" else "ficou"} de fora. Baixe, corrija e importe de novo: o que já entrou não se repete.", erro = true) {
                OutlinedButton(onClick = aoBaixarErros, modifier = Modifier.heightIn(min = 48.dp)) { Text("Baixar linhas com problema") }
            }
        }
        BotaoBloco(if (temContas) "Ver contas" else "Ver clientes", principal = true, aoClicar = aoVer)
        BotaoBloco("Desfazer importação", aoClicar = aoDesfazer)
        BotaoBloco("Fechar", aoClicar = aoFechar)
    }
}

// ---------------------------------------------------------------------------------------- histórico e desfazer

@Composable
fun FolhaAtualizarExcel(aoBaixar: () -> Unit, aoEnviar: () -> Unit, aoFechar: () -> Unit) {
    Folha(
        titulo = "Atualizar pelo Excel",
        subtitulo = "Para mudar várias contas de uma vez: valor, vencimento, descrição ou baixa.",
        aoFechar = aoFechar,
    ) {
        listOf(
            "Baixe a planilha para editar." to "Ela tem o código de cada conta e o CPF/CNPJ do cliente.",
            "Mude no Excel o que quiser." to "Não mexa na coluna “Código da conta”. Célula vazia não apaga nada.",
            "Envie a planilha de volta." to "Você vê o antes e o depois de cada conta antes de gravar.",
        ).forEachIndexed { i, (t, d) ->
            Row(verticalAlignment = Alignment.Top) {
                Box(Modifier.size(24.dp).background(MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) {
                    Text("${i + 1}", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                }
                Column(Modifier.padding(start = 10.dp)) {
                    Text(t, fontWeight = FontWeight.SemiBold)
                    Text(d, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Text("Só um cliente? Abra o cliente e toque em “Atualizar pelo Excel”.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        BotaoBloco("Baixar planilha para editar", principal = true, aoClicar = aoBaixar)
        BotaoBloco("Enviar planilha editada", aoClicar = aoEnviar)
        BotaoBloco("Fechar", aoClicar = aoFechar)
    }
}

@Composable
fun FolhaRegistroImp(reg: ImportacaoReg, podeDesfazer: Boolean, aoDesfazer: () -> Unit, aoFechar: () -> Unit) {
    val resumo = remember(reg) { RegistroJson.resumoDeJson(reg.resumo) }
    val itens = remember(reg) { RegistroJson.itensDeJson(reg.itens) }
    Folha(
        titulo = "Registro da importação",
        subtitulo = "${reg.arquivo} · ${quandoTexto(reg.quando)}\n${resumo.texto()}",
        aoFechar = aoFechar,
    ) {
        if (reg.desfeita != null) Alerta("Desfeita em ${quandoTexto(reg.desfeita)}" + (if (reg.mantidos > 0) ". ${mantidosTexto(reg.mantidos)}" else "."))
        listOf(
            TipoItem.ATUALIZADO to "O que mudou", TipoItem.NOVO to "O que entrou",
            TipoItem.ERRO to "Linhas que ficaram de fora", TipoItem.AVISO to "Para conferir",
        ).forEach { (tipo, titulo) ->
            if (itens.any { it.tipo == tipo }) {
                Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                ListaItens(itens, tipo)
            }
        }
        if (itens.isEmpty()) Text("Nada mudou nesta importação.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (podeDesfazer) BotaoBloco("Desfazer esta importação", aoClicar = aoDesfazer)
        BotaoBloco("Fechar", aoClicar = aoFechar)
    }
}

@Composable
fun FolhaDesfazerImp(reg: ImportacaoReg, aoConfirmar: () -> Unit, aoFechar: () -> Unit) {
    val itens = remember(reg) { resumoDoDesfazer(RegistroJson.desfazerDeJson(reg.desfazer)) }
    Folha(titulo = "Desfazer a importação?", subtitulo = "${reg.arquivo} · ${quandoTexto(reg.quando)}", aoFechar = aoFechar) {
        Column { itens.forEach { Text("• $it") } }
        Text(
            "O que você mudou depois da importação, como uma baixa ou um comprovante, fica como está.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        BotaoBloco("Desfazer importação", perigo = true, aoClicar = aoConfirmar)
        BotaoBloco("Manter como está", aoClicar = aoFechar)
    }
}
