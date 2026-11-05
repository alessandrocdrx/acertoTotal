package io.github.alessandrocdrx.acertototal

import io.github.alessandrocdrx.acertototal.dados.Cliente
import io.github.alessandrocdrx.acertototal.dados.Conta
import io.github.alessandrocdrx.acertototal.planilha.Campos
import io.github.alessandrocdrx.acertototal.planilha.CampoPlanilha
import io.github.alessandrocdrx.acertototal.planilha.Csv
import io.github.alessandrocdrx.acertototal.planilha.Datas
import io.github.alessandrocdrx.acertototal.planilha.Gravacao
import io.github.alessandrocdrx.acertototal.planilha.Importacao
import io.github.alessandrocdrx.acertototal.planilha.LeituraDoc
import io.github.alessandrocdrx.acertototal.planilha.LeitorArquivo
import io.github.alessandrocdrx.acertototal.planilha.Leitura
import io.github.alessandrocdrx.acertototal.planilha.Plano
import io.github.alessandrocdrx.acertototal.planilha.RegistroJson
import io.github.alessandrocdrx.acertototal.planilha.TipoArquivo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

private val HOJE = LocalDate.of(2026, 11, 5)

/** Planeja a partir de linhas de texto, como se viessem de uma planilha com os títulos padrão. */
private fun planejar(
    linhas: List<List<String>>,
    titulos: List<String> = listOf("CPF/CNPJ", "Nome", "Telefone", "E-mail", "Descrição", "Valor", "Vencimento", "Pago em", "Código da conta"),
    clientes: List<Cliente> = emptyList(),
    contas: List<Conta> = emptyList(),
    producao: Boolean = true,
): Plano {
    val aoa = listOf(titulos) + linhas
    val aba = Campos.analisarAba("A", aoa)!!
    val mapa = Campos.autoMapa(aba.cols)
    var n = 0
    return Importacao.planejar(
        Campos.linhasDoMapa(aba, mapa), "teste.xlsx", Campos.soClientes(mapa), clientes, contas, HOJE, producao,
    ) { "novo${++n}" }
}

private fun linha(doc: String = "", nome: String = "", tel: String = "", mail: String = "", desc: String = "", valor: String = "", venc: String = "", pago: String = "", cod: String = "") =
    listOf(doc, nome, tel, mail, desc, valor, venc, pago, cod)

private const val MARINA = "637.129.261-70"
private const val PADARIA = "71.296.668/0001-76"

class CamposTest {
    @Test fun reconheceTitulosComOutrosNomes() {
        assertEquals(CampoPlanilha.NOME, Campos.campoDoTitulo("Cliente"))
        assertEquals(CampoPlanilha.VENC, Campos.campoDoTitulo("Data de Vencimento"))
        assertEquals(CampoPlanilha.VALOR, Campos.campoDoTitulo("Valor (R$)"))
        assertEquals(CampoPlanilha.DOC, Campos.campoDoTitulo("CPF"))
        assertEquals(CampoPlanilha.CODIGO, Campos.campoDoTitulo("Código da conta"))
        assertNull(Campos.campoDoTitulo("Observações internas do financeiro sobre a cobrança deste cliente aqui"))
        assertNull(Campos.campoDoTitulo(""))
    }

    // R47: títulos fora da linha 1.
    @Test fun achaALinhaDeTitulosEOsNumerosDeLinhaDoExcel() {
        val aoa = listOf(
            listOf("Relatório de cobrança"), listOf(""),
            listOf("CPF", "Cliente", "Valor", "Data de vencimento"),
            listOf(MARINA, "Marina", "150,00", "10/11/2026"),
            emptyList(),
            listOf(PADARIA, "Padaria", "20", "11/11/2026"),
        )
        val a = Campos.analisarAba("Aba", aoa)!!
        assertEquals(3, a.linhaCab)
        assertEquals(listOf(4, 6), a.linhas.map { it.linha }) // a linha em branco é pulada e os números batem com o Excel
        assertEquals("C", a.cols.first { it.campo == CampoPlanilha.VALOR }.letra)
        assertEquals("150,00", a.cols.first { it.campo == CampoPlanilha.VALOR }.exemplo)
    }

    @Test fun semTitulosReconhecidosDevolveNull() {
        assertNull(Campos.analisarAba("A", listOf(listOf("a", "b"), listOf("1", "2"))))
    }

    @Test fun mapaIncompletoDizOQueFalta() {
        assertEquals("", Campos.faltaColuna(mapOf(CampoPlanilha.NOME to 0))) // CPF/CNPJ é opcional
        assertTrue(Campos.faltaColuna(mapOf(CampoPlanilha.DOC to 0)).startsWith("Não achei a coluna com o nome"))
        assertTrue(Campos.faltaColuna(mapOf(CampoPlanilha.DOC to 0, CampoPlanilha.NOME to 1, CampoPlanilha.VENC to 2)).contains("do vencimento, mas não a do valor"))
        assertEquals("", Campos.faltaColuna(mapOf(CampoPlanilha.DOC to 0, CampoPlanilha.NOME to 1)))
        assertEquals("", Campos.faltaColuna(mapOf(CampoPlanilha.CODIGO to 0)))
        assertEquals(CampoPlanilha.VALOR, Campos.validarMapa(mapOf(CampoPlanilha.DOC to 0, CampoPlanilha.NOME to 1, CampoPlanilha.VENC to 2))!!.first)
        assertNull(Campos.validarMapa(mapOf(CampoPlanilha.DOC to 0, CampoPlanilha.NOME to 1)))
    }
}

class LeituraTest {
    // R39: data só vale se existe.
    @Test fun datasEmVariosFormatos() {
        val d = LocalDate.of(2026, 11, 10)
        assertEquals(d, Datas.parse("10/11/2026"))
        assertEquals(d, Datas.parse("10-11-2026"))
        assertEquals(d, Datas.parse("10.11.26"))
        assertEquals(d, Datas.parse("2026-11-10"))
        assertEquals(d, Datas.parse("2026-11-10T00:00:00"))
        assertEquals(d, Datas.parse("46336")) // número de data do Excel
        assertNull(Datas.parse("32/13/2026"))
        assertNull(Datas.parse("30/02/2026"))
        assertNull(Datas.parse("amanhã"))
        assertNull(Datas.parse(""))
        assertNull(Datas.parse("150")) // um número pequeno não é data
    }

    // R49: "10/11/2026" é 10 de novembro, nunca 11 de outubro.
    @Test fun csvBrasileiroEntendeDiaMes() {
        val csv = "CPF/CNPJ;Nome;Valor;Vencimento\n$MARINA;\"Silva; João\";1.234,56;10/11/2026\n"
        val rows = Csv.ler(csv)
        assertEquals(listOf("CPF/CNPJ", "Nome", "Valor", "Vencimento"), rows[0])
        assertEquals("Silva; João", rows[1][1]) // ponto e vírgula dentro de aspas
        assertEquals("1.234,56", rows[1][2])
        assertEquals(LocalDate.of(2026, 11, 10), Datas.parse(rows[1][3]))
    }

    @Test fun csvDecodificaWindows1252ESeparadorCerto() {
        val bytes = "Nome;Valor\nJoão;10,00\n".toByteArray(charset("windows-1252"))
        val texto = Csv.decodificar(bytes)
        assertTrue(texto.contains("João"))
        assertEquals(';', Csv.separador(texto))
        assertEquals(',', Csv.separador("a,b,c\n1,2,3"))
        assertEquals("Olá", Csv.decodificar("﻿Olá".toByteArray(Charsets.UTF_8))) // tira a marca de ordem de bytes
    }

    // R48: o tipo vem do conteúdo, não do nome.
    @Test fun tipoDoArquivoPeloConteudo() {
        assertEquals(TipoArquivo.XLSX, LeitorArquivo.tipo(byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0)))
        assertEquals(TipoArquivo.IMAGEM, LeitorArquivo.tipo(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0)))
        assertEquals(TipoArquivo.PDF, LeitorArquivo.tipo("%PDF-1.4".toByteArray()))
        assertEquals(TipoArquivo.TEXTO, LeitorArquivo.tipo("a;b\n1;2".toByteArray()))
        assertEquals(TipoArquivo.OUTRO, LeitorArquivo.tipo(ByteArray(50) { 0 }))
        assertEquals(TipoArquivo.XLS_ANTIGO, LeitorArquivo.tipo(byteArrayOf(0xD0.toByte(), 0xCF.toByte(), 0x11, 0xE0.toByte())))
    }

    @Test fun errosClarosParaArquivosQueNaoSaoPlanilha() {
        assertEquals("O arquivo está vazio", (LeitorArquivo.ler(ByteArray(0)) as Leitura.Erro).titulo)
        assertEquals("Esse arquivo não é uma planilha", (LeitorArquivo.ler("%PDF-1.4".toByteArray()) as Leitura.Erro).titulo)
        assertTrue((LeitorArquivo.ler(byteArrayOf(0x50, 0x4B, 0x03, 0x04, 1, 2, 3)) as Leitura.Erro).titulo.startsWith("Não deu para ler"))
        assertTrue(LeitorArquivo.ler("a;b\n1;2".toByteArray()) is Leitura.Ok)
    }

    // O Excel tira o zero da frente do CPF: 01234567890 vira 1234567890.
    @Test fun cpfSemOZeroDaFrente() {
        val d = LeituraDoc.ler("1234567890", producao = true)
        assertTrue(d.ok && d.zeros)
        assertEquals("01234567890", d.d)
        assertTrue(LeituraDoc.ler("637.129.261-70", producao = true).ok)
        assertFalse(LeituraDoc.ler("63712926171", producao = true).ok)
        assertTrue(LeituraDoc.ler("63712926171", producao = false).teste) // versão de teste aceita, avisando
        assertFalse(LeituraDoc.ler("", producao = true).ok)
    }
}

class PlanoImportacaoTest {
    private val joana = "193.573.925-56"

    @Test fun clienteComVariasContasEmVariasLinhas() {
        val pl = planejar(listOf(
            linha(joana, "Joana Prado", "(11) 97777-1111", "joana@exemplo.com", "Mensalidade", "200", "10/11/2026"),
            linha(joana, "Joana Prado", "", "", "Mensalidade", "200", "10/12/2026"),
            linha(joana, "Joana Prado", "", "", "Taxa extra", "75,50", "15/12/2026"),
        ))
        assertEquals(1, pl.clientesNovos.size) // 3 linhas com o mesmo CPF viram 1 cliente
        assertEquals(3, pl.contasNovas.size)
        assertEquals(7_550L, pl.contasNovas[2].valor)
        assertEquals("(11) 97777-1111", pl.clientesNovos[0].telefone)
        assertEquals(0, pl.erros.size)
        assertEquals("1 cliente e 3 contas", Importacao.textoMudancas(pl, true, true))
        assertEquals(1, pl.grupos.size) // a prévia agrupa por cliente (R55)
        assertEquals(3, pl.grupos[0].contasNovas.size)
    }

    // R38: importar de novo não duplica nada.
    @Test fun reimportarDaZeroMudancas() {
        val linhas = listOf(
            linha(MARINA, "Marina Alves", "", "", "Mensalidade", "180,00", "10/11/2026"),
            linha(PADARIA, "Padaria Bom Pão", "", "", "Pães", "1.250,00", "11/11/2026"),
        )
        val primeira = planejar(linhas)
        var n = 0
        val ap = Gravacao.aplicar(primeira, true, true, emptyList()) { "x${++n}" }
        val segunda = planejar(linhas, clientes = ap.clientesNovos, contas = ap.contasNovas)
        assertEquals(0, segunda.clientesNovos.size)
        assertEquals(0, segunda.contasNovas.size)
        assertEquals(0, segunda.contasAtualizadas.size)
        assertEquals(0, segunda.clientesAtualizados.size)
        assertEquals(2, segunda.semMudanca.size)
        assertEquals("", Importacao.textoMudancas(segunda, true, true))
    }

    @Test fun mudancaDeValorEBaixaSemCodigo() {
        val cli = Cliente("c1", "Marina Alves", "63712926170")
        val conta = Conta("k1", "c1", "Mensalidade", 18_000, LocalDate.of(2026, 11, 10))
        val pl = planejar(
            listOf(linha(MARINA, "Marina Alves", desc = "Mensalidade", valor = "200,00", venc = "10/11/2026", pago = "04/11/2026")),
            clientes = listOf(cli), contas = listOf(conta),
        )
        val mud = pl.contasAtualizadas.single().mud
        assertEquals(listOf("valor", "pagoEm"), mud.map { it.campo })
        assertEquals("20000", mud[0].v)
        assertEquals("paga em 04/11/2026", mud[1].para)
        assertEquals("em aberto", mud[1].de)
    }

    // R53: linha com código atualiza aquela conta.
    @Test fun linhaComCodigoAtualizaAConta() {
        val cli = Cliente("c1", "Marina Alves", "63712926170", "(11) 90000-0000")
        val conta = Conta("k1", "c1", "Mensalidade", 18_000, LocalDate.of(2026, 11, 10))
        val pl = planejar(
            listOf(linha(MARINA, "Marina Alves", tel = "(11) 98765-4321", desc = "Mensalidade nov", valor = "190,00", venc = "12/11/2026", cod = "k1")),
            clientes = listOf(cli), contas = listOf(conta),
        )
        assertEquals(0, pl.contasNovas.size)
        assertEquals(listOf("descricao", "valor", "venc"), pl.contasAtualizadas.single().mud.map { it.campo })
        assertEquals("Telefone", pl.clientesAtualizados.single().mud.single().rot) // o contato também muda
        val ap = Gravacao.aplicar(pl, true, true, listOf(conta)) { "x" }
        assertEquals(LocalDate.of(2026, 11, 12), ap.contasAtualizadas.single().venc)
        assertEquals("Mensalidade nov", ap.contasAtualizadas.single().descricao)
        assertEquals("(11) 98765-4321", ap.clientesAtualizados.single().telefone)
    }

    @Test fun codigoDeOutroCpfEBarrado() {
        val cli = Cliente("c1", "Marina Alves", "63712926170")
        val conta = Conta("k1", "c1", "Mensalidade", 18_000, LocalDate.of(2026, 11, 10))
        val pl = planejar(listOf(linha(PADARIA, "Padaria", valor = "10", venc = "10/11/2026", cod = "k1")), clientes = listOf(cli), contas = listOf(conta))
        val e = pl.erros.single()
        assertEquals("CPF/CNPJ", e.problemas.single().coluna)
        assertTrue(e.problemas.single().texto.startsWith("não é do dono desta conta"))
        assertTrue(e.problemas.single().texto.contains("Marina Alves"))
    }

    // R53: código que não existe + CPF = restaurar a cópia num celular novo, com o mesmo código.
    @Test fun restaurarCopiaCriaContaComOMesmoCodigo() {
        val pl = planejar(listOf(linha(MARINA, "Marina Alves", desc = "Mensalidade", valor = "180,00", venc = "10/11/2026", cod = "abc123")))
        assertEquals("abc123", pl.contasNovas.single().id)
        val ap = Gravacao.aplicar(pl, true, true, emptyList()) { "outro" }
        assertEquals("abc123", ap.contasNovas.single().id)
        // Se o código já existe no app, o app cria um novo em vez de sobrescrever.
        val colisao = Gravacao.aplicar(pl, true, true, listOf(Conta("abc123", "x", "d", 1, HOJE))) { "novoId" }
        assertEquals("novoId", colisao.contasNovas.single().id)
    }

    @Test fun codigoInexistenteSemNomeNemCpfDizComoCorrigir() {
        val pl = planejar(listOf(linha(valor = "10", venc = "10/11/2026", cod = "zzz")))
        assertTrue(pl.erros.single().problemas.any { it.coluna == "Código da conta" && it.texto.contains("apague o código") })
    }

    // Restaurar a cópia num celular novo: código que não existe, mas a linha tem o nome, cria a conta com o mesmo código.
    @Test fun codigoInexistenteComNomeCriaAContaComOMesmoCodigo() {
        val pl = planejar(listOf(linha(nome = "Ana", desc = "X", valor = "10", venc = "10/11/2026", cod = "zzz")))
        assertEquals(0, pl.erros.size)
        assertEquals("zzz", pl.contasNovas.single().id)
    }

    // R44: a linha, a coluna, a célula e como corrigir; vários problemas de uma vez.
    @Test fun errosDizemLinhaColunaCelulaEComoCorrigir() {
        val pl = planejar(listOf(
            linha(MARINA, "Marina", valor = "abc", venc = "32/13/2026"),
            linha(MARINA, "Marina", valor = "10", venc = "10/11/2026", pago = "30/12/2026"),
        ))
        assertEquals(2, pl.erros.size)
        val e1 = pl.erros[0]
        assertEquals(2, e1.linha)
        assertEquals(listOf("Valor", "Vencimento"), e1.problemas.map { it.coluna })
        assertEquals(listOf("F2", "G2"), e1.problemas.map { it.ref })
        assertEquals("“abc” não é um valor. Use só números, por exemplo 150,00.", e1.problemas[0].texto)
        assertTrue(e1.problemas[1].texto.contains("por exemplo 10/11/2026"))
        assertEquals("30/12/2026 é depois de hoje. Se ainda não foi paga, deixe vazio.", pl.erros[1].problemas.single().texto)
        assertEquals(0, pl.clientesNovos.size) // linha com problema não cria nada
    }

    @Test fun valorZeroOuNegativoERecusado() {
        val pl = planejar(listOf(linha(MARINA, "Marina", valor = "0", venc = "10/11/2026")))
        assertTrue(pl.erros.single().problemas.single().texto.contains("maior que zero"))
    }

    @Test fun contaRepetidaNoMesmoArquivo() {
        val l = linha(MARINA, "Marina", desc = "Mensalidade", valor = "10", venc = "10/11/2026")
        val pl = planejar(listOf(l, l))
        assertEquals(1, pl.contasNovas.size)
        assertTrue(pl.erros.single().problemas.single().texto.startsWith("Conta repetida: igual à linha 2"))
    }

    // R52 e R59: cliente antigo sem CPF/CNPJ.
    @Test fun clienteAntigoSemDocumentoCompletaPelaPlanilha() {
        val antigo = Cliente("a1", "Zeca Antigo", "")
        // Sem CPF/CNPJ na linha, o nome acha o cliente que já está no app: nada de cliente repetido.
        val sem = planejar(listOf(linha(nome = "Zeca Antigo", desc = "Y", valor = "10", venc = "10/11/2026")), clientes = listOf(antigo))
        assertEquals(0, sem.erros.size)
        assertEquals(0, sem.clientesNovos.size)
        assertEquals("a1", sem.contasNovas.single().clienteId)
        val com = planejar(listOf(linha("193.573.925-56", "Zeca Antigo", desc = "X", valor = "10", venc = "10/11/2026")), clientes = listOf(antigo))
        assertEquals(0, com.clientesNovos.size) // é o mesmo cliente, agora com o número
        assertEquals("doc", com.clientesAtualizados.single().mud.single().campo)
        assertEquals(1, com.contasNovas.size)
    }

    // CPF/CNPJ é opcional: o cliente novo entra só com o nome, e a mesma pessoa em duas linhas é um cliente só.
    @Test fun linhaSemCpfCriaClientePeloNome() {
        val pl = planejar(listOf(
            linha(nome = "Ana", desc = "A", valor = "10", venc = "10/11/2026"),
            linha(nome = "ana", desc = "B", valor = "20", venc = "10/12/2026"),
        ))
        assertEquals(0, pl.erros.size)
        assertEquals(1, pl.clientesNovos.size)
        assertEquals("", pl.clientesNovos.single().doc)
        assertEquals(2, pl.contasNovas.size)
    }

    @Test fun linhaSemCpfENemNomeEBarrada() {
        val pl = planejar(listOf(linha(tel = "11999990000", valor = "10", venc = "10/11/2026")))
        assertTrue(pl.erros.single().problemas.single().texto.startsWith("está vazio. Informe o nome do cliente"))
    }

    @Test fun cpfInvalidoNaProducaoMasAceitoNoTesteComAviso() {
        val l = linha("63712926171", "Ana", desc = "X", valor = "10", venc = "10/11/2026")
        assertEquals(1, planejar(listOf(l), producao = true).erros.size)
        val teste = planejar(listOf(l), producao = false)
        assertEquals(0, teste.erros.size)
        assertEquals(1, teste.avisos.size) // R61: sempre avisando
    }

    @Test fun zeroDaFrenteDoExcelCompletaEAvisa() {
        val pl = planejar(listOf(linha("1234567890", "Ana", desc = "X", valor = "10", venc = "10/11/2026")))
        assertEquals("01234567890", pl.clientesNovos.single().doc)
        assertTrue(pl.avisos.single().problemas.single().texto.contains("o Excel tirou o zero da frente"))
    }

    @Test fun nomeDiferenteParaOMesmoCpfAvisaQueOMudara() {
        val cli = Cliente("c1", "Marina Alves", "63712926170")
        val pl = planejar(listOf(linha(MARINA, "Marina A. Souza", desc = "X", valor = "10", venc = "10/11/2026")), clientes = listOf(cli))
        assertEquals("Nome", pl.clientesAtualizados.single().mud.single().rot)
        assertTrue(pl.avisos.single().problemas.single().texto.contains("o nome muda para “Marina A. Souza”"))
    }

    @Test fun soClientesQuandoNaoHaColunasDeConta() {
        val aoa = listOf(listOf("CPF/CNPJ", "Nome"), listOf(MARINA, "Marina"), listOf(PADARIA, "Padaria"))
        val aba = Campos.analisarAba("A", aoa)!!
        val mapa = Campos.autoMapa(aba.cols)
        assertTrue(Campos.soClientes(mapa))
        var n = 0
        val pl = Importacao.planejar(Campos.linhasDoMapa(aba, mapa), "c.xlsx", true, emptyList(), emptyList(), HOJE, true) { "n${++n}" }
        assertEquals(2, pl.clientesNovos.size)
        assertEquals(0, pl.contasNovas.size)
    }

    @Test fun emailEscritoErradoEntraSemOEmailComAviso() {
        val pl = planejar(listOf(linha(MARINA, "Marina", mail = "marina-sem-arroba", desc = "X", valor = "10", venc = "10/11/2026")))
        assertEquals("", pl.clientesNovos.single().email)
        assertEquals("E-mail", pl.avisos.single().problemas.single().coluna)
    }

    // R83: uma linha sem código e com outro vencimento parece mudança de conta que já existe.
    @Test fun avisaQuandoParecemudancaDeConta() {
        val cli = Cliente("c1", "Marina Alves", "63712926170")
        val conta = Conta("k1", "c1", "Mensalidade", 18_000, LocalDate.of(2026, 11, 10))
        val pl = planejar(listOf(linha(MARINA, "Marina Alves", desc = "Mensalidade", valor = "180,00", venc = "20/11/2026")), clientes = listOf(cli), contas = listOf(conta))
        assertEquals(1, pl.parecemAtualizacao.size)
        assertEquals(conta, Importacao.contaAnterior(pl.parecemAtualizacao[0], listOf(conta)))
        // Se já está paga, não é mudança de vencimento: é uma conta nova de verdade.
        val paga = conta.copy(pagoEm = LocalDate.of(2026, 11, 11))
        val pl2 = planejar(listOf(linha(MARINA, "Marina Alves", desc = "Mensalidade", valor = "180,00", venc = "20/11/2026")), clientes = listOf(cli), contas = listOf(paga))
        assertEquals(0, pl2.parecemAtualizacao.size)
    }

    // R55: duas escolhas independentes: cadastrar o que é novo e atualizar o que mudou.
    @Test fun escolherSoNovosOuSoAtualizacoes() {
        val cli = Cliente("c1", "Marina Alves", "63712926170")
        val conta = Conta("k1", "c1", "Mensalidade", 18_000, LocalDate.of(2026, 11, 10))
        val pl = planejar(listOf(
            linha(MARINA, "Marina Alves", desc = "Mensalidade", valor = "200,00", venc = "10/11/2026"),
            linha(PADARIA, "Padaria", desc = "Pães", valor = "50", venc = "11/11/2026"),
        ), clientes = listOf(cli), contas = listOf(conta))
        assertEquals("1 cliente, 1 conta e 1 atualização", Importacao.textoMudancas(pl, true, true))
        assertEquals("1 atualização", Importacao.textoMudancas(pl, false, true))
        assertEquals("1 cliente e 1 conta", Importacao.textoMudancas(pl, true, false))
        assertEquals(0, Importacao.totalMudancas(pl, false, false))
        val soNovos = Gravacao.aplicar(pl, true, false, listOf(conta)) { "n" }
        assertEquals(0, soNovos.contasAtualizadas.size)
        assertEquals(1, soNovos.contasNovas.size)
    }
}

class DesfazerImportacaoTest {
    private fun importar(): Triple<List<Cliente>, List<Conta>, io.github.alessandrocdrx.acertototal.planilha.Aplicacao> {
        val cli = Cliente("c1", "Marina Alves", "63712926170", "(11) 90000-0000")
        val conta = Conta("k1", "c1", "Mensalidade", 18_000, LocalDate.of(2026, 11, 10))
        val pl = planejar(listOf(
            linha(MARINA, "Marina Alves", tel = "(11) 98765-4321", desc = "Mensalidade", valor = "200,00", venc = "10/11/2026"),
            linha(PADARIA, "Padaria Bom Pão", desc = "Pães", valor = "50", venc = "11/11/2026"),
        ), clientes = listOf(cli), contas = listOf(conta))
        var n = 0
        val ap = Gravacao.aplicar(pl, true, true, listOf(conta)) { "g${++n}" }
        return Triple(listOf(cli), listOf(conta), ap)
    }

    /** O estado do banco depois da importação. */
    private fun depois(c: List<Cliente>, k: List<Conta>, ap: io.github.alessandrocdrx.acertototal.planilha.Aplicacao): Pair<List<Cliente>, List<Conta>> {
        val clientes = c.map { x -> ap.clientesAtualizados.firstOrNull { it.id == x.id } ?: x } + ap.clientesNovos
        val contas = k.map { x -> ap.contasAtualizadas.firstOrNull { it.id == x.id } ?: x } + ap.contasNovas
        return clientes to contas
    }

    @Test fun desfazerDevolveTudoAoQueEra() {
        val (c, k, ap) = importar()
        val (clientes, contas) = depois(c, k, ap)
        assertEquals(2, clientes.size)
        assertEquals(2, contas.size)
        val r = Gravacao.calcularDesfazer(ap.desfazer, clientes, contas)
        assertEquals(listOf("g1"), r.contasExcluir) // a conta nova sai
        assertEquals(1, r.clientesExcluir.size) // o cliente novo sai
        assertEquals(0, r.mantidos)
        assertEquals(18_000L, r.contasRestaurar.single().valor) // o valor volta ao de antes
        assertEquals("(11) 90000-0000", r.clientesRestaurar.single().telefone) // e o telefone também
    }

    // R50: o que a pessoa usou depois fica.
    @Test fun oQueFoiUsadoDepoisFica() {
        val (c, k, ap) = importar()
        val (clientes, contas) = depois(c, k, ap)
        val usada = contas.map { if (it.id == "g1") it.copy(pagoEm = HOJE) else it } // deu baixa na conta que entrou
        val r = Gravacao.calcularDesfazer(ap.desfazer, clientes, usada)
        assertTrue(r.contasExcluir.isEmpty())
        assertTrue(r.clientesExcluir.isEmpty()) // o cliente novo tem conta, então fica
        assertEquals(2, r.mantidos)
        val editada = contas.map { if (it.id == "k1") it.copy(valor = 99_900) else it } // mudou o valor da conta atualizada
        val r2 = Gravacao.calcularDesfazer(ap.desfazer, clientes, editada)
        assertTrue(r2.contasRestaurar.isEmpty())
        assertEquals(1, r2.mantidos)
    }

    @Test fun comprovanteDeContaNovaImpedeExcluir() {
        val (c, k, ap) = importar()
        val (clientes, contas) = depois(c, k, ap)
        val comComp = contas.map { if (it.id == "g1") it.copy(compArquivo = "foto.jpg") else it }
        assertTrue(Gravacao.calcularDesfazer(ap.desfazer, clientes, comComp).contasExcluir.isEmpty())
    }

    @Test fun historicoVoltaIgualDoJson() {
        val (_, _, ap) = importar()
        assertEquals(ap.desfazer, RegistroJson.desfazerDeJson(RegistroJson.desfazerParaJson(ap.desfazer)))
        assertEquals(ap.resumo, RegistroJson.resumoDeJson(RegistroJson.resumoParaJson(ap.resumo)))
        assertEquals(ap.itens, RegistroJson.itensDeJson(RegistroJson.itensParaJson(ap.itens)))
        assertEquals("1 cliente novo · 1 conta nova · 2 atualizações", ap.resumo.texto())
        assertNotNull(ap.itens.firstOrNull { it.titulo.startsWith("Cliente novo: Padaria Bom Pão") })
    }
}
