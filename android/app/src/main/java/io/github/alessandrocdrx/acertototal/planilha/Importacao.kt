package io.github.alessandrocdrx.acertototal.planilha

import io.github.alessandrocdrx.acertototal.dados.Cliente
import io.github.alessandrocdrx.acertototal.dados.Conta
import io.github.alessandrocdrx.acertototal.dominio.Dinheiro
import io.github.alessandrocdrx.acertototal.dominio.Documento
import io.github.alessandrocdrx.acertototal.dominio.brl
import io.github.alessandrocdrx.acertototal.dominio.dataCompleta
import io.github.alessandrocdrx.acertototal.planilha.Campos.fmtCel
import io.github.alessandrocdrx.acertototal.planilha.Campos.norm
import io.github.alessandrocdrx.acertototal.planilha.Campos.str
import io.github.alessandrocdrx.acertototal.planilha.Campos.vazio
import java.time.LocalDate

/** Uma linha da planilha já separada por campo, com a referência da célula (ex.: "E6") de cada um. */
data class LinhaDados(val linha: Int, val g: Map<CampoPlanilha, String>, val ref: Map<CampoPlanilha, String>, val bruto: List<String>)

/** [coluna] e [ref] vazios quando o problema é da linha toda (ex.: conta repetida). */
data class Problema(val coluna: String?, val ref: String, val texto: String)
data class LinhaProblema(val linha: Int, val nome: String, val problemas: List<Problema>, val bruto: List<String>)
data class LinhaAviso(val linha: Int, val nome: String, val problemas: List<Problema>)

/** Uma mudança de dado que já existe. [v] é o valor novo como texto: centavos, data ISO ou texto. */
data class MudancaCampo(val campo: String, val rot: String, val de: String, val para: String, val v: String)

class ClienteNovo(val id: String, val nome: String, val doc: String, var telefone: String, var email: String)
data class AtualizacaoCliente(val cliente: Cliente, val mud: MutableList<MudancaCampo> = mutableListOf())
data class ContaNova(
    val clienteId: String, val clienteNome: String, val descricao: String,
    val valor: Long, val venc: LocalDate, val pagoEm: LocalDate?,
    /** Código vindo da planilha (restaurar uma cópia). Null = o app cria um. */
    val id: String?,
)
data class AtualizacaoConta(val conta: Conta, val clienteNome: String, val mud: List<MudancaCampo>)
data class ContaIgual(val conta: Conta, val clienteNome: String)

/** Tudo de um cliente na importação, junto: o que entra e o que muda (R55). */
class GrupoCliente(val existente: Cliente?, val novoRef: ClienteNovo?, var novo: Boolean) {
    val nome: String get() = novoRef?.nome ?: existente?.nome.orEmpty()
    val doc: String get() = novoRef?.doc ?: existente?.doc.orEmpty()
    val telefone: String get() = novoRef?.telefone ?: existente?.telefone.orEmpty()
    val email: String get() = novoRef?.email ?: existente?.email.orEmpty()
    val mudCliente = mutableListOf<MudancaCampo>()
    val contasNovas = mutableListOf<ContaNova>()
    val contasMud = mutableListOf<AtualizacaoConta>()
}

class Plano(val arquivo: String, val soClientes: Boolean) {
    val clientesNovos = mutableListOf<ClienteNovo>()
    val clientesAtualizados = mutableListOf<AtualizacaoCliente>()
    val contasNovas = mutableListOf<ContaNova>()
    val contasAtualizadas = mutableListOf<AtualizacaoConta>()
    val semMudanca = mutableListOf<ContaIgual>()
    val erros = mutableListOf<LinhaProblema>()
    val avisos = mutableListOf<LinhaAviso>()
    val grupos = mutableListOf<GrupoCliente>()
    /** Contas novas que parecem mudança de uma conta que o app já tem (R83). */
    var parecemAtualizacao: List<ContaNova> = emptyList()
}

object Importacao {
    const val EX_DATA = "10/11/2026"

    /**
     * Monta o plano da importação sem gravar nada.
     * Linha com "Código da conta" atualiza aquela conta. Sem código, o CPF/CNPJ (se houver) ou o nome acha (ou cria)
     * o cliente, e a conta é achada por cliente + descrição + vencimento: assim reimportar não duplica (R38, R53).
     */
    fun planejar(
        linhas: List<LinhaDados>,
        arquivo: String,
        soClientes: Boolean,
        clientes: List<Cliente>,
        contas: List<Conta>,
        hoje: LocalDate,
        producao: Boolean,
        novoId: () -> String,
    ): Plano {
        val pl = Plano(arquivo, soClientes)
        val porDoc = mutableMapOf<String, Cliente>()
        val porNome = mutableMapOf<String, Cliente>()
        clientes.forEach { if (it.doc.isNotEmpty()) porDoc[it.doc] = it; porNome.putIfAbsent(norm(it.nome), it) }
        val clientePorId = clientes.associateBy { it.id }
        val contaPorId = contas.associateBy { it.id }
        val novos = mutableMapOf<String, ClienteNovo>()
        val vistas = mutableMapOf<String, Int>()
        val vistosCod = mutableMapOf<String, Int>()
        val grupos = mutableMapOf<String, GrupoCliente>()
        val cliAt = mutableMapOf<String, AtualizacaoCliente>()

        fun grupoExistente(cl: Cliente): GrupoCliente =
            grupos.getOrPut(cl.id) { GrupoCliente(cl, null, false).also { pl.grupos += it } }
        fun grupoNovo(nv: ClienteNovo): GrupoCliente =
            grupos.getOrPut(nv.id) { GrupoCliente(null, nv, true).also { pl.grupos += it } }

        fun valorAtual(cl: Cliente, campo: String) = when (campo) {
            "doc" -> cl.doc; "nome" -> cl.nome; "telefone" -> cl.telefone; "email" -> cl.email; else -> ""
        }
        fun mudarCliente(cl: Cliente, campo: String, rot: String, v: String, deTxt: String? = null, paraTxt: String? = null) {
            if (vazio(v)) return
            val atual = valorAtual(cl, campo)
            if (v == atual || (campo == "nome" && norm(v) == norm(atual))) return
            var at = cliAt[cl.id]
            if (at != null && at.mud.any { it.campo == campo }) return
            if (at == null) { at = AtualizacaoCliente(cl); cliAt[cl.id] = at; pl.clientesAtualizados += at }
            val m = MudancaCampo(campo, rot, deTxt ?: atual.ifEmpty { "(vazio)" }, paraTxt ?: v, v)
            at.mud += m
            grupoExistente(cl).mudCliente += m
        }
        fun baixaMud(ct: Conta, pg: LocalDate) = MudancaCampo(
            "pagoEm", "Baixa", if (ct.pagoEm != null) "paga em ${dataCompleta(ct.pagoEm)}" else "em aberto", "paga em ${dataCompleta(pg)}", pg.toString(),
        )

        linhas.forEach { L ->
            val g = L.g
            val probs = mutableListOf<Problema>()
            val avs = mutableListOf<Problema>()
            fun p(lista: MutableList<Problema>, k: CampoPlanilha, txt: String) { lista += Problema(k.rot, L.ref[k].orEmpty(), txt) }
            val nome = str(g[CampoPlanilha.NOME])
            val tel = str(g[CampoPlanilha.TELEFONE])
            var mail = str(g[CampoPlanilha.EMAIL])
            val cod = str(g[CampoPlanilha.CODIGO])
            val dr = if (vazio(g[CampoPlanilha.DOC])) null else LeituraDoc.ler(g[CampoPlanilha.DOC], producao)
            val tv = !vazio(g[CampoPlanilha.VALOR])
            val tvc = !vazio(g[CampoPlanilha.VENC])
            val tp = !vazio(g[CampoPlanilha.PAGO])
            var valor: Long? = null
            var venc: LocalDate? = null
            var pg: LocalDate? = null

            fun lerValor() {
                valor = Dinheiro.parse(g[CampoPlanilha.VALOR])
                val v = valor
                if (v == null) p(probs, CampoPlanilha.VALOR, "“${fmtCel(g[CampoPlanilha.VALOR])}” não é um valor. Use só números, por exemplo 150,00.")
                else if (v <= 0) p(probs, CampoPlanilha.VALOR, "“${fmtCel(g[CampoPlanilha.VALOR])}” precisa ser maior que zero.")
            }
            fun lerVenc() {
                venc = Datas.parse(g[CampoPlanilha.VENC])
                if (venc == null) p(probs, CampoPlanilha.VENC, "“${fmtCel(g[CampoPlanilha.VENC])}” não é uma data válida. Use dia/mês/ano, por exemplo $EX_DATA.")
            }
            fun lerPago() {
                pg = Datas.parse(g[CampoPlanilha.PAGO])
                val d = pg
                if (d == null) p(probs, CampoPlanilha.PAGO, "“${fmtCel(g[CampoPlanilha.PAGO])}” não é uma data válida. Use dia/mês/ano ou deixe vazio se não foi paga.")
                else if (d.isAfter(hoje)) p(probs, CampoPlanilha.PAGO, "${dataCompleta(d)} é depois de hoje. Se ainda não foi paga, deixe vazio.")
            }

            if (dr != null && !dr.ok) {
                p(probs, CampoPlanilha.DOC, "“${dr.raw}” não é um CPF nem um CNPJ válido. Confira os números: CPF tem 11, CNPJ tem 14.")
            }
            if (dr != null && dr.teste) p(avs, CampoPlanilha.DOC, "“${dr.raw}” ${Documento.AVISO_TESTE}")
            if (dr != null && dr.zeros) {
                p(avs, CampoPlanilha.DOC, "o Excel tirou o zero da frente (“${dr.raw}”). Usamos ${Documento.formatar(dr.d)}. Para não acontecer de novo, formate a coluna como Texto.")
            }
            if (mail.isNotEmpty() && !Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(mail)) {
                p(avs, CampoPlanilha.EMAIL, "“$mail” não parece um e-mail. O cliente fica sem esse e-mail.")
                mail = ""
            }
            if (tel.isNotEmpty() && tel.count { it.isDigit() } < 8) {
                p(avs, CampoPlanilha.TELEFONE, "“$tel” tem poucos dígitos. Entra assim mesmo; confira depois.")
            }
            fun fimErro(nm: String? = null) { pl.erros += LinhaProblema(L.linha, nm ?: nome, probs.toList(), L.bruto) }

            // 1) Linha com código: atualiza a conta que já existe.
            val ct = if (cod.isNotEmpty()) contaPorId[cod] else null
            // Código que não existe no app (ex.: restaurar a cópia num celular novo): com nome ou CPF/CNPJ, a conta é criada com o mesmo código.
            if (cod.isNotEmpty() && ct == null && !(dr != null && dr.ok) && nome.isEmpty()) {
                p(probs, CampoPlanilha.CODIGO, "“$cod” não existe no app e a linha não tem nome nem CPF/CNPJ para criar a conta. Se é uma conta nova, apague o código e preencha o nome do cliente.")
                return@forEach fimErro()
            }
            if (ct != null) {
                val dono = clientePorId[ct.clienteId] ?: Cliente(ct.clienteId, "(sem cliente)")
                vistosCod[cod]?.let { probs += Problema(null, "", "Código repetido: esta conta já está na linha $it. Deixe só uma.") }
                if (dr != null && dr.ok && dono.doc.isNotEmpty() && dono.doc != dr.d) {
                    p(probs, CampoPlanilha.DOC, "não é do dono desta conta. A conta $cod é de ${dono.nome} (${Documento.formatar(dono.doc)}). Para trocar o cliente de uma conta, use o app.")
                }
                if (tv) lerValor()
                if (tvc) lerVenc()
                if (tp) lerPago()
                if (probs.isNotEmpty()) return@forEach fimErro(dono.nome)
                vistosCod[cod] = L.linha
                if (avs.isNotEmpty()) pl.avisos += LinhaAviso(L.linha, dono.nome, avs.toList())
                if (dr != null && dr.ok && dono.doc.isEmpty() && dr.d !in porDoc) {
                    mudarCliente(dono, "doc", "CPF/CNPJ", dr.d, "(vazio)", Documento.formatar(dr.d))
                    porDoc[dr.d] = dono
                }
                mudarCliente(dono, "nome", "Nome", nome)
                mudarCliente(dono, "telefone", "Telefone", tel)
                mudarCliente(dono, "email", "E-mail", mail)
                val mud = mutableListOf<MudancaCampo>()
                val dsc = str(g[CampoPlanilha.DESCRICAO])
                if (dsc.isNotEmpty() && dsc != ct.descricao) mud += MudancaCampo("descricao", "Descrição", ct.descricao, dsc, dsc)
                val v = valor
                if (v != null && v != ct.valor) mud += MudancaCampo("valor", "Valor", brl(ct.valor), brl(v), v.toString())
                val vc = venc
                if (vc != null && vc != ct.venc) mud += MudancaCampo("venc", "Vencimento", dataCompleta(ct.venc), dataCompleta(vc), vc.toString())
                val pd = pg
                if (pd != null && pd != ct.pagoEm) mud += baixaMud(ct, pd)
                if (mud.isNotEmpty()) {
                    val a = AtualizacaoConta(ct, dono.nome, mud)
                    pl.contasAtualizadas += a
                    grupoExistente(dono).contasMud += a
                } else pl.semMudanca += ContaIgual(ct, dono.nome)
                return@forEach
            }

            // 2) Sem código: o CPF/CNPJ diz quem é o cliente.
            var cl: Cliente? = null
            var nv: ClienteNovo? = null
            if (dr == null) {
                // Sem CPF/CNPJ na linha: o nome diz quem é o cliente.
                if (nome.isEmpty()) p(probs, CampoPlanilha.NOME, "está vazio. Informe o nome do cliente (ou o CPF/CNPJ).")
                else {
                    cl = porNome[norm(nome)]
                    nv = novos["n:" + norm(nome)]
                }
            } else if (dr.ok) {
                cl = porDoc[dr.d]
                nv = novos[dr.d]
                if (cl == null && nv == null && nome.isNotEmpty()) cl = porNome[norm(nome)]?.takeIf { it.doc.isEmpty() }
            }
            if (dr != null && dr.ok && cl == null && nv == null && nome.isEmpty()) p(probs, CampoPlanilha.NOME, "está vazio. Cliente novo precisa de nome.")
            val temConta = !soClientes && (tv || tvc || tp)
            if (temConta) {
                if (!tv) p(probs, CampoPlanilha.VALOR, "está vazio. Preencha o valor, por exemplo 150,00.") else lerValor()
                if (!tvc) p(probs, CampoPlanilha.VENC, "está vazio. Preencha a data, por exemplo $EX_DATA.") else lerVenc()
                if (tp) lerPago()
            }
            if (probs.isNotEmpty()) return@forEach fimErro()
            val docN = if (dr != null && dr.ok) dr.d else ""
            val desc = str(g[CampoPlanilha.DESCRICAO]).ifEmpty { "Conta" }
            val chave = (docN.ifEmpty { "n:" + norm(nome) }) + "|" + norm(desc) + "|" + venc
            if (temConta && vistas[chave] != null) {
                probs += Problema(null, "", "Conta repetida: igual à linha ${vistas[chave]} (mesmo cliente, descrição e vencimento). Apague uma das duas.")
                return@forEach fimErro()
            }
            if (cl != null && nome.isNotEmpty() && norm(nome) != norm(cl.nome)) {
                p(avs, CampoPlanilha.NOME, "este ${if (docN.isNotEmpty()) Documento.tipo(docN) else "cliente"} é de “${cl.nome}” no app. Se você atualizar, o nome muda para “$nome”. Confira se o número está certo.")
            }
            if (avs.isNotEmpty()) pl.avisos += LinhaAviso(L.linha, nome.ifEmpty { cl?.nome.orEmpty() }, avs.toList())
            if (cl == null && nv == null) {
                nv = ClienteNovo(novoId(), nome, docN, tel, mail)
                novos[docN.ifEmpty { "n:" + norm(nome) }] = nv
                pl.clientesNovos += nv
                grupoNovo(nv)
            } else if (nv != null) {
                if (tel.isNotEmpty() && nv.telefone.isEmpty()) nv.telefone = tel
                if (mail.isNotEmpty() && nv.email.isEmpty()) nv.email = mail
            } else if (cl != null) {
                if (docN.isNotEmpty() && cl.doc.isEmpty()) {
                    mudarCliente(cl, "doc", "CPF/CNPJ", docN, "(vazio)", Documento.formatar(docN))
                    porDoc[docN] = cl
                }
                mudarCliente(cl, "nome", "Nome", nome)
                mudarCliente(cl, "telefone", "Telefone", tel)
                mudarCliente(cl, "email", "E-mail", mail)
            }
            if (!temConta) return@forEach
            vistas[chave] = L.linha
            val donoId = cl?.id ?: nv!!.id
            val donoNome = cl?.nome ?: nv!!.nome
            val vv = valor!!
            val vc = venc!!
            val ex = cl?.let { c0 -> contas.firstOrNull { it.clienteId == c0.id && norm(it.descricao) == norm(desc) && it.venc == vc } }
            if (ex == null) {
                val cn = ContaNova(donoId, donoNome, desc, vv, vc, pg, if (cod.isNotEmpty() && vistosCod[cod] == null) cod else null)
                if (cod.isNotEmpty()) vistosCod[cod] = L.linha
                pl.contasNovas += cn
                (if (cl != null) grupoExistente(cl) else grupoNovo(nv!!)).contasNovas += cn
                return@forEach
            }
            val md = mutableListOf<MudancaCampo>()
            if (ex.valor != vv) md += MudancaCampo("valor", "Valor", brl(ex.valor), brl(vv), vv.toString())
            val pd = pg
            if (pd != null && pd != ex.pagoEm) md += baixaMud(ex, pd)
            if (md.isNotEmpty()) {
                val a = AtualizacaoConta(ex, cl!!.nome, md)
                pl.contasAtualizadas += a
                grupoExistente(cl).contasMud += a
            } else pl.semMudanca += ContaIgual(ex, cl!!.nome)
        }

        // "Parece atualização": o cliente já tem uma conta em aberto com a mesma descrição, mas a linha veio sem código e com outro vencimento.
        // Quem mudou o vencimento no Excel e importou (em vez de "Atualizar pelo Excel") cria uma conta nova ao lado da antiga.
        pl.parecemAtualizacao = pl.contasNovas.filter { cn ->
            cn.id == null && contas.any { it.clienteId == cn.clienteId && it.pagoEm == null && norm(it.descricao) == norm(cn.descricao) && it.venc != cn.venc }
        }
        return pl
    }

    fun contaAnterior(cn: ContaNova, contas: List<Conta>): Conta? =
        contas.firstOrNull { it.clienteId == cn.clienteId && it.pagoEm == null && norm(it.descricao) == norm(cn.descricao) && it.venc != cn.venc }

    // ------------------------------------------------------------------ texto da prévia e do histórico

    fun totalMudancas(pl: Plano, novos: Boolean, atualizar: Boolean): Int =
        (if (novos) pl.clientesNovos.size + pl.contasNovas.size else 0) +
            (if (atualizar) pl.clientesAtualizados.size + pl.contasAtualizadas.size else 0)

    /** Texto do botão: diz o que vai ser gravado ("1 cliente e 8 contas", "5 atualizações"). */
    fun textoMudancas(pl: Plano, novos: Boolean, atualizar: Boolean): String {
        val c = if (novos) pl.clientesNovos.size else 0
        val k = if (novos) pl.contasNovas.size else 0
        val a = if (atualizar) pl.clientesAtualizados.size + pl.contasAtualizadas.size else 0
        val p = mutableListOf<String>()
        if (c > 0) p += "$c cliente" + if (c > 1) "s" else ""
        if (k > 0) p += "$k conta" + if (k > 1) "s" else ""
        if (a > 0) p += "$a " + if (a > 1) "atualizações" else "atualização"
        return if (p.size > 1) p.dropLast(1).joinToString(", ") + " e " + p.last() else p.firstOrNull().orEmpty()
    }

    private fun docTxt(doc: String) = if (doc.isNotEmpty()) " · ${Documento.formatar(doc)}" else ""

    /** Transforma o plano em itens de texto, agrupados por cliente: os mesmos na prévia e no histórico. */
    fun itens(pl: Plano, novos: Boolean, atualizar: Boolean): List<ItemPlano> {
        val it = mutableListOf<ItemPlano>()
        fun prob(x: List<Problema>) = x.map { p ->
            LinhaItem.Prob(if (p.coluna != null) p.coluna + (if (p.ref.isNotEmpty()) " (${p.ref})" else "") + ":" else "", p.texto)
        }
        pl.grupos.forEach { g ->
            if (atualizar && (g.mudCliente.isNotEmpty() || g.contasMud.isNotEmpty())) {
                val l = mutableListOf<LinhaItem>()
                g.mudCliente.forEach { m -> l += LinhaItem.Mudanca(m.rot, m.de, m.para) }
                g.contasMud.forEach { x ->
                    l += LinhaItem.Sub("Conta: ${x.conta.descricao} · vence ${dataCompleta(x.conta.venc)}")
                    x.mud.forEach { m -> l += LinhaItem.Mudanca(m.rot, m.de, m.para) }
                }
                it += ItemPlano(TipoItem.ATUALIZADO, g.nome + docTxt(g.doc), l)
            }
            if (novos && (g.novo || g.contasNovas.isNotEmpty())) {
                val l = mutableListOf<LinhaItem>()
                if (g.novo) l += LinhaItem.Texto("Cliente novo · " + listOf(g.telefone, g.email).filter { x -> x.isNotEmpty() }.joinToString(" · ").ifEmpty { "sem contato" })
                if (g.contasNovas.isNotEmpty()) l += LinhaItem.Texto("${g.contasNovas.size} " + if (g.contasNovas.size > 1) "contas novas:" else "conta nova:")
                g.contasNovas.forEach { c ->
                    l += LinhaItem.Texto("+ ${c.descricao} · ${brl(c.valor)} · vence ${dataCompleta(c.venc)}" + (c.pagoEm?.let { d -> " · paga em ${dataCompleta(d)}" } ?: ""))
                }
                it += ItemPlano(TipoItem.NOVO, (if (g.novo) "Cliente novo: " else "") + g.nome + docTxt(g.doc), l)
            }
        }
        pl.erros.forEach { e -> it += ItemPlano(TipoItem.ERRO, "Linha ${e.linha}" + if (e.nome.isNotEmpty()) " · ${e.nome}" else "", prob(e.problemas)) }
        pl.avisos.forEach { e -> it += ItemPlano(TipoItem.AVISO, "Linha ${e.linha}" + if (e.nome.isNotEmpty()) " · ${e.nome}" else "", prob(e.problemas)) }
        return it
    }
}

enum class TipoItem { ATUALIZADO, NOVO, ERRO, AVISO }

sealed interface LinhaItem {
    data class Texto(val t: String) : LinhaItem
    data class Sub(val t: String) : LinhaItem
    /** Riscado é como está; em negrito é como vai ficar. */
    data class Mudanca(val rot: String, val de: String, val para: String) : LinhaItem
    data class Prob(val col: String, val txt: String) : LinhaItem
}

data class ItemPlano(val tipo: TipoItem, val titulo: String, val linhas: List<LinhaItem>)
