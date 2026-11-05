package io.github.alessandrocdrx.acertototal.dominio

import java.net.URLEncoder
import java.time.LocalDate

/** "Meu negócio": o nome e a chave Pix que vão na mensagem de cobrança. Ficam só no aparelho. */
data class Negocio(val nome: String = "", val pix: String = "")

/** O que a mensagem precisa saber de cada conta em aberto. */
data class ContaParaCobrar(val descricao: String, val valor: Long, val venc: LocalDate)

/** Cobrar pelo WhatsApp (R81): a mensagem é montada no aparelho e o app nunca envia nada sozinho. */
object Cobranca {
    /**
     * Telefone no formato do WhatsApp: `55` + DDD + número. Aceita com ou sem `55`; devolve vazio quando não dá
     * para saber (nada de link quebrado).
     */
    fun telefoneWhats(texto: String): String {
        val d = texto.filter { it.isDigit() }
        if (d.startsWith("55") && (d.length == 12 || d.length == 13)) return d
        if (d.length == 10 || d.length == 11) return "55$d"
        return ""
    }

    fun linkWhats(telefone: String, mensagem: String): String =
        "https://wa.me/$telefone?text=" + URLEncoder.encode(mensagem, "UTF-8").replace("+", "%20")

    private fun semEspacoDuro(s: String) = s.replace(' ', ' ')

    fun mensagem(clienteNome: String, contas: List<ContaParaCobrar>, negocio: Negocio, hoje: LocalDate): String {
        val primeiro = clienteNome.trim().split(Regex("\\s+")).firstOrNull().orEmpty()
        val lista = contas.sortedBy { it.venc }
        fun linha(c: ContaParaCobrar): String {
            val atraso = if (c.venc.isBefore(hoje)) diasEntre(c.venc, hoje) else 0
            val prazo = when {
                atraso > 0 -> ", venceu em ${dataCurta(c.venc, hoje)} (há ${plural(atraso, "dia")})"
                c.venc == hoje -> ", vence hoje"
                else -> ", vence em ${dataCurta(c.venc, hoje)}"
            }
            return "• ${c.descricao} — ${brl(c.valor)}$prazo"
        }
        val t = StringBuilder("Olá, $primeiro! Tudo bem?\n")
        if (lista.size == 1) t.append("Passando para lembrar de uma conta em aberto:\n").append(linha(lista[0]))
        else {
            t.append("Passando para lembrar de ${lista.size} contas em aberto:\n")
            t.append(lista.take(8).joinToString("\n") { linha(it) })
            if (lista.size > 8) t.append("\n• e mais ${plural(lista.size - 8, "conta")}")
            t.append("\nTotal: ${brl(lista.sumOf { it.valor })}.")
        }
        if (negocio.pix.isNotBlank()) t.append("\n\nPode pagar pelo Pix: ${negocio.pix}")
        t.append("\n\nQualquer dúvida, é só me chamar.")
        if (negocio.nome.isNotBlank()) t.append("\n").append(negocio.nome)
        return semEspacoDuro(t.toString())
    }
}
