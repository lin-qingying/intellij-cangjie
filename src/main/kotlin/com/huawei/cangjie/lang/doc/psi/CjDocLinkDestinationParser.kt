package com.huawei.cangjie.lang.doc.psi

import com.huawei.cangjie.lang.CjLanguage
import com.huawei.cangjie.lang.core.lexer.CangJieLexer
import com.huawei.cangjie.lang.core.parser.CangJieParser
import com.huawei.cangjie.lang.doc.psi.CjDocElementTypes.DOC_LINK_DEFINITION
import com.intellij.lang.LanguageParserDefinitions
import com.intellij.lang.impl.PsiBuilderImpl
import com.intellij.lang.parser.GeneratedParserUtilBase
import com.intellij.psi.TokenType
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.impl.source.tree.TreeElement
import com.intellij.util.CharTable



private class LinkTextParts private constructor(
    private val text: CharSequence,
    private val startOffset: Int,
    private val endOffset: Int,
) {
    constructor(text: CharSequence) : this(text, startOffset = 0, endOffset = text.length)

    val prefix: CharSequence get() = text.subSequence(0, startOffset)
    val suffix: CharSequence get() = text.subSequence(endOffset, text.length)
    val content: CharSequence get() = text.subSequence(startOffset, endOffset)
    private val contentLength: Int get() = endOffset - startOffset

    fun subSequence(start: Int, end: Int): LinkTextParts {
        if (start == 0 && end == contentLength) return this
        check(start in 0..end && end <= contentLength)
        return LinkTextParts(text, startOffset + start, startOffset + end)
    }

    fun removePrefix(length: Int): LinkTextParts = subSequence(length, contentLength)
    fun removeSuffix(length: Int): LinkTextParts = subSequence(0, contentLength - length)
}


object CjDocLinkDestinationParser {

    fun parse(text: CharSequence, charTable: CharTable): TreeElement =
        doParse(text, charTable, isShortLink = false) ?: docDataLeaf(text, charTable)

    fun parseShortLink(text: CharSequence, charTable: CharTable): TreeElement? =
        doParse(text, charTable, isShortLink = true)

    private fun doParse(text: CharSequence, charTable: CharTable, isShortLink: Boolean): TreeElement? {
        val info = parseLink(text, isShortLink) ?: return null
        val path = parseCjPath(info.content, charTable) ?: return null
        return parsePathAndCreateNodes(info, path, charTable)
    }

    private fun parseLink(text: CharSequence, isShortLink: Boolean): LinkTextParts? {
        if (text.isEmpty() || text.contains("/")) return null

        return LinkTextParts(text)
            .run { if (isShortLink) preprocessShortLink() else this }
            ?.trimBackticks()
            ?.removeHashAnchor()
            ?.removeDisambiguator()
            ?.takeIf { canBeCorrectLink(it.content) }
    }

    private fun LinkTextParts.preprocessShortLink(): LinkTextParts? =
        trimBrackets()
            .trimWhitespaces()

    // "[func]" -> "func"
    private fun LinkTextParts.trimBrackets(): LinkTextParts {
        val content = content
        return if (content.startsWith('[') && content.endsWith(']')) {
            subSequence(1, content.length - 1)
        } else {
            this
        }
    }

    private fun LinkTextParts.trimBackticks(): LinkTextParts? = trimChar { it == '`' }

    private fun LinkTextParts.trimWhitespaces(): LinkTextParts? = trimChar { it.isWhitespace() }

    private fun LinkTextParts.trimChar(filter: (Char) -> Boolean): LinkTextParts? {
        val content = content
        var start = 0
        var end = content.length
        while (filter(content[start])) {
            ++start
        }
        if (start == end) return null
        while (filter(content[end - 1])) {
            --end
        }
        return subSequence(start, end)
    }


    private fun LinkTextParts.removeHashAnchor(): LinkTextParts? {
        val parts = content.split('#')
        if (parts.size > 2) return null  // multiple #'s - invalid link
        if (parts.size < 2) return this  // no anchors

        val link = parts[0]
        val hash = parts[1]

        // anchor to an element of the current page - ignore
        if (link.isBlank()) return null

        return removeSuffix("#".length + hash.length)
    }

    // "fn@func" -> "func"
    // "gen!()" -> "gen"
    private fun LinkTextParts.removeDisambiguator(): LinkTextParts? {
        val index = content.indexOf('@')
        if (index != -1) {
            val prefix = content.substring(0, index)
            if (prefix !in KNOWN_PREFIXES) return null
            return removePrefix(prefix.length + "@".length)
                .takeIf { it.content.isNotBlank() }
        } else {
            for (suffix in KNOWN_SUFFIXES) {
                if (content.endsWith(suffix) && content.length > suffix.length) {
                    return removeSuffix(suffix.length)
                }
            }
        }
        return this
    }

    private val KNOWN_PREFIXES: Set<String> = hashSetOf(
        "struct", "enum", "trait", "union", "module", "mod", "const", "constant", "static",
        "function", "fn", "method", "derive", "type", "value", "macro", "prim", "primitive"
    )
    private val KNOWN_SUFFIXES: Array<String> = arrayOf("!()", "!{}", "![]", "()", "!")

    private fun canBeCorrectLink(link: CharSequence): Boolean =
        link.all { it.isAlphanumeric() || it in ":_<>, !*&;" }

    private fun Char.isAlphanumeric(): Boolean = Character.isAlphabetic(code) || isDigit()

    private fun parsePathAndCreateNodes(info: LinkTextParts, path: TreeElement, charTable: CharTable): TreeElement? {
        val root = DOC_LINK_DEFINITION.createCompositeNode()
        val prefix = info.prefix
        val suffix = info.suffix
        if (prefix.isNotEmpty()) {
            root.rawAddChildrenWithoutNotifications(docDataLeaf(prefix, charTable))
        }
        root.rawAddChildrenWithoutNotifications(path)
        if (suffix.isNotEmpty()) {
            root.rawAddChildrenWithoutNotifications(docDataLeaf(suffix, charTable))
        }
        return root.firstChildNode
    }

    private fun parseCjPath(pathText: CharSequence, charTable: CharTable): TreeElement? {
        val parserDefinition = LanguageParserDefinitions.INSTANCE.forLanguage(CjLanguage)
            ?: error("No parser definition for language $CjLanguage")
        val plainBuilder = PsiBuilderImpl(null, null, parserDefinition, CangJieLexer(), charTable, pathText, null, null)

        val builder = GeneratedParserUtilBase.adapt_builder_(
            DOC_LINK_DEFINITION,
            plainBuilder,
            CangJieParser(),
            CangJieParser.EXTENDS_SETS_
        )

        val rootMarker = GeneratedParserUtilBase.enter_section_(builder, 0, GeneratedParserUtilBase._NONE_, null)



        GeneratedParserUtilBase.exit_section_(builder, 0, rootMarker, DOC_LINK_DEFINITION, true, true, GeneratedParserUtilBase.TRUE_CONDITION)

        val treeBuilt = builder.treeBuilt
        if (treeBuilt.findChildByType(TokenType.ERROR_ELEMENT) != null) return null
        return treeBuilt.firstChildNode as TreeElement
    }

    private fun docDataLeaf(text: CharSequence, charTable: CharTable) =
        LeafPsiElement(CjDocElementTypes.DOC_DATA, charTable.intern(text))
}
