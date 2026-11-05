package io.github.alessandrocdrx.acertototal.planilha

import io.github.alessandrocdrx.acertototal.dados.Cliente
import io.github.alessandrocdrx.acertototal.dados.Conta
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** O que cada importação registrou, para o histórico e para o desfazer (R50). */
data class ResumoImp(
    val clientesNovos: Int = 0,
    val contasNovas: Int = 0,
    val clientesAtualizados: Int = 0,
    val contasAtualizadas: Int = 0,
    val erros: Int = 0,
) {
    fun texto(): String {
        val p = mutableListOf<String>()
        if (clientesNovos > 0) p += "$clientesNovos cliente" + if (clientesNovos > 1) "s novos" else " novo"
        if (contasNovas > 0) p += "$contasNovas conta" + if (contasNovas > 1) "s novas" else " nova"
        val at = clientesAtualizados + contasAtualizadas
        if (at > 0) p += "$at " + if (at > 1) "atualizações" else "atualização"
        if (erros > 0) p += "$erros linha" + (if (erros > 1) "s" else "") + " com problema"
        return p.joinToString(" · ").ifEmpty { "nenhuma mudança" }
    }
}

data class CampoDesfazer(val tipo: String, val id: String, val campo: String, val antes: String, val depois: String)
data class ContaDesfazer(val id: String, val valor: Long, val venc: String, val pagoEm: String)
data class Desfazer(val clientes: List<String>, val contas: List<ContaDesfazer>, val campos: List<CampoDesfazer>)

/** Tudo o que a importação grava, já pronto para o banco. */
data class Aplicacao(
    val clientesNovos: List<Cliente>,
    val clientesAtualizados: List<Cliente>,
    val contasNovas: List<Conta>,
    val contasAtualizadas: List<Conta>,
    val desfazer: Desfazer,
    val resumo: ResumoImp,
    val itens: List<ItemPlano>,
)

object Gravacao {
    private fun campoDoCliente(c: Cliente, campo: String) = when (campo) {
        "doc" -> c.doc; "nome" -> c.nome; "telefone" -> c.telefone; "email" -> c.email; else -> ""
    }

    private fun comCampo(c: Cliente, campo: String, v: String) = when (campo) {
        "doc" -> c.copy(doc = v); "nome" -> c.copy(nome = v); "telefone" -> c.copy(telefone = v); "email" -> c.copy(email = v); else -> c
    }

    private fun campoDaConta(c: Conta, campo: String) = when (campo) {
        "descricao" -> c.descricao; "valor" -> c.valor.toString(); "venc" -> c.venc.toString(); "pagoEm" -> c.pagoEm?.toString().orEmpty(); else -> ""
    }

    private fun comCampo(c: Conta, campo: String, v: String) = when (campo) {
        "descricao" -> c.copy(descricao = v)
        "valor" -> c.copy(valor = v.toLong())
        "venc" -> c.copy(venc = LocalDate.parse(v))
        "pagoEm" -> c.copy(pagoEm = if (v.isEmpty()) null else LocalDate.parse(v))
        else -> c
    }

    /**
     * Decide o que gravar. [novos] e [atualizar] são as duas escolhas independentes da prévia (R55).
     * Código vindo da planilha só vale se ainda não existe no app: senão o app cria um novo.
     */
    fun aplicar(pl: Plano, novos: Boolean, atualizar: Boolean, contasExistentes: List<Conta>, novoId: () -> String): Aplicacao {
        val campos = mutableListOf<CampoDesfazer>()
        val clientesNovos = if (novos) pl.clientesNovos.map { Cliente(it.id, it.nome, it.doc, it.telefone, it.email, "") } else emptyList()
        val clientesAtu = mutableListOf<Cliente>()
        val contasAtu = mutableListOf<Conta>()
        if (atualizar) {
            pl.clientesAtualizados.forEach { x ->
                var c = x.cliente
                x.mud.forEach { m ->
                    campos += CampoDesfazer("cliente", c.id, m.campo, campoDoCliente(x.cliente, m.campo), m.v)
                    c = comCampo(c, m.campo, m.v)
                }
                clientesAtu += c
            }
            pl.contasAtualizadas.forEach { x ->
                var c = x.conta
                x.mud.forEach { m ->
                    campos += CampoDesfazer("conta", c.id, m.campo, campoDaConta(x.conta, m.campo), m.v)
                    c = comCampo(c, m.campo, m.v)
                }
                contasAtu += c
            }
        }
        val ocupados = contasExistentes.map { it.id }.toMutableSet()
        val contasNovas = if (novos) pl.contasNovas.map { n ->
            val id = n.id?.takeIf { it !in ocupados } ?: novoId()
            ocupados += id
            Conta(id, n.clienteId, n.descricao, n.valor, n.venc, n.pagoEm)
        } else emptyList()
        val desfazer = Desfazer(
            clientes = clientesNovos.map { it.id },
            contas = contasNovas.map { ContaDesfazer(it.id, it.valor, it.venc.toString(), it.pagoEm?.toString().orEmpty()) },
            campos = campos,
        )
        val resumo = ResumoImp(
            clientesNovos = clientesNovos.size, contasNovas = contasNovas.size,
            clientesAtualizados = if (atualizar) pl.clientesAtualizados.size else 0,
            contasAtualizadas = if (atualizar) pl.contasAtualizadas.size else 0,
            erros = pl.erros.size,
        )
        return Aplicacao(clientesNovos, clientesAtu, contasNovas, contasAtu, desfazer, resumo, Importacao.itens(pl, novos, atualizar))
    }

    /** O que o desfazer faz com os dados de agora. [mantidos] são itens que a pessoa usou depois e por isso ficam (R50). */
    data class ResultadoDesfazer(
        val clientesRestaurar: List<Cliente>,
        val contasRestaurar: List<Conta>,
        val contasExcluir: List<String>,
        val clientesExcluir: List<String>,
        val mantidos: Int,
    )

    fun calcularDesfazer(d: Desfazer, clientes: List<Cliente>, contas: List<Conta>): ResultadoDesfazer {
        val cli = clientes.associateBy { it.id }.toMutableMap()
        val con = contas.associateBy { it.id }.toMutableMap()
        val cliMudados = mutableSetOf<String>()
        val conMudadas = mutableSetOf<String>()
        var mantidos = 0
        d.campos.forEach { f ->
            if (f.tipo == "cliente") {
                val o = cli[f.id] ?: return@forEach
                if (campoDoCliente(o, f.campo) == f.depois) { cli[f.id] = comCampo(o, f.campo, f.antes); cliMudados += f.id } else mantidos++
            } else {
                val o = con[f.id] ?: return@forEach
                if (campoDaConta(o, f.campo) == f.depois) { con[f.id] = comCampo(o, f.campo, f.antes); conMudadas += f.id } else mantidos++
            }
        }
        val excluirContas = mutableListOf<String>()
        d.contas.forEach { k ->
            val c = con[k.id] ?: return@forEach
            if (c.valor == k.valor && c.venc.toString() == k.venc && c.pagoEm?.toString().orEmpty() == k.pagoEm && c.compArquivo == null) {
                excluirContas += k.id
                con.remove(k.id)
                conMudadas -= k.id
            } else mantidos++
        }
        val excluirClientes = mutableListOf<String>()
        d.clientes.forEach { id ->
            if (con.values.any { it.clienteId == id }) { mantidos++; return@forEach }
            if (cli.remove(id) != null) { excluirClientes += id; cliMudados -= id }
        }
        return ResultadoDesfazer(
            clientesRestaurar = cliMudados.mapNotNull { cli[it] },
            contasRestaurar = conMudadas.mapNotNull { con[it] },
            contasExcluir = excluirContas,
            clientesExcluir = excluirClientes,
            mantidos = mantidos,
        )
    }
}

/** Texto de "o que o desfazer faz", mostrado antes de confirmar. */
fun resumoDoDesfazer(d: Desfazer): List<String> {
    val p = mutableListOf<String>()
    if (d.contas.isNotEmpty()) p += if (d.contas.size > 1) "Saem as ${d.contas.size} contas que entraram." else "Sai a conta que entrou."
    if (d.clientes.isNotEmpty()) p += if (d.clientes.size > 1) "Saem os ${d.clientes.size} clientes novos." else "Sai o cliente novo."
    if (d.campos.isNotEmpty()) p += if (d.campos.size > 1) "${d.campos.size} dados voltam ao que eram antes." else "1 dado volta ao que era antes."
    return p
}

/** "05/11/2026 às 13:31", a partir do momento guardado no registro (ISO). */
fun quandoTexto(iso: String): String = try {
    val d = java.time.LocalDateTime.parse(iso)
    "%02d/%02d/%04d às %02d:%02d".format(d.dayOfMonth, d.monthValue, d.year, d.hour, d.minute)
} catch (_: Exception) {
    iso
}

fun mantidosTexto(n: Int): String = if (n > 1) "$n itens que você usou depois ficaram." else "1 item que você usou depois ficou."

/** Serialização do histórico em JSON (org.json já vem no Android). */
object RegistroJson {
    fun resumoParaJson(r: ResumoImp) = JSONObject()
        .put("clientesNovos", r.clientesNovos).put("contasNovas", r.contasNovas)
        .put("clientesAtualizados", r.clientesAtualizados).put("contasAtualizadas", r.contasAtualizadas).put("erros", r.erros).toString()

    fun resumoDeJson(s: String): ResumoImp = JSONObject(s).let {
        ResumoImp(it.optInt("clientesNovos"), it.optInt("contasNovas"), it.optInt("clientesAtualizados"), it.optInt("contasAtualizadas"), it.optInt("erros"))
    }

    fun desfazerParaJson(d: Desfazer) = JSONObject()
        .put("clientes", JSONArray(d.clientes))
        .put("contas", JSONArray().also { a -> d.contas.forEach { a.put(JSONObject().put("id", it.id).put("valor", it.valor).put("venc", it.venc).put("pagoEm", it.pagoEm)) } })
        .put("campos", JSONArray().also { a ->
            d.campos.forEach { a.put(JSONObject().put("tipo", it.tipo).put("id", it.id).put("campo", it.campo).put("antes", it.antes).put("depois", it.depois)) }
        }).toString()

    fun desfazerDeJson(s: String): Desfazer {
        val o = JSONObject(s)
        val cl = o.getJSONArray("clientes").let { a -> (0 until a.length()).map { a.getString(it) } }
        val co = o.getJSONArray("contas").let { a ->
            (0 until a.length()).map { a.getJSONObject(it).let { x -> ContaDesfazer(x.getString("id"), x.getLong("valor"), x.getString("venc"), x.getString("pagoEm")) } }
        }
        val ca = o.getJSONArray("campos").let { a ->
            (0 until a.length()).map { a.getJSONObject(it).let { x -> CampoDesfazer(x.getString("tipo"), x.getString("id"), x.getString("campo"), x.getString("antes"), x.getString("depois")) } }
        }
        return Desfazer(cl, co, ca)
    }

    fun itensParaJson(itens: List<ItemPlano>) = JSONArray().also { arr ->
        itens.forEach { i ->
            arr.put(JSONObject().put("tipo", i.tipo.name).put("titulo", i.titulo).put("linhas", JSONArray().also { ls ->
                i.linhas.forEach { l ->
                    ls.put(
                        when (l) {
                            is LinhaItem.Texto -> JSONObject().put("k", "t").put("a", l.t)
                            is LinhaItem.Sub -> JSONObject().put("k", "s").put("a", l.t)
                            is LinhaItem.Mudanca -> JSONObject().put("k", "m").put("a", l.rot).put("b", l.de).put("c", l.para)
                            is LinhaItem.Prob -> JSONObject().put("k", "p").put("a", l.col).put("b", l.txt)
                        },
                    )
                }
            }))
        }
    }.toString()

    fun itensDeJson(s: String): List<ItemPlano> {
        val arr = JSONArray(s)
        return (0 until arr.length()).map { n ->
            val o = arr.getJSONObject(n)
            val ls = o.getJSONArray("linhas")
            ItemPlano(
                TipoItem.valueOf(o.getString("tipo")), o.getString("titulo"),
                (0 until ls.length()).map { k ->
                    val l = ls.getJSONObject(k)
                    when (l.getString("k")) {
                        "t" -> LinhaItem.Texto(l.getString("a"))
                        "s" -> LinhaItem.Sub(l.getString("a"))
                        "m" -> LinhaItem.Mudanca(l.getString("a"), l.getString("b"), l.getString("c"))
                        else -> LinhaItem.Prob(l.getString("a"), l.getString("b"))
                    }
                },
            )
        }
    }
}
