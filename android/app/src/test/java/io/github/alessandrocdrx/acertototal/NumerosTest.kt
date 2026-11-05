package io.github.alessandrocdrx.acertototal

import io.github.alessandrocdrx.acertototal.dados.ContaComCliente
import io.github.alessandrocdrx.acertototal.dominio.calcularEstatisticas
import io.github.alessandrocdrx.acertototal.dominio.confereApagar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class NumerosTest {
    private val hoje = LocalDate.of(2026, 11, 15)

    private fun conta(id: String, cli: String, venc: LocalDate, pago: LocalDate?, reais: Long = 100) = ContaComCliente(
        id, cli, cli, "Mensalidade", reais * 100, venc, pago, false,
    )

    @Test fun taxaDeRecebimentoDoPeriodo() {
        val contas = listOf(
            conta("1", "A", LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 7), 300), // venceu no mês e foi paga
            conta("2", "B", LocalDate.of(2026, 11, 10), null, 100),                    // venceu no mês, não paga
            conta("3", "C", LocalDate.of(2026, 11, 25), null, 500),                    // ainda vai vencer: fora da taxa
        )
        val e = calcularEstatisticas(contas, 1, hoje)
        assertEquals(40_000L, e.vencido)
        assertEquals(30_000L, e.vencidoPago)
        assertEquals(75, e.percentualRecebido)
        assertEquals(30_000L, e.recebido)
    }

    @Test fun semContasVencidasNaoHaPercentual() {
        val e = calcularEstatisticas(listOf(conta("1", "A", hoje.plusDays(5), null)), 1, hoje)
        assertNull(e.percentualRecebido)
    }

    @Test fun periodoDeTresMesesAlcancaMesesAnteriores() {
        val contas = listOf(conta("1", "A", LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 12), 200))
        assertEquals(0L, calcularEstatisticas(contas, 1, hoje).recebido)
        assertEquals(20_000L, calcularEstatisticas(contas, 3, hoje).recebido)
    }

    @Test fun seisMesesComOAtualPorUltimo() {
        val e = calcularEstatisticas(emptyList(), 1, hoje)
        assertEquals(listOf("jun", "jul", "ago", "set", "out", "nov"), e.meses.map { it.rotulo })
    }

    @Test fun faixasDeAtrasoEQuemMaisDeve() {
        val contas = listOf(
            conta("1", "Ana", hoje.minusDays(3), null, 100),    // 1 a 7
            conta("2", "Ana", hoje.minusDays(20), null, 100),   // 8 a 30
            conta("3", "Bia", hoje.minusDays(45), null, 900),   // mais de 30
        )
        val e = calcularEstatisticas(contas, 1, hoje)
        assertEquals(listOf(1, 1, 1), e.faixas.map { it.quantas })
        assertEquals(3, e.atrasadas)
        assertEquals(listOf("Bia", "Ana"), e.devedores.map { it.nome })
        assertEquals(20, e.devedores.last().maisAntiga)
    }

    @Test fun prazoMedioEProximosSeteDias() {
        val contas = listOf(
            conta("1", "A", LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 4)),   // 2 dias depois
            conta("2", "A", LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 3)),   // no dia
            conta("3", "B", hoje.plusDays(3), null, 250),                            // vence em 3 dias
            conta("4", "B", hoje.plusDays(20), null, 999),                           // longe demais
        )
        val e = calcularEstatisticas(contas, 1, hoje)
        assertEquals(1, e.prazoMedioDias)
        assertEquals(1, e.proximasQuantas)
        assertEquals(25_000L, e.proximasTotal)
    }

    // R60: "APAGAR" em maiúsculas ou minúsculas, com ou sem espaços nas pontas.
    @Test fun confirmacaoDeApagarTudo() {
        assertTrue(confereApagar("APAGAR"))
        assertTrue(confereApagar(" apagar "))
        assertFalse(confereApagar("apaga"))
        assertFalse(confereApagar(""))
        assertFalse(confereApagar(null))
    }
}
