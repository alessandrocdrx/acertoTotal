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

/**
 * Ponte chamada pela página (window.acertoTotalAndroid). Faz o papel da permissão de
 * download do artefato: recebe o Excel gerado e salva na pasta Downloads.
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
        )
    }
}
