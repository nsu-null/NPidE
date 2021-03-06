package ru.nsu_null.npide.parser.generator

import org.antlr.v4.runtime.Lexer
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Paths

class G4LanguageManager(
    val languageName: String
) {
    companion object {
        var baseDir = "./tmp_antlr_generated_sources"
    }

    private val classNameToClass = HashMap<String, Class<*>>()
    private fun loadClass(className: String): Class<*> {
        if (classNameToClass.containsKey(className)) {
            return classNameToClass[className]!!
        }
        val file = File(Paths.get(baseDir).toString())
        val url: URL = file.toURI().toURL()
        val urlClassLoader = URLClassLoader.newInstance(
            arrayOf(
                url
            ), Lexer::class.java.classLoader
        )
        try {
            val clazz = urlClassLoader.loadClass("${languageName}${className}");
            classNameToClass[className] = clazz
            return clazz
        } catch (e: ClassNotFoundException) {
            val newClassName = if (className == "Parser") {
                "";
            } else {
                "_" + className.lowercase();
            }

            val clazz = urlClassLoader.loadClass("${languageName}${newClassName}");
            classNameToClass[className] = clazz
            return clazz
        }
    }

    fun loadLexerClass(): Class<*> {
        return loadClass("Lexer")
    }

    fun loadParserClass(): Class<*> {
        return loadClass("Parser")
    }

    fun loadListenerClass(): Class<*> {
        return loadClass("Listener")
    }

    fun loadParserSubclass(subclassName: String): Class<*>? {
        val ParserClass = loadParserClass()
        for (clazz in ParserClass.declaredClasses) {
            if (clazz.name == "${languageName}Parser$${subclassName}" || clazz.name == "${languageName}$${subclassName}") {
                return clazz
            }
        }
        return null
    }
}