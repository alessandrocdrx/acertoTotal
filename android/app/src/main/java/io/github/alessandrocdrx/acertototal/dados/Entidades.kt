package io.github.alessandrocdrx.acertototal.dados

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import java.time.LocalDate

@Entity(tableName = "clientes", indices = [Index(value = ["doc"], unique = true)])
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
