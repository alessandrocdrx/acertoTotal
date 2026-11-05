package io.github.alessandrocdrx.acertototal.dominio

import java.text.NumberFormat
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale

/** A situação vem sempre escrita, e a cor só reforça (R3). */
enum class Situacao(val rotulo: String) {
    PAGA("Paga"),
    PENDENTE("Em aberto"),
    ATRASADA("Atrasada"),
}

fun situacaoDe(venc: LocalDate, pagoEm: LocalDate?, hoje: LocalDate): Situacao = when {
    pagoEm != null -> Situacao.PAGA
    venc.isBefore(hoje) -> Situacao.ATRASADA
    else -> Situacao.PENDENTE
}

fun diasEntre(de: LocalDate, ate: LocalDate): Int = ChronoUnit.DAYS.between(de, ate).toInt()

private val PT_BR = Locale("pt", "BR")

/** Dinheiro é guardado em centavos (Long): nada de erro de arredondamento. */
fun brl(centavos: Long): String = NumberFormat.getCurrencyInstance(PT_BR).format(centavos / 100.0)

/** Sem centavos, para cartões de resumo. */
fun brlInteiro(centavos: Long): String =
    NumberFormat.getCurrencyInstance(PT_BR).apply { maximumFractionDigits = 0; minimumFractionDigits = 0 }
        .format(centavos / 100.0)

fun dataCompleta(d: LocalDate): String = "%02d/%02d/%04d".format(d.dayOfMonth, d.monthValue, d.year)

/** Data curta: sem o ano quando é o ano atual (a lista fica mais leve). */
fun dataCurta(d: LocalDate, hoje: LocalDate): String =
    if (d.year == hoje.year) "%02d/%02d".format(d.dayOfMonth, d.monthValue) else dataCompleta(d)

fun plural(n: Int, singular: String, plural: String = singular + "s"): String =
    "$n ${if (n == 1) singular else plural}"

val MESES_LONGO = listOf(
    "janeiro", "fevereiro", "março", "abril", "maio", "junho",
    "julho", "agosto", "setembro", "outubro", "novembro", "dezembro",
)

/** Texto de prazo da conta: "Atrasada há 5 dias", "Vence hoje", "Vence em 3 dias", "Paga em 12/10". */
fun infoPrazo(venc: LocalDate, pagoEm: LocalDate?, hoje: LocalDate): String =
    when (situacaoDe(venc, pagoEm, hoje)) {
        Situacao.PAGA -> "Paga em ${dataCurta(pagoEm!!, hoje)}"
        Situacao.ATRASADA -> "Atrasada há ${plural(diasEntre(venc, hoje), "dia")}"
        Situacao.PENDENTE -> diasEntre(hoje, venc).let { if (it == 0) "Vence hoje" else "Vence em ${plural(it, "dia")}" }
    }
