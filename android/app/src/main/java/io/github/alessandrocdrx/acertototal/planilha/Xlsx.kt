package io.github.alessandrocdrx.acertototal.planilha

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Uma aba da planilha: o nome e as linhas, todas as células como texto. */
data class AbaXlsx(val nome: String, val linhas: List<List<String>>)

/**
 * Gerador e leitor de .xlsx sem biblioteca externa: um .xlsx é um zip de arquivos XML.
 * Escreve com textos na própria célula (sem tabela de textos compartilhados) e lê os dois jeitos.
 */
object Xlsx {
    private const val NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
    private const val NS_REL = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
    private const val NS_PKG = "http://schemas.openxmlformats.org/package/2006/relationships"
    private const val CABECALHO = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"

    // ------------------------------------------------------------------ escrever

    fun escrever(abas: List<AbaXlsx>): ByteArray {
        require(abas.isNotEmpty()) { "A planilha precisa de ao menos uma aba" }
        val nomes = nomesUnicos(abas.map { it.nome })
        val saida = ByteArrayOutputStream()
        ZipOutputStream(saida).use { zip ->
            fun arquivo(nome: String, conteudo: String) {
                zip.putNextEntry(ZipEntry(nome))
                zip.write(conteudo.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            val sobre = StringBuilder(CABECALHO).append("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">")
                .append("<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>")
                .append("<Default Extension=\"xml\" ContentType=\"application/xml\"/>")
                .append("<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>")
                .append("<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>")
            abas.indices.forEach { i ->
                sobre.append("<Override PartName=\"/xl/worksheets/sheet${i + 1}.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>")
            }
            arquivo("[Content_Types].xml", sobre.append("</Types>").toString())
            arquivo(
                "_rels/.rels",
                CABECALHO + "<Relationships xmlns=\"$NS_PKG\"><Relationship Id=\"rId1\" Type=\"$NS_REL/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>",
            )
            val livro = StringBuilder(CABECALHO).append("<workbook xmlns=\"$NS\" xmlns:r=\"$NS_REL\"><sheets>")
            nomes.forEachIndexed { i, n -> livro.append("<sheet name=\"${esc(n)}\" sheetId=\"${i + 1}\" r:id=\"rId${i + 1}\"/>") }
            arquivo("xl/workbook.xml", livro.append("</sheets></workbook>").toString())
            val rels = StringBuilder(CABECALHO).append("<Relationships xmlns=\"$NS_PKG\">")
            abas.indices.forEach { i ->
                rels.append("<Relationship Id=\"rId${i + 1}\" Type=\"$NS_REL/worksheet\" Target=\"worksheets/sheet${i + 1}.xml\"/>")
            }
            rels.append("<Relationship Id=\"rId${abas.size + 1}\" Type=\"$NS_REL/styles\" Target=\"styles.xml\"/>")
            arquivo("xl/_rels/workbook.xml.rels", rels.append("</Relationships>").toString())
            arquivo("xl/styles.xml", ESTILOS)
            abas.forEachIndexed { i, a -> arquivo("xl/worksheets/sheet${i + 1}.xml", planilha(a)) }
        }
        return saida.toByteArray()
    }

    /** Estilo 0 = normal, estilo 1 = negrito (primeira linha). */
    private val ESTILOS = CABECALHO + "<styleSheet xmlns=\"$NS\">" +
        "<fonts count=\"2\"><font><sz val=\"11\"/><name val=\"Calibri\"/></font><font><b/><sz val=\"11\"/><name val=\"Calibri\"/></font></fonts>" +
        "<fills count=\"2\"><fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"gray125\"/></fill></fills>" +
        "<borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders>" +
        "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>" +
        "<cellXfs count=\"2\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>" +
        "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/></cellXfs>" +
        "<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles></styleSheet>"

    private fun planilha(aba: AbaXlsx): String {
        val colunas = aba.linhas.maxOfOrNull { it.size } ?: 0
        val sb = StringBuilder(CABECALHO).append("<worksheet xmlns=\"$NS\">")
        sb.append("<sheetViews><sheetView workbookViewId=\"0\"><pane ySplit=\"1\" topLeftCell=\"A2\" activePane=\"bottomLeft\" state=\"frozen\"/></sheetView></sheetViews>")
        if (colunas > 0) {
            sb.append("<cols>")
            for (c in 0 until colunas) {
                val maior = aba.linhas.maxOf { it.getOrNull(c)?.length ?: 0 }
                // Largura pelo maior texto da coluna, entre 12 e 60 caracteres; a primeira linha longa de "Como editar" não estica.
                val largura = if (aba.linhas.size > 1 && colunas == 1) 100 else maior.coerceIn(12, 60) + 2
                sb.append("<col min=\"${c + 1}\" max=\"${c + 1}\" width=\"$largura\" customWidth=\"1\"/>")
            }
            sb.append("</cols>")
        }
        sb.append("<sheetData>")
        aba.linhas.forEachIndexed { r, linha ->
            sb.append("<row r=\"${r + 1}\">")
            linha.forEachIndexed { c, valor ->
                if (valor.isNotEmpty()) {
                    sb.append("<c r=\"${coluna(c)}${r + 1}\" t=\"inlineStr\"").append(if (r == 0) " s=\"1\"" else "").append("><is><t xml:space=\"preserve\">")
                        .append(esc(valor)).append("</t></is></c>")
                }
            }
            sb.append("</row>")
        }
        return sb.append("</sheetData></worksheet>").toString()
    }

    /** 0 → A, 25 → Z, 26 → AA. */
    fun coluna(indice: Int): String {
        var n = indice
        val sb = StringBuilder()
        do {
            sb.insert(0, 'A' + n % 26)
            n = n / 26 - 1
        } while (n >= 0)
        return sb.toString()
    }

    private fun indiceDaColuna(ref: String): Int? {
        val letras = ref.takeWhile { it.isLetter() }.uppercase()
        if (letras.isEmpty()) return null
        return letras.fold(0) { acc, ch -> acc * 26 + (ch - 'A' + 1) } - 1
    }

    /** O Excel recusa nomes de aba com mais de 31 letras, com [ ] : * ? / \ ou repetidos. */
    private fun nomesUnicos(nomes: List<String>): List<String> {
        val usados = mutableSetOf<String>()
        return nomes.map { original ->
            val base = original.replace(Regex("[\\[\\]:*?/\\\\]"), " ").trim().ifEmpty { "Aba" }.take(31)
            var nome = base
            var n = 2
            while (!usados.add(nome.lowercase())) nome = base.take(31 - " ($n)".length) + " ($n)".also { n++ }
            nome
        }
    }

    /** Tira caracteres que o XML não aceita e troca os que têm significado nele. */
    private fun esc(s: String): String {
        val sb = StringBuilder(s.length + 8)
        for (ch in s) {
            when {
                ch == '&' -> sb.append("&amp;")
                ch == '<' -> sb.append("&lt;")
                ch == '>' -> sb.append("&gt;")
                ch == '"' -> sb.append("&quot;")
                ch.code < 0x20 && ch != '\t' && ch != '\n' && ch != '\r' -> Unit
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    // ------------------------------------------------------------------ ler

    class ArquivoInvalido(mensagem: String) : Exception(mensagem)

    fun ler(bytes: ByteArray): List<AbaXlsx> {
        val partes = mutableMapOf<String, ByteArray>()
        try {
            ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                var e = zip.nextEntry
                while (e != null) {
                    if (!e.isDirectory) partes[e.name.trimStart('/')] = zip.readBytes()
                    e = zip.nextEntry
                }
            }
        } catch (ex: Exception) {
            throw ArquivoInvalido("O arquivo não é um .xlsx válido")
        }
        val livro = partes["xl/workbook.xml"] ?: throw ArquivoInvalido("O arquivo não é uma planilha do Excel")
        val alvos = alvosDasRelacoes(partes["xl/_rels/workbook.xml.rels"])
        val textos = partes["xl/sharedStrings.xml"]?.let(::lerTextosCompartilhados).orEmpty()
        val abas = mutableListOf<AbaXlsx>()
        val p = novoLeitor(livro)
        while (p.next() != XmlPullParser.END_DOCUMENT) {
            if (p.eventType == XmlPullParser.START_TAG && p.name == "sheet") {
                val nome = p.getAttributeValue(null, "name").orEmpty()
                val id = p.getAttributeValue(null, "r:id") ?: p.getAttributeValue(NS_REL, "id")
                val caminho = alvos[id]?.let { if (it.startsWith("/")) it.trimStart('/') else "xl/$it" }
                val dados = caminho?.let(partes::get) ?: continue
                abas += AbaXlsx(nome, lerPlanilha(dados, textos))
            }
        }
        return abas
    }

    private fun novoLeitor(bytes: ByteArray): XmlPullParser =
        XmlPullParserFactory.newInstance().newPullParser().apply { setInput(ByteArrayInputStream(bytes), null) }

    private fun alvosDasRelacoes(bytes: ByteArray?): Map<String, String> {
        if (bytes == null) return emptyMap()
        val m = mutableMapOf<String, String>()
        val p = novoLeitor(bytes)
        while (p.next() != XmlPullParser.END_DOCUMENT) {
            if (p.eventType == XmlPullParser.START_TAG && p.name == "Relationship") {
                m[p.getAttributeValue(null, "Id").orEmpty()] = p.getAttributeValue(null, "Target").orEmpty()
            }
        }
        return m
    }

    private fun lerTextosCompartilhados(bytes: ByteArray): List<String> {
        val lista = mutableListOf<String>()
        val p = novoLeitor(bytes)
        var atual: StringBuilder? = null
        var fonetico = 0
        while (p.next() != XmlPullParser.END_DOCUMENT) {
            when (p.eventType) {
                XmlPullParser.START_TAG -> when (p.name) {
                    "si" -> atual = StringBuilder()
                    "rPh" -> fonetico++
                    "t" -> { val t = p.nextText(); if (fonetico == 0) atual?.append(t) }
                }
                XmlPullParser.END_TAG -> when (p.name) {
                    "rPh" -> fonetico--
                    "si" -> { lista += atual?.toString().orEmpty(); atual = null }
                }
            }
        }
        return lista
    }

    private fun lerPlanilha(bytes: ByteArray, textos: List<String>): List<List<String>> {
        val linhas = mutableListOf<List<String>>()
        val p = novoLeitor(bytes)
        var celulas = sortedMapOf<Int, String>()
        var proxima = 0
        var linhaAtual = -1
        while (p.next() != XmlPullParser.END_DOCUMENT) {
            if (p.eventType == XmlPullParser.START_TAG) {
                when (p.name) {
                    "row" -> {
                        celulas = sortedMapOf()
                        proxima = 0
                        linhaAtual = (p.getAttributeValue(null, "r")?.toIntOrNull() ?: (linhas.size + 1)) - 1
                        // Linhas em branco no meio do arquivo continuam existindo: o número da linha tem que bater com o do Excel.
                        while (linhas.size < linhaAtual) linhas += emptyList<String>()
                    }
                    "c" -> {
                        val col = indiceDaColuna(p.getAttributeValue(null, "r").orEmpty()) ?: proxima
                        val valor = lerCelula(p, p.getAttributeValue(null, "t"), textos)
                        if (valor.isNotEmpty()) celulas[col] = valor
                        proxima = col + 1
                    }
                }
            } else if (p.eventType == XmlPullParser.END_TAG && p.name == "row" && linhaAtual >= 0) {
                val largura = (celulas.keys.maxOrNull() ?: -1) + 1
                linhas += List(largura) { celulas[it].orEmpty() }
                linhaAtual = -1
            }
        }
        return linhas
    }

    /** Lê até o fim da tag <c>. Texto na própria célula, texto compartilhado ou número: tudo vira texto. */
    private fun lerCelula(p: XmlPullParser, tipo: String?, textos: List<String>): String {
        var valor = ""
        val inline = StringBuilder()
        var profundidade = 1
        while (profundidade > 0) {
            when (p.next()) {
                XmlPullParser.START_TAG -> {
                    profundidade++
                    when (p.name) {
                        "v" -> { valor = p.nextText(); profundidade-- }
                        "t" -> { inline.append(p.nextText()); profundidade-- }
                    }
                }
                XmlPullParser.END_TAG -> profundidade--
                XmlPullParser.END_DOCUMENT -> return valor
            }
        }
        return when (tipo) {
            "s" -> valor.trim().toIntOrNull()?.let { textos.getOrNull(it) }.orEmpty()
            "inlineStr" -> inline.toString()
            "b" -> if (valor == "1") "VERDADEIRO" else "FALSO"
            else -> if (inline.isNotEmpty()) inline.toString() else valor
        }
    }
}
