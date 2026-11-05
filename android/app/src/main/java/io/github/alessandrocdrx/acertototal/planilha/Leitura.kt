package io.github.alessandrocdrx.acertototal.planilha

import io.github.alessandrocdrx.acertototal.dominio.Documento
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.time.LocalDate

/** Datas das planilhas. Só aceita datas que existem (R39): 32/13/2026 e 30/02/2026 viram null. */
object Datas {
    private val BARRAS = Regex("^(\\d{1,2})[/.\\-](\\d{1,2})[/.\\-](\\d{4}|\\d{2})$")
    private val ISO = Regex("^(\\d{4})-(\\d{1,2})-(\\d{1,2})(?:[T ].*)?$")
    private val SERIAL = Regex("^\\d+(\\.\\d+)?$")

    private fun real(a: Int, m: Int, d: Int): LocalDate? = try {
        LocalDate.of(a, m, d)
    } catch (_: Exception) {
        null
    }

    /**
     * Aceita dd/mm/aaaa (também com - ou .), dd/mm/aa, aaaa-mm-dd e o número de data do Excel.
     * O Excel guarda datas como número de dias desde 30/12/1899. Como um número solto também pode ser texto,
     * só vale como data entre 20000 e 80000 (anos 1954 a 2118).
     */
    fun parse(v: String?): LocalDate? {
        val s = v.orEmpty().trim()
        if (s.isEmpty()) return null
        BARRAS.matchEntire(s)?.let { m ->
            val ano = m.groupValues[3].let { if (it.length == 2) "20$it" else it }.toInt()
            return real(ano, m.groupValues[2].toInt(), m.groupValues[1].toInt())
        }
        ISO.matchEntire(s)?.let { m -> return real(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt()) }
        if (SERIAL.matches(s)) {
            val dias = s.toDouble().toLong()
            if (dias in 20000..80000) return LocalDate.of(1899, 12, 30).plusDays(dias)
        }
        return null
    }
}

/** CPF/CNPJ lido de uma célula. */
data class DocLido(
    val d: String,
    val ok: Boolean,
    val raw: String,
    /** O Excel tirou o zero da frente e o app completou. */
    val zeros: Boolean = false,
    /** Dígito errado aceito só porque é a versão de teste. */
    val teste: Boolean = false,
)

object LeituraDoc {
    /**
     * O Excel apaga o zero da frente de números (01234567890 vira 1234567890): quando completar com zeros
     * dá um documento válido, aceita e avisa.
     */
    fun ler(v: String?, producao: Boolean): DocLido {
        val raw = Campos.fmtCel(v)
        val d = Documento.soDigitos(v)
        if (d.isEmpty()) return DocLido("", false, raw)
        if (Documento.valido(d)) return DocLido(d, true, raw)
        if (Regex("^\\d+$").matches(raw.trim()) && d.length < 14) {
            val tamanhos = if (d.length < 11) listOf(11, 14) else listOf(14)
            for (t in tamanhos) {
                val p = d.padStart(t, '0')
                if (Documento.valido(p)) return DocLido(p, true, raw, zeros = true)
            }
        }
        if (Documento.aceito(d, producao)) return DocLido(d, true, raw, teste = true)
        return DocLido(d, false, raw)
    }
}

/** CSV do Excel brasileiro: separador ";" e acentos em Windows-1252. */
object Csv {
    /** UTF-8 se for válido; senão Windows-1252. Tira a marca de ordem de bytes do começo. */
    fun decodificar(bytes: ByteArray): String {
        val texto = try {
            Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes)).toString()
        } catch (_: CharacterCodingException) {
            String(bytes, charset("windows-1252"))
        }
        return texto.removePrefix("﻿")
    }

    /** O separador que mais aparece nas primeiras linhas; empate fica com ";", que é o do Excel brasileiro. */
    fun separador(texto: String): Char {
        val amostra = texto.lineSequence().take(5).joinToString("\n")
        val cont = listOf(';', ',', '\t').associateWith { c -> amostra.count { it == c } }
        return cont.maxByOrNull { it.value }?.takeIf { it.value > 0 }?.key ?: ';'
    }

    /** Lê aspas (com "" dentro) e quebras de linha dentro das aspas. Tudo vira texto puro (R49). */
    fun ler(texto: String, sep: Char = separador(texto)): List<List<String>> {
        val linhas = mutableListOf<List<String>>()
        var linha = mutableListOf<String>()
        val atual = StringBuilder()
        var aspas = false
        var i = 0
        fun fecharCelula() { linha += atual.toString(); atual.setLength(0) }
        fun fecharLinha() { fecharCelula(); linhas += linha.dropLastWhile { it.isEmpty() }; linha = mutableListOf() }
        while (i < texto.length) {
            val c = texto[i]
            when {
                aspas -> if (c == '"') {
                    if (i + 1 < texto.length && texto[i + 1] == '"') { atual.append('"'); i++ } else aspas = false
                } else atual.append(c)
                c == '"' -> aspas = true
                c == sep -> fecharCelula()
                c == '\r' -> Unit
                c == '\n' -> fecharLinha()
                else -> atual.append(c)
            }
            i++
        }
        if (atual.isNotEmpty() || linha.isNotEmpty()) fecharLinha()
        return linhas
    }
}

/** O que o arquivo escolhido é, pelo conteúdo e não pelo nome (R48): no Android, arquivos do WhatsApp e do Drive chegam sem extensão. */
enum class TipoArquivo { XLSX, XLS_ANTIGO, IMAGEM, PDF, TEXTO, OUTRO }

sealed interface Leitura {
    data class Ok(val abas: List<AbaXlsx>) : Leitura
    /** [modelo]: oferece baixar a planilha modelo junto com a mensagem. */
    data class Erro(val titulo: String, val texto: String, val modelo: Boolean = false) : Leitura
}

object LeitorArquivo {
    const val LIMITE_BYTES = 10 * 1024 * 1024
    const val LIMITE_LINHAS = 5000

    fun tipo(b: ByteArray): TipoArquivo {
        fun come(vararg h: Int) = b.size >= h.size && h.indices.all { (b[it].toInt() and 0xFF) == h[it] }
        return when {
            come(0x50, 0x4B, 0x03, 0x04) -> TipoArquivo.XLSX
            come(0xD0, 0xCF, 0x11, 0xE0) -> TipoArquivo.XLS_ANTIGO
            come(0xFF, 0xD8, 0xFF) || come(0x89, 0x50, 0x4E, 0x47) || come(0x47, 0x49, 0x46, 0x38) || come(0x52, 0x49, 0x46, 0x46) -> TipoArquivo.IMAGEM
            come(0x25, 0x50, 0x44, 0x46) -> TipoArquivo.PDF
            else -> {
                val ruins = b.take(2048).count { x -> val c = x.toInt() and 0xFF; c == 0 || c < 9 || (c in 14..31 && c != 27) }
                if (ruins == 0) TipoArquivo.TEXTO else TipoArquivo.OUTRO
            }
        }
    }

    fun ler(bytes: ByteArray): Leitura {
        if (bytes.isEmpty()) return Leitura.Erro("O arquivo está vazio", "Abra a planilha no Excel, confira se ela tem dados e salve de novo.")
        if (bytes.size > LIMITE_BYTES) {
            return Leitura.Erro("Arquivo grande demais", "O limite é 10 MB. Divida a planilha em partes menores e importe uma de cada vez.")
        }
        return when (tipo(bytes)) {
            TipoArquivo.IMAGEM -> Leitura.Erro("Esse arquivo não é uma planilha", "É uma imagem. Escolha o arquivo do Excel (.xlsx) ou CSV.")
            TipoArquivo.PDF -> Leitura.Erro("Esse arquivo não é uma planilha", "É um PDF. Escolha o arquivo do Excel (.xlsx) ou CSV.")
            TipoArquivo.OUTRO -> Leitura.Erro("Esse arquivo não é uma planilha", "Escolha um arquivo do Excel (.xlsx) ou CSV.")
            TipoArquivo.XLS_ANTIGO -> Leitura.Erro(
                "A planilha é do Excel antigo ou tem senha",
                "Abra no Excel, tire a senha se houver e use “Salvar como” › Pasta de Trabalho do Excel (.xlsx). Depois importe aqui.",
            )
            TipoArquivo.TEXTO -> Leitura.Ok(listOf(AbaXlsx("CSV", Csv.ler(Csv.decodificar(bytes)))))
            TipoArquivo.XLSX -> try {
                Leitura.Ok(Xlsx.ler(bytes))
            } catch (_: Exception) {
                Leitura.Erro(
                    "Não deu para ler o arquivo",
                    "O arquivo pode estar danificado. Abra no Excel e use “Salvar como” › Pasta de Trabalho do Excel (.xlsx).",
                )
            }
        }
    }
}
