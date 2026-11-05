package io.github.alessandrocdrx.acertototal

import io.github.alessandrocdrx.acertototal.dados.ContaComCliente
import io.github.alessandrocdrx.acertototal.dados.Exemplo
import io.github.alessandrocdrx.acertototal.dominio.Documento
import io.github.alessandrocdrx.acertototal.dominio.Situacao
import io.github.alessandrocdrx.acertototal.dominio.dataCurta
import io.github.alessandrocdrx.acertototal.dominio.infoPrazo
import io.github.alessandrocdrx.acertototal.dominio.situacaoDe
import io.github.alessandrocdrx.acertototal.ui.ContasViewModel
import io.github.alessandrocdrx.acertototal.ui.FiltroContas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DocumentoTest {
    @Test fun cpfValidoEInvalido() {
        assertTrue(Documento.cpfOk("63712926170"))
        assertFalse(Documento.cpfOk("63712926171"))
        assertFalse(Documento.cpfOk("11111111111")) // todos iguais
        assertFalse(Documento.cpfOk("123"))
    }

    @Test fun cnpjValidoEInvalido() {
        assertTrue(Documento.cnpjOk("71296668000176"))
        assertFalse(Documento.cnpjOk("71296668000177"))
        assertFalse(Documento.cnpjOk("00000000000000"))
    }

    // R61: o app de teste aceita dígito errado (com aviso); a produção só aceita número válido.
    @Test fun testeAceitaDigitoErradoMasProducaoNao() {
        assertTrue(Documento.aceito("63712926171", producao = false))
        assertFalse(Documento.aceito("63712926171", producao = true))
        assertTrue(Documento.aceito("63712926170", producao = true))
        assertFalse(Documento.aceito("123", producao = false)) // tamanho errado nunca passa
    }

    @Test fun formataComPontuacaoEGuardaSoNumeros() {
        assertEquals("637.129.261-70", Documento.formatar("63712926170"))
        assertEquals("71.296.668/0001-76", Documento.formatar("71296668000176"))
        assertEquals("63712926170", Documento.soDigitos("637.129.261-70"))
        assertEquals("CNPJ", Documento.tipo("71296668000176"))
        assertEquals("CPF", Documento.tipo("63712926170"))
    }
}

class SituacaoTest {
    private val hoje = LocalDate.of(2026, 11, 5)

    @Test fun situacaoPorData() {
        assertEquals(Situacao.PAGA, situacaoDe(hoje.minusDays(9), hoje.minusDays(8), hoje))
        assertEquals(Situacao.ATRASADA, situacaoDe(hoje.minusDays(1), null, hoje))
        assertEquals(Situacao.PENDENTE, situacaoDe(hoje, null, hoje)) // vence hoje ainda não está atrasada
    }

    // R3: a situação vem sempre escrita.
    @Test fun textoDoPrazo() {
        assertEquals("Atrasada há 5 dias", infoPrazo(hoje.minusDays(5), null, hoje))
        assertEquals("Atrasada há 1 dia", infoPrazo(hoje.minusDays(1), null, hoje))
        assertEquals("Vence hoje", infoPrazo(hoje, null, hoje))
        assertEquals("Vence em 3 dias", infoPrazo(hoje.plusDays(3), null, hoje))
        assertEquals("Paga em 01/11", infoPrazo(hoje.minusDays(9), LocalDate.of(2026, 11, 1), hoje))
    }

    @Test fun dataCurtaSemAnoNoAnoAtual() {
        assertEquals("23/10", dataCurta(LocalDate.of(2026, 10, 23), hoje))
        assertEquals("23/10/2025", dataCurta(LocalDate.of(2025, 10, 23), hoje))
    }
}

class ContasFiltroTest {
    private val hoje = LocalDate.of(2026, 11, 5)

    private fun conta(id: String, cliente: String, desc: String, venc: Long, pago: Long? = null) = ContaComCliente(
        id = id, clienteId = cliente, clienteNome = cliente, descricao = desc, valor = 10_000,
        venc = hoje.plusDays(venc), pagoEm = pago?.let { hoje.plusDays(it) }, temComprovante = false,
    )

    private val contas = listOf(
        conta("a", "Marina", "Mensalidade", -5),       // atrasada
        conta("b", "Marina", "Mensalidade", 25),       // a vencer
        conta("c", "Padaria", "Pães", -10),            // atrasada, a mais velha
        conta("d", "Padaria", "Pães", -40, -38),       // paga em outro mês
        conta("e", "Carlos", "Parcela 1/6", -4, 0),    // paga hoje
    )

    @Test fun semFiltroMostraSoOQueFaltaReceberComAMaisAntigaPrimeiro() {
        val ui = ContasViewModel.montar(contas, FiltroContas.ABERTAS, "", hoje)
        assertEquals(listOf("c", "a", "b"), ui.lista.map { it.id })
    }

    @Test fun cartoesDeResumo() {
        val ui = ContasViewModel.montar(contas, FiltroContas.ABERTAS, "", hoje)
        assertEquals(1, ui.aVencer.quantas)
        assertEquals(2, ui.atrasadas.quantas)
        assertEquals(20_000L, ui.atrasadas.centavos)
        assertEquals(1, ui.pagasNoMes.quantas) // só a paga hoje; a de outro mês não conta
    }

    @Test fun pagasComAMaisRecentePrimeiro() {
        val ui = ContasViewModel.montar(contas, FiltroContas.PAGAS, "", hoje)
        assertEquals(listOf("e", "d"), ui.lista.map { it.id })
    }

    // R77: não existe "Todas": com busca, procura em todas as contas, inclusive as pagas.
    @Test fun buscaOlhaTodasAsContas() {
        val ui = ContasViewModel.montar(contas, FiltroContas.ATRASADAS, "pães", hoje)
        assertEquals(setOf("c", "d"), ui.lista.map { it.id }.toSet())
    }

    @Test fun exemploPreencheOPeriodoPadrao() { // R19
        val (clientes, lista) = Exemplo.gerar(hoje)
        assertEquals(5, clientes.size)
        assertTrue(lista.any { it.pagoEm == null && it.venc < hoje })   // atrasadas
        assertTrue(lista.any { it.pagoEm == null && it.venc >= hoje })  // a vencer
        assertTrue(lista.any { it.pagoEm != null && it.pagoEm.month == hoje.month }) // pagas no mês
        assertTrue(clientes.all { Documento.valido(it.doc) })
    }
}
