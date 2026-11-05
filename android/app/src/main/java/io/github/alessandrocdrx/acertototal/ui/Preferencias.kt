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

    fun salvarTema(ctx: Context, tema: Tema) {
        ctx.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE).edit().putString("tema", tema.name).apply()
    }
}
