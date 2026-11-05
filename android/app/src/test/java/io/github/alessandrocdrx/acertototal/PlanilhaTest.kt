package io.github.alessandrocdrx.acertototal

import io.github.alessandrocdrx.acertototal.dados.Cliente
import io.github.alessandrocdrx.acertototal.dados.Conta
import io.github.alessandrocdrx.acertototal.planilha.AbaXlsx
import io.github.alessandrocdrx.acertototal.planilha.Exportacao
import io.github.alessandrocdrx.acertototal.planilha.FiltrosExport
import io.github.alessandrocdrx.acertototal.planilha.FormatoExport
import io.github.alessandrocdrx.acertototal.planilha.StatusExport
import io.github.alessandrocdrx.acertototal.planilha.Xlsx
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class XlsxTest {
    @Test fun idaEVoltaPreservaTextosAcentosEBrancos() {
        val abas = listOf(
            AbaXlsx("Contas", listOf(listOf("Nome", "Valor", "Obs"), listOf("João & <Cia>", "1.234,56", ""), listOf("", "", "fim"))),
            AbaXlsx("Como editar", listOf(listOf("Passo 1"), listOf("Passo 2"))),
        )
        val lido = Xlsx.ler(Xlsx.escrever(abas))
        assertEquals(listOf("Contas", "Como editar"), lido.map { it.nome })
        assertEquals(listOf("Nome", "Valor", "Obs"), lido[0].linhas[0])
        assertEquals(listOf("João & <Cia>", "1.234,56"), lido[0].linhas[1]) // célula vazia no fim some, sem perder as de antes
        assertEquals(listOf("", "", "fim"), lido[0].linhas[2])
        assertEquals("Passo 2", lido[1].linhas[1][0])
    }

    @Test fun nomesDeAbaInvalidosViramNomesValidos() {
        val longo = "A".repeat(40)
        val lido = Xlsx.ler(Xlsx.escrever(listOf(AbaXlsx("a/b:c", listOf(listOf("x"))), AbaXlsx(longo, listOf(listOf("y"))), AbaXlsx("a/b:c", listOf(listOf("z"))))))
        assertTrue(lido.all { it.nome.length <= 31 && !it.nome.contains(Regex("[\\[\\]:*?/\\\\]")) })
        assertEquals(3, lido.map { it.nome.lowercase() }.toSet().size) // sem nomes repetidos
    }

    @Test fun caracteresDeControleNaoQuebramOArquivo() {
        val lido = Xlsx.ler(Xlsx.escrever(listOf(AbaXlsx("A", listOf(listOf("ok\u0001\u0002fim"))))))
        assertEquals("okfim", lido[0].linhas[0][0])
    }

    @Test fun letrasDeColuna() {
        assertEquals("A", Xlsx.coluna(0))
        assertEquals("Z", Xlsx.coluna(25))
        assertEquals("AA", Xlsx.coluna(26))
        assertEquals("AB", Xlsx.coluna(27))
    }

    @Test(expected = Xlsx.ArquivoInvalido::class)
    fun arquivoQueNaoEPlanilhaDaErroClaro() {
        Xlsx.ler("isto não é um zip".toByteArray())
    }
}

class ExportacaoTest {
    private val hoje = LocalDate.of(2026, 11, 5)
    private val marina = Cliente("m", "Marina Alves", "63712926170", "(11) 98765-4321", "marina@exemplo.com")
    private val padaria = Cliente("p", "Padaria Bom Pão", "71296668000176")
    private val sozinho = Cliente("s", "Sem Contas", "94127724242")
    private val clientes = listOf(padaria, marina, sozinho)

    private fun conta(id: String, cli: Cliente, desc: String, reais: Long, venc: LocalDate, pago: LocalDate? = null) =
        Conta(id, cli.id, desc, reais * 100 + 50, venc, pago)

    private val contas = listOf(
        conta("c1", marina, "Mensalidade", 180, hoje.minusDays(5)),        // atrasada
        conta("c2", padaria, "Pães", 1250, hoje.minusDays(10)),            // atrasada
        conta("c3", marina, "Mensalidade", 180, hoje.plusDays(25)),        // a vencer
        conta("c4", padaria, "Pães", 1250, hoje.minusDays(40), hoje.minusDays(38)), // paga
    )

    @Test fun relatorioAtrasadasPorVencimentoComDiasDeAtraso() {
        val p = Exportacao.relatorioOuEditar(clientes, contas, FiltrosExport(), hoje)
        assertEquals("contas-atrasadas", p.arquivo)
        assertEquals(Exportacao.CABECALHO_RELATORIO, p.cabecalho)
        assertEquals(listOf("c2", "c1").map { id -> contas.first { it.id == id }.descricao }, p.linhas.map { it[3] })
        val linha = p.linhas.first() // a mais antiga primeiro
        assertEquals("Padaria Bom Pão", linha[0])
        assertEquals("71.296.668/0001-76", linha[1])
        assertEquals("1250,50", linha[4])
        assertEquals("26/10/2026", linha[5])
        assertEquals("Atrasada", linha[6])
        assertEquals("10", linha[8])
        assertEquals("Atrasadas", p.filtros) // os filtros vêm escritos (R34)
    }

    @Test fun paraEditarTemOCodigoDaContaEAgrupaPorCliente() {
        val f = FiltrosExport(formato = FormatoExport.EDITAR, status = StatusExport.TODAS)
        val p = Exportacao.relatorioOuEditar(clientes, contas, f, hoje)
        assertEquals(Exportacao.CABECALHO_EDITAR, p.cabecalho)
        assertEquals(listOf("Marina Alves", "Marina Alves", "Padaria Bom Pão", "Padaria Bom Pão"), p.linhas.map { it[2] })
        assertEquals(listOf("c1", "c3"), p.linhas.take(2).map { it[0] }) // o código é o que liga a linha à conta
        assertEquals("Como editar", p.extras.single().nome)
        assertEquals("", p.linhas.first { it[0] == "c1" }[8]) // não paga: "Pago em" vazio
        assertEquals("28/09/2026", p.linhas.first { it[0] == "c4" }[8]) // paga em 28/09: sai a data, para dar baixa pelo Excel
    }

    @Test fun filtrosPorClienteEPeriodo() {
        val soPadaria = Exportacao.contasFiltradas(contas, clientes, FiltrosExport(status = StatusExport.TODAS, clienteId = "p"), hoje)
        assertEquals(setOf("c2", "c4"), soPadaria.map { it.id }.toSet())
        val periodo = FiltrosExport(status = StatusExport.TODAS, de = hoje.minusDays(12), ate = hoje)
        assertEquals(setOf("c1", "c2"), Exportacao.contasFiltradas(contas, clientes, periodo, hoje).map { it.id }.toSet())
        assertTrue(FiltrosExport(de = hoje, ate = hoje.minusDays(1)).periodoInvertido)
        assertFalse(FiltrosExport(de = hoje.minusDays(1), ate = hoje).periodoInvertido)
    }

    @Test fun resumoDosFiltrosEscritoPorExtenso() {
        val f = FiltrosExport(status = StatusExport.ABERTAS, clienteId = "m", de = LocalDate.of(2026, 11, 1), ate = LocalDate.of(2026, 11, 30))
        assertEquals("Em aberto · Marina Alves · vencimento de 01/11/2026 até 30/11/2026", Exportacao.resumoFiltros(f, clientes))
    }

    @Test fun copiaDeSegurancaLevaTudoInclusiveClienteSemContas() {
        val p = Exportacao.copiaDeSeguranca(clientes, contas)
        assertEquals("backup-acertototal", p.arquivo)
        assertEquals(5, p.linhas.size) // 4 contas + 1 cliente sem contas
        val semContas = p.linhas.single { it[2] == "Sem Contas" }
        assertEquals("", semContas[0]) // sem código: é só o cliente
        assertEquals("941.277.242-42", semContas[1])
    }

    @Test fun nomeDeArquivoSemAcentoNemEspaco() {
        assertEquals("padaria-bom-pao", Exportacao.slug("Padaria Bom Pão"))
        assertEquals("em-aberto", Exportacao.slug("Em aberto"))
    }

    @Test fun planilhaGravadaEmXlsxVoltaIgual() {
        val p = Exportacao.copiaDeSeguranca(clientes, contas)
        val lido = Xlsx.ler(Xlsx.escrever(p.abas()))
        assertEquals(p.cabecalho, lido[0].linhas[0])
        assertEquals(p.linhas.size + 1, lido[0].linhas.size)
        assertEquals(p.linhas[0].dropLastWhile { it.isEmpty() }, lido[0].linhas[1])
    }

    @Test fun tsvParaColarNoExcel() {
        val p = Exportacao.relatorioOuEditar(clientes, contas, FiltrosExport(), hoje)
        val linhas = p.tsv().lines()
        assertEquals(1 + p.linhas.size, linhas.size)
        assertEquals(Exportacao.CABECALHO_RELATORIO.joinToString("\t"), linhas[0])
    }
}
