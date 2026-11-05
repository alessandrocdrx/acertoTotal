package io.github.alessandrocdrx.acertototal.dominio

/**
 * CPF/CNPJ: a chave do cliente (R52). Guardado só com números, mostrado com pontuação.
 * Portado de prototipo/acertototal.html (cpfOk, cnpjOk, docOk, docAceito, fmtDoc, lerDoc).
 */
object Documento {

    fun soDigitos(s: String?): String = s.orEmpty().filter { it.isDigit() }

    private fun todosIguais(d: String) = d.all { it == d[0] }

    fun cpfOk(d: String): Boolean {
        if (d.length != 11 || !d.all { it.isDigit() } || todosIguais(d)) return false
        for (n in 9..10) {
            var soma = 0
            for (i in 0 until n) soma += (d[i] - '0') * (n + 1 - i)
            if (soma * 10 % 11 % 10 != d[n] - '0') return false
        }
        return true
    }

    fun cnpjOk(d: String): Boolean {
        if (d.length != 14 || !d.all { it.isDigit() } || todosIguais(d)) return false
        val pesos = intArrayOf(6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2)
        for (n in 12..13) {
            var soma = 0
            for (i in 0 until n) soma += (d[i] - '0') * pesos[i + 13 - n]
            val r = soma % 11
            if ((if (r < 2) 0 else 11 - r) != d[n] - '0') return false
        }
        return true
    }

    fun valido(d: String): Boolean = when (d.length) {
        11 -> cpfOk(d)
        14 -> cnpjOk(d)
        else -> false
    }

    /**
     * Versão de teste (R61): aceita CPF/CNPJ com dígito errado (11 ou 14 números), sempre avisando.
     * A produção só aceita número válido.
     */
    fun aceito(d: String, producao: Boolean): Boolean =
        valido(d) || (!producao && (d.length == 11 || d.length == 14))

    fun formatar(doc: String?): String {
        val d = soDigitos(doc)
        return when (d.length) {
            11 -> "${d.substring(0, 3)}.${d.substring(3, 6)}.${d.substring(6, 9)}-${d.substring(9)}"
            14 -> "${d.substring(0, 2)}.${d.substring(2, 5)}.${d.substring(5, 8)}/${d.substring(8, 12)}-${d.substring(12)}"
            else -> d
        }
    }

    fun tipo(doc: String?): String = if (soDigitos(doc).length == 14) "CNPJ" else "CPF"

    const val AVISO_TESTE =
        "não é um número válido. Foi aceito só porque esta é a versão de teste; na versão oficial seria recusado."
}
