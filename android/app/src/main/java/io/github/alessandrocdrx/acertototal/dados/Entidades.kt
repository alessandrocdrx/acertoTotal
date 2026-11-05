package io.github.alessandrocdrx.acertototal.dados

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import java.time.LocalDate

fun novoId(): String = java.util.UUID.randomUUID().toString().take(8)

// O índice do CPF/CNPJ é só para buscar rápido. Que ele não repita (R52) quem garante é a validação: um índice
// único recusaria dois clientes antigos sem documento, que ficam com o campo vazio (R59).
@Entity(tableName = "clientes", indices = [Index(value = ["doc"])])
data class Cliente(
    @PrimaryKey val id: String,
    val nome: String,
    /** CPF/CNPJ só com números. Vazio = cliente antigo ainda sem o número (R59). */
    val doc: String = "",
    val telefone: String = "",
    val email: String = "",
    val obs: String = "",
)

@Entity(
    tableName = "contas",
    foreignKeys = [
        ForeignKey(
            entity = Cliente::class,
            parentColumns = ["id"],
            childColumns = ["clienteId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("clienteId"), Index("venc"), Index("pagoEm")],
)
data class Conta(
    @PrimaryKey val id: String,
    val clienteId: String,
    val descricao: String,
    /** Em centavos. */
    val valor: Long,
    val venc: LocalDate,
    val pagoEm: LocalDate? = null,
    val compNome: String? = null,
    val compTipo: String? = null,
    /** Nome do arquivo do comprovante na pasta do app (R27: imagem reduzida a 1280 px). */
    val compArquivo: String? = null,
)

/**
 * Uma importação já feita, para o histórico e para o desfazer (R50). Os detalhes ficam em JSON:
 * [resumo] (contagens), [itens] (o que mudou, por cliente) e [desfazer] (o que voltar atrás).
 */
@Entity(tableName = "importacoes")
data class ImportacaoReg(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val quando: String,
    val arquivo: String,
    val resumo: String,
    val itens: String,
    val desfazer: String,
    /** Quando foi desfeita; null enquanto vale. */
    val desfeita: String? = null,
    /** Itens que a pessoa usou depois e por isso o desfazer deixou como estavam. */
    val mantidos: Int = 0,
)

class Conversores {
    @TypeConverter
    fun deTexto(v: String?): LocalDate? = v?.let(LocalDate::parse)

    @TypeConverter
    fun paraTexto(d: LocalDate?): String? = d?.toString()
}

/** Conta junto com o nome do cliente, para a lista (R7: o que importa primeiro). */
data class ContaComCliente(
    val id: String,
    val clienteId: String,
    val clienteNome: String,
    val descricao: String,
    val valor: Long,
    val venc: LocalDate,
    val pagoEm: LocalDate?,
    val temComprovante: Boolean,
)
