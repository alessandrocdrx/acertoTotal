package io.github.alessandrocdrx.acertototal.dados

/** Único ponto de escrita dos dados. As telas nunca falam com o Room direto. */
class Repositorio(private val banco: Banco) {
    val contas = banco.contas().todas()
    val clientes = banco.clientes().todos()

    suspend fun conta(id: String): Conta? = banco.contas().porId(id)
    suspend fun cliente(id: String): Cliente? = banco.clientes().porId(id)

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

    suspend fun salvarCliente(c: Cliente) = banco.clientes().inserirTodos(listOf(c))

    suspend fun quantasContasDoCliente(id: String): Int = banco.contas().quantasDoCliente(id)

    /** Cliente com contas não pode ser excluído. Devolve o cliente removido ou null. */
    suspend fun excluirCliente(id: String): Cliente? {
        val c = banco.clientes().porId(id) ?: return null
        if (banco.contas().quantasDoCliente(id) > 0) return null
        banco.clientes().excluir(id)
        return c
    }
}
