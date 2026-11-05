package io.github.alessandrocdrx.acertototal

import io.github.alessandrocdrx.acertototal.dados.Cliente
import io.github.alessandrocdrx.acertototal.dados.ContaComCliente
import io.github.alessandrocdrx.acertototal.dominio.CampoForm
import io.github.alessandrocdrx.acertototal.dominio.Dinheiro
import io.github.alessandrocdrx.acertototal.dominio.Validacao
import io.github.alessandrocdrx.acertototal.ui.ClientesViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class DinheiroTest {
    @Test fun aceitaOsFormatosBrasileirosEAmericanos() {
        assertEquals(15_000L, Dinheiro.parse("150"))
        assertEquals(15_000L, Dinheiro.parse("150,00"))
        assertEquals(123_456L, Dinheiro.parse("R$ 1.234,56"))
        assertEquals(123_456L, Dinheiro.parse("1,234.56"))
        assertEquals(123_400L, Dinheiro.parse("1.234"))
        assertEquals(1_050L, Dinheiro.parse("10,5"))
    }

    @Test fun recusaOQueNaoEUmNumero() {
        assertNull(Dinheiro.parse(""))
        assertNull(Dinheiro.parse("abc"))
        assertNull(Dinheiro.parse("--"))
        assertNull(Dinheiro.parse(null))
    }

    @Test fun voltaParaOCampoComVirgula() {
        assertEquals("150,00", Dinheiro.paraCampo(15_000))
        assertEquals("0,05", Dinheiro.paraCampo(5))
        assertEquals("1234,56", Dinheiro.paraCampo(123_456))
    }
}

class ValidacaoTest {
    private val hoje = LocalDate.of(2026, 11, 5)

    @Test fun contaExigeClienteValorEVencimento() {
        assertEquals(CampoForm.CLIENTE, Validacao.conta(null, "150", hoje)!!.campo)
        assertEquals("Informe o valor. Ex.: 150,00", Validacao.conta("x", "", hoje)!!.mensagem)
        assertEquals(CampoForm.VALOR, Validacao.conta("x", "0", hoje)!!.campo) // valor zero não vale
        assertEquals(CampoForm.VENCIMENTO, Validacao.conta("x", "150", null)!!.campo)
        assertNull(Validacao.conta("x", "150", hoje)) // cliente sem CPF/CNPJ pode ter conta
    }

    // R10: a data de pagamento não aceita dia futuro.
    @Test fun baixaNaoAceitaDataFutura() {
        assertEquals("A data do pagamento não pode ser depois de hoje", Validacao.baixa(hoje.plusDays(1), hoje)!!.mensagem)
        assertEquals(CampoForm.DATA_PAGO, Validacao.baixa(null, hoje)!!.campo)
        assertNull(Validacao.baixa(hoje, hoje))
        assertNull(Validacao.baixa(hoje.minusDays(3), hoje))
    }

    @Test fun clienteExigeSoONome() {
        assertEquals(CampoForm.NOME, Validacao.cliente(" ", "63712926170", null, producao = true)!!.campo)
        assertNull("CPF/CNPJ é opcional", Validacao.cliente("Ana", "", null, producao = true))
        assertEquals(
            "CPF tem 11 números e CNPJ tem 14. Confira os números.",
            Validacao.cliente("Ana", "123", null, producao = true)!!.mensagem,
        )
        assertNull(Validacao.cliente("Ana", "637.129.261-70", null, producao = true)) // com pontuação
    }

    // R61: dígito errado passa no teste e é recusado na produção.
    @Test fun digitoErradoPassaNoTesteMasNaoNaProducao() {
        assertNull(Validacao.cliente("Ana", "63712926171", null, producao = false))
        assertEquals("CPF inválido. Confira os números.", Validacao.cliente("Ana", "63712926171", null, producao = true)!!.mensagem)
    }

    // R52: o CPF/CNPJ não pode repetir, mas dois clientes podem ter o mesmo nome.
    @Test fun documentoRepetidoEBarrado() {
        val e = Validacao.cliente("Outra Marina", "63712926170", "Marina Alves", producao = true)
        assertNotNull(e)
        assertEquals("Esse CPF já é de Marina Alves.", e!!.mensagem)
    }
}

class ClientesListaTest {
    private val hoje = LocalDate.of(2026, 11, 5)
    private val marina = Cliente("m", "Marina Alves", "63712926170", "(11) 98765-4321")
    private val padaria = Cliente("p", "Padaria Bom Pão", "71296668000176")
    private val antigo = Cliente("a", "Zeca Antigo", "") // sem CPF/CNPJ (R59)

    private fun conta(id: String, cli: Cliente, venc: Long, pago: Long? = null) = ContaComCliente(
        id, cli.id, cli.nome, "Mensalidade", 10_000, hoje.plusDays(venc), pago?.let { hoje.plusDays(it) }, false,
    )

    private val contas = listOf(
        conta("1", marina, -5), conta("2", marina, -12), conta("3", marina, 20),
        conta("4", padaria, -3, -3),
    )

    @Test fun mostraQuantoCadaClienteDeveEAContaMaisAntiga() {
        val ui = ClientesViewModel.montar(listOf(padaria, marina, antigo), contas, "", hoje)
        assertEquals(listOf("Marina Alves", "Padaria Bom Pão", "Zeca Antigo"), ui.linhas.map { it.cliente.nome })
        val m = ui.linhas.first { it.cliente.id == "m" }
        assertEquals(3, m.nContas)
        assertEquals(30_000L, m.aberto)
        assertEquals(2, m.atrasadas)
        assertEquals(12, m.maisAntiga)
        assertEquals(0L, ui.linhas.first { it.cliente.id == "p" }.aberto) // "Em dia"
    }

    @Test fun buscaPorNomeOuTelefone() {
        val todos = listOf(marina, padaria, antigo)
        fun achar(q: String) = ClientesViewModel.montar(todos, contas, q, hoje).linhas.map { it.cliente.id }
        assertEquals(listOf("m"), achar("mari"))
        assertEquals(listOf("p"), achar("pao")) // sem acento acha "Pão"
        assertEquals(listOf("m"), achar("98765"))
        assertEquals(listOf("m"), achar("(11) 98765-4321")) // com pontuação
        assertEquals(emptyList<String>(), achar("71.296.668")) // CPF/CNPJ não entra na busca
        assertEquals(emptyList<String>(), achar("zzz"))
    }
}
