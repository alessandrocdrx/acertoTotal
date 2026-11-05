package io.github.alessandrocdrx.acertototal.dominio

import io.github.alessandrocdrx.acertototal.dados.ContaComCliente
import java.time.LocalDate
import kotlin.math.roundToInt

data class MesTotal(val rotulo: String, val total: Long)
data class FaixaAtraso(val rotulo: String, val quantas: Int, val total: Long)
data class Devedor(val clienteId: String, val nome: String, val quantas: Int, val total: Long, val maisAntiga: Int)

/** Tudo o que a aba Números mostra. Portado de `stats()` do protótipo. */
data class Estatisticas(
    val recebido: Long,
    /** Contas que venceram dentro do período, e quanto delas já foi pago. */
    val vencido: Long,
    val vencidoPago: Long,
    val temVencidas: Boolean,
    val meses: List<MesTotal>,
    val faixas: List<FaixaAtraso>,
    val atrasadas: Int,
    val devedores: List<Devedor>,
    /** Média de dias entre vencimento e pagamento; negativo = pagou antes. Null sem pagamentos no período. */
    val prazoMedioDias: Int?,
    val proximasQuantas: Int,
    val proximasTotal: Long,
) {
    /** Taxa de recebimento em %, ou null quando ainda não há contas vencidas no período. */
    val percentualRecebido: Int? =
        if (temVencidas && vencido > 0) (vencidoPago * 100.0 / vencido).roundToInt() else null
}

private val MES_CURTO = listOf("jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez")

fun calcularEstatisticas(contas: List<ContaComCliente>, periodoMeses: Int, hoje: LocalDate): Estatisticas {
    val inicio = hoje.withDayOfMonth(1).minusMonths((periodoMeses - 1).toLong())
    val pagas = contas.filter { it.pagoEm != null }
    val recebidas = pagas.filter { c -> c.pagoEm!!.let { !it.isBefore(inicio) && !it.isAfter(hoje) } }
    val vencidas = contas.filter { !it.venc.isBefore(inicio) && !it.venc.isAfter(hoje) }
    val abertas = contas.filter { it.pagoEm == null }
    val atrasadas = abertas.filter { it.venc.isBefore(hoje) }
    val proximas = abertas.filter { !it.venc.isBefore(hoje) && !it.venc.isAfter(hoje.plusDays(7)) }

    val meses = (5 downTo 0).map { i ->
        val m = hoje.withDayOfMonth(1).minusMonths(i.toLong())
        MesTotal(
            MES_CURTO[m.monthValue - 1],
            pagas.filter { it.pagoEm!!.year == m.year && it.pagoEm.monthValue == m.monthValue }.sumOf { it.valor },
        )
    }
    val faixas = listOf("1 a 7 dias" to 1..7, "8 a 30 dias" to 8..30, "Mais de 30 dias" to 31..Int.MAX_VALUE).map { (rotulo, dias) ->
        val l = atrasadas.filter { diasEntre(it.venc, hoje) in dias }
        FaixaAtraso(rotulo, l.size, l.sumOf { it.valor })
    }
    val devedores = atrasadas.groupBy { it.clienteId }.map { (id, l) ->
        Devedor(id, l.first().clienteNome, l.size, l.sumOf { it.valor }, l.maxOf { diasEntre(it.venc, hoje) })
    }.sortedByDescending { it.total }.take(5)

    return Estatisticas(
        recebido = recebidas.sumOf { it.valor },
        vencido = vencidas.sumOf { it.valor },
        vencidoPago = vencidas.filter { it.pagoEm != null }.sumOf { it.valor },
        temVencidas = vencidas.isNotEmpty(),
        meses = meses,
        faixas = faixas,
        atrasadas = atrasadas.size,
        devedores = devedores,
        prazoMedioDias = if (recebidas.isEmpty()) null
        else (recebidas.sumOf { diasEntre(it.venc, it.pagoEm!!) }.toDouble() / recebidas.size).roundToInt(),
        proximasQuantas = proximas.size,
        proximasTotal = proximas.sumOf { it.valor },
    )
}

/** Só na versão de teste: apagar tudo, confirmado digitando APAGAR (R60). Maiúsculas ou minúsculas. */
const val PALAVRA_APAGAR = "APAGAR"

fun confereApagar(texto: String?): Boolean = texto.orEmpty().trim().uppercase() == PALAVRA_APAGAR
