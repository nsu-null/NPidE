package ru.nsu_null.npide.ui.console.watches

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.nsu_null.npide.ui.GitBranchTellerView
import ru.nsu_null.npide.ui.console.Console

@Composable
fun WatchesView(modifier: Modifier, console: Console, onControlPanelSwitchRequest: () -> Unit, onCloseRequest: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(15.dp).then(modifier)) {
        Column(Modifier.fillMaxSize()) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.Start) {
                    if (console.processIsAttached) {
                        Icon(Icons.Default.Done, "Process is attached", tint = Color.Green)
                    } else {
                        Icon(Icons.Default.DoNotTouch, "No process attached", tint = Color.Red)
                    }
                    Spacer(Modifier.padding(3.dp))
                    val processMessage = if (!console.processIsAttached)
                        "No process attached" else "Process '${console.attachedProcessLabel}' is attached"
                    Text(processMessage, textAlign = TextAlign.Center)
                }
                Icon(Icons.Default.Settings, "Switch to control panel", tint = Color.LightGray,
                    modifier = Modifier.clickable { onControlPanelSwitchRequest() })
                Spacer(Modifier.padding(3.dp))
                Icon(Icons.Default.ArrowDownward, "Hide console", tint = Color.LightGray,
                    modifier = Modifier.clickable { onCloseRequest() })
            }
            Divider(Modifier.padding(0.dp, 15.dp))
            Column(Modifier.fillMaxSize()) {
                Text("THIS IS WATCHES", modifier = Modifier.weight(0.8f))
                GitBranchTellerView(Modifier.weight(0.2f))
            }
        }
    }
}