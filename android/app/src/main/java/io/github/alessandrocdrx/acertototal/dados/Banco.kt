package io.github.alessandrocdrx.acertototal.dados

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.alessandrocdrx.acertototal.BuildConfig
import java.time.LocalDate
import java.util.UUID

@Database(entities = [Cliente::class, Conta::class, ImportacaoReg::class], version = 2, exportSchema = false)
@TypeConverters(Conversores::class)
abstract class Banco : RoomDatabase() {
    abstract fun contas(): ContaDao
    abstract fun clientes(): ClienteDao
    abstract fun importacoes(): ImportacaoDao

    companion object {
        @Volatile
        private var instancia: Banco? = null

        /** Versão 2: histórico das importações e índice do CPF/CNPJ sem exigir único. Os dados ficam como estão. */
        private val MIGRACAO_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP INDEX IF EXISTS `index_clientes_doc`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_clientes_doc` ON `clientes` (`doc`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `importacoes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`quando` TEXT NOT NULL, `arquivo` TEXT NOT NULL, `resumo` TEXT NOT NULL, `itens` TEXT NOT NULL, " +
                        "`desfazer` TEXT NOT NULL, `desfeita` TEXT, `mantidos` INTEGER NOT NULL)",
                )
            }
        }

        fun obter(ctx: Context): Banco = instancia ?: synchronized(this) {
            instancia ?: Room.databaseBuilder(ctx.applicationContext, Banco::class.java, "acertototal.db")
                .addMigrations(MIGRACAO_1_2)
                .build().also { instancia = it }
        }
    }
}

/**
 * Dados de exemplo (R19): só no app de teste, para a demonstração nunca abrir vazia.
 * O app da Play começa limpo.
 */
object Exemplo {
    private fun novoId() = UUID.randomUUID().toString().take(8)

    suspend fun semearSeVazio(banco: Banco, hoje: LocalDate = LocalDate.now()) {
        if (BuildConfig.COBRANCA_REAL) return
        if (banco.clientes().quantos() > 0) return
        val (clientes, contas) = gerar(hoje)
        banco.clientes().inserirTodos(clientes)
        banco.contas().inserirTodas(contas)
    }

    fun gerar(hoje: LocalDate): Pair<List<Cliente>, List<Conta>> {
        val c = listOf(
            Cliente(novoId(), "Marina Alves", "63712926170", "(11) 98765-4321", "marina@exemplo.com"),
            Cliente(novoId(), "Padaria Bom Pão", "71296668000176", "(11) 3456-7890"),
            Cliente(novoId(), "Carlos Eduardo Lima", "94127724242", "(21) 99876-5432", "carlos.lima@exemplo.com"),
            Cliente(novoId(), "Studio Fênix Pilates", "82258215000171", "(31) 98888-1212", "contato@fenix.exemplo.com"),
            Cliente(novoId(), "Rafaela Nunes", "97124171407", "(41) 99123-4567"),
        )
        fun conta(cli: Int, desc: String, reais: Int, venc: Long, pago: Long?, comp: Boolean = false) = Conta(
            id = novoId(), clienteId = c[cli].id, descricao = desc, valor = reais * 100L,
            venc = hoje.plusDays(venc), pagoEm = pago?.let { hoje.plusDays(it) },
            compNome = if (comp) "comprovante-pix.pdf" else null,
            compTipo = if (comp) "application/pdf" else null,
            compArquivo = null,
        )
        val contas = mutableListOf(
            conta(0, "Mensalidade", 180, -35, -33), conta(0, "Mensalidade", 180, -5, null), conta(0, "Mensalidade", 180, 25, null),
            conta(1, "Fornecimento de pães", 1250, -40, -38), conta(1, "Fornecimento de pães", 1250, -10, null),
            conta(2, "Parcela 2/6", 420, -3, null), conta(2, "Parcela 3/6", 420, 27, null),
            conta(3, "Mensalidade do estúdio", 890, -30, -30), conta(3, "Mensalidade do estúdio", 890, 2, null),
            conta(4, "Consultoria", 600, -1, null), conta(4, "Consultoria", 600, 14, null),
            conta(3, "Taxa de matrícula", 150, -2, 0), conta(2, "Parcela 1/6", 420, -4, 0),
        )
        for (m in 1..5) {
            val base = -30L * m
            contas += conta(0, "Mensalidade", 180, base - 8, base - 8 + m % 3)
            contas += conta(1, "Fornecimento de pães", 1250, base - 12, base - 12 + (m * 2) % 5)
            contas += conta(3, "Mensalidade do estúdio", 890, base - 1, base - 1 + if (m % 2 == 1) 0 else 3)
            contas += conta(4, "Consultoria", 600, base - 3, base - 3 + (m % 3) - 1)
        }
        return c to contas
    }
}
