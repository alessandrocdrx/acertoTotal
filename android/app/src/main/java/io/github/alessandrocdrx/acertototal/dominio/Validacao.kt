package io.github.alessandrocdrx.acertototal.dominio

import java.time.LocalDate

enum class CampoForm { CLIENTE, VALOR, VENCIMENTO, DATA_PAGO, NOME, DOC, EMAIL }

/** O erro aparece embaixo do campo, em vermelho, explicando como corrigir (R8). */
data class ErroForm(val campo: CampoForm, val mensagem: String)

object Validacao {

    /** Nova conta e edição de conta. Mostra só o primeiro erro, como o protótipo. */
    fun conta(clienteId: String?, valorTxt: String, venc: LocalDate?): ErroForm? {
        if (clienteId.isNullOrEmpty()) return ErroForm(CampoForm.CLIENTE, "Escolha o cliente")
        val valor = Dinheiro.parse(valorTxt)
        if (valor == null || valor <= 0) return ErroForm(CampoForm.VALOR, "Informe o valor. Ex.: 150,00")
        if (venc == null) return ErroForm(CampoForm.VENCIMENTO, "Informe o vencimento")
        return null
    }

    fun baixa(data: LocalDate?, hoje: LocalDate): ErroForm? = when {
        data == null -> ErroForm(CampoForm.DATA_PAGO, "Informe a data do pagamento")
        data.isAfter(hoje) -> ErroForm(CampoForm.DATA_PAGO, "A data do pagamento não pode ser depois de hoje")
        else -> null
    }

    /** Só o nome é obrigatório. O CPF/CNPJ é opcional: se vier, tem que ser válido e não pode ser de outro cliente. */
    fun cliente(nome: String, docTxt: String, donoDoDoc: String?, producao: Boolean, email: String = ""): ErroForm? {
        if (nome.isBlank()) return ErroForm(CampoForm.NOME, "Informe o nome do cliente")
        // O e-mail é opcional; se vier, precisa ter @ e domínio (R94).
        if (email.isNotBlank() && !Email.valido(email)) return ErroForm(CampoForm.EMAIL, "E-mail incompleto. Ex.: nome@gmail.com")
        val d = Documento.soDigitos(docTxt)
        if (d.isEmpty()) return null
        if (!Documento.aceito(d, producao)) {
            val causa = if (d.length == 11 || d.length == 14) "${Documento.tipo(d)} inválido" else "CPF tem 11 números e CNPJ tem 14"
            return ErroForm(CampoForm.DOC, "$causa. Confira os números.")
        }
        if (donoDoDoc != null) return ErroForm(CampoForm.DOC, "Esse ${Documento.tipo(d)} já é de $donoDoDoc.")
        return null
    }
}
