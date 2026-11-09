package io.github.alessandrocdrx.acertototal.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.alessandrocdrx.acertototal.dados.Cliente
import io.github.alessandrocdrx.acertototal.dominio.dataCompleta
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Janela que sobe da base da tela. Tem título e sempre uma saída escrita (R69). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Folha(
    titulo: String,
    aoFechar: () -> Unit,
    subtitulo: String? = null,
    conteudo: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = aoFechar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).imePadding().padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
        ) {
            Text(titulo, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (subtitulo != null) Text(subtitulo, color = MaterialTheme.colorScheme.onSurfaceVariant)
            conteudo()
        }
    }
}

/**
 * Janela com o botão principal fixo embaixo, visível mesmo com o conteúdo rolado (R46).
 * O que está em [rodape] não rola junto com [conteudo].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolhaComRodape(
    titulo: String,
    aoFechar: () -> Unit,
    subtitulo: String? = null,
    rodape: @Composable ColumnScope.() -> Unit,
    conteudo: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = aoFechar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.imePadding()) {
            Column(
                Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
            ) {
                Text(titulo, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (subtitulo != null) Text(subtitulo, color = MaterialTheme.colorScheme.onSurfaceVariant)
                conteudo()
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Column(
                Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
            ) { rodape() }
        }
    }
}

/** Botão de largura total e pelo menos 48 dp de altura (R12). */
@Composable
fun BotaoBloco(texto: String, aoClicar: () -> Unit, principal: Boolean = false, perigo: Boolean = false, habilitado: Boolean = true) {
    val m = Modifier.fillMaxWidth().heightIn(min = 48.dp)
    when {
        principal -> Button(onClick = aoClicar, modifier = m, enabled = habilitado) { Text(texto) }
        perigo -> TextButton(
            onClick = aoClicar, modifier = m, enabled = habilitado,
            colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) { Text(texto) }
        else -> OutlinedButton(onClick = aoClicar, modifier = m, enabled = habilitado) { Text(texto) }
    }
}

/**
 * Campo com rótulo sempre visível (R11), exemplo numa dica abaixo do rótulo e não dentro do campo (R9),
 * e o erro embaixo do campo, em vermelho, explicando como corrigir (R8).
 */
@Composable
fun CampoForm(
    rotulo: String,
    valor: String,
    aoMudar: (String) -> Unit,
    modifier: Modifier = Modifier,
    dica: String? = null,
    erro: String? = null,
    aviso: String? = null,
    teclado: KeyboardType = KeyboardType.Text,
    prefixo: String? = null,
    linhas: Int = 1,
    /** Mostra o valor com pontuação (telefone, CPF/CNPJ) sem mudar o que fica guardado (R94). */
    mascara: ((String) -> String)? = null,
) {
    Column(modifier.fillMaxWidth()) {
        Text(rotulo, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        if (dica != null) Text(dica, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = valor,
            onValueChange = aoMudar,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp).semantics { contentDescription = rotulo },
            isError = erro != null,
            supportingText = when {
                erro != null -> ({ Text(erro) })
                aviso != null -> ({ Text(aviso, color = MaterialTheme.acerto.aviso) })
                else -> null
            },
            prefix = prefixo?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = teclado),
            visualTransformation = mascara?.let { MascaraVisual(it) } ?: androidx.compose.ui.text.input.VisualTransformation.None,
            singleLine = linhas == 1,
            minLines = linhas,
            colors = camposAcerto(),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampoData(
    rotulo: String,
    data: LocalDate?,
    aoMudar: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    maxima: LocalDate? = null,
    erro: String? = null,
    /** Quando informado, aparece um "x" para esvaziar o campo (datas opcionais, como nos filtros). */
    aoLimpar: (() -> Unit)? = null,
) {
    var aberto by remember { mutableStateOf(false) }
    Column(modifier.fillMaxWidth()) {
        Text(rotulo, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = data?.let(::dataCompleta).orEmpty(),
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            isError = erro != null,
            supportingText = erro?.let { { Text(it) } },
            trailingIcon = {
                androidx.compose.foundation.layout.Row {
                    if (aoLimpar != null && data != null) {
                        IconButton(onClick = aoLimpar, modifier = Modifier.heightIn(min = 48.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "Limpar $rotulo")
                        }
                    }
                    IconButton(onClick = { aberto = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Icon(Icons.Filled.DateRange, contentDescription = "Escolher $rotulo")
                    }
                }
            },
            colors = camposAcerto(),
        )
    }
    if (aberto) {
        val limite = maxima?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = data?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = limite == null || utcTimeMillis <= limite
            },
        )
        DatePickerDialog(
            onDismissRequest = { aberto = false },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let { aoMudar(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                    aberto = false
                }) { Text("Escolher") }
            },
            dismissButton = { TextButton(onClick = { aberto = false }) { Text("Cancelar") } },
        ) { DatePicker(state = estado) }
    }
}

/** Cliente sem CPF/CNPJ aparece desativado na lista: não recebe conta nova (R59). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeletorCliente(
    clientes: List<Cliente>,
    selecionado: String?,
    aoEscolher: (String) -> Unit,
    modifier: Modifier = Modifier,
    erro: String? = null,
) {
    var aberto by remember { mutableStateOf(false) }
    Column(modifier.fillMaxWidth()) {
        Text("Cliente", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        ExposedDropdownMenuBox(expanded = aberto, onExpandedChange = { aberto = it }) {
            OutlinedTextField(
                value = clientes.firstOrNull { it.id == selecionado }?.nome.orEmpty(),
                onValueChange = {},
                readOnly = true,
                placeholder = { Text("Escolha o cliente") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = aberto) },
                isError = erro != null,
                supportingText = erro?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp).menuAnchor(MenuAnchorType.PrimaryNotEditable),
                colors = camposAcerto(),
            )
            ExposedDropdownMenu(expanded = aberto, onDismissRequest = { aberto = false }) {
                clientes.sortedBy { it.nome.lowercase() }.forEach { c ->
                    DropdownMenuItem(
                        text = { Text(c.nome) },
                        onClick = { aoEscolher(c.id); aberto = false },
                    )
                }
            }
        }
    }
}

/**
 * Máscara visual: o campo guarda só os números e mostra com pontuação (R94).
 * O cursor anda pelos números: depois do n-ésimo número na tela fica depois do n-ésimo número guardado.
 */
class MascaraVisual(private val formatar: (String) -> String) : androidx.compose.ui.text.input.VisualTransformation {
    override fun filter(text: androidx.compose.ui.text.AnnotatedString): androidx.compose.ui.text.input.TransformedText {
        val original = text.text
        val visto = formatar(original)
        // posicoes[i] = onde o i-ésimo caractere guardado aparece na tela; o cursor fica logo depois dele.
        val posicoes = IntArray(original.length)
        var j = 0
        for (i in original.indices) {
            while (j < visto.length && visto[j] != original[i]) j++
            posicoes[i] = j.coerceAtMost(visto.length)
            j++
        }
        val mapa = object : androidx.compose.ui.text.input.OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = when {
                offset <= 0 -> 0
                offset >= original.length -> visto.length
                else -> posicoes[offset - 1] + 1
            }
            override fun transformedToOriginal(offset: Int): Int =
                posicoes.count { it < offset.coerceIn(0, visto.length) }
        }
        return androidx.compose.ui.text.input.TransformedText(androidx.compose.ui.text.AnnotatedString(visto), mapa)
    }
}
