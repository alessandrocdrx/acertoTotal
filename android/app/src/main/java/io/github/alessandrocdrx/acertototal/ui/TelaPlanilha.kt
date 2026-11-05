package io.github.alessandrocdrx.acertototal.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.alessandrocdrx.acertototal.dados.ImportacaoReg
import io.github.alessandrocdrx.acertototal.dominio.InfoBackup
import io.github.alessandrocdrx.acertototal.planilha.RegistroJson
import io.github.alessandrocdrx.acertototal.planilha.quandoTexto

/** Uma tarefa por cartão, com o nome do que a pessoa quer fazer (R45, R56). */
@Composable
fun TelaPlanilha(
    backup: InfoBackup,
    aoImportar: () -> Unit,
    aoAtualizar: () -> Unit,
    aoExportar: () -> Unit,
    aoBackup: () -> Unit,
    registros: List<ImportacaoReg>,
    aoAbrirRegistro: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("O que você quer fazer?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Opcao(Icons.Filled.FileUpload, "Importar planilha", "Cadastrar clientes e contas de uma vez", aoImportar)
        Opcao(Icons.Filled.Edit, "Atualizar pelo Excel", "Mudar valores, vencimentos e baixas de várias contas", aoAtualizar)
        Opcao(Icons.Filled.FileDownload, "Exportar relatório", "Contas em Excel para ler ou mandar a alguém", aoExportar)
        Opcao(
            Icons.Filled.Backup, "Cópia de segurança", "Tudo numa planilha, para guardar ou trocar de celular", aoBackup,
            extra = { SeloSituacao(backup.tom, backup.texto) },
        )
        if (registros.isNotEmpty()) {
            Text("Últimas importações", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp))
            registros.take(5).forEach { r ->
                Card(
                    onClick = { aoAbrirRegistro(r.id) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(r.arquivo, fontWeight = FontWeight.SemiBold)
                        Text(quandoTexto(r.quando), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            (if (r.desfeita != null) "Desfeita · " else "") + RegistroJson.resumoDeJson(r.resumo).texto(),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
