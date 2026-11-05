package io.github.alessandrocdrx.acertototal.dominio

import java.math.BigDecimal
import java.math.RoundingMode

/** Valor digitado pela pessoa vira centavos. Portado de `parseMoney` do protótipo. */
object Dinheiro {
    private val SO_NUMEROS = Regex("^-?[\\d.,]+$")
    private val MILHARES = Regex("^\\d{1,3}(\\.\\d{3})+$")
    private val DECIMAL = Regex("^\\d+(\\.\\d+)?$")

    /** Aceita "150", "150,00", "R$ 1.234,56" e "1,234.56". Devolve null se não for um número. */
    fun parse(entrada: String?): Long? {
        var s = entrada.orEmpty().replace("R$", "", ignoreCase = true).filter { !it.isWhitespace() && it != ' ' }
        if (!SO_NUMEROS.matches(s) || s.none { it.isDigit() }) return null
        val negativo = s.startsWith("-")
        s = s.removePrefix("-")
        val virgula = s.lastIndexOf(',')
        val ponto = s.lastIndexOf('.')
        when {
            virgula > -1 && ponto > -1 -> {
                val decimal = if (virgula > ponto) ',' else '.'
                val milhar = if (decimal == ',') "." else ","
                s = s.replace(milhar, "").replace(decimal, '.')
            }
            virgula > -1 -> s = if (s.count { it == ',' } > 1) s.replace(",", "") else s.replace(',', '.')
            ponto > -1 && MILHARES.matches(s) -> s = s.replace(".", "")
        }
        if (!DECIMAL.matches(s)) return null
        val centavos = BigDecimal(s).setScale(2, RoundingMode.HALF_UP).movePointRight(2).toLong()
        return if (negativo) -centavos else centavos
    }

    /** Para preencher o campo ao editar: 15000 vira "150,00". */
    fun paraCampo(centavos: Long): String = "%d,%02d".format(centavos / 100, centavos % 100)
}
