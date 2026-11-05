package io.github.alessandrocdrx.acertototal.ui

import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File

/** Entrega de arquivos ao aparelho: salvar em Downloads e compartilhar com outros apps. */
object Arquivos {
    const val MIME_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

    private fun nomeSeguro(nome: String) = nome.replace(Regex("[^A-Za-z0-9._-]"), "_").take(120)

    /** Grava na pasta Downloads (sem pedir permissão de armazenamento). Devolve false se não deu. */
    fun salvarEmDownloads(ctx: Context, nome: String, bytes: ByteArray, mime: String = MIME_XLSX): Boolean = try {
        val seguro = nomeSeguro(nome)
        val resolver = ctx.contentResolver
        val valores = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, seguro)
            put(MediaStore.Downloads.MIME_TYPE, mime)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, valores)
        if (uri == null) false else {
            val escreveu = resolver.openOutputStream(uri)?.use { it.write(bytes); true } ?: false
            valores.clear()
            valores.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, valores, null, null)
            if (!escreveu) resolver.delete(uri, null, null)
            escreveu
        }
    } catch (_: Exception) {
        false
    }

    /** Abre o arquivo no app de fotos ou de PDF do aparelho (zoom, imprimir). */
    fun abrir(ctx: Context, nome: String, bytes: ByteArray, mime: String): Boolean = try {
        val pasta = File(ctx.cacheDir, "comprovantes").apply { mkdirs() }
        pasta.listFiles()?.forEach { it.delete() }
        val arquivo = File(pasta, nomeSeguro(nome)).apply { writeBytes(bytes) }
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".arquivos", arquivo)
        val ver = Intent(Intent.ACTION_VIEW).setDataAndType(uri, mime)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(ver)
        true
    } catch (_: Exception) {
        false
    }

    /** Abre a janela de compartilhar do Android (WhatsApp, e-mail, Drive) com o arquivo. */
    fun compartilhar(ctx: Context, nome: String, bytes: ByteArray, mime: String = MIME_XLSX, titulo: String = "Enviar planilha"): Boolean = try {
        val pasta = File(ctx.cacheDir, "comprovantes").apply { mkdirs() }
        pasta.listFiles()?.forEach { it.delete() } // guarda só o último, para não ocupar espaço
        val arquivo = File(pasta, nomeSeguro(nome)).apply { writeBytes(bytes) }
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".arquivos", arquivo)
        val envio = Intent(Intent.ACTION_SEND).setType(mime).putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        ctx.startActivity(Intent.createChooser(envio, titulo).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: Exception) {
        false
    }
}
