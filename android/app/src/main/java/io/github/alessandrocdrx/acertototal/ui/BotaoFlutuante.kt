package io.github.alessandrocdrx.acertototal.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow

/**
 * Estendido no topo da lista, encolhe para só o "+" ao rolar (R12) e sai da frente ao rolar para
 * baixo, para nunca cobrir o "Dar baixa"; volta ao rolar para cima (R79).
 */
@Composable
fun BotaoNovo(texto: String, estado: LazyListState, aoClicar: () -> Unit) {
    var visivel by remember { mutableStateOf(true) }
    val noTopo by remember { derivedStateOf { estado.firstVisibleItemIndex == 0 && estado.firstVisibleItemScrollOffset <= 24 } }
    LaunchedEffect(estado) {
        var antIndice = 0
        var antDesloc = 0
        snapshotFlow { estado.firstVisibleItemIndex to estado.firstVisibleItemScrollOffset }.collect { (i, o) ->
            val desceu = i > antIndice || (i == antIndice && o > antDesloc + 6)
            val subiu = i < antIndice || (i == antIndice && o < antDesloc - 6)
            val topo = i == 0 && o <= 24
            if (topo || subiu) visivel = true else if (desceu) visivel = false
            antIndice = i
            antDesloc = o
        }
    }
    AnimatedVisibility(visible = visivel, enter = scaleIn(), exit = scaleOut()) {
        ExtendedFloatingActionButton(
            onClick = aoClicar,
            expanded = noTopo,
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text(texto) },
        )
    }
}
