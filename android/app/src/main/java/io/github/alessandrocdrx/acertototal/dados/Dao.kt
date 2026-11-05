package io.github.alessandrocdrx.acertototal.dados

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ContaDao {
    @Query(
        """SELECT c.id, c.clienteId, cl.nome AS clienteNome, c.descricao, c.valor, c.venc, c.pagoEm,
                  (c.compArquivo IS NOT NULL) AS temComprovante
           FROM contas c JOIN clientes cl ON cl.id = c.clienteId""",
    )
    fun todas(): Flow<List<ContaComCliente>>

    @Query("SELECT * FROM contas WHERE id = :id")
    suspend fun porId(id: String): Conta?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(conta: Conta)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirTodas(contas: List<Conta>)

    @Update
    suspend fun atualizar(conta: Conta)

    @Query("DELETE FROM contas WHERE id = :id")
    suspend fun excluir(id: String)

    @Query("SELECT COUNT(*) FROM contas")
    suspend fun quantas(): Int

    @Query("SELECT COUNT(*) FROM contas WHERE clienteId = :clienteId")
    suspend fun quantasDoCliente(clienteId: String): Int
}

@Dao
interface ClienteDao {
    @Query("SELECT * FROM clientes ORDER BY nome COLLATE NOCASE")
    fun todos(): Flow<List<Cliente>>

    @Query("SELECT * FROM clientes WHERE id = :id")
    suspend fun porId(id: String): Cliente?

    @Query("SELECT * FROM clientes WHERE doc = :doc AND doc != '' LIMIT 1")
    suspend fun porDoc(doc: String): Cliente?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun inserir(cliente: Cliente)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirTodos(clientes: List<Cliente>)

    @Update
    suspend fun atualizar(cliente: Cliente)

    @Query("DELETE FROM clientes WHERE id = :id")
    suspend fun excluir(id: String)

    @Query("SELECT COUNT(*) FROM clientes")
    suspend fun quantos(): Int
}
