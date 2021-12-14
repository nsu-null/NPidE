package ru.nsu_null.npide.parser.compose_support

import TokenHighlighter
import org.antlr.v4.runtime.Vocabulary
import org.fife.ui.rsyntaxtextarea.Style
import org.fife.ui.rsyntaxtextarea.SyntaxScheme
import java.awt.Color

class LanguageSyntaxScheme(val tokenHighlighter: TokenHighlighter, val tokenNumToTokenName: Vocabulary) :
    SyntaxScheme(false) {

    override fun getStyle(index: Int): Style {
        val tokenName: String;
        if (tokenNumToTokenName.getSymbolicName(index) == null) {
            tokenName = "ID"
        } else {
            tokenName = tokenNumToTokenName.getSymbolicName(index);
        }

        val color: Color = Color.decode(tokenHighlighter.getColor(tokenName))
        return Style(color)
    }

}