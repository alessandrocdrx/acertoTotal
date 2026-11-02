package io.github.alessandrocdrx.acertototal

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Base64
import android.webkit.JavascriptInterface
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

/**
 * Ponte chamada pela página (window.acertoTotalAndroid). Faz o papel da permissão de
 * download do artefato: recebe o Excel gerado (ou um comprovante) e salva na pasta Downloads.
 * Respostas seguem os códigos do protótipo: "saved", "rejected_extension", "unavailable".
 */
class Ponte(
    private val ctx: Context,
    private val assinatura: Assinatura? = null,
    private val atividade: Activity? = null,
) {

    /** Estado da assinatura em JSON. Vazio quando a cobrança real não existe (app de teste): a página simula. */
    @JavascriptInterface
    fun assinaturaEstado(): String = assinatura?.estado() ?: ""

    @JavascriptInterface
    fun assinar(plano: String): String = assinatura?.assinar(atividade, plano) ?: "indisponivel"

    @JavascriptInterface
    fun atualizarAssinatura() {
        assinatura?.iniciar()
    }

    /** Abre, na Google Play, a página para trocar de plano ou cancelar. */
    @JavascriptInterface
    fun gerenciarAssinatura() {
        val uri = Uri.parse("https://play.google.com/store/account/subscriptions?sku=${Assinatura.PRODUTO}&package=${ctx.packageName}")
        try {
            ctx.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
        }
    }


    /** Abre o comprovante no app de fotos ou de PDF do celular (zoom, imprimir). */
    @JavascriptInterface
    fun abrirArquivo(nome: String, base64: String): String {
        val (uri, mime) = arquivoTemporario(nome, base64) ?: return "unavailable"
        val intent = Intent(Intent.ACTION_VIEW).setDataAndType(uri, mime)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        return iniciar(intent)
    }

    /** Abre a janela de compartilhar do Android (WhatsApp, e-mail, Drive) com o comprovante. */
    @JavascriptInterface
    fun compartilharArquivo(nome: String, base64: String): String {
        val (uri, mime) = arquivoTemporario(nome, base64) ?: return "unavailable"
        val envio = Intent(Intent.ACTION_SEND).setType(mime).putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val escolha = Intent.createChooser(envio, "Enviar comprovante").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return iniciar(escolha)
    }

    private fun arquivoTemporario(nome: String, base64: String): Pair<Uri, String>? {
        val seguro = nome.replace(Regex("[^A-Za-z0-9._-]"), "_").take(120)
        val mime = MIMES[seguro.substringAfterLast('.', "").lowercase()] ?: return null
        return try {
            val pasta = File(ctx.cacheDir, "comprovantes").apply { mkdirs() }
            pasta.listFiles()?.forEach { it.delete() } // guarda só o último, para não ocupar espaço
            val arquivo = File(pasta, seguro).apply { writeBytes(Base64.decode(base64, Base64.DEFAULT)) }
            FileProvider.getUriForFile(ctx, ctx.packageName + ".arquivos", arquivo) to mime
        } catch (_: Exception) {
            null
        }
    }

    private fun iniciar(intent: Intent): String = try {
        (atividade ?: ctx).startActivity(intent)
        "ok"
    } catch (_: ActivityNotFoundException) {
        "sem_app"
    } catch (_: Exception) {
        "unavailable"
    }

    @JavascriptInterface
    fun salvarArquivo(nome: String, base64: String): String {
        val seguro = nome.replace(Regex("[^A-Za-z0-9._-]"), "_").take(120)
        val mime = MIMES[seguro.substringAfterLast('.', "").lowercase()] ?: return "rejected_extension"
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            val resolver = ctx.contentResolver
            val valores = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, seguro)
                put(MediaStore.Downloads.MIME_TYPE, mime)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, valores) ?: return "unavailable"
            resolver.openOutputStream(uri)?.use { it.write(bytes) } ?: return "unavailable"
            valores.clear()
            valores.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, valores, null, null)
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(ctx, "Salvo em Downloads: $seguro", Toast.LENGTH_LONG).show()
            }
            "saved"
        } catch (_: Exception) {
            "unavailable"
        }
    }

    private companion object {
        val MIMES = mapOf(
            "xlsx" to "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "csv" to "text/csv",
            // Comprovantes que a pessoa salva da conta paga
            "jpg" to "image/jpeg",
            "png" to "image/png",
            "pdf" to "application/pdf",
        )
    }
}
