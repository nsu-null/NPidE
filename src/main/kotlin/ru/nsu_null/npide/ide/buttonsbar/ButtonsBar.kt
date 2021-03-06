package ru.nsu_null.npide.ide.buttonsbar

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.nsu_null.npide.ide.common.Settings
import ru.nsu_null.npide.ide.console.Console
import ru.nsu_null.npide.ide.editor.Editors
import ru.nsu_null.npide.ide.npide.NPIDE
import ru.nsu_null.npide.ide.projectstrategies.DebuggerAbility

private val MinFontSize = 6.sp
private val MaxFontSize = 40.sp

@Composable
fun ButtonsBar(settings: Settings, editors: Editors, console: Console) = Box(
    Modifier
        .height(32.dp)
        .fillMaxWidth()
        .padding(4.dp)
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {

        ButtonsBarButton("Build") { build() }

        ButtonsBarButton("Run") { run() }

        ButtonsBarButton("Debug") { debug() }

        ButtonsBarButton("Save") { editors.active!!.writeContents(editors.active!!.content) }

        ButtonsBarDebugButton("Step", DebuggerAbility.Step) { step() }

        ButtonsBarDebugButton("Continue", DebuggerAbility.Continue) { cont() }

    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun RowScope.ButtonsBarDebugButton(name: String, ability: DebuggerAbility, onClick: () -> Unit) {
    var active by remember { mutableStateOf(false) }
    val enabled = ability in NPIDE.debugger.abilities
    Button(
        onClick = onClick,
        enabled = enabled,
        contentPadding = PaddingValues(),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if (active) LocalContentColor.current.copy(alpha = 0.20f) else LocalContentColor.current),
        modifier = Modifier.align(Alignment.CenterVertically)
            .align(Alignment.CenterVertically)
            .clipToBounds()
            .pointerMoveFilter(
                onEnter = {
                    active = true
                    true
                },
                onExit = {
                    active = false
                    true
                }
            )
    ) {
        Text(
            text = name,
            color = LocalContentColor.current.copy(alpha = 0.60f),
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun RowScope.ButtonsBarButton(name: String, onClick: () -> Unit) {
    var active by remember { mutableStateOf(false) }
    Button(
        onClick = onClick,
        contentPadding = PaddingValues(),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if (active) LocalContentColor.current.copy(alpha = 0.20f) else LocalContentColor.current),
        modifier = Modifier.align(Alignment.CenterVertically)
            .align(Alignment.CenterVertically)
            .clipToBounds()
            .pointerMoveFilter(
                onEnter = {
                    active = true
                    true
                },
                onExit = {
                    active = false
                    true
                }
            )
    ) {
        Text(
            text = name,
            color = LocalContentColor.current.copy(alpha = 0.60f),
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp
        )
    }
}

private fun Density.scale(scale: Float) = Density(density * scale, fontScale * scale)
private operator fun TextUnit.minus(other: TextUnit) = (value - other.value).sp
private operator fun TextUnit.div(other: TextUnit) = value / other.value
