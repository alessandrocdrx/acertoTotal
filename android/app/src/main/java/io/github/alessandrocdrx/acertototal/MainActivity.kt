package io.github.alessandrocdrx.acertototal

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.webkit.WebViewAssetLoader

/**
 * Casca Android do protótipo acertoTotal: a interface é a mesma página testada em
 * prototipo/acertototal.html, servida das assets do APK (funciona sem internet).
 */
class MainActivity : ComponentActivity() {

    private lateinit var web: WebView
    private var arquivoCallback: ValueCallback<Array<Uri>>? = null

    private val escolherArquivo =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
            val cb = arquivoCallback
            arquivoCallback = null
            cb?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(r.resultCode, r.data))
        }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Barras do sistema claras, com ícones escuros, combinando com o tema claro do app.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )

        val raiz = FrameLayout(this).apply { setBackgroundColor(Color.WHITE) }
        web = WebView(this)
        raiz.addView(web, FrameLayout.LayoutParams(-1, -1))
        setContentView(raiz)

        // Mantém o conteúdo fora da barra de status, da barra de navegação e do teclado.
        ViewCompat.setOnApplyWindowInsetsListener(raiz) { v, insets ->
            val b = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            v.setPadding(b.left, b.top, b.right, b.bottom)
            WindowInsetsCompat.CONSUMED
        }

        val carregador = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true // os dados ficam no localStorage, como no protótipo
            allowFileAccess = false
            allowContentAccess = false
            setSupportZoom(false)
        }
        web.addJavascriptInterface(Ponte(applicationContext), "acertoTotalAndroid")

        web.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                carregador.shouldInterceptRequest(request.url)

            // Qualquer link para fora do app abre no navegador, nunca dentro da casca.
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                if (request.url.host == ORIGEM_HOST) return false
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, request.url))
                } catch (_: ActivityNotFoundException) {
                }
                return true
            }
        }

        web.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                view: WebView,
                callback: ValueCallback<Array<Uri>>,
                params: FileChooserParams,
            ): Boolean {
                arquivoCallback?.onReceiveValue(null)
                arquivoCallback = callback
                val tipos = params.acceptTypes
                    .flatMap { it.split(',') }
                    .map { it.trim().lowercase() }
                    .filter { it.isNotEmpty() }
                    .flatMap(::tiposMime)
                    .distinct()
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                    .addCategory(Intent.CATEGORY_OPENABLE)
                    .setType("*/*")
                if (tipos.isNotEmpty()) intent.putExtra(Intent.EXTRA_MIME_TYPES, tipos.toTypedArray())
                return try {
                    escolherArquivo.launch(Intent.createChooser(intent, "Escolher arquivo"))
                    true
                } catch (_: ActivityNotFoundException) {
                    arquivoCallback = null
                    false
                }
            }
        }

        // Voltar fecha a janela aberta no app; sem janela aberta, sai do app.
        onBackPressedDispatcher.addCallback(this) {
            web.evaluateJavascript(JS_FECHAR_JANELA) { fechou ->
                if (fechou != "true") finish()
            }
        }

        if (savedInstanceState == null) web.loadUrl("https://$ORIGEM_HOST/assets/index.html")
        else web.restoreState(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        web.saveState(outState)
    }

    // Planilhas que chegam pelo WhatsApp, Drive ou e-mail muitas vezes vêm como "application/octet-stream"
    // e ficariam apagadas no seletor. Aceitamos esse tipo também: o app confere o conteúdo e explica se não for planilha.
    private fun tiposMime(aceito: String): List<String> = when (aceito) {
        ".xlsx", ".xlsm" -> listOf(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-excel.sheet.macroEnabled.12",
            "application/octet-stream",
            "application/zip",
        )
        ".xls" -> listOf("application/vnd.ms-excel", "application/x-excel", "application/octet-stream")
        ".ods" -> listOf("application/vnd.oasis.opendocument.spreadsheet")
        ".csv" -> listOf("text/csv", "text/comma-separated-values", "application/csv", "text/plain", "application/octet-stream")
        else -> listOf(aceito)
    }

    private companion object {
        const val ORIGEM_HOST = "appassets.androidplatform.net"
        const val JS_FECHAR_JANELA =
            "(function(){var l=document.getElementById('layer');" +
                "if(l&&l.firstChild){document.dispatchEvent(new KeyboardEvent('keydown',{key:'Escape'}));return true}" +
                "return false})()"
    }
}
