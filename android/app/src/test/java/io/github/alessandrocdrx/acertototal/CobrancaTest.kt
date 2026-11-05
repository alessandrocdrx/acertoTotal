package io.github.alessandrocdrx.acertototal

import io.github.alessandrocdrx.acertototal.dominio.Cobranca
import io.github.alessandrocdrx.acertototal.dominio.ContaParaCobrar
import io.github.alessandrocdrx.acertototal.dominio.Negocio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CobrancaTest {
    private val hoje = LocalDate.of(2026, 11, 5)

    @Test fun telefone_vira_55_ddd_numero() {
        assertEquals("5511987654321", Cobranca.telefoneWhats("(11) 98765-4321"))
        assertEquals("551134567890", Cobranca.telefoneWhats("(11) 3456-7890"))
        assertEquals("5511987654321", Cobranca.telefoneWhats("+55 11 98765-4321"))
    }

    @Test fun telefone_incompleto_nao_gera_link() {
        assertEquals("", Cobranca.telefoneWhats(""))
        assertEquals("", Cobranca.telefoneWhats("98765-4321"))
        assertEquals("", Cobranca.telefoneWhats("123"))
    }

    @Test fun mensagem_de_uma_conta_atrasada() {
        val m = Cobranca.mensagem(
            "Marina Alves", listOf(ContaParaCobrar("Mensalidade", 18000, hoje.minusDays(5))),
            Negocio("Padaria Bom Pão", "11999990000"), hoje,
        )
        assertTrue(m.startsWith("Olá, Marina! Tudo bem?\nPassando para lembrar de uma conta em aberto:\n"))
        assertTrue(m.contains("• Mensalidade — R$ 180,00, venceu em 31/10 (há 5 dias)"))
        assertTrue(m.contains("Pode pagar pelo Pix: 11999990000"))
        assertTrue(m.endsWith("Qualquer dúvida, é só me chamar.\nPadaria Bom Pão"))
        assertFalse("sem espaço duro, para o WhatsApp não quebrar", m.contains(' '))
    }

    @Test fun vence_hoje_e_um_dia_de_atraso() {
        val m = Cobranca.mensagem(
            "Rafaela", listOf(ContaParaCobrar("A", 1000, hoje), ContaParaCobrar("B", 2000, hoje.minusDays(1))), Negocio(), hoje,
        )
        assertTrue(m.contains("vence hoje"))
        assertTrue(m.contains("(há 1 dia)"))
        assertTrue(m.contains("2 contas em aberto"))
        assertTrue(m.contains("Total: R$ 30,00."))
        assertFalse(m.contains("Pix"))
    }

    @Test fun mais_de_oito_contas_resume_o_resto() {
        val contas = (1..10).map { ContaParaCobrar("Parcela $it", 1000, hoje.plusDays(it.toLong())) }
        val m = Cobranca.mensagem("Carlos Eduardo", contas, Negocio(), hoje)
        assertTrue(m.contains("• Parcela 8 "))
        assertFalse(m.contains("• Parcela 9 "))
        assertTrue(m.contains("• e mais 2 contas"))
        assertTrue(m.contains("Total: R$ 100,00."))
    }

    @Test fun link_codifica_o_texto_sem_mais() {
        val l = Cobranca.linkWhats("5511987654321", "Olá! R$ 10,00\nok")
        assertTrue(l.startsWith("https://wa.me/5511987654321?text="))
        assertFalse(l.contains(' '))
        assertFalse(l.contains('+'))
        assertTrue(l.contains("%0A"))
    }
}
