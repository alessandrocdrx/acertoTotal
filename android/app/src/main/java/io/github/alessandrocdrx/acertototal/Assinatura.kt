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
import org.json.JSONObject

/**
 * Assinatura pela Google Play (Play Billing 8). Um produto de assinatura, "acertototal_premium", com 4 planos
 * base: semanal, mensal, semestral e anual. O app só pergunta à Play "esta conta tem assinatura ativa?" e guarda
 * a última resposta, para funcionar sem internet. A página recebe o estado como JSON (ver estado()).
 */
class Assinatura(
    private val ctx: Context,
    private val aoMudar: (String) -> Unit,
) : PurchasesUpdatedListener {

    private val principal = Handler(Looper.getMainLooper())
    private val prefs = ctx.getSharedPreferences("assinatura", Context.MODE_PRIVATE)

    private val cliente: BillingClient = BillingClient.newBuilder(ctx)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    @Volatile private var detalhes: ProductDetails? = null
    @Volatile private var precos: Map<String, String> = emptyMap()
    @Volatile private var pronto = false
    @Volatile private var erro = ""

    /** Por que não dá para assinar agora: "" (tudo certo), "sem_planos" (a Play respondeu, mas o produto não existe/está
     *  inativo no Console) ou "play" (não deu para falar com a Play). A página escolhe o aviso por este código. */
    @Volatile private var motivo = ""

    /** Conecta na Play (se preciso), busca os preços e confere as compras da conta. */
    fun iniciar() {
        if (cliente.isReady) {
            buscarPrecos()
            conferirCompras()
            return
        }
        cliente.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(r: BillingResult) {
                if (r.responseCode == BillingClient.BillingResponseCode.OK) {
                    erro = ""
                    motivo = ""
                    buscarPrecos()
                    conferirCompras()
                } else {
                    pronto = false
                    motivo = "play"
                    erro = "A Google Play não respondeu (${r.responseCode})."
                    avisar()
                }
            }

            override fun onBillingServiceDisconnected() {
                pronto = false
                motivo = "play"
            }
        })
    }

    fun fechar() {
        if (cliente.isReady) cliente.endConnection()
    }

    /** Estado entregue à página. "ativa" vem da última conferência com a Play, guardada no aparelho. */
    fun estado(): String = JSONObject()
        .put("real", true)
        .put("ativa", prefs.getBoolean(CHAVE_ATIVA, false))
        .put("pronto", pronto)
        .put("erro", erro)
        .put("motivo", motivo)
        .put("precos", JSONObject(precos))
        .toString()

    /** Abre a tela de compra da Play para o plano ("semanal", "mensal", "semestral" ou "anual"). */
    fun assinar(atividade: Activity?, plano: String): String {
        val d = detalhes ?: return "indisponivel"
        val oferta = d.subscriptionOfferDetails?.firstOrNull { it.basePlanId == plano && it.offerId == null }
            ?: return "indisponivel"
        if (atividade == null || !cliente.isReady) return "indisponivel"
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
        principal.post { cliente.launchBillingFlow(atividade, parametros) }
        return "ok"
    }

    override fun onPurchasesUpdated(r: BillingResult, compras: MutableList<Purchase>?) {
        when (r.responseCode) {
            BillingClient.BillingResponseCode.OK -> tratar(compras.orEmpty())
            BillingClient.BillingResponseCode.USER_CANCELED -> avisar()
            else -> {
                erro = "A compra não foi concluída (${r.responseCode})."
                avisar()
            }
        }
    }

    private fun buscarPrecos() {
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
        cliente.queryProductDetailsAsync(consulta) { r, resultado ->
            if (r.responseCode == BillingClient.BillingResponseCode.OK) {
                val d = resultado.productDetailsList.firstOrNull()
                detalhes = d
                precos = d?.subscriptionOfferDetails
                    ?.filter { it.offerId == null }
                    ?.associate { it.basePlanId to it.pricingPhases.pricingPhaseList.last().formattedPrice }
                    .orEmpty()
                pronto = precos.isNotEmpty()
                motivo = if (pronto) "" else "sem_planos"
                if (!pronto) erro = "Os planos ainda não estão disponíveis na Google Play."
            } else {
                motivo = "play"
                erro = "Não consegui buscar os preços na Google Play (${r.responseCode})."
            }
            avisar()
        }
    }

    private fun conferirCompras() {
        val consulta = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        cliente.queryPurchasesAsync(consulta) { r, compras ->
            if (r.responseCode == BillingClient.BillingResponseCode.OK) tratar(compras)
        }
    }

    private fun tratar(compras: List<Purchase>) {
        val validas = compras.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED && PRODUTO in it.products }
        // A Play devolve o dinheiro se a compra não for confirmada em 3 dias.
        validas.filter { !it.isAcknowledged }.forEach {
            cliente.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(it.purchaseToken).build()) {}
        }
        prefs.edit().putBoolean(CHAVE_ATIVA, validas.isNotEmpty()).apply()
        avisar()
    }

    private fun avisar() {
        aoMudar(estado())
    }

    companion object {
        const val PRODUTO = "acertototal_premium"
        private const val CHAVE_ATIVA = "ativa"
    }
}
