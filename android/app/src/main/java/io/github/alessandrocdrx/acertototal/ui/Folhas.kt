package io.github.alessandrocdrx.acertototal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.alessandrocdrx.acertototal.BuildConfig
import io.github.alessandrocdrx.acertototal.dados.Anexo
import io.github.alessandrocdrx.acertototal.dados.Cliente
import io.github.alessandrocdrx.acertototal.dados.ContaComCliente
import io.github.alessandrocdrx.acertototal.dominio.CampoForm
import io.github.alessandrocdrx.acertototal.dominio.Dinheiro
import io.github.alessandrocdrx.acertototal.dominio.Documento
import io.github.alessandrocdrx.acertototal.dominio.ErroForm
import io.github.alessandrocdrx.acertototal.dominio.Situacao
import io.github.alessandrocdrx.acertototal.dominio.Validacao
import io.github.alessandrocdrx.acertototal.dominio.brl
import io.github.alessandrocdrx.acertototal.dominio.dataCompleta
import io.github.alessandrocdrx.acertototal.dominio.infoPrazo
import io.github.alessandrocdrx.acertototal.dominio.situacaoDe
import java.time.LocalDate

@Composable
private fun Linha(rotulo: String, conteudo: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(rotulo, color = MaterialTheme.colorScheme.onSurfaceVariant)
        conteudo()
    }
}

/** Detalhe da conta. Em aberto, a baixa está aqui, com a data de hoje já preenchida (R2). */
@Composable
fun FolhaConta(
    conta: ContaComCliente,
    hoje: LocalDate,
    aoFechar: () -> Unit,
    aoBaixar: (LocalDate, Anexo?) -> Unit,
    aoAnexar: (Anexo) -> Unit,
    aoRemoverComprovante: () -> Unit,
    aoDesfazerBaixa: () -> Unit,
    aoEditar: () -> Unit,
    aoExcluir: () -> Unit,
) {
    val sit = situacaoDe(conta.venc, conta.pagoEm, hoje)
    var data by remember { mutableStateOf<LocalDate?>(hoje) }
    var erro by remember { mutableStateOf<ErroForm?>(null) }
    var pendente by remember { mutableStateOf<Anexo?>(null) }
    val anexoDaConta = if (conta.compArquivo != null)
        Anexo(conta.compArquivo, conta.compNome ?: "comprovante", conta.compTipo ?: "image/jpeg") else null
    Folha(titulo = conta.clienteNome, subtitulo = conta.descricao, aoFechar = aoFechar) {
        Linha("Valor") { Text(brl(conta.valor), fontWeight = FontWeight.Bold) }
        Linha("Vencimento") { Text(dataCompleta(conta.venc)) }
        Linha("Situação") { SeloSituacao(sit, infoPrazo(conta.venc, conta.pagoEm, hoje)) }
        if (conta.pagoEm != null) Linha("Pago em") { Text(dataCompleta(conta.pagoEm)) }
        HorizontalDivider()
        if (sit != Situacao.PAGA) {
            Text("Dar baixa", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            CampoData(
                rotulo = "Data do pagamento", data = data, maxima = hoje,
                aoMudar = { data = it; erro = null },
                erro = erro?.takeIf { it.campo == CampoForm.DATA_PAGO }?.mensagem,
            )
            SecaoComprovante(atual = pendente, aoAnexar = { pendente = it }, aoRemover = { pendente = null })
            BotaoBloco("Marcar como paga", principal = true, aoClicar = {
                val e = Validacao.baixa(data, hoje)
                if (e != null) erro = e else aoBaixar(data!!, pendente)
            })
        } else {
            SecaoComprovante(atual = anexoDaConta, aoAnexar = aoAnexar, aoRemover = aoRemoverComprovante)
            HorizontalDivider()
            BotaoBloco("Desfazer baixa", aoClicar = aoDesfazerBaixa)
        }
        BotaoBloco("Editar conta", aoClicar = aoEditar)
        BotaoBloco("Fechar", aoClicar = aoFechar)
        BotaoBloco("Excluir esta conta", perigo = true, aoClicar = aoExcluir)
    }
}

/** Nova conta e editar conta usam a mesma janela. */
@Composable
fun FolhaFormConta(
    inicial: ContaComCliente?,
    clientes: List<Cliente>,
    clientePre: String?,
    hoje: LocalDate,
    aoFechar: () -> Unit,
    aoSalvar: (clienteId: String, descricao: String, valor: Long, venc: LocalDate) -> Unit,
) {
    var clienteId by remember { mutableStateOf(inicial?.clienteId ?: clientePre?.takeIf { id -> clientes.any { it.id == id && it.doc.isNotEmpty() } }) }
    var descricao by remember { mutableStateOf(inicial?.descricao.orEmpty()) }
    var valorTxt by remember { mutableStateOf(inicial?.let { Dinheiro.paraCampo(it.valor) }.orEmpty()) }
    var venc by remember { mutableStateOf<LocalDate?>(inicial?.venc ?: hoje.plusDays(7)) }
    var erro by remember { mutableStateOf<ErroForm?>(null) }
    fun erroDe(c: CampoForm) = erro?.takeIf { it.campo == c }?.mensagem

    Folha(
        titulo = if (inicial == null) "Nova conta" else "Editar conta",
        subtitulo = if (inicial == null) "Uma cobrança a receber." else
            "Mude o que estiver errado." + if (inicial.pagoEm != null) " Esta conta está paga; a data do pagamento não muda aqui." else "",
        aoFechar = aoFechar,
    ) {
        SeletorCliente(clientes, clienteId, aoEscolher = { clienteId = it; erro = null }, erro = erroDe(CampoForm.CLIENTE))
        CampoForm("Descrição", descricao, { descricao = it }, dica = "Ex.: Mensalidade de novembro")
        CampoForm(
            "Valor", valorTxt, { valorTxt = it; erro = null }, dica = "Use vírgula nos centavos. Ex.: 150,00",
            prefixo = "R$ ", teclado = KeyboardType.Decimal, erro = erroDe(CampoForm.VALOR),
        )
        CampoData("Vencimento", venc, { venc = it; erro = null }, erro = erroDe(CampoForm.VENCIMENTO))
        BotaoBloco(if (inicial == null) "Salvar conta" else "Salvar alterações", principal = true, aoClicar = {
            val tem = clientes.firstOrNull { it.id == clienteId }?.doc?.isNotEmpty() == true
            val e = Validacao.conta(clienteId, tem, valorTxt, venc)
            if (e != null) erro = e else aoSalvar(clienteId!!, descricao, Dinheiro.parse(valorTxt)!!, venc!!)
        })
        BotaoBloco("Cancelar", aoClicar = aoFechar)
    }
}

/** Novo cliente, editar e "Completar cadastro" (cliente antigo sem CPF/CNPJ, R59). */
@Composable
fun FolhaFormCliente(
    inicial: Cliente?,
    aoFechar: () -> Unit,
    aoSalvar: (nome: String, doc: String, tel: String, email: String, obs: String, aoErro: (ErroForm) -> Unit) -> Unit,
) {
    var nome by remember { mutableStateOf(inicial?.nome.orEmpty()) }
    var doc by remember { mutableStateOf(inicial?.doc?.let(Documento::formatar).orEmpty()) }
    var tel by remember { mutableStateOf(inicial?.telefone.orEmpty()) }
    var email by remember { mutableStateOf(inicial?.email.orEmpty()) }
    var obs by remember { mutableStateOf(inicial?.obs.orEmpty()) }
    var erro by remember { mutableStateOf<ErroForm?>(null) }
    fun erroDe(c: CampoForm) = erro?.takeIf { it.campo == c }?.mensagem
    val falta = inicial != null && inicial.doc.isEmpty()

    // O aviso do teste muda enquanto a pessoa digita, nunca ao sair do campo (R62).
    val d = Documento.soDigitos(doc)
    val avisoTeste = if (!BuildConfig.COBRANCA_REAL && Documento.aceito(d, false) && !Documento.valido(d))
        "Atenção: este ${Documento.tipo(d)} ${Documento.AVISO_TESTE}" else null

    Folha(
        titulo = when { falta -> "Completar cadastro"; inicial != null -> "Editar cliente"; else -> "Novo cliente" },
        subtitulo = if (falta) "Falta o CPF ou CNPJ de ${inicial?.nome}." else "Nome e CPF ou CNPJ são obrigatórios.",
        aoFechar = aoFechar,
    ) {
        CampoForm("Nome", nome, { nome = it; erro = null }, erro = erroDe(CampoForm.NOME))
        CampoForm(
            "CPF ou CNPJ", doc, { doc = it; erro = null },
            dica = "Identifica o cliente nas planilhas. Pode digitar com ou sem pontos.",
            teclado = KeyboardType.Number, erro = erroDe(CampoForm.DOC), aviso = avisoTeste,
        )
        CampoForm("Telefone", tel, { tel = it }, teclado = KeyboardType.Phone)
        CampoForm("E-mail", email, { email = it }, teclado = KeyboardType.Email)
        CampoForm("Observações", obs, { obs = it }, linhas = 2)
        BotaoBloco(if (inicial == null) "Salvar cliente" else "Salvar alterações", principal = true, aoClicar = {
            aoSalvar(nome, doc, tel, email, obs) { erro = it }
        })
        BotaoBloco("Cancelar", aoClicar = aoFechar)
    }
}

@Composable
fun FolhaCliente(
    cliente: Cliente,
    contas: List<ContaComCliente>,
    hoje: LocalDate,
    aoFechar: () -> Unit,
    aoNovaConta: () -> Unit,
    aoEditar: () -> Unit,
    aoAtualizarExcel: () -> Unit,
    aoExcluir: () -> Unit,
    aoAbrirConta: (ContaComCliente) -> Unit,
) {
    val contato = listOf(cliente.telefone, cliente.email).filter { it.isNotBlank() }.joinToString("\n").ifEmpty { "Sem contato" }
    val docTxt = if (cliente.doc.isEmpty()) "" else
        "${Documento.tipo(cliente.doc)} ${Documento.formatar(cliente.doc)}" +
            (if (Documento.valido(cliente.doc)) "" else " · não é válido (aceito na versão de teste)") + "\n"
    Folha(titulo = cliente.nome, subtitulo = docTxt + contato, aoFechar = aoFechar) {
        if (cliente.doc.isEmpty()) {
            Text(
                "Falta o CPF ou CNPJ. Sem ele não dá para lançar contas nem usar planilhas com este cliente.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            BotaoBloco("Completar cadastro", principal = true, aoClicar = aoEditar)
        } else {
            BotaoBloco("Nova conta para este cliente", principal = true, aoClicar = aoNovaConta)
            BotaoBloco("Editar dados", aoClicar = aoEditar)
            if (contas.isNotEmpty()) BotaoBloco("Atualizar pelo Excel", aoClicar = aoAtualizarExcel)
        }
        if (contas.isEmpty()) BotaoBloco("Excluir cliente", perigo = true, aoClicar = aoExcluir)
        Text("Contas (${contas.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (contas.isEmpty()) Text("Sem contas ainda.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        contas.sortedWith { a, b ->
            if (a.pagoEm != null && b.pagoEm != null) b.pagoEm.compareTo(a.pagoEm)
            else compareValuesBy(a, b, { it.pagoEm != null }, { it.venc })
        }.forEach { c ->
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { aoAbrirConta(c) }.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(c.descricao, fontWeight = FontWeight.SemiBold)
                    SeloSituacao(situacaoDe(c.venc, c.pagoEm, hoje), infoPrazo(c.venc, c.pagoEm, hoje))
                }
                Text(brl(c.valor), fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
            }
        }
        BotaoBloco("Fechar", aoClicar = aoFechar)
    }
}

/** Texto do aviso quando o cliente tem documento que só vale no teste. */
fun avisoDocumentoTeste(cliente: Cliente): String? =
    if (cliente.doc.isNotEmpty() && !Documento.valido(cliente.doc))
        "o ${Documento.tipo(cliente.doc)} não é válido (aceito só na versão de teste)" else null
