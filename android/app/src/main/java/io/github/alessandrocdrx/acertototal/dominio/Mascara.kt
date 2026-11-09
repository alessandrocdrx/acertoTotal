package io.github.alessandrocdrx.acertototal.dominio

/**
 * Máscaras dos campos (R94): a pessoa digita só números e a pontuação entra sozinha enquanto digita.
 * O campo guarda só os números; a máscara é só a forma de mostrar.
 */
object Mascara {

    /** Encaixa os dígitos no padrão ('#' = um dígito), parando no último dígito digitado. */
    fun aplicar(digitos: String, padrao: String): String {
        if (digitos.isEmpty()) return ""
        val sb = StringBuilder()
        var i = 0
        for (ch in padrao) {
            if (i >= digitos.length) break
            if (ch == '#') sb.append(digitos[i++]) else sb.append(ch)
        }
        return sb.toString()
    }

    /** (11) 3456-7890 com 10 números; (11) 98765-4321 com 11. */
    fun telefone(digitos: String): String =
        aplicar(digitos, if (digitos.length >= 11) "(##) #####-####" else "(##) ####-####")

    /** Até 11 números, CPF (000.000.000-00); de 12 a 14, CNPJ (00.000.000/0000-00). */
    fun documento(digitos: String): String =
        aplicar(digitos, if (digitos.length <= 11) "###.###.###-##" else "##.###.###/####-##")
}

object Telefone {
    const val MAX = 11

    /** Só os números, sem o 55 do Brasil na frente, até 11 (DDD + número). */
    fun digitos(texto: String?): String {
        val d = Documento.soDigitos(texto)
        val semPais = if (d.startsWith("55") && (d.length == 12 || d.length == 13)) d.substring(2) else d
        return semPais.take(MAX)
    }

    /** Como o telefone aparece: com máscara quando tem DDD (10 ou 11 números); senão, como foi escrito. */
    fun exibir(texto: String?): String {
        val d = digitos(texto)
        return if (d.length == 10 || d.length == 11) Mascara.telefone(d) else texto.orEmpty().trim()
    }
}

/** Valor em reais (R94): a pessoa digita só números e eles enchem a partir dos centavos. */
object ValorDigitado {
    /** Até 999.999.999,99. */
    const val MAX = 11

    /** Só os números, sem zeros à esquerda (que não mudam o valor). */
    fun digitos(texto: String?): String = Documento.soDigitos(texto).trimStart('0').take(MAX)

    /** "15000" vira "150,00"; "5" vira "0,05"; "150000" vira "1.500,00". */
    fun formatar(digitos: String): String {
        if (digitos.isEmpty()) return ""
        val centavos = digitos.toLong()
        val reais = (centavos / 100).toString().reversed().chunked(3).joinToString(".").reversed()
        return "$reais,${"%02d".format(centavos % 100)}"
    }

    /** Para editar uma conta: 15000 centavos vira os dígitos "15000". */
    fun deCentavos(centavos: Long): String = if (centavos <= 0) "" else centavos.toString()
}

object Email {
    private val FORMATO = Regex("""^[^\s@]+@[^\s@]+\.[^\s@]{2,}$""")
    fun valido(texto: String): Boolean = FORMATO.matches(texto.trim())
}

/**
 * Chave Pix (R94): CPF, CNPJ e telefone ganham pontuação ao salvar; e-mail e chave aleatória ficam como foram
 * escritos. Onze números com CPF válido são CPF; senão, telefone.
 */
object ChavePix {
    private val SO_NUMEROS_E_PONTUACAO = Regex("""^[\d\s.()/+-]+$""")

    fun formatar(texto: String): String {
        val t = texto.trim()
        if (!SO_NUMEROS_E_PONTUACAO.matches(t)) return t
        val d = Documento.soDigitos(t)
        return when {
            d.length == 14 && Documento.cnpjOk(d) -> Documento.formatar(d)
            d.length == 11 && !t.startsWith("+") && Documento.cpfOk(d) -> Documento.formatar(d)
            Telefone.digitos(t).length in 10..11 && (d.length in 10..11 || d.startsWith("55")) -> "+55 " + Mascara.telefone(Telefone.digitos(t))
            else -> t
        }
    }
}
