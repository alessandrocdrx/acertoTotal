package io.github.alessandrocdrx.acertototal.dados

import androidx.room.withTransaction
import io.github.alessandrocdrx.acertototal.planilha.Aplicacao
import io.github.alessandrocdrx.acertototal.planilha.Gravacao
import io.github.alessandrocdrx.acertototal.planilha.RegistroJson

/** Único ponto de escrita dos dados. As telas nunca falam com o Room direto. */
class Repositorio(private val banco: Banco) {
    val contas = banco.contas().todas()
    val contasCompletas = banco.contas().completas()
    val clientes = banco.clientes().todos()
    val importacoes = banco.importacoes().recentes()

    suspend fun conta(id: String): Conta? = banco.contas().porId(id)
    suspend fun cliente(id: String): Cliente? = banco.clientes().porId(id)
    suspend fun clientesAgora(): List<Cliente> = banco.clientes().todosAgora()
    suspend fun contasAgora(): List<Conta> = banco.contas().todasAgora()

    /** Grava (ou regrava) a conta. Serve para nova, edição, baixa e para o "Desfazer". */
    suspend fun salvarConta(c: Conta) = banco.contas().inserir(c)

    /** Devolve a conta removida, para o aviso poder oferecer "Desfazer". */
    suspend fun excluirConta(id: String): Conta? {
        val c = banco.contas().porId(id) ?: return null
        banco.contas().excluir(id)
        return c
    }

    /** Outro cliente que já usa este CPF/CNPJ (ignora o próprio, na edição). */
    suspend fun donoDoDoc(doc: String, ignorarId: String?): Cliente? =
        banco.clientes().porDoc(doc)?.takeIf { it.id != ignorarId }

    /**
     * Cliente novo entra; cliente que já existe é atualizado. Não pode ser "substituir": o Room apagaria a linha
     * antes de gravar de novo, e a chave estrangeira recusa apagar um cliente que tem contas.
     */
    suspend fun salvarCliente(c: Cliente) {
        if (banco.clientes().porId(c.id) != null) banco.clientes().atualizar(c) else banco.clientes().inserir(c)
    }

    suspend fun quantasContasDoCliente(id: String): Int = banco.contas().quantasDoCliente(id)

    /** Cliente com contas não pode ser excluído. Devolve o cliente removido ou null. */
    suspend fun excluirCliente(id: String): Cliente? {
        val c = banco.clientes().porId(id) ?: return null
        if (banco.contas().quantasDoCliente(id) > 0) return null
        banco.clientes().excluir(id)
        return c
    }

    /** Só na versão de teste (R60). As contas saem antes dos clientes por causa da chave estrangeira. */
    suspend fun apagarTudo() = banco.withTransaction {
        banco.contas().apagarTodas()
        banco.clientes().apagarTodos()
        banco.importacoes().apagarTodas()
    }

    // ---------------------------------------------------------------- importação

    /** Grava tudo de uma vez ou nada: se algo falhar no meio, o banco fica como estava (R37). Devolve o id do registro. */
    suspend fun gravarImportacao(ap: Aplicacao, arquivo: String, quando: String): Long = banco.withTransaction {
        banco.clientes().inserirTodos(ap.clientesNovos)
        banco.clientes().atualizarTodos(ap.clientesAtualizados)
        banco.contas().atualizarTodas(ap.contasAtualizadas)
        banco.contas().inserirTodas(ap.contasNovas)
        val id = banco.importacoes().inserir(
            ImportacaoReg(
                quando = quando, arquivo = arquivo,
                resumo = RegistroJson.resumoParaJson(ap.resumo),
                itens = RegistroJson.itensParaJson(ap.itens),
                desfazer = RegistroJson.desfazerParaJson(ap.desfazer),
            ),
        )
        banco.importacoes().apararAntigas()
        id
    }

    /** Desfaz a importação com o que está no banco agora. Devolve quantos itens ficaram por já terem sido usados, ou null se não dá. */
    suspend fun desfazerImportacao(id: Long, quando: String): Int? = banco.withTransaction {
        val reg = banco.importacoes().porId(id) ?: return@withTransaction null
        if (reg.desfeita != null) return@withTransaction null
        // Só a mais recente que ainda vale pode ser desfeita: as de depois dela partem do que ela deixou.
        val recentes = banco.importacoes().recentesAgora()
        if (recentes.takeWhile { it.id != id }.any { it.desfeita == null }) return@withTransaction null
        val r = Gravacao.calcularDesfazer(RegistroJson.desfazerDeJson(reg.desfazer), banco.clientes().todosAgora(), banco.contas().todasAgora())
        banco.contas().excluirVarias(r.contasExcluir)
        banco.clientes().atualizarTodos(r.clientesRestaurar)
        banco.contas().atualizarTodas(r.contasRestaurar)
        banco.clientes().excluirVarios(r.clientesExcluir)
        banco.importacoes().atualizar(reg.copy(desfeita = quando, mantidos = r.mantidos))
        r.mantidos
    }
}
