package io.github.alessandrocdrx.acertototal

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Por que não dá para assinar agora: [NENHUM] (tudo certo ou ainda carregando), [SEM_PLANOS] (a Play respondeu, mas
 * o produto não existe ou está inativo no Console) ou [PLAY] (não deu para falar com a Play). A tela escolhe o
 * aviso por este código (R75).
 */
enum class MotivoAssinatura { NENHUM, SEM_PLANOS, PLAY }

data class EstadoAssinatura(
    /** O app da Play cobra de verdade; o de teste só simula e nunca cobra (R65). */
    val real: Boolean,
    /** Última resposta da Play, guardada no aparelho: quem já assinou abre o app liberado sem internet. */
    val ativa: Boolean = false,
    val pronto: Boolean = false,
    val motivo: MotivoAssinatura = MotivoAssinatura.NENHUM,
    val precos: Map<String, String> = emptyMap(),
    val erro: String = "",
)

/**
 * Assinatura pela Google Play (Play Billing 8). Um produto de assinatura, "acertototal_premium", com 4 planos
 * base: semanal, mensal, semestral e anual. O app só pergunta à Play "esta conta tem assinatura ativa?" e guarda
 * a última resposta. Na versão de teste ([real] = false) não fala com a Play: a assinatura é simulada.
 */
class Assinatura(
    private val ctx: Context,
    private val real: Boolean = BuildConfig.COBRANCA_REAL,
) : PurchasesUpdatedListener {

    private val principal = Handler(Looper.getMainLooper())
    private val prefs = ctx.getSharedPreferences("assinatura", Context.MODE_PRIVATE)

    private val _estado = MutableStateFlow(
        EstadoAssinatura(real = real, ativa = prefs.getBoolean(if (real) CHAVE_ATIVA else CHAVE_SIMULADA, false)),
    )
    val estado: StateFlow<EstadoAssinatura> = _estado.asStateFlow()

    private val cliente: BillingClient? = if (!real) null else BillingClient.newBuilder(ctx)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    @Volatile private var detalhes: ProductDetails? = null

    /** Conecta na Play (se preciso), busca os preços e confere as compras da conta. */
    fun iniciar() {
        val c = cliente ?: return
        if (c.isReady) {
            buscarPrecos(c)
            conferirCompras(c)
            return
        }
        c.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(r: BillingResult) {
                if (r.responseCode == BillingClient.BillingResponseCode.OK) {
                    _estado.update { it.copy(erro = "", motivo = MotivoAssinatura.NENHUM) }
                    buscarPrecos(c)
                    conferirCompras(c)
                } else {
                    _estado.update { it.copy(pronto = false, motivo = MotivoAssinatura.PLAY, erro = "A Google Play não respondeu (${r.responseCode}).") }
                }
            }

            override fun onBillingServiceDisconnected() {
                _estado.update { it.copy(pronto = false, motivo = MotivoAssinatura.PLAY) }
            }
        })
    }

    fun fechar() {
        cliente?.takeIf { it.isReady }?.endConnection()
    }

    /**
     * Abre a tela de compra da Play para o plano ("semanal", "mensal", "semestral" ou "anual").
     * Na simulação, só marca como assinado. Devolve false se não deu para abrir a compra.
     */
    fun assinar(atividade: Activity?, plano: String): Boolean {
        if (!real) {
            prefs.edit().putBoolean(CHAVE_SIMULADA, true).putString("plano_simulado", plano).apply()
            _estado.update { it.copy(ativa = true) }
            return true
        }
        val c = cliente ?: return false
        val d = detalhes ?: return false
        val oferta = d.subscriptionOfferDetails?.firstOrNull { it.basePlanId == plano && it.offerId == null } ?: return false
        if (atividade == null || !c.isReady) return false
        val parametros = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(d)
                        .setOfferToken(oferta.offerToken)
                        .build(),
                ),
            )
            .build()
        // A tela de compra precisa ser aberta pela thread principal.
        principal.post { c.launchBillingFlow(atividade, parametros) }
        return true
    }

    /** Só na simulação: cancela a assinatura de mentira. */
    fun cancelarSimulada() {
        if (real) return
        prefs.edit().putBoolean(CHAVE_SIMULADA, false).remove("plano_simulado").apply()
        _estado.update { it.copy(ativa = false) }
    }

    /** "Já assinei: restaurar" pergunta de novo à Play. */
    fun atualizar() = iniciar()

    override fun onPurchasesUpdated(r: BillingResult, compras: MutableList<Purchase>?) {
        when (r.responseCode) {
            BillingClient.BillingResponseCode.OK -> tratar(compras.orEmpty())
            BillingClient.BillingResponseCode.USER_CANCELED -> Unit
            else -> _estado.update { it.copy(erro = "A compra não foi concluída (${r.responseCode}).") }
        }
    }

    private fun buscarPrecos(c: BillingClient) {
        val consulta = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUTO)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build(),
                ),
            )
            .build()
        c.queryProductDetailsAsync(consulta) { r, resultado ->
            if (r.responseCode == BillingClient.BillingResponseCode.OK) {
                val d = resultado.productDetailsList.firstOrNull()
                detalhes = d
                val precos = d?.subscriptionOfferDetails
                    ?.filter { it.offerId == null }
                    ?.associate { it.basePlanId to it.pricingPhases.pricingPhaseList.last().formattedPrice }
                    .orEmpty()
                val pronto = precos.isNotEmpty()
                _estado.update {
                    it.copy(
                        precos = precos, pronto = pronto,
                        motivo = if (pronto) MotivoAssinatura.NENHUM else MotivoAssinatura.SEM_PLANOS,
                        erro = if (pronto) "" else "Os planos ainda não estão disponíveis na Google Play.",
                    )
                }
            } else {
                _estado.update { it.copy(motivo = MotivoAssinatura.PLAY, erro = "Não consegui buscar os preços na Google Play (${r.responseCode}).") }
            }
        }
    }

    private fun conferirCompras(c: BillingClient) {
        val consulta = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        c.queryPurchasesAsync(consulta) { r, compras ->
            if (r.responseCode == BillingClient.BillingResponseCode.OK) tratar(compras)
        }
    }

    private fun tratar(compras: List<Purchase>) {
        val validas = compras.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED && PRODUTO in it.products }
        // A Play devolve o dinheiro se a compra não for confirmada em 3 dias.
        validas.filter { !it.isAcknowledged }.forEach {
            cliente?.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(it.purchaseToken).build()) {}
        }
        prefs.edit().putBoolean(CHAVE_ATIVA, validas.isNotEmpty()).apply()
        _estado.update { it.copy(ativa = validas.isNotEmpty()) }
    }

    companion object {
        const val PRODUTO = "acertototal_premium"
        private const val CHAVE_ATIVA = "ativa"
        private const val CHAVE_SIMULADA = "simulada"
    }
}
