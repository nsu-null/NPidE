package ru.nsu_null.npide.ui.editor

import androidx.compose.runtime.mutableStateListOf
import ru.nsu_null.npide.platform.File
import ru.nsu_null.npide.util.SingleSelection

class Editors {
    private val selection = SingleSelection()

    var editors = mutableStateListOf<Editor>()
        private set

    val active: Editor? get() = selection.selected as Editor?

    lateinit var openedFile: File

    fun open(file: File) {
        val editor = Editor(file)
        openedFile = file
        editor.selection = selection
        editor.close = {
            close(editor)
        }
        editors.add(editor)
        editor.activate()
    }

    private fun close(editor: Editor) {
        val index = editors.indexOf(editor)
        editors.remove(editor)
        if (editor.isActive) {
            selection.selected = editors.getOrNull(index.coerceAtMost(editors.lastIndex))
        }
    }
}