package io.github.alessandrocdrx.acertototal.dados

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

/** Um comprovante já guardado na pasta do app. */
data class Anexo(val arquivo: String, val nome: String, val tipo: String)

fun Conta.anexo(): Anexo? = compArquivo?.let { Anexo(it, compNome ?: "comprovante", compTipo ?: "image/jpeg") }

fun Conta.comAnexo(a: Anexo?): Conta = copy(compArquivo = a?.arquivo, compNome = a?.nome, compTipo = a?.tipo)

sealed interface ResultadoAnexo {
    data class Ok(val anexo: Anexo) : ResultadoAnexo
    data class Erro(val mensagem: String) : ResultadoAnexo
}

/** O que a pessoa pode anexar: foto (JPEG ou PNG) ou PDF. O tipo vem do conteúdo, não do nome (R48). */
enum class TipoComprovante(val mime: String, val extensao: String) {
    JPEG("image/jpeg", "jpg"), PNG("image/png", "png"), PDF("application/pdf", "pdf");

    val ehImagem get() = this != PDF
}

object Comprovantes {
    /** R27: o armazenamento é pequeno, então a foto nunca passa de 1280 px no lado maior. */
    const val LADO_MAXIMO = 1280
    const val QUALIDADE_JPEG = 85
    const val MAX_ENTRADA = 25L * 1024 * 1024
    const val MAX_PDF = 5L * 1024 * 1024

    fun tipoPeloConteudo(b: ByteArray): TipoComprovante? = when {
        b.size >= 3 && b[0] == 0xFF.toByte() && b[1] == 0xD8.toByte() && b[2] == 0xFF.toByte() -> TipoComprovante.JPEG
        b.size >= 4 && b[0] == 0x89.toByte() && b[1] == 'P'.code.toByte() && b[2] == 'N'.code.toByte() && b[3] == 'G'.code.toByte() -> TipoComprovante.PNG
        b.size >= 4 && b[0] == '%'.code.toByte() && b[1] == 'P'.code.toByte() && b[2] == 'D'.code.toByte() && b[3] == 'F'.code.toByte() -> TipoComprovante.PDF
        else -> null
    }

    /** Tamanho depois de reduzir: mantém a proporção e nunca aumenta. Pura, para testar. */
    fun tamanhoReduzido(largura: Int, altura: Int, maximo: Int = LADO_MAXIMO): Pair<Int, Int> {
        val maior = maxOf(largura, altura)
        if (maior <= maximo || maior <= 0) return largura to altura
        val f = maximo.toDouble() / maior
        return maxOf(1, Math.round(largura * f).toInt()) to maxOf(1, Math.round(altura * f).toInt())
    }

    /** Potência de 2 para o decodificador já abrir a foto menor, sem encher a memória. */
    fun amostra(largura: Int, altura: Int, maximo: Int = LADO_MAXIMO): Int {
        var s = 1
        while (maxOf(largura, altura) / (s * 2) >= maximo) s *= 2
        return s
    }

    private fun pasta(ctx: Context) = File(ctx.filesDir, "comprovantes").apply { mkdirs() }

    fun arquivo(ctx: Context, nome: String) = File(pasta(ctx), nome)

    /** Lê o arquivo escolhido, confere o tipo, reduz a foto e guarda. Roda fora da tela principal. */
    fun guardar(ctx: Context, uri: Uri, nomeOriginal: String?): ResultadoAnexo {
        val bytes = try {
            ctx.contentResolver.openInputStream(uri)?.use { ent ->
                val saida = ByteArrayOutputStream()
                val buf = ByteArray(64 * 1024)
                var total = 0L
                while (true) {
                    val n = ent.read(buf)
                    if (n < 0) break
                    total += n
                    if (total > MAX_ENTRADA) return ResultadoAnexo.Erro("O arquivo é muito grande (mais de 25 MB). Escolha uma foto ou um PDF menor.")
                    saida.write(buf, 0, n)
                }
                saida.toByteArray()
            }
        } catch (_: Exception) {
            null
        } ?: return ResultadoAnexo.Erro("Não consegui abrir o arquivo. Tente escolher de novo.")
        return guardarBytes(ctx, bytes, nomeOriginal)
    }

    fun guardarBytes(ctx: Context, bytes: ByteArray, nomeOriginal: String?): ResultadoAnexo {
        val tipo = tipoPeloConteudo(bytes)
            ?: return ResultadoAnexo.Erro("Este arquivo não é uma foto nem um PDF. Escolha uma foto (JPG ou PNG) ou um PDF.")
        val final: ByteArray
        val tipoFinal: TipoComprovante
        if (tipo == TipoComprovante.PDF) {
            if (bytes.size > MAX_PDF) return ResultadoAnexo.Erro("Este PDF é grande demais (mais de 5 MB) para guardar no aparelho.")
            final = bytes
            tipoFinal = tipo
        } else {
            final = reduzirImagem(bytes) ?: return ResultadoAnexo.Erro("Não consegui ler esta foto. Tente outra.")
            tipoFinal = TipoComprovante.JPEG
        }
        val base = nomeOriginal?.substringBeforeLast('.')?.replace(Regex("[^A-Za-z0-9_-]"), "_")?.take(40)?.ifEmpty { null } ?: "comprovante"
        val arquivo = "${System.currentTimeMillis().toString(36)}-${(0..999).random()}.${tipoFinal.extensao}"
        return try {
            File(pasta(ctx), arquivo).writeBytes(final)
            ResultadoAnexo.Ok(Anexo(arquivo, "$base.${tipoFinal.extensao}", tipoFinal.mime))
        } catch (_: Exception) {
            ResultadoAnexo.Erro("Não consegui guardar o comprovante. Confira se há espaço no aparelho.")
        }
    }

    /** JPEG de no máximo [LADO_MAXIMO] px, já na posição certa (a câmera grava a foto de lado e avisa no EXIF). */
    private fun reduzirImagem(bytes: ByteArray): ByteArray? {
        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, limites)
        if (limites.outWidth <= 0 || limites.outHeight <= 0) return null
        val opc = BitmapFactory.Options().apply { inSampleSize = amostra(limites.outWidth, limites.outHeight) }
        var bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opc) ?: return null
        val (l, a) = tamanhoReduzido(bmp.width, bmp.height)
        if (l != bmp.width || a != bmp.height) bmp = Bitmap.createScaledBitmap(bmp, l, a, true)
        val orient = try {
            ExifInterface(ByteArrayInputStream(bytes)).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } catch (_: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
        val m = Matrix()
        when (orient) {
            ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
            else -> Unit
        }
        if (!m.isIdentity) bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
        val saida = ByteArrayOutputStream()
        // PNG com transparência viraria fundo preto no JPEG: pinta de branco antes.
        val plano = Bitmap.createBitmap(bmp.width, bmp.height, Bitmap.Config.ARGB_8888)
        android.graphics.Canvas(plano).apply { drawColor(android.graphics.Color.WHITE); drawBitmap(bmp, 0f, 0f, null) }
        plano.compress(Bitmap.CompressFormat.JPEG, QUALIDADE_JPEG, saida)
        return saida.toByteArray()
    }

    fun ler(ctx: Context, arquivo: String): ByteArray? = try {
        File(pasta(ctx), arquivo).takeIf { it.exists() }?.readBytes()
    } catch (_: Exception) {
        null
    }

    fun remover(ctx: Context, arquivo: String) {
        runCatching { File(pasta(ctx), arquivo).delete() }
    }

    /** Apaga arquivos que nenhuma conta usa mais (anexo cancelado, conta excluída). Roda ao abrir o app. */
    fun limparOrfaos(ctx: Context, usados: Set<String>) {
        pasta(ctx).listFiles()?.forEach { if (it.name !in usados) it.delete() }
    }

    fun apagarTodos(ctx: Context) {
        pasta(ctx).listFiles()?.forEach { it.delete() }
    }
}
