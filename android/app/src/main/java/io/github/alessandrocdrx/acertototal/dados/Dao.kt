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

    /** Todos os campos de cada conta: para exportar e para a cópia de segurança. */
    @Query("SELECT * FROM contas")
    fun completas(): Flow<List<Conta>>

    @Query("SELECT * FROM contas")
    suspend fun todasAgora(): List<Conta>

    @Query("SELECT * FROM contas WHERE id = :id")
    suspend fun porId(id: String): Conta?

    @Update
    suspend fun atualizarTodas(contas: List<Conta>)

    @Query("DELETE FROM contas WHERE id IN (:ids)")
    suspend fun excluirVarias(ids: List<String>)

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

    @Query("DELETE FROM contas")
    suspend fun apagarTodas()

    @Query("SELECT COUNT(*) FROM contas WHERE clienteId = :clienteId")
    suspend fun quantasDoCliente(clienteId: String): Int
}

@Dao
interface ImportacaoDao {
    /** As 10 últimas, da mais recente para a mais antiga. */
    @Query("SELECT * FROM importacoes ORDER BY id DESC LIMIT 10")
    fun recentes(): Flow<List<ImportacaoReg>>

    @Query("SELECT * FROM importacoes ORDER BY id DESC LIMIT 10")
    suspend fun recentesAgora(): List<ImportacaoReg>

    @Query("SELECT * FROM importacoes WHERE id = :id")
    suspend fun porId(id: Long): ImportacaoReg?

    @Insert
    suspend fun inserir(r: ImportacaoReg): Long

    @Update
    suspend fun atualizar(r: ImportacaoReg)

    /** Guarda só as 10 últimas. */
    @Query("DELETE FROM importacoes WHERE id NOT IN (SELECT id FROM importacoes ORDER BY id DESC LIMIT 10)")
    suspend fun apararAntigas()

    @Query("DELETE FROM importacoes")
    suspend fun apagarTodas()
}

@Dao
interface ClienteDao {
    @Query("SELECT * FROM clientes ORDER BY nome COLLATE NOCASE")
    fun todos(): Flow<List<Cliente>>

    @Query("SELECT * FROM clientes")
    suspend fun todosAgora(): List<Cliente>

    @Query("SELECT * FROM clientes WHERE id = :id")
    suspend fun porId(id: String): Cliente?

    @Update
    suspend fun atualizarTodos(clientes: List<Cliente>)

    @Query("DELETE FROM clientes WHERE id IN (:ids)")
    suspend fun excluirVarios(ids: List<String>)

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

    @Query("DELETE FROM clientes")
    suspend fun apagarTodos()

    @Query("SELECT COUNT(*) FROM clientes")
    suspend fun quantos(): Int
}
