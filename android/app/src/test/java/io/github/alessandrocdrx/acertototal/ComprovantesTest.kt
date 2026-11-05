package io.github.alessandrocdrx.acertototal

import io.github.alessandrocdrx.acertototal.dados.Anexo
import io.github.alessandrocdrx.acertototal.dados.Comprovantes
import io.github.alessandrocdrx.acertototal.dados.Conta
import io.github.alessandrocdrx.acertototal.dados.TipoComprovante
import io.github.alessandrocdrx.acertototal.dados.anexo
import io.github.alessandrocdrx.acertototal.dados.comAnexo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ComprovantesTest {
    @Test fun foto_grande_e_reduzida_para_1280_no_lado_maior() {
        assertEquals(1280 to 960, Comprovantes.tamanhoReduzido(4000, 3000))
        assertEquals(960 to 1280, Comprovantes.tamanhoReduzido(3000, 4000))
    }

    @Test fun foto_pequena_nao_e_ampliada() {
        assertEquals(800 to 600, Comprovantes.tamanhoReduzido(800, 600))
        assertEquals(1280 to 720, Comprovantes.tamanhoReduzido(1280, 720))
    }

    @Test fun reducao_nunca_zera_um_lado() {
        val (l, a) = Comprovantes.tamanhoReduzido(20000, 3)
        assertEquals(1280, l)
        assertEquals(1, a)
    }

    @Test fun amostra_decodifica_menor_sem_passar_do_limite() {
        assertEquals(1, Comprovantes.amostra(1000, 800))
        assertEquals(1, Comprovantes.amostra(2000, 1500))
        assertEquals(2, Comprovantes.amostra(2600, 1900))
        assertEquals(4, Comprovantes.amostra(5200, 3900))
    }

    @Test fun tipo_vem_do_conteudo_e_nao_do_nome() {
        val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())
        val png = byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte())
        val pdf = "%PDF-1.7".toByteArray()
        assertEquals(TipoComprovante.JPEG, Comprovantes.tipoPeloConteudo(jpeg))
        assertEquals(TipoComprovante.PNG, Comprovantes.tipoPeloConteudo(png))
        assertEquals(TipoComprovante.PDF, Comprovantes.tipoPeloConteudo(pdf))
        assertNull(Comprovantes.tipoPeloConteudo("MZ executavel.pdf".toByteArray()))
        assertNull(Comprovantes.tipoPeloConteudo(ByteArray(0)))
    }

    @Test fun conta_guarda_e_devolve_o_anexo() {
        val c = Conta("1", "c", "Mensalidade", 10000, LocalDate.of(2026, 1, 1))
        assertNull(c.anexo())
        val a = Anexo("abc.jpg", "recibo.jpg", "image/jpeg")
        assertEquals(a, c.comAnexo(a).anexo())
        assertNull(c.comAnexo(a).comAnexo(null).anexo())
    }
}
