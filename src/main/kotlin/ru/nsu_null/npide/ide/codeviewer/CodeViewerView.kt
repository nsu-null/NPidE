package ru.nsu_null.npide.ide.codeviewer

import androidx.compose.animation.core.Spring.StiffnessLow
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.nsu_null.npide.ide.buttonsbar.ButtonsBar
import ru.nsu_null.npide.ide.console.ClosedConsole
import ru.nsu_null.npide.ide.console.ConsolePane
import ru.nsu_null.npide.ide.editor.EditorEmptyView
import ru.nsu_null.npide.ide.editor.EditorTabsView
import ru.nsu_null.npide.ide.editor.EditorView
import ru.nsu_null.npide.ide.filetree.FileTreeView
import ru.nsu_null.npide.ide.filetree.FileTreeViewTabView
import ru.nsu_null.npide.ide.npide.NPIDE
import ru.nsu_null.npide.ide.util.SplitterState
import ru.nsu_null.npide.ide.util.VerticalSplittable

@ExperimentalFoundationApi
@Composable
fun CodeViewerView(model: CodeViewer) {
    val panelState = remember { PanelState() }

    val animatedSize = if (panelState.splitter.isResizing) {
        if (panelState.isExpanded) panelState.expandedSize else panelState.collapsedSize
    } else {
        animateDpAsState(
            if (panelState.isExpanded) panelState.expandedSize else panelState.collapsedSize,
            SpringSpec(stiffness = StiffnessLow)
        ).value
    }

    VerticalSplittable(
        Modifier.fillMaxSize(),
        panelState.splitter,
        onResize = {
            panelState.expandedSize =
                (panelState.expandedSize + it).coerceAtLeast(panelState.expandedSizeMin)
        }
    ) {
        ResizablePanel(Modifier.width(animatedSize).fillMaxHeight(), panelState) {
            Column {
                FileTreeViewTabView()
                FileTreeView(model.fileTree)
            }
        }

        Box {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(0.07f)) {
                    ButtonsBar(model.settings, model.editors, NPIDE.console)
                }
                Box(Modifier.weight(0.05f)) {
                    EditorTabsView(model.editors)
                }
                if (model.editors.active != null) {
                    Box(Modifier.weight(1f)) {
                        EditorView(model.editors.active!!, model.settings)
                    }
                } else {
                    EditorEmptyView(Modifier.weight(1f))
                }
                var showConsole by remember { mutableStateOf(true) }
                if (showConsole) {
                    Divider(Modifier.background(Color.Black).height(1.dp))
                    Box(Modifier.weight(0.4f)) {
                        ConsolePane(model.settings, NPIDE.console) { showConsole = false }
                    }
                } else {
                    Divider(Modifier.background(Color.Black).height(1.dp))
                    Box(Modifier.weight(0.05f)) {
                        ClosedConsole(model.settings, NPIDE.console) { showConsole = true }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun GitBranchTellerView(modifier: Modifier = Modifier) {
    val currentPath = NPIDE.currentProject!!.rootFolder.filepath
    val currentGitBranch = remember { mutableStateOf("") }
    LaunchedEffect(currentPath) {
        currentGitBranch.value = getGitBranchByPath(currentPath)
    }
    Row(modifier = modifier) {
        Icon(painterResource("icons8-git.svg"),
            "Git icon", modifier = Modifier.padding(2.dp))

        Spacer(Modifier.padding(3.dp))
        Box(modifier = Modifier, Alignment.BottomStart) {
            Text(currentGitBranch.value, fontSize = 17.sp, textAlign = TextAlign.Center)
        }
    }
}

private fun getGitBranchByPath(currentPath: String): String {
    val gitProcess = Runtime.getRuntime().exec(arrayOf("git","--git-dir=$currentPath/.git", "branch", "--show-current"))
    fun java.io.InputStream.readTextCompletely(): String = reader().use { it.readText() }

    val maybeBranch = gitProcess.inputStream.readTextCompletely()
    val errors = gitProcess.errorStream.readTextCompletely()

    if (errors.isNotEmpty()) {
        if (errors.startsWith("fatal: not a git repository:")) {
            return "[not a git repository]"
        }
        return "[git not found on system]"
    }

    return maybeBranch
}

private class PanelState {
    val collapsedSize = 24.dp
    var expandedSize by mutableStateOf(300.dp)
    val expandedSizeMin = 90.dp
    var isExpanded by mutableStateOf(true)
    val splitter = SplitterState()
}

@Composable
private fun ResizablePanel(
    modifier: Modifier,
    state: PanelState,
    content: @Composable () -> Unit,
) {
    val alpha by animateFloatAsState(if (state.isExpanded) 1f else 0f, SpringSpec(stiffness = StiffnessLow))

    Box(modifier) {
        Box(Modifier.fillMaxSize().graphicsLayer(alpha = alpha)) {
            content()
        }

        Icon(
            if (state.isExpanded) Icons.Default.ArrowBack else Icons.Default.ArrowForward,
            contentDescription = if (state.isExpanded) "Collapse" else "Expand",
            tint = LocalContentColor.current,
            modifier = Modifier
                .padding(top = 4.dp)
                .width(24.dp)
                .clickable {
                    state.isExpanded = !state.isExpanded
                }
                .padding(4.dp)
                .align(Alignment.TopEnd)
        )
    }
}