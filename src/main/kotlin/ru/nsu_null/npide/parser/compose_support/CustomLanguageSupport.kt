package ru.nsu_null.npide.parser.compose_support

import me.tomassetti.kanvas.AntlrLexerFactory
import me.tomassetti.kanvas.BaseLanguageSupport
import me.tomassetti.kanvas.ParserData
import me.tomassetti.kolasu.parsing.Parser
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CharStreams.fromString
import org.antlr.v4.runtime.Lexer
import org.antlr.v4.runtime.Vocabulary
import org.fife.ui.rsyntaxtextarea.SyntaxScheme

class CustomLanguageSupport(
    private val tokenHighlighter: TokenHighlighter,
    private val tokenNumToTokenName: Vocabulary,
    val LexerClass: Class<*>
) :
    BaseLanguageSupport<RootNode<String>>() {
    override val syntaxScheme: SyntaxScheme
        get() = LanguageSyntaxScheme(tokenHighlighter, tokenNumToTokenName)
    override val antlrLexerFactory: AntlrLexerFactory
        get() = object : AntlrLexerFactory {
            override fun create(code: String): Lexer {
                return LexerClass.getConstructor(CharStream::class.java).newInstance(fromString(code)) as Lexer
            }

        }
    override val parser: Parser<RootNode<String>>
        get() = TODO("Not yet implemented")
    override val parserData: ParserData?
        get() = null
}
