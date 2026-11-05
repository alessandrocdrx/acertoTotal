package io.github.alessandrocdrx.acertototal.planilha

import io.github.alessandrocdrx.acertototal.dominio.dataCompleta
import java.time.LocalDate

/** Planilhas que o app gera para ajudar a importar: o modelo, um exemplo e as linhas que ficaram de fora. */
object Modelos {
    private val TITULOS_MODELO = listOf("CPF/CNPJ", "Nome", "Telefone", "E-mail", "Descrição", "Valor", "Vencimento", "Pago em")

    /** Abas do arquivo "modelo": os títulos prontos e uma aba que explica cada coluna. */
    fun modelo(): List<AbaXlsx> {
        val como = mutableListOf(listOf("Coluna", "Precisa?", "O que colocar"))
        CampoPlanilha.entries.filter { it != CampoPlanilha.CODIGO }.forEach { c ->
            val precisa = when (c.req) {
                Req.SIM -> "Sim"
                Req.NOVO -> "Sim, para cliente novo"
                Req.CONTA -> "Sim, para criar conta"
                else -> "Não"
            }
            como += listOf(c.rotL, precisa, "Exemplo: ${c.exemplo}" + (c.dica?.let { ". $it" } ?: ""))
        }
        como += listOf(listOf(""))
        como += listOf(listOf("Cada linha é uma conta. Um cliente com várias contas ocupa várias linhas com o mesmo CPF/CNPJ."))
        como += listOf(listOf("Para mudar contas que já existem, use no app: Planilha › Atualizar pelo Excel."))
        return listOf(AbaXlsx("Contas", listOf(TITULOS_MODELO)), AbaXlsx("Como preencher", como))
    }

    /** "Testar com a planilha de exemplo": 3 contas de uma cliente, uma oficina e um cliente com parcela. */
    fun exemplo(hoje: LocalDate): AbaXlsx {
        fun d(dias: Long) = dataCompleta(hoje.plusDays(dias))
        return AbaXlsx(
            "Exemplo",
            listOf(
                listOf("CPF/CNPJ", "Nome", "Telefone", "E-mail", "Descrição", "Valor", "Vencimento"),
                listOf("193.573.925-56", "Joana Prado", "(11) 97777-1111", "joana@exemplo.com", "Mensalidade", "200", d(-8)),
                listOf("193.573.925-56", "Joana Prado", "", "", "Mensalidade", "200", d(22)),
                listOf("193.573.925-56", "Joana Prado", "", "", "Mensalidade", "200", d(52)),
                listOf("85.761.863/0001-34", "Oficina Três Irmãos", "(11) 4002-8922", "", "Revisão de frota", "2300", d(-2)),
                listOf("932.462.921-24", "Tiago Moreira", "(19) 98111-2020", "", "Parcela 1/3", "350", d(5)),
            ),
        )
    }

    /**
     * As linhas que ficaram de fora, com as mesmas colunas do arquivo e mais "Linha no arquivo" e "Problema" (R51).
     * A pessoa corrige ali e importa de novo; o que já entrou não se repete e as duas colunas extras são ignoradas.
     */
    fun linhasComProblema(aba: AbaAnalisada, erros: List<LinhaProblema>): AbaXlsx {
        val cabecalho = listOf("Linha no arquivo", "Problema") + aba.cols.map { it.titulo.ifEmpty { it.letra } }
        val linhas = erros.map { e ->
            val problema = e.problemas.joinToString(" | ") { p ->
                (if (p.coluna != null) p.coluna + (if (p.ref.isNotEmpty()) " (${p.ref})" else "") + ": " else "") + p.texto
            }
            listOf(e.linha.toString(), problema) + aba.cols.map { e.bruto.getOrNull(it.j).orEmpty() }
        }
        return AbaXlsx("Com problema", listOf(cabecalho) + linhas)
    }

    /** Nome do arquivo das linhas com problema: "linhas-com-problema-<nome do arquivo>". */
    fun nomeLinhasComProblema(arquivo: String): String =
        "linhas-com-problema-" + (Exportacao.slug(arquivo.substringBeforeLast('.', arquivo)).ifEmpty { "planilha" })
}
