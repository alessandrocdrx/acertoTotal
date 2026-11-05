package io.github.alessandrocdrx.acertototal.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.alessandrocdrx.acertototal.dados.Cliente
import io.github.alessandrocdrx.acertototal.dominio.Documento
import io.github.alessandrocdrx.acertototal.dominio.brl
import io.github.alessandrocdrx.acertototal.dominio.plural

@Composable
fun TelaClientes(
    ui: ClientesUi,
    aoBuscar: (String) -> Unit,
    aoAbrir: (Cliente) -> Unit,
    modifier: Modifier = Modifier,
    estado: LazyListState = rememberLazyListState(),
) {
    LazyColumn(
        modifier = modifier,
        state = estado,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (ui.todos.size > 3) {
            item("busca") {
                OutlinedTextField(
                    value = ui.busca, onValueChange = aoBuscar, modifier = Modifier.fillMaxWidth(),
                    label = { Text("Buscar por nome ou telefone") }, singleLine = true, colors = camposAcerto(),
                )
            }
        }
        if (ui.linhas.isEmpty()) {
            item("vazio") {
                Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (ui.busca.isNotBlank()) {
                        Text("Nenhum cliente encontrado para “${ui.busca.trim()}”.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedButton(onClick = { aoBuscar("") }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Limpar busca") }
                    } else {
                        Text("Nenhum cliente ainda. Toque em “Novo cliente” ou importe uma planilha.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        items(ui.linhas, key = { it.cliente.id }) { l -> LinhaDeCliente(l, aoAbrir) }
    }
}

@Composable
private fun LinhaDeCliente(l: LinhaCliente, aoAbrir: (Cliente) -> Unit) {
    val c = l.cliente
    val cores = MaterialTheme.acerto
    Card(
        onClick = { aoAbrir(c) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(c.nome, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                val partes = buildList {
                    add(plural(l.nContas, "conta"))
                    if (l.maisAntiga > 0) add("a mais antiga há ${plural(l.maisAntiga, "dia")}")
                    if (c.doc.isNotEmpty() && !Documento.valido(c.doc)) add("CPF/CNPJ inválido (teste)")
                }
                Text(partes.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (l.atrasadas > 0) {
                    Surface(color = cores.ruimFundo, shape = RoundedCornerShape(50), modifier = Modifier.padding(top = 6.dp)) {
                        Text(
                            plural(l.atrasadas, "atrasada"),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = cores.ruim,
                        )
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 8.dp)) {
                if (l.aberto > 0) {
                    Text("Deve", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(brl(l.aberto), fontWeight = FontWeight.Bold)
                } else {
                    Text("Em dia", fontWeight = FontWeight.Bold, color = cores.ok)
                }
            }
            // Linha de lista tem seta: parece tocável (R84).
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
