package io.github.alessandrocdrx.acertototal

import androidx.compose.ui.text.AnnotatedString
import io.github.alessandrocdrx.acertototal.dominio.Documento
import io.github.alessandrocdrx.acertototal.dominio.Mascara
import io.github.alessandrocdrx.acertototal.dominio.Telefone
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
}
