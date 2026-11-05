package io.github.alessandrocdrx.acertototal

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.alessandrocdrx.acertototal.dados.Cliente
import io.github.alessandrocdrx.acertototal.dados.ContaComCliente
import io.github.alessandrocdrx.acertototal.dominio.Avaliacao
import io.github.alessandrocdrx.acertototal.dominio.Faixa
import io.github.alessandrocdrx.acertototal.dominio.Negocio
import io.github.alessandrocdrx.acertototal.ui.FaixaDoTopo
import io.github.alessandrocdrx.acertototal.ui.FolhaConta
import io.github.alessandrocdrx.acertototal.ui.FolhaCobrar
import io.github.alessandrocdrx.acertototal.ui.FolhaPlanos
import io.github.alessandrocdrx.acertototal.ui.FolhaSomenteConsulta
import io.github.alessandrocdrx.acertototal.ui.TemaAcerto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/** Testes de tela (Compose): o que a pessoa lê e toca. A lógica fica nos testes de unidade. */
class TelasTest {
    @get:Rule val regra = createComposeRule()

    private val hoje = LocalDate.of(2026, 11, 5)
    private val conta = ContaComCliente("c1", "cl1", "Marina Alves", "Mensalidade", 18000, hoje.minusDays(5), null, false)
    private val cliente = Cliente("cl1", "Marina Alves", "63712926170", "(11) 98765-4321", "", "")

    private fun folhaConta(consulta: Boolean, aoBaixar: () -> Unit = {}) = regra.setContent {
        TemaAcerto(escuro = false) {
            FolhaConta(
                conta = conta, hoje = hoje, somenteConsulta = consulta, aoFechar = {},
                aoBaixar = { _, _ -> aoBaixar() }, aoAnexar = {}, aoRemoverComprovante = {},
                aoDesfazerBaixa = {}, aoCobrar = {}, aoEditar = {}, aoExcluir = {},
            )
        }
    }

    @Test fun conta_em_aberto_ja_vem_com_a_baixa_pronta() {
        var baixou = 0
        folhaConta(consulta = false, aoBaixar = { baixou++ })
        regra.onNodeWithText("Dar baixa").assertIsDisplayed()
        regra.onNodeWithText("05/11/2026").assertIsDisplayed() // a data de hoje já preenchida (R2)
        regra.onNodeWithText("Marcar como paga").performClick()
        assertEquals(1, baixou)
    }

    @Test fun conta_em_modo_consulta_nao_mostra_botoes_de_escrita() {
        folhaConta(consulta = true)
        regra.onNodeWithText("Teste grátis encerrado: o app está só para consulta. Assine para continuar.").assertIsDisplayed()
        regra.onNodeWithText("Marcar como paga").assertDoesNotExist()
        regra.onNodeWithText("Editar conta").assertDoesNotExist()
        regra.onNodeWithText("Excluir esta conta").assertDoesNotExist()
    }

    @Test fun cobranca_sem_telefone_explica_e_nao_tem_link() {
        regra.setContent {
            TemaAcerto(escuro = false) {
                FolhaCobrar(
                    cliente = cliente.copy(telefone = ""), contas = listOf(conta), negocio = Negocio(), hoje = hoje,
                    aoEditarCliente = {}, aoMeuNegocio = {}, aoFechar = {},
                )
            }
        }
        regra.onNodeWithText("Editar dados").assertIsDisplayed()
        regra.onNodeWithText("Copiar mensagem").assertIsDisplayed()
        regra.onNodeWithText("Abrir no WhatsApp").assertDoesNotExist()
    }

    @Test fun cobranca_com_telefone_oferece_o_whatsapp() {
        regra.setContent {
            TemaAcerto(escuro = false) {
                FolhaCobrar(
                    cliente = cliente, contas = listOf(conta), negocio = Negocio("Padaria", "11999990000"), hoje = hoje,
                    aoEditarCliente = {}, aoMeuNegocio = {}, aoFechar = {},
                )
            }
        }
        regra.onNodeWithText("Abrir no WhatsApp").assertIsDisplayed()
        regra.onNodeWithText("Editar dados").assertDoesNotExist()
        regra.onNodeWithText("Cadastrar agora").assertDoesNotExist() // o Pix já está cadastrado
    }

    @Test fun faixa_do_teste_encerrado_leva_aos_planos() {
        var tocou = false
        regra.setContent {
            TemaAcerto(escuro = false) {
                FaixaDoTopo(Faixa(listOf("Teste grátis encerrado."), encerrado = true, acao = "Assinar")) { tocou = true }
            }
        }
        regra.onNodeWithText("Assinar").performClick()
        assertTrue(tocou)
    }

    @Test fun janela_de_consulta_tem_saida_escrita() {
        var planos = false
        regra.setContent { TemaAcerto(escuro = false) { FolhaSomenteConsulta(aoVerPlanos = { planos = true }, aoFechar = {}) } }
        regra.onNodeWithText("Fechar").assertIsDisplayed()
        regra.onNodeWithText("Ver planos").performClick()
        assertTrue(planos)
    }

    @Test fun planos_mudam_o_texto_do_botao_ao_escolher() {
        regra.setContent {
            TemaAcerto(escuro = false) {
                FolhaPlanos(
                    estado = EstadoAssinatura(real = false), avaliacao = Avaliacao(0, 7, false), agora = 1000,
                    aoAssinar = { _, _ -> }, aoRestaurar = {}, aoCancelarSimulada = {}, aoFechar = {},
                )
            }
        }
        regra.onNodeWithText("Assinar o plano mensal por R$ 9,99").assertExists()
        regra.onNodeWithText("Anual · 10% mais barato").performClick()
        regra.onNodeWithText("Assinar o plano anual por R$ 107,90").assertExists()
    }
}
