package io.github.alessandrocdrx.acertototal

import io.github.alessandrocdrx.acertototal.planilha.Campos
import io.github.alessandrocdrx.acertototal.planilha.Importacao
import io.github.alessandrocdrx.acertototal.planilha.Modelos
import io.github.alessandrocdrx.acertototal.planilha.Xlsx
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ModelosTest {
    private val hoje = LocalDate.of(2026, 11, 5)

    // O modelo que o app entrega tem que ser reconhecido pelo próprio app, sem ajustar colunas.
    @Test fun oModeloEReconhecidoPeloProprioApp() {
        val aba = Campos.analisarAba("Contas", Modelos.modelo()[0].linhas)!!
        assertEquals("", Campos.faltaColuna(Campos.autoMapa(aba.cols)))
        assertEquals(8, aba.cols.count { it.campo != null })
    }

    // A planilha de exemplo importa limpa: 3 clientes e 5 contas, sem erro.
    @Test fun oExemploImportaSemProblemas() {
        val aba = Campos.analisarAba("Exemplo", Modelos.exemplo(hoje).linhas)!!
        val mapa = Campos.autoMapa(aba.cols)
        var n = 0
        val pl = Importacao.planejar(Campos.linhasDoMapa(aba, mapa), "Exemplo", false, emptyList(), emptyList(), hoje, producao = true) { "i${++n}" }
        assertEquals(0, pl.erros.size)
        assertEquals(3, pl.clientesNovos.size)
        assertEquals(5, pl.contasNovas.size)
    }

    @Test fun exemploSobreviveAoVaiEVemDoXlsx() {
        val lido = Xlsx.ler(Xlsx.escrever(listOf(Modelos.exemplo(hoje))))
        assertEquals(Modelos.exemplo(hoje).linhas.first(), lido[0].linhas.first())
        assertEquals(6, lido[0].linhas.size)
    }

    // R51: as linhas com problema voltam numa planilha com as colunas originais, "Linha no arquivo" e "Problema".
    @Test fun planilhaDeLinhasComProblema() {
        val aoa = listOf(
            listOf("CPF/CNPJ", "Nome", "Valor", "Vencimento"),
            listOf("637.129.261-70", "Marina", "10", "10/11/2026"),
            listOf("637.129.261-70", "Marina", "abc", "10/12/2026"),
        )
        val aba = Campos.analisarAba("A", aoa)!!
        var n = 0
        val pl = Importacao.planejar(Campos.linhasDoMapa(aba, Campos.autoMapa(aba.cols)), "x.xlsx", false, emptyList(), emptyList(), hoje, true) { "i${++n}" }
        val saida = Modelos.linhasComProblema(aba, pl.erros)
        assertEquals(listOf("Linha no arquivo", "Problema", "CPF/CNPJ", "Nome", "Valor", "Vencimento"), saida.linhas[0])
        val l = saida.linhas[1]
        assertEquals("3", l[0])
        assertTrue(l[1].startsWith("Valor (C3): “abc” não é um valor."))
        assertEquals(listOf("637.129.261-70", "Marina", "abc", "10/12/2026"), l.drop(2))
        // A pessoa corrige o valor e importa o arquivo corrigido: as duas colunas extras são ignoradas.
        val corrigida = saida.linhas.map { it.drop(2) }.mapIndexed { i, r -> if (i == 1) r.toMutableList().also { it[2] = "10" } else r }
        val aba2 = Campos.analisarAba("A", corrigida)!!
        assertEquals(4, aba2.cols.count { it.campo != null })
    }

    @Test fun nomeDoArquivoDasLinhasComProblema() {
        assertEquals("linhas-com-problema-clientes-de-novembro", Modelos.nomeLinhasComProblema("Clientes de Novembro.xlsx"))
        assertEquals("linhas-com-problema-planilha", Modelos.nomeLinhasComProblema(".xlsx"))
    }
}
