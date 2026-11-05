package io.github.alessandrocdrx.acertototal.planilha

import io.github.alessandrocdrx.acertototal.dados.Cliente
import io.github.alessandrocdrx.acertototal.dados.Conta
import io.github.alessandrocdrx.acertototal.dominio.Dinheiro
import io.github.alessandrocdrx.acertototal.dominio.Documento
import io.github.alessandrocdrx.acertototal.dominio.Situacao
import io.github.alessandrocdrx.acertototal.dominio.dataCompleta
import io.github.alessandrocdrx.acertototal.dominio.diasEntre
import io.github.alessandrocdrx.acertototal.dominio.situacaoDe
import java.text.Normalizer
import java.time.LocalDate

/**
 * Exportar tem dois formatos, com nomes pelo uso (R54):
 * "Relatório" para ler ou mandar a alguém, "Para editar" para voltar pela importação.
 */
enum class FormatoExport { RELATORIO, EDITAR }

enum class StatusExport(val rotulo: String) {
    TODAS("Todas"),
    ABERTAS("Em aberto"),
    ATRASADAS("Atrasadas"),
    PAGAS("Pagas"),
}

data class FiltrosExport(
    val formato: FormatoExport = FormatoExport.RELATORIO,
    val status: StatusExport = StatusExport.ATRASADAS,
    val clienteId: String? = null,
    val de: LocalDate? = null,
    val ate: LocalDate? = null,
) {
    /** Os filtros abertos como a tela os oferece: "Limpar filtros" só aparece quando mudou algo. */
    val padrao: Boolean get() = status == StatusExport.ATRASADAS && clienteId == null && de == null && ate == null

    /** A data final antes da inicial não exporta nada: a tela pede para corrigir. */
    val periodoInvertido: Boolean get() = de != null && ate != null && de.isAfter(ate)
}

/** Planilha pronta para mostrar na prévia e gravar. */
data class PlanilhaPronta(
    val titulo: String,
    /** Nome base do arquivo, sem a data e sem a extensão. */
    val arquivo: String,
    val cabecalho: List<String>,
    val linhas: List<List<String>>,
    val extras: List<AbaXlsx> = emptyList(),
    /** Os filtros escritos por extenso (R34). Vazio na cópia de segurança. */
    val filtros: String = "",
) {
    fun abas(): List<AbaXlsx> = listOf(AbaXlsx("Contas", listOf(cabecalho) + linhas)) + extras

    /** Para "Copiar para colar no Excel": colunas separadas por tabulação. */
    fun tsv(): String = (listOf(cabecalho) + linhas).joinToString("\n") { it.joinToString("\t") }
}

object Exportacao {
    val CABECALHO_EDITAR = listOf("Código da conta", "CPF/CNPJ", "Nome", "Telefone", "E-mail", "Descrição", "Valor", "Vencimento", "Pago em")
    val CABECALHO_RELATORIO = listOf("Cliente", "CPF/CNPJ", "Telefone", "Descrição", "Valor", "Vencimento", "Situação", "Pago em", "Dias de atraso")

    val COMO_EDITAR = listOf(
        listOf("Como atualizar pela planilha"),
        listOf("1. Na aba anterior, mude o valor, o vencimento, a descrição ou o “Pago em” (data do pagamento, para dar baixa)."),
        listOf("2. Não mude a coluna “Código da conta”: ela liga cada linha à conta no app."),
        listOf("3. Para incluir uma conta nova, use uma linha com o código vazio e o CPF/CNPJ preenchido."),
        listOf("4. Célula vazia não apaga nada no app."),
        listOf("5. No app, abra Planilha › Enviar planilha editada. Você vê o antes e o depois de cada mudança antes de gravar."),
    )

    fun slug(texto: String): String =
        Normalizer.normalize(texto, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")
            .lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')

    /** Aplica os filtros. Para editar, as contas de cada cliente ficam juntas; no relatório, por vencimento. */
    fun contasFiltradas(contas: List<Conta>, clientes: List<Cliente>, f: FiltrosExport, hoje: LocalDate): List<Conta> {
        val nomes = clientes.associate { it.id to it.nome.lowercase() }
        val lista = contas.filter { c ->
            val s = situacaoDe(c.venc, c.pagoEm, hoje)
            when (f.status) {
                StatusExport.TODAS -> true
                StatusExport.ABERTAS -> s != Situacao.PAGA
                StatusExport.ATRASADAS -> s == Situacao.ATRASADA
                StatusExport.PAGAS -> s == Situacao.PAGA
            } && (f.clienteId == null || c.clienteId == f.clienteId) &&
                (f.de == null || !c.venc.isBefore(f.de)) && (f.ate == null || !c.venc.isAfter(f.ate))
        }
        return if (f.formato == FormatoExport.EDITAR) lista.sortedWith(compareBy({ nomes[it.clienteId].orEmpty() }, { it.venc }))
        else lista.sortedBy { it.venc }
    }

    fun resumoFiltros(f: FiltrosExport, clientes: List<Cliente>): String {
        val partes = mutableListOf(f.status.rotulo)
        f.clienteId?.let { id -> partes += clientes.firstOrNull { it.id == id }?.nome ?: "cliente" }
        if (f.de != null || f.ate != null) {
            partes += "vencimento" + (f.de?.let { " de ${dataCompleta(it)}" } ?: "") + (f.ate?.let { " até ${dataCompleta(it)}" } ?: "")
        }
        return partes.joinToString(" · ")
    }

    private fun dataOuVazio(d: LocalDate?): String = d?.let(::dataCompleta).orEmpty()

    fun linhaEditar(c: Conta, cl: Cliente?): List<String> = listOf(
        c.id, Documento.formatar(cl?.doc), cl?.nome.orEmpty(), cl?.telefone.orEmpty(), cl?.email.orEmpty(),
        c.descricao, Dinheiro.paraCampo(c.valor), dataCompleta(c.venc), dataOuVazio(c.pagoEm),
    )

    fun linhaRelatorio(c: Conta, cl: Cliente?, hoje: LocalDate): List<String> {
        val s = situacaoDe(c.venc, c.pagoEm, hoje)
        return listOf(
            cl?.nome.orEmpty(), Documento.formatar(cl?.doc), cl?.telefone.orEmpty(), c.descricao,
            Dinheiro.paraCampo(c.valor), dataCompleta(c.venc), s.rotulo, dataOuVazio(c.pagoEm),
            if (s == Situacao.ATRASADA) diasEntre(c.venc, hoje).toString() else "",
        )
    }

    fun relatorioOuEditar(clientes: List<Cliente>, contas: List<Conta>, f: FiltrosExport, hoje: LocalDate): PlanilhaPronta {
        val porId = clientes.associateBy { it.id }
        val lista = contasFiltradas(contas, clientes, f, hoje)
        val base = slug(f.status.rotulo) + (f.clienteId?.let { id -> porId[id]?.nome?.let { "-" + slug(it) } } ?: "")
        return if (f.formato == FormatoExport.EDITAR) {
            PlanilhaPronta(
                titulo = "Planilha para editar", arquivo = "editar-contas-$base",
                cabecalho = CABECALHO_EDITAR, linhas = lista.map { linhaEditar(it, porId[it.clienteId]) },
                extras = listOf(AbaXlsx("Como editar", COMO_EDITAR)), filtros = resumoFiltros(f, clientes),
            )
        } else {
            PlanilhaPronta(
                titulo = "Relatório de contas", arquivo = "contas-$base",
                cabecalho = CABECALHO_RELATORIO, linhas = lista.map { linhaRelatorio(it, porId[it.clienteId], hoje) },
                filtros = resumoFiltros(f, clientes),
            )
        }
    }

    /** Cópia de segurança = planilha para editar com tudo, inclusive clientes sem contas. Importar de volta restaura. */
    fun copiaDeSeguranca(clientes: List<Cliente>, contas: List<Conta>): PlanilhaPronta {
        val porCliente = contas.groupBy { it.clienteId }
        val linhas = clientes.sortedBy { it.nome.lowercase() }.flatMap { cl ->
            val cs = porCliente[cl.id].orEmpty().sortedBy { it.venc }
            if (cs.isEmpty()) listOf(listOf("", Documento.formatar(cl.doc), cl.nome, cl.telefone, cl.email, "", "", "", ""))
            else cs.map { linhaEditar(it, cl) }
        }
        return PlanilhaPronta(
            titulo = "Cópia de segurança", arquivo = "backup-acertototal",
            cabecalho = CABECALHO_EDITAR, linhas = linhas, extras = listOf(AbaXlsx("Como editar", COMO_EDITAR)),
        )
    }
}
