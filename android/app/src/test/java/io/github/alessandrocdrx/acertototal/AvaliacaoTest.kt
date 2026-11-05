package io.github.alessandrocdrx.acertototal

import io.github.alessandrocdrx.acertototal.dominio.Avaliacao
import io.github.alessandrocdrx.acertototal.dominio.DadosAvaliacao
import io.github.alessandrocdrx.acertototal.dominio.FaixaTopo
import io.github.alessandrocdrx.acertototal.dominio.Planos
import io.github.alessandrocdrx.acertototal.dominio.Teste
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AvaliacaoTest {
    private val dia = 86_400_000L
    private val t0 = 1_800_000_000_000L

    @Test fun primeiro_uso_comeca_a_contar() {
        val (dados, av) = Teste.conferir(null, t0, 7)
        assertEquals(t0, dados.inicio)
        assertEquals(7, av.diasRestantes(t0))
        assertFalse(av.encerrada(t0))
    }

    @Test fun contagem_nao_reinicia_ao_abrir_de_novo() {
        val (d1, _) = Teste.conferir(null, t0, 7)
        val (d2, av) = Teste.conferir(d1, t0 + 3 * dia, 7)
        assertEquals(t0, d2.inicio)
        assertEquals(4, av.diasRestantes(t0 + 3 * dia))
    }

    @Test fun depois_do_prazo_encerra() {
        val (d, _) = Teste.conferir(null, t0, 7)
        val (_, av) = Teste.conferir(d, t0 + 7 * dia, 7)
        assertTrue(av.encerrada(t0 + 7 * dia))
        assertEquals(0, av.diasRestantes(t0 + 8 * dia))
    }

    @Test fun relogio_voltado_mais_de_um_dia_encerra_e_nao_se_desfaz() {
        val (d1, _) = Teste.conferir(null, t0, 7)
        val (d2, _) = Teste.conferir(d1, t0 + 3 * dia, 7)
        val (d3, av) = Teste.conferir(d2, t0 + dia, 7) // voltou 2 dias
        assertTrue(av.relogioAtrasado)
        assertTrue(av.encerrada(t0 + dia))
        val (_, av2) = Teste.conferir(d3, t0 + 5 * dia, 7)
        assertTrue("continua encerrado", av2.relogioAtrasado)
    }

    @Test fun pequena_diferenca_de_relogio_nao_conta() {
        val (d1, _) = Teste.conferir(DadosAvaliacao(t0, t0 + dia / 2, false), t0 + dia / 4, 7)
        assertFalse(d1.relogioAtrasado)
    }

    @Test fun limite_zero_desliga_tudo() {
        val (_, av) = Teste.conferir(null, t0, 0)
        assertFalse(av.encerrada(t0 + 1000 * dia))
        assertFalse(Teste.somenteConsulta(av, false, t0 + 1000 * dia))
    }

    @Test fun assinatura_ativa_libera_depois_do_prazo() {
        val (_, av) = Teste.conferir(DadosAvaliacao(t0, t0, false), t0 + 9 * dia, 7)
        assertTrue(Teste.somenteConsulta(av, false, t0 + 9 * dia))
        assertFalse(Teste.somenteConsulta(av, true, t0 + 9 * dia))
    }

    @Test fun faixa_na_versao_de_teste_sempre_avisa() {
        val av = Avaliacao(t0, 7, false)
        val f = FaixaTopo.montar(av, false, t0 + dia, producao = false)
        assertEquals("Ambiente de teste: os dados podem ser apagados.", f.textos[0])
        assertTrue(f.textos[1].startsWith("Teste grátis: faltam 6 dias"))
        assertNull(f.acao)
    }

    @Test fun faixa_na_producao_so_nos_ultimos_3_dias() {
        val av = Avaliacao(t0, 32, false)
        assertTrue(FaixaTopo.montar(av, false, t0 + 10 * dia, producao = true).textos.isEmpty())
        val f = FaixaTopo.montar(av, false, t0 + 30 * dia, producao = true)
        assertEquals(1, f.textos.size)
        assertEquals("Ver planos", f.acao)
    }

    @Test fun faixa_encerrada_convida_a_assinar() {
        val av = Avaliacao(t0, 7, false)
        val f = FaixaTopo.montar(av, false, t0 + 8 * dia, producao = true)
        assertTrue(f.encerrado)
        assertEquals("Assinar", f.acao)
        assertTrue(f.textos[0].contains("só para consulta"))
    }

    @Test fun ultimo_dia_diz_termina_hoje() {
        val av = Avaliacao(t0, 7, false)
        assertTrue(FaixaTopo.cartao(av, false, t0 + 6 * dia + dia / 2).startsWith("Teste grátis: termina hoje"))
    }

    @Test fun planos_calculam_por_mes_e_economia() {
        assertEquals(999, Planos.porId("mensal").porMesCentavos)
        assertEquals(948, Planos.porId("semestral").porMesCentavos)
        assertEquals(899, Planos.porId("anual").porMesCentavos)
        assertEquals(1296, Planos.porId("semanal").porMesCentavos)
        // A economia anunciada confere com os preços: 5% e 10% sobre o mensal.
        assertEquals(5, Math.round((1 - 5690.0 / 6 / 999) * 100).toInt())
        assertEquals(10, Math.round((1 - 10790.0 / 12 / 999) * 100).toInt())
    }
}
