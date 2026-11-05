package io.github.alessandrocdrx.acertototal.ui

import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import io.github.alessandrocdrx.acertototal.dados.Anexo
import io.github.alessandrocdrx.acertototal.dados.Comprovantes
import io.github.alessandrocdrx.acertototal.dados.ResultadoAnexo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Comprovante da conta: anexar (câmera ou arquivo), ver, compartilhar, salvar em Downloads e remover (R27, R73).
 * Serve para antes da baixa (o anexo espera o "Marcar como paga") e para a conta já paga.
 */
@Composable
fun SecaoComprovante(
    atual: Anexo?,
    aoAnexar: (Anexo) -> Unit,
    aoRemover: () -> Unit,
) {
    val ctx = LocalContext.current
    val escopo = rememberCoroutineScope()
    var retorno by remember { mutableStateOf<Retorno?>(null) }
    var ocupado by remember { mutableStateOf(false) }
    var vendo by remember { mutableStateOf(false) }

    fun receber(uri: Uri, nome: String?) {
        ocupado = true
        retorno = null
        escopo.launch {
            val r = withContext(Dispatchers.IO) { Comprovantes.guardar(ctx, uri, nome) }
            ocupado = false
            when (r) {
                is ResultadoAnexo.Ok -> { aoAnexar(r.anexo); retorno = Retorno("Comprovante anexado.") }
                is ResultadoAnexo.Erro -> retorno = Retorno(r.mensagem, erro = true)
            }
        }
    }

    val escolher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val nome = runCatching {
                ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                    if (c.moveToFirst()) c.getString(0) else null
                }
            }.getOrNull()
            receber(uri, nome)
        }
    }
    val fotoUri = remember {
        val pasta = File(ctx.cacheDir, "camera").apply { mkdirs() }
        FileProvider.getUriForFile(ctx, ctx.packageName + ".arquivos", File(pasta, "foto.jpg"))
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) receber(fotoUri, "foto-comprovante.jpg")
    }

    fun bytesDoAnexo(a: Anexo) = Comprovantes.ler(ctx, a.arquivo)

    Text("Comprovante", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    if (atual == null) {
        Text(
            "Opcional. Foto ou PDF do pagamento, guardado só neste aparelho.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        BotaoBloco(
            if (ocupado) "Guardando…" else "Tirar foto", habilitado = !ocupado,
            aoClicar = { runCatching { camera.launch(fotoUri) }.onFailure { retorno = Retorno("Não encontrei o app da câmera. Use “Escolher foto ou PDF”.", erro = true) } },
        )
        BotaoBloco("Escolher foto ou PDF", habilitado = !ocupado, aoClicar = { escolher.launch(arrayOf("image/*", "application/pdf")) })
    } else {
        Text(
            (if (atual.tipo == "application/pdf") "PDF anexado: " else "Foto anexada: ") + atual.nome,
            fontWeight = FontWeight.SemiBold,
        )
        BotaoBloco("Ver comprovante", aoClicar = {
            if (atual.tipo.startsWith("image/")) vendo = true
            else {
                val b = bytesDoAnexo(atual)
                val ok = b != null && Arquivos.abrir(ctx, atual.nome, b, atual.tipo)
                if (!ok) retorno = Retorno("Não encontrei um app para abrir PDF. Use “Salvar na pasta Downloads”.", erro = true)
            }
        })
        BotaoBloco("Compartilhar comprovante", aoClicar = {
            val b = bytesDoAnexo(atual)
            val ok = b != null && Arquivos.compartilhar(ctx, atual.nome, b, atual.tipo, "Enviar comprovante")
            if (!ok) retorno = Retorno("Não foi possível compartilhar. Tente salvar na pasta Downloads.", erro = true)
        })
        BotaoBloco("Salvar na pasta Downloads", aoClicar = {
            val b = bytesDoAnexo(atual)
            val ok = b != null && Arquivos.salvarEmDownloads(ctx, atual.nome, b, atual.tipo)
            retorno = if (ok) Retorno("Comprovante salvo em Downloads: ${atual.nome}")
            else Retorno("Não foi possível salvar. Confira se há espaço no aparelho.", erro = true)
        })
        BotaoBloco("Trocar comprovante", habilitado = !ocupado, aoClicar = { escolher.launch(arrayOf("image/*", "application/pdf")) })
        BotaoBloco("Remover comprovante", perigo = true, aoClicar = aoRemover)
    }
    retorno?.let {
        Text(
            it.texto,
            color = if (it.erro) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
    }
    if (vendo && atual != null) VerFoto(atual, aoFechar = { vendo = false })
}

/** Foto em tela cheia com saída escrita (R69). */
@Composable
private fun VerFoto(anexo: Anexo, aoFechar: () -> Unit) {
    val ctx = LocalContext.current
    val bmp by produceState<android.graphics.Bitmap?>(null, anexo.arquivo) {
        value = withContext(Dispatchers.IO) {
            Comprovantes.ler(ctx, anexo.arquivo)?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
        }
    }
    Dialog(onDismissRequest = aoFechar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.fillMaxSize().background(Color.Black).statusBarsPadding().navigationBarsPadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val b = bmp
            if (b == null) Text("Abrindo a foto…", color = Color.White, modifier = Modifier.weight(1f))
            else Image(
                b.asImageBitmap(), contentDescription = "Foto do comprovante ${anexo.nome}",
                modifier = Modifier.weight(1f).fillMaxWidth(), contentScale = ContentScale.Fit,
            )
            BotaoBloco("Fechar", principal = true, aoClicar = aoFechar)
        }
    }
}
