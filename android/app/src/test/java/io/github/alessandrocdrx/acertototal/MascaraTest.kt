package io.github.alessandrocdrx.acertototal

import androidx.compose.ui.text.AnnotatedString
import io.github.alessandrocdrx.acertototal.dominio.Documento
import io.github.alessandrocdrx.acertototal.dominio.Mascara
import io.github.alessandrocdrx.acertototal.dominio.Telefone
import io.github.alessandrocdrx.acertototal.dominio.ValorDigitado
import io.github.alessandrocdrx.acertototal.dominio.Email
import io.github.alessandrocdrx.acertototal.dominio.ChavePix
import io.github.alessandrocdrx.acertototal.dominio.Dinheiro
import io.github.alessandrocdrx.acertototal.dominio.Validacao
import io.github.alessandrocdrx.acertototal.dominio.CampoForm
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import io.github.alessandrocdrx.acertototal.ui.MascaraVisual
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** R94: telefone e CPF/CNPJ com máscara enquanto digita, e o CPF/CNPJ conferido na hora. */
class MascaraTest {

    @Test fun telefoneVaiGanhandoPontuacao() {
        assertEquals("", Mascara.telefone(""))
        assertEquals("(1", Mascara.telefone("1"))
        assertEquals("(11", Mascara.telefone("11"))
        assertEquals("(11) 9", Mascara.telefone("119"))
        assertEquals("(11) 9876-5", Mascara.telefone("1198765"))
        assertEquals("(11) 3456-7890", Mascara.telefone("1134567890"))
        assertEquals("(11) 98765-4321", Mascara.telefone("11987654321"))
    }

    @Test fun cpfViraCnpjDepoisDe11Numeros() {
        assertEquals("123.4", Mascara.documento("1234"))
        assertEquals("529.982.247-25", Mascara.documento("52998224725"))
        assertEquals("114.447.770", Mascara.documento("114447770"))
        assertEquals("11.444.777/0001-61", Mascara.documento("11444777000161"))
    }

    @Test fun telefoneColadoComPaisOuPontuacao() {
        assertEquals("11987654321", Telefone.digitos("+55 (11) 98765-4321"))
        assertEquals("11987654321", Telefone.digitos("11 98765 4321 999"))
        assertEquals("(11) 98765-4321", Telefone.exibir("11987654321"))
        assertEquals("(21) 3333-4444", Telefone.exibir("2133334444"))
        assertEquals("ramal 22", Telefone.exibir(" ramal 22 "))
    }

    @Test fun cpfECnpjInvalidosAvisamAssimQueCompletam() {
        assertNull(Documento.erroAoDigitar("5299822472"))             // incompleto: ainda não é erro
        assertNull(Documento.erroAoDigitar("52998224725"))            // CPF válido
        assertEquals("CPF inválido. Confira os números.", Documento.erroAoDigitar("52998224724"))
        assertNull(Documento.erroAoDigitar("11444777000161"))         // CNPJ válido
        assertEquals("CNPJ inválido. Confira os números.", Documento.erroAoDigitar("11444777000162"))
        assertEquals("CPF inválido. Confira os números.", Documento.erroAoDigitar("11111111111"))
    }

    @Test fun cursorAndaPelosNumeros() {
        val t = MascaraVisual(Mascara::telefone).filter(AnnotatedString("11987654321"))
        assertEquals("(11) 98765-4321", t.text.text)
        val m = t.offsetMapping
        assertEquals(0, m.originalToTransformed(0))
        assertEquals(2, m.originalToTransformed(1))           // "(1|"
        assertEquals(6, m.originalToTransformed(3))           // "(11) 9|"
        assertEquals(15, m.originalToTransformed(11))         // no fim
        assertEquals(2, m.transformedToOriginal(5))           // "(11) |" fica depois do 2º número
        assertEquals(11, m.transformedToOriginal(15))
        for (o in 0..11) assertEquals(o, m.transformedToOriginal(m.originalToTransformed(o)))
        // Digitando aos poucos, o cursor fica sempre no fim do texto mostrado.
        for (n in 0..11) {
            val parcial = "11987654321".take(n)
            val tp = MascaraVisual(Mascara::telefone).filter(AnnotatedString(parcial))
            assertEquals(tp.text.length, tp.offsetMapping.originalToTransformed(n))
        }
    }

    @Test fun valorEnchePelosCentavos() {
        assertEquals("", ValorDigitado.formatar(""))
        assertEquals("0,05", ValorDigitado.formatar("5"))
        assertEquals("1,50", ValorDigitado.formatar("150"))
        assertEquals("150,00", ValorDigitado.formatar("15000"))
        assertEquals("1.500,00", ValorDigitado.formatar("150000"))
        assertEquals("1.234.567,89", ValorDigitado.formatar("123456789"))
        assertEquals("15000", ValorDigitado.digitos("R$ 0150,00"))       // colado com pontuação e zero na frente
        assertEquals("15000", ValorDigitado.deCentavos(15000))
        assertEquals(150000L, Dinheiro.parse(ValorDigitado.formatar("150000")))  // o que vai para o banco
        assertEquals("1.500,00", MascaraVisual(ValorDigitado::formatar).filter(AnnotatedString("150000")).text.text)
        val m = MascaraVisual(ValorDigitado::formatar).filter(AnnotatedString("5")).offsetMapping
        assertEquals(4, m.originalToTransformed(1))                    // cursor depois do 5 em "0,05"
    }

    @Test fun emailPrecisaDeArrobaEDominio() {
        assertTrue(Email.valido("marina@gmail.com"))
        assertTrue(Email.valido(" contato@fenix.com.br "))
        assertFalse(Email.valido("marina"))
        assertFalse(Email.valido("marina@gmail"))
        assertFalse(Email.valido("marina @gmail.com"))
        val e = Validacao.cliente("Ana", "", null, producao = true, email = "ana@")
        assertEquals(CampoForm.EMAIL, e!!.campo)
        assertEquals("E-mail incompleto. Ex.: nome@gmail.com", e.mensagem)
        assertNull("e-mail é opcional", Validacao.cliente("Ana", "", null, producao = true, email = ""))
    }

    @Test fun chavePixGanhaPontuacaoQuandoENumero() {
        assertEquals("529.982.247-25", ChavePix.formatar("52998224725"))
        assertEquals("11.444.777/0001-61", ChavePix.formatar("11444777000161"))
        assertEquals("+55 (11) 98765-4321", ChavePix.formatar("11987654321"))   // 11 números que não são CPF
        assertEquals("+55 (11) 98765-4321", ChavePix.formatar("+55 11 98765-4321"))
        assertEquals("marina@gmail.com", ChavePix.formatar(" marina@gmail.com "))
        assertEquals("123e4567-e89b-12d3-a456-426614174000", ChavePix.formatar("123e4567-e89b-12d3-a456-426614174000"))
        assertEquals("12345", ChavePix.formatar("12345"))                       // não sabe o que é: fica como está
    }
}
