package io.github.alessandrocdrx.acertototal

import io.github.alessandrocdrx.acertototal.dominio.Situacao
import io.github.alessandrocdrx.acertototal.dominio.backupPendente
import io.github.alessandrocdrx.acertototal.dominio.infoBackup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BackupTest {
    private val hoje = LocalDate.of(2026, 11, 5)

    @Test fun nuncaFeitaPedeParaFazerAgora() {
        val i = infoBackup(null, hoje)
        assertEquals("Nunca feita", i.texto)
        assertEquals(Situacao.ATRASADA, i.tom)
        assertTrue(backupPendente(null, hoje))
    }

    @Test fun textoPelaIdadeDaCopia() {
        assertEquals("Feita hoje", infoBackup(hoje, hoje).texto)
        assertEquals("Feita ontem", infoBackup(hoje.minusDays(1), hoje).texto)
        assertEquals("Feita há 5 dias", infoBackup(hoje.minusDays(5), hoje).texto)
        assertEquals("Última cópia em 31/10/2026.", infoBackup(hoje.minusDays(5), hoje).dica)
    }

    // Passou de 7 dias: fica pendente (ponto vermelho no ☰). Até 7 dias está em dia.
    @Test fun pendenteSoDepoisDeUmaSemana() {
        assertFalse(backupPendente(hoje.minusDays(7), hoje))
        assertTrue(backupPendente(hoje.minusDays(8), hoje))
        assertEquals(Situacao.PAGA, infoBackup(hoje.minusDays(7), hoje).tom)
        assertEquals(Situacao.PENDENTE, infoBackup(hoje.minusDays(8), hoje).tom)
        assertEquals("Já faz mais de uma semana. Faça uma nova cópia.", infoBackup(hoje.minusDays(8), hoje).dica)
    }
}
