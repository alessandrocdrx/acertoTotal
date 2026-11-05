package io.github.alessandrocdrx.acertototal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.alessandrocdrx.acertototal.dominio.InfoBackup

/** Uma tarefa por cartão, com o nome do que a pessoa quer fazer (R45, R56). */
@Composable
fun TelaPlanilha(
    backup: InfoBackup,
    aoExportar: () -> Unit,
    aoBackup: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("O que você quer fazer?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Opcao(Icons.Filled.FileDownload, "Exportar relatório", "Contas em Excel para ler ou mandar a alguém", aoExportar)
        Opcao(
            Icons.Filled.Backup, "Cópia de segurança", "Tudo numa planilha, para guardar ou trocar de celular", aoBackup,
            extra = { SeloSituacao(backup.tom, backup.texto) },
        )
    }
}
