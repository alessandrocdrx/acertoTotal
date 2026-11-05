package io.github.alessandrocdrx.acertototal.ui

import android.content.Context

enum class Tema(val rotulo: String) {
    AUTO("Automático"),
    CLARO("Claro"),
    ESCURO("Escuro"),
}

/** Ajustes simples do aparelho. Os dados do app ficam no Room, não aqui. */
object Preferencias {
    private const val ARQUIVO = "ajustes"

    /** O tema claro é o padrão (R18): o escuro só entra quando a pessoa escolhe. */
    fun tema(ctx: Context): Tema =
        runCatching { Tema.valueOf(ctx.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE).getString("tema", null) ?: "CLARO") }
            .getOrDefault(Tema.CLARO)

    /** Data da última cópia de segurança baixada, ou null se nunca foi feita. */
    fun ultimoBackup(ctx: Context): java.time.LocalDate? =
        ctx.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE).getString("ultimo_backup", null)
            ?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() }

    fun salvarUltimoBackup(ctx: Context, data: java.time.LocalDate) {
        ctx.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE).edit().putString("ultimo_backup", data.toString()).apply()
    }

    /** "Apagar tudo" também zera a data da cópia: não há mais o que proteger. */
    fun limparUltimoBackup(ctx: Context) {
        ctx.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE).edit().remove("ultimo_backup").apply()
    }

    /** "Meu negócio": nome e chave Pix da mensagem de cobrança. "Apagar tudo" não mexe nisto. */
    fun negocio(ctx: Context): io.github.alessandrocdrx.acertototal.dominio.Negocio {
        val p = ctx.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE)
        return io.github.alessandrocdrx.acertototal.dominio.Negocio(p.getString("negocio_nome", "").orEmpty(), p.getString("negocio_pix", "").orEmpty())
    }

    fun salvarNegocio(ctx: Context, n: io.github.alessandrocdrx.acertototal.dominio.Negocio) {
        ctx.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE).edit().putString("negocio_nome", n.nome).putString("negocio_pix", n.pix).apply()
    }

    fun salvarTema(ctx: Context, tema: Tema) {
        ctx.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE).edit().putString("tema", tema.name).apply()
    }
}
