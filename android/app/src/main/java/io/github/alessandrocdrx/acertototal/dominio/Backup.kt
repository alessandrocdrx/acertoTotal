package io.github.alessandrocdrx.acertototal.dominio

import java.time.LocalDate

/** Como a cópia de segurança aparece: o tom reaproveita as cores das situações (vermelho, amarelo, verde). */
data class InfoBackup(val tom: Situacao, val texto: String, val dica: String)

/** Portado de `backupInfo()` do protótipo. */
fun infoBackup(ultimo: LocalDate?, hoje: LocalDate): InfoBackup {
    if (ultimo == null) {
        return InfoBackup(Situacao.ATRASADA, "Nunca feita", "Faça agora: é o que salva seus dados se o celular for trocado ou perdido.")
    }
    val d = diasEntre(ultimo, hoje)
    return InfoBackup(
        tom = if (d > 7) Situacao.PENDENTE else Situacao.PAGA,
        texto = when {
            d <= 0 -> "Feita hoje"
            d == 1 -> "Feita ontem"
            else -> "Feita há $d dias"
        },
        dica = if (d > 7) "Já faz mais de uma semana. Faça uma nova cópia." else "Última cópia em ${dataCompleta(ultimo)}.",
    )
}

/** Cópia pendente: ponto vermelho no ☰ e aviso dentro do Menu, sem ocupar o topo da lista (R78). */
fun backupPendente(ultimo: LocalDate?, hoje: LocalDate): Boolean = ultimo == null || diasEntre(ultimo, hoje) > 7
