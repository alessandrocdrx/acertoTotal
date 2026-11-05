package io.github.alessandrocdrx.acertototal.planilha

import java.text.Normalizer

/** Quando o campo é preciso: sempre, só para cliente novo, só para criar conta, ou só na planilha para editar. */
enum class Req(val texto: String?) {
    SIM("obrigatória"), NOVO("cliente novo"), CONTA("para contas"), EDITAR("para atualizar"), NENHUM(null)
}

/**
 * Campos do app e os nomes de coluna que cada um reconhece (sem acento, minúsculo).
 * Portado de CAMPOS do protótipo.
 */
enum class CampoPlanilha(
    val rot: String,
    val rotL: String,
    val req: Req,
    val exemplo: String,
    val dica: String?,
    val sinonimos: List<String>,
    val contem: List<String>,
) {
    DOC(
        "CPF/CNPJ", "CPF ou CNPJ", Req.NENHUM, "123.456.789-09", "Opcional. Sem ele, o cliente é reconhecido pelo nome.",
        listOf("cpf/cnpj", "cpf cnpj", "cpf", "cnpj", "cpf ou cnpj", "cpf/cnpj do cliente", "documento", "doc"), listOf("cpf", "cnpj"),
    ),
    NOME(
        "Nome", "Nome do cliente", Req.NOVO, "Marina Alves", null,
        listOf("nome", "cliente", "nome do cliente", "nome completo", "razao social", "devedor", "pagador"), listOf("nome", "cliente"),
    ),
    TELEFONE(
        "Telefone", "Telefone", Req.NENHUM, "(11) 98888-7777", null,
        listOf("telefone", "celular", "whatsapp", "fone", "tel", "contato"), listOf("telefone", "fone", "celular", "whats"),
    ),
    EMAIL("E-mail", "E-mail", Req.NENHUM, "marina@exemplo.com", null, listOf("e-mail", "email", "e mail"), listOf("mail")),
    DESCRICAO(
        "Descrição", "Descrição", Req.NENHUM, "Mensalidade", "Se ficar vazia, a conta entra como “Conta”.",
        listOf("descricao", "referencia", "servico", "produto", "historico", "detalhe", "item"), listOf("descri", "referen", "servi"),
    ),
    VALOR("Valor", "Valor", Req.CONTA, "150,00", null, listOf("valor", "total", "preco", "quantia"), listOf("valor", "preco")),
    VENC(
        "Vencimento", "Vencimento", Req.CONTA, "10/11/2026", "Dia/mês/ano.",
        listOf("vencimento", "data de vencimento", "vence", "venc", "vencto", "data venc"), listOf("venc"),
    ),
    PAGO(
        "Pago em", "Pago em", Req.NENHUM, "05/11/2026", "Data do pagamento. Preencher dá baixa; vazio não muda nada.",
        listOf("pago em", "data de pagamento", "data do pagamento", "pagamento", "pago", "quitado em", "data pagamento"),
        listOf("pago", "pagamento", "quitad"),
    ),
    CODIGO(
        "Código da conta", "Código da conta", Req.EDITAR, "k3f9a2b", "Vem na planilha para editar. Não altere.",
        listOf("codigo da conta", "codigo", "cod", "id", "id da conta"), listOf("codigo"),
    ),
}

/** Uma coluna da planilha: posição, título, letra, o campo que o app reconheceu e um exemplo. */
data class Coluna(val j: Int, val titulo: String, val letra: String, val campo: CampoPlanilha?, val exemplo: String)

/** Uma linha de dados com o número que ela tem no arquivo (o mesmo que o Excel mostra). */
data class LinhaArq(val linha: Int, val v: List<String>)

/** Uma aba em que o app achou os títulos. */
data class AbaAnalisada(
    val nome: String,
    val linhaCab: Int,
    val cols: List<Coluna>,
    val linhas: List<LinhaArq>,
    val score: Int,
)

object Campos {
    fun norm(t: String?): String =
        Normalizer.normalize(t.orEmpty(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")
            .lowercase().replace(Regex("\\s+"), " ").trim()

    fun vazio(v: String?): Boolean = v == null || v.trim().isEmpty()

    /** Texto limpo: sem espaços nas pontas e sem espaços repetidos. */
    fun str(v: String?): String = if (vazio(v)) "" else v!!.trim().replace(Regex("\\s+"), " ")

    /** Como a célula aparece num exemplo: número com vírgula decimal. */
    fun fmtCel(v: String?): String {
        val s = v.orEmpty().trim()
        return if (Regex("^-?\\d+\\.\\d+$").matches(s)) s.replace('.', ',') else s
    }

    private fun normCab(t: String?): String =
        norm(t).replace(Regex("[^a-z0-9 \\-]"), " ").replace(Regex("\\s+"), " ").trim()

    private val ORDEM_TEM = listOf(
        CampoPlanilha.CODIGO, CampoPlanilha.DOC, CampoPlanilha.VENC, CampoPlanilha.VALOR, CampoPlanilha.PAGO,
        CampoPlanilha.EMAIL, CampoPlanilha.TELEFONE, CampoPlanilha.DESCRICAO, CampoPlanilha.NOME,
    )

    /** Reconhece o campo pelo título: primeiro igual a um nome conhecido, depois "contém" um pedaço. */
    fun campoDoTitulo(t: String?): CampoPlanilha? {
        val n = normCab(t)
        if (n.isEmpty()) return null
        CampoPlanilha.entries.firstOrNull { c -> c.sinonimos.any { normCab(it) == n } }?.let { return it }
        if (n.length > 40) return null // frase longa não é título de coluna
        return ORDEM_TEM.firstOrNull { c -> c.contem.any { n.contains(it) } }
    }

    /** Acha a linha de títulos nas 15 primeiras linhas: a que reconhece mais campos. Null se não achou. */
    fun analisarAba(nome: String, aoa: List<List<String>>): AbaAnalisada? {
        var cab = -1
        var score = 0
        for (i in 0 until minOf(15, aoa.size)) {
            val vistos = mutableSetOf<CampoPlanilha>()
            aoa[i].forEach { t -> campoDoTitulo(t)?.let { vistos += it } }
            if (vistos.size > score && (vistos.size >= 2 || CampoPlanilha.NOME in vistos)) {
                score = vistos.size
                cab = i
            }
        }
        if (cab < 0) return null
        val largura = aoa.maxOfOrNull { it.size } ?: 0
        val linhas = (cab + 1 until aoa.size).mapNotNull { r ->
            val row = aoa[r]
            if (row.any { !vazio(it) }) LinhaArq(r + 1, row) else null
        }
        val cols = (0 until largura).mapNotNull { j ->
            val tit = str(aoa[cab].getOrNull(j))
            val ex = linhas.take(30).firstNotNullOfOrNull { l -> l.v.getOrNull(j)?.takeIf { !vazio(it) } }?.let(::fmtCel).orEmpty()
            if (tit.isNotEmpty() || ex.isNotEmpty()) Coluna(j, tit, Xlsx.coluna(j), campoDoTitulo(tit), ex) else null
        }
        return AbaAnalisada(nome, cab + 1, cols, linhas, score)
    }

    /** Cada campo na primeira coluna que o app reconheceu para ele. */
    fun autoMapa(cols: List<Coluna>): Map<CampoPlanilha, Int> {
        val m = mutableMapOf<CampoPlanilha, Int>()
        cols.forEach { c -> c.campo?.let { if (it !in m) m[it] = c.j } }
        return m
    }

    /** Sem valor, vencimento nem pagamento mapeados: só os clientes são importados. */
    fun soClientes(m: Map<CampoPlanilha, Int>): Boolean =
        CampoPlanilha.VALOR !in m && CampoPlanilha.VENC !in m && CampoPlanilha.PAGO !in m

    /** Confere o mapa que a pessoa escolheu. Devolve o campo com problema e o que dizer a ele, ou null se está certo. */
    fun validarMapa(m: Map<CampoPlanilha, Int>): Pair<CampoPlanilha, String>? {
        val cod = CampoPlanilha.CODIGO in m
        return when {
            CampoPlanilha.NOME !in m && !cod -> CampoPlanilha.NOME to "Escolha a coluna com o nome do cliente"
            !cod && CampoPlanilha.VALOR !in m && CampoPlanilha.VENC in m ->
                CampoPlanilha.VALOR to "Para importar contas, escolha também a coluna do valor"
            !cod && CampoPlanilha.VENC !in m && CampoPlanilha.VALOR in m ->
                CampoPlanilha.VENC to "Para importar contas, escolha também a coluna do vencimento"
            else -> null
        }
    }

    /** Separa cada linha da aba por campo, guardando a célula de origem de cada valor (ex.: "E6"). */
    fun linhasDoMapa(aba: AbaAnalisada, mapa: Map<CampoPlanilha, Int>): List<LinhaDados> {
        val letra = aba.cols.associate { it.j to it.letra }
        return aba.linhas.map { l ->
            val g = mutableMapOf<CampoPlanilha, String>()
            val ref = mutableMapOf<CampoPlanilha, String>()
            mapa.forEach { (campo, j) ->
                g[campo] = l.v.getOrNull(j).orEmpty()
                ref[campo] = (letra[j] ?: Xlsx.coluna(j)) + l.linha
            }
            LinhaDados(l.linha, g, ref, l.v)
        }
    }

    /** Explica o que falta no mapa de colunas, ou devolve vazio se está tudo certo. */
    fun faltaColuna(m: Map<CampoPlanilha, Int>): String {
        val cod = CampoPlanilha.CODIGO in m
        val nome = CampoPlanilha.NOME in m
        val valor = CampoPlanilha.VALOR in m
        val venc = CampoPlanilha.VENC in m
        return when {
            !nome && !cod -> "Não achei a coluna com o nome do cliente. Escolha abaixo qual é."
            !cod && valor != venc ->
                "Achei a coluna " + (if (!valor) "do vencimento, mas não a do valor" else "do valor, mas não a do vencimento") +
                    ". Escolha abaixo. Para importar só clientes, marque “Não usar” nas duas."
            else -> ""
        }
    }
}
