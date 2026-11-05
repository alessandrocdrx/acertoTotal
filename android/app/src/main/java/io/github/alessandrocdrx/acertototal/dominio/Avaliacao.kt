package io.github.alessandrocdrx.acertototal.dominio

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.ceil
import kotlin.math.max

private const val DIA_MS = 86_400_000L

/** O que fica guardado no aparelho, à parte dos dados: restaurar uma cópia ou importar não reinicia a contagem (R41). */
data class DadosAvaliacao(val inicio: Long, val visto: Long, val relogioAtrasado: Boolean)

/**
 * Período de teste grátis. [limiteDias] vem do pacote: 7 no app de teste e 32 no da Play durante a aprovação (R67, R85).
 * Com 0 não há contagem nem trava.
 */
data class Avaliacao(val inicio: Long, val limiteDias: Int, val relogioAtrasado: Boolean) {
    val fim: Long get() = inicio + limiteDias * DIA_MS

    fun diasRestantes(agora: Long): Int = max(0.0, ceil((fim - agora).toDouble() / DIA_MS)).toInt()

    fun encerrada(agora: Long): Boolean = limiteDias > 0 && (relogioAtrasado || agora >= fim)

    fun fimComoData(zona: ZoneId = ZoneId.systemDefault()): LocalDate = Instant.ofEpochMilli(fim).atZone(zona).toLocalDate()

    fun fimTexto(zona: ZoneId = ZoneId.systemDefault()): String = dataCompleta(fimComoData(zona))
}

object Teste {
    /**
     * Confere o que estava guardado com a hora de agora. Primeiro uso: começa a contar. Relógio voltado mais de 1 dia
     * em relação ao último uso: o teste é tratado como encerrado (R43 diz até onde essa defesa vai).
     */
    fun conferir(salvo: DadosAvaliacao?, agora: Long, limiteDias: Int): Pair<DadosAvaliacao, Avaliacao> {
        val base = salvo ?: DadosAvaliacao(inicio = agora, visto = agora, relogioAtrasado = false)
        val atrasado = base.relogioAtrasado || agora < base.visto - DIA_MS
        val novo = DadosAvaliacao(base.inicio, max(base.visto, agora), atrasado)
        return novo to Avaliacao(novo.inicio, limiteDias, atrasado)
    }

    /** O app só consulta: vale quando o teste acabou e não há assinatura (R41). */
    fun somenteConsulta(av: Avaliacao, assinaturaAtiva: Boolean, agora: Long): Boolean = av.encerrada(agora) && !assinaturaAtiva
}

/**
 * Trava global do modo "só consulta" (R41). A tela esconde os botões e o repositório recusa a escrita, para que
 * nenhum caminho esqueça a regra. Quem a liga é a tela principal, a partir do teste e da assinatura.
 */
object Licenca {
    @Volatile var somenteConsulta: Boolean = false
}

const val MSG_SOMENTE_CONSULTA = "Teste grátis encerrado: o app está só para consulta. Assine para continuar."

/** O que a faixa do topo mostra. [acao] é o texto do botão, ou null quando não há botão. */
data class Faixa(val textos: List<String>, val encerrado: Boolean, val acao: String?)

object FaixaTopo {
    /**
     * Na versão de teste mostra sempre. No app da Play o aviso do teste só aparece nos últimos 3 dias; antes
     * disso a contagem fica no cartão Assinatura, no Menu.
     */
    fun montar(av: Avaliacao, assinaturaAtiva: Boolean, agora: Long, producao: Boolean): Faixa {
        val linhas = mutableListOf<String>()
        var acao: String? = null
        val fim = Teste.somenteConsulta(av, assinaturaAtiva, agora)
        if (fim) {
            linhas += if (av.relogioAtrasado) "O relógio do aparelho foi atrasado. O teste grátis foi encerrado e o app está só para consulta."
            else "Teste grátis encerrado em ${av.fimTexto()}. O app está só para consulta: dá para ver tudo, mas não cadastrar, dar baixa, importar ou exportar."
            acao = "Assinar"
        } else {
            if (!producao) linhas += "Ambiente de teste: os dados podem ser apagados."
            if (av.limiteDias > 0 && !assinaturaAtiva) {
                val d = av.diasRestantes(agora)
                if (!producao || d <= 3) {
                    linhas += "Teste grátis: ${if (d <= 1) "termina hoje" else "faltam $d dias"} (até ${av.fimTexto()})."
                    if (d <= 3) acao = "Ver planos"
                }
            }
        }
        return Faixa(linhas, fim, acao)
    }

    /** O texto do cartão Assinatura, no Menu. */
    fun cartao(av: Avaliacao, assinaturaAtiva: Boolean, agora: Long): String = when {
        assinaturaAtiva -> "Ativa. Obrigado por assinar!"
        Teste.somenteConsulta(av, false, agora) -> "Teste grátis encerrado. Assine para continuar."
        av.limiteDias > 0 -> {
            val d = av.diasRestantes(agora)
            "Teste grátis: ${if (d <= 1) "termina hoje" else "faltam $d dias"} (até ${av.fimTexto()})."
        }
        else -> "Escolha um plano quando quiser."
    }
}

/** Os 4 planos do produto `acertototal_premium` (R64). Os preços reais vêm da Play; estes valem para a simulação. */
data class Plano(val id: String, val nome: String, val ciclo: String, val precoCentavos: Long, val meses: Double, val economia: Int = 0) {
    val porMesCentavos: Long get() = Math.round(precoCentavos / meses)
}

object Planos {
    val todos = listOf(
        Plano("semanal", "Semanal", "a cada semana", 299, 12.0 / 52),
        Plano("mensal", "Mensal", "todo mês", 999, 1.0),
        Plano("semestral", "Semestral", "a cada 6 meses", 5690, 6.0, economia = 5),
        Plano("anual", "Anual", "uma vez por ano", 10790, 12.0, economia = 10),
    )
    const val PADRAO = "mensal"

    fun porId(id: String) = todos.first { it.id == id }
}
