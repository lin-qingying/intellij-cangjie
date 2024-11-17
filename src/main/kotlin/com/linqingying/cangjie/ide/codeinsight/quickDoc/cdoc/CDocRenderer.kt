/*
 * Copyright 2024 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package com.linqingying.cangjie.ide.codeinsight.quickDoc.cdoc

import com.linqingying.cangjie.CangJieBundle
import com.linqingying.cangjie.doc.CDocTemplate
import com.linqingying.cangjie.doc.insert
import com.linqingying.cangjie.doc.psi.impl.CDocLink
import com.linqingying.cangjie.doc.psi.impl.CDocName
import com.linqingying.cangjie.doc.psi.impl.CDocSection
import com.linqingying.cangjie.doc.psi.impl.CDocTag
import com.linqingying.cangjie.highlighter.CangJieHighlightingColors
import com.linqingying.cangjie.highlighter.textAttributesKeyForCjElement
import com.linqingying.cangjie.ide.codeinsight.quickDoc.cdoc.CangJieIdeDescriptorRendererHighlightingManager.Companion.eraseTypeParameter
import com.linqingying.cangjie.lang.CangJieLanguage
import com.linqingying.cangjie.psi.CjBlockExpression
import com.linqingying.cangjie.psi.CjDeclarationWithBody
import com.linqingying.cangjie.psi.psiUtil.findDescendantOfType
import com.linqingying.cangjie.psi.psiUtil.getChildrenOfType
import com.linqingying.cangjie.references.CDocReference
import com.intellij.codeInsight.documentation.DocumentationManagerUtil
import com.intellij.lang.documentation.DocumentationMarkup.*
import com.intellij.lang.documentation.DocumentationSettings
import com.intellij.lang.documentation.QuickDocHighlightingHelper
import com.intellij.lang.documentation.QuickDocHighlightingHelper.appendStyledCodeBlock
import com.intellij.lang.documentation.QuickDocHighlightingHelper.appendStyledCodeFragment
import com.intellij.lang.documentation.QuickDocHighlightingHelper.appendStyledFragment
import com.intellij.lang.documentation.QuickDocHighlightingHelper.appendStyledInlineCode
import com.intellij.lang.documentation.QuickDocHighlightingHelper.appendStyledLinkFragment
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMTokenTypes
import org.intellij.markdown.parser.MarkdownParser
import org.jetbrains.annotations.Nls


object CDocRenderer {

    private fun StringBuilder.appendCDocContent(docComment: CDocTag): StringBuilder =
        append(markdownToHtml(docComment, allowSingleParagraph = true))

    private fun StringBuilder.appendCDocSections(sections: List<CDocSection>) {
        fun findTagsByName(name: String) =
            sequence { sections.forEach { yieldAll(it.findTagsByName(name)) } }

        fun findTagByName(name: String) = findTagsByName(name).firstOrNull()

        appendTag(findTagByName("receiver"), CangJieBundle.message("cdoc.section.title.receiver"))

        val paramTags = findTagsByName("param").filter { it.getSubjectName() != null }
        appendTagList(paramTags, CangJieBundle.message("cdoc.section.title.parameters"), CangJieHighlightingColors.PARAMETER)

        val propertyTags = findTagsByName("property").filter { it.getSubjectName() != null }
        appendTagList(
            propertyTags, CangJieBundle.message("cdoc.section.title.properties"), CangJieHighlightingColors.INSTANCE_PROPERTY
        )

        appendTag(findTagByName("constructor"), CangJieBundle.message("cdoc.section.title.constructor"))

        appendTag(findTagByName("return"), CangJieBundle.message("cdoc.section.title.returns"))

        val throwTags = findTagsByName("throws").filter { it.getSubjectName() != null }
        val exceptionTags = findTagsByName("exception").filter { it.getSubjectName() != null }
        appendThrows(throwTags, exceptionTags)

        appendAuthors(findTagsByName("author"))
        appendTag(findTagByName("since"), CangJieBundle.message("cdoc.section.title.since"))
        appendTag(findTagByName("suppress"), CangJieBundle.message("cdoc.section.title.suppress"))

        appendSeeAlso(findTagsByName("see"))

        val sampleTags = findTagsByName("sample").filter { it.getSubjectLink() != null }
        appendSamplesList(sampleTags)
    }

    fun StringBuilder.renderCDoc(
        contentTag: CDocTag,
        sections: List<CDocSection> = if (contentTag is CDocSection) listOf(contentTag) else emptyList()
    ) {
        insert(CDocTemplate.DescriptionBodyTemplate.CangJie()) {
            content {
                appendCDocContent(contentTag)
            }
            sections {
                appendCDocSections(sections)
            }
        }
    }

    private fun StringBuilder.appendHyperlink(kDocLink: CDocLink) {
        val linkText = kDocLink.getLinkText()
        if (DumbService.isDumb(kDocLink.project)) {
            append(linkText)
        } else {
            DocumentationManagerUtil.createHyperlink(
                this,
                linkText,
                highlightQualifiedName(linkText, getTargetLinkElementAttributes(kDocLink.getTargetElement())),
                false,
                true
            )
        }
    }

    private fun getTargetLinkElementAttributes(element: PsiElement?): TextAttributes {
        return element
            ?.let { textAttributesKeyForCjElement(it)?.attributesKey }
            ?.let { getTargetLinkElementAttributes(it) }
            ?: TextAttributes().apply {
                foregroundColor = EditorColorsManager.getInstance().globalScheme.getColor(
                    DefaultLanguageHighlighterColors.DOC_COMMENT_LINK)
            }
    }

    private fun getTargetLinkElementAttributes(key: TextAttributesKey): TextAttributes {
        return tuneAttributesForLink(EditorColorsManager.getInstance().globalScheme.getAttributes(key))
    }

    private fun highlightQualifiedName(qualifiedName: String, lastSegmentAttributes: TextAttributes): String {
        val linkComponents = qualifiedName.split(".")
        val qualifiedPath = linkComponents.subList(0, linkComponents.lastIndex)
        val elementName = linkComponents.last()
        return buildString {
            for (pathSegment in qualifiedPath) {
                val segmentAttributes = when {
                    pathSegment.isEmpty() || pathSegment.first().isLowerCase() -> DefaultLanguageHighlighterColors.IDENTIFIER
                    else -> CangJieHighlightingColors.CLASS
                }
                appendStyledLinkFragment(pathSegment, segmentAttributes)
                appendStyledLinkFragment(".", CangJieHighlightingColors.DOT)
            }
            appendStyledLinkFragment(elementName, lastSegmentAttributes)
        }
    }

    private fun CDocLink.getTargetElement(): PsiElement? {
        return getChildrenOfType<CDocName>().last().references.firstOrNull { it is CDocReference }?.resolve()
    }



    private fun PsiElement.extractExampleText() = when (this) {
        is CjDeclarationWithBody -> {
            when (val bodyExpression = bodyExpression) {
                is CjBlockExpression -> bodyExpression.text.removeSurrounding("{", "}")
                else -> bodyExpression!!.text
            }
        }

        else -> text
    }

    private fun trimCommonIndent(text: String): String {
        fun String.leadingIndent() = indexOfFirst { !it.isWhitespace() }

        val lines = text.split('\n')
        val minIndent = lines.filter { it.trim().isNotEmpty() }.minOfOrNull(String::leadingIndent) ?: 0
        return lines.joinToString("\n") { it.drop(minIndent) }
    }

    private fun StringBuilder.appendSection(title: String, content: StringBuilder.() -> Unit) {
        append(SECTION_HEADER_START, title, ":", SECTION_SEPARATOR)
        content()
        append(SECTION_END)
    }

    private fun StringBuilder.appendSamplesList(sampleTags: Sequence<CDocTag>) {
        if (!sampleTags.any()) return

        appendSection(CangJieBundle.message("cdoc.section.title.samples")) {
            sampleTags.forEach {
                it.getSubjectLink()?.let { subjectLink ->
                    append("<p>")
                    this@appendSamplesList.appendHyperlink(subjectLink)
                    this@appendSamplesList.appendStyledCodeBlock(
                        subjectLink.project,
                        CangJieLanguage ,
                        if (DumbService.isDumb(subjectLink.project))
                            "// " + CangJieBundle.message("cdoc.comment.unresolved")
                        else when (val target = subjectLink.getTargetElement()) {
                            null -> "// " + CangJieBundle.message("cdoc.comment.unresolved")
                            else -> trimCommonIndent(target.extractExampleText()).htmlEscape()
                        }
                    )
                }
            }
        }
    }

    private fun StringBuilder.appendSeeAlso(seeTags: Sequence<CDocTag>) {
        if (!seeTags.any()) return

        val iterator = seeTags.iterator()

        appendSection(CangJieBundle.message("cdoc.section.title.see.also")) {
            while (iterator.hasNext()) {
                val tag = iterator.next()
                val subjectName = tag.getSubjectName()
                val link = tag.getChildrenOfType<CDocLink>().lastOrNull()
                when {
                    link != null -> this.appendHyperlink(link)
                    subjectName != null -> DocumentationManagerUtil.createHyperlink(this, subjectName, subjectName, false, true)
                    else -> append(tag.getContent())
                }
                if (iterator.hasNext()) {
                    append(",<br>")
                }
            }
        }
    }

    private fun StringBuilder.appendAuthors(authorTags: Sequence<CDocTag>) {
        if (!authorTags.any()) return

        val iterator = authorTags.iterator()

        appendSection(CangJieBundle.message("cdoc.section.title.author")) {
            while (iterator.hasNext()) {
                append(iterator.next().getContent())
                if (iterator.hasNext()) {
                    append(", ")
                }
            }
        }
    }

    private fun StringBuilder.appendThrows(throwsTags: Sequence<CDocTag>, exceptionsTags: Sequence<CDocTag>) {
        if (!throwsTags.any() && !exceptionsTags.any()) return

        appendSection(CangJieBundle.message("cdoc.section.title.throws")) {

            fun CDocTag.append() {
                val subjectName = getSubjectName()
                if (subjectName != null) {
                    append("<p><code>")
                    val highlightedLinkLabel =
                        highlightQualifiedName(subjectName, getTargetLinkElementAttributes(CangJieHighlightingColors.CLASS))
                    DocumentationManagerUtil.createHyperlink(this@appendSection, subjectName, highlightedLinkLabel, false, true)
                    append("</code>")
                    val exceptionDescription = markdownToHtml(this)
                    if (exceptionDescription.isNotBlank()) {
                        append(" - $exceptionDescription")
                    }
                }
            }

            throwsTags.forEach { it.append() }
            exceptionsTags.forEach { it.append() }
        }
    }


    private fun StringBuilder.appendTagList(tags: Sequence<CDocTag>, title: String, titleAttributes: TextAttributesKey) {
        if (!tags.any()) {
            return
        }

        appendSection(title) {
            tags.forEach {
                val subjectName = it.getSubjectName() ?: return@forEach

                append("<p><code>")
                when (val link = it.getChildrenOfType<CDocLink>().firstOrNull()) {
                    null -> appendStyledLinkFragment(subjectName, titleAttributes)
                    else -> appendHyperlink(link)
                }

                append("</code>")
                val elementDescription = markdownToHtml(it)
                if (elementDescription.isNotBlank()) {
                    append(" - $elementDescription")
                }
            }
        }
    }

    private fun StringBuilder.appendTag(tag: CDocTag?, title: String) {
        if (tag != null) {
            appendSection(title) {
                append(markdownToHtml(tag))
            }
        }
    }

    private fun markdownToHtml(comment: CDocTag, allowSingleParagraph: Boolean = false): String {
        val markdownTree = MarkdownParser(GFMFlavourDescriptor()).buildMarkdownTreeFromString(comment.getContent())
        val markdownNode = MarkdownNode(markdownTree, null, comment)

        // Avoid wrapping the entire converted contents in a <p> tag if it's just a single paragraph
        val maybeSingleParagraph = markdownNode.children.singleOrNull { it.type != MarkdownTokenTypes.EOL }

        val firstParagraphOmitted = when {
            maybeSingleParagraph != null && !allowSingleParagraph -> {
                maybeSingleParagraph.children.joinToString("") { if (it.text == "\n") " " else it.toHtml() }
            }

            else -> markdownNode.toHtml()
        }

        val topMarginOmitted = when {
            firstParagraphOmitted.startsWith("<p>") -> firstParagraphOmitted.replaceFirst("<p>", "<p style='margin-top:0;padding-top:0;'>")
            else -> firstParagraphOmitted
        }

        return topMarginOmitted
    }

    class MarkdownNode(val node: ASTNode, val parent: MarkdownNode?, val comment: CDocTag) {
        val children: List<MarkdownNode> = node.children.map { MarkdownNode(it, this, comment) }
        val endOffset: Int get() = node.endOffset
        val startOffset: Int get() = node.startOffset
        val type: IElementType get() = node.type
        val text: String get() = comment.getContent().substring(startOffset, endOffset)
        fun child(type: IElementType): MarkdownNode? = children.firstOrNull { it.type == type }
    }

    private fun MarkdownNode.visit(action: (MarkdownNode, () -> Unit) -> Unit) {
        action(this) {
            for (child in children) {
                child.visit(action)
            }
        }
    }

    private fun MarkdownNode.toHtml(): String {
        if (node.type == MarkdownTokenTypes.WHITE_SPACE) {
            return text   // do not trim trailing whitespace
        }

        val sb = StringBuilder()
        visit { node, processChildren ->
            fun wrapChildren(tag: String, newline: Boolean = false) {
                sb.append("<$tag>")
                processChildren()
                sb.append("</$tag>")
                if (newline) sb.appendLine()
            }

            val nodeType = node.type
            val nodeText = node.text
            when (nodeType) {
                MarkdownElementTypes.UNORDERED_LIST -> wrapChildren("ul", newline = true)
                MarkdownElementTypes.ORDERED_LIST -> wrapChildren("ol", newline = true)
                MarkdownElementTypes.LIST_ITEM -> wrapChildren("li")
                MarkdownElementTypes.EMPH -> wrapChildren("em")
                MarkdownElementTypes.STRONG -> wrapChildren("strong")
                GFMElementTypes.STRIKETHROUGH -> wrapChildren("del")
                MarkdownElementTypes.ATX_1 -> wrapChildren("h1")
                MarkdownElementTypes.ATX_2 -> wrapChildren("h2")
                MarkdownElementTypes.ATX_3 -> wrapChildren("h3")
                MarkdownElementTypes.ATX_4 -> wrapChildren("h4")
                MarkdownElementTypes.ATX_5 -> wrapChildren("h5")
                MarkdownElementTypes.ATX_6 -> wrapChildren("h6")
                MarkdownElementTypes.BLOCK_QUOTE -> wrapChildren("blockquote")
                MarkdownElementTypes.PARAGRAPH -> {
                    sb.trimEnd()
                    wrapChildren("p", newline = true)
                }

                MarkdownElementTypes.CODE_SPAN -> {
                    val startDelimiter = node.child(MarkdownTokenTypes.BACKTICK)?.text
                    if (startDelimiter != null) {
                        val text = node.text.substring(startDelimiter.length).removeSuffix(startDelimiter)
                        sb.appendStyledInlineCode(comment.project, CangJieLanguage , text)
                    }
                }

                MarkdownElementTypes.CODE_BLOCK,
                MarkdownElementTypes.CODE_FENCE -> {
                    sb.trimEnd()
                    var language: String? = null
                    val contents = StringBuilder()
                    node.children.forEach { child ->
                        when (child.type) {
                            MarkdownTokenTypes.CODE_FENCE_CONTENT, MarkdownTokenTypes.CODE_LINE, MarkdownTokenTypes.EOL ->
                                contents.append(child.text)

                            MarkdownTokenTypes.FENCE_LANG ->
                                language = child.text.trim().split(' ')[0]
                        }
                    }
                    sb.appendStyledCodeBlock(
                        project = comment.project,
                        language = QuickDocHighlightingHelper.guessLanguage(language) ?: CangJieLanguage ,
                        code = contents
                    )
                }

                MarkdownTokenTypes.FENCE_LANG, MarkdownTokenTypes.CODE_LINE, MarkdownTokenTypes.CODE_FENCE_CONTENT -> {
                    // skip
                }

                MarkdownElementTypes.SHORT_REFERENCE_LINK,
                MarkdownElementTypes.FULL_REFERENCE_LINK -> {
                    val linkLabelNode = node.child(MarkdownElementTypes.LINK_LABEL)
                    val linkLabelContent = linkLabelNode?.children
                        ?.dropWhile { it.type == MarkdownTokenTypes.LBRACKET }
                        ?.dropLastWhile { it.type == MarkdownTokenTypes.RBRACKET }
                    if (linkLabelContent != null) {
                        val label = linkLabelContent.joinToString(separator = "") { it.text }
                        val linkText = node.child(MarkdownElementTypes.LINK_TEXT)?.toHtml() ?: label
                        if (DumbService.isDumb(comment.project)) {
                            sb.append(linkText)
                        } else {
                            comment.findDescendantOfType<CDocName> { it.text == label }
                                ?.references
                                ?.firstOrNull { it is CDocReference }
                                ?.resolve()
                                ?.let { resolvedLinkElement ->
                                    DocumentationManagerUtil.createHyperlink(
                                        sb,
                                        label,
                                        highlightQualifiedName(linkText, getTargetLinkElementAttributes(resolvedLinkElement)),
                                        false,
                                        true
                                    )
                                }
                                ?: sb.appendStyledFragment(label, CangJieHighlightingColors.RESOLVED_TO_ERROR)
                        }
                    } else {
                        sb.append(node.text)
                    }
                }

                MarkdownElementTypes.INLINE_LINK -> {
                    val label = node.child(MarkdownElementTypes.LINK_TEXT)?.toHtml()
                    val destination = node.child(MarkdownElementTypes.LINK_DESTINATION)?.text
                    if (label != null && destination != null) {
                        sb.append("<a href=\"$destination\">$label</a>")
                    } else {
                        sb.append(node.text)
                    }
                }

                MarkdownTokenTypes.TEXT,
                MarkdownTokenTypes.WHITE_SPACE,
                MarkdownTokenTypes.COLON,
                MarkdownTokenTypes.SINGLE_QUOTE,
                MarkdownTokenTypes.DOUBLE_QUOTE,
                MarkdownTokenTypes.LPAREN,
                MarkdownTokenTypes.RPAREN,
                MarkdownTokenTypes.LBRACKET,
                MarkdownTokenTypes.RBRACKET,
                MarkdownTokenTypes.EXCLAMATION_MARK,
                GFMTokenTypes.CHECK_BOX,
                GFMTokenTypes.GFM_AUTOLINK -> {
                    sb.append(nodeText)
                }

                MarkdownTokenTypes.EOL -> {
                    sb.append(" ")
                }

                MarkdownTokenTypes.GT -> sb.append("&gt;")
                MarkdownTokenTypes.LT -> sb.append("&lt;")

                MarkdownElementTypes.LINK_TEXT -> {
                    val childrenWithoutBrackets = node.children.drop(1).dropLast(1)
                    for (child in childrenWithoutBrackets) {
                        sb.append(child.toHtml())
                    }
                }

                MarkdownTokenTypes.EMPH -> {
                    val parentNodeType = node.parent?.type
                    if (parentNodeType != MarkdownElementTypes.EMPH && parentNodeType != MarkdownElementTypes.STRONG) {
                        sb.append(node.text)
                    }
                }

                GFMTokenTypes.TILDE -> {
                    if (node.parent?.type != GFMElementTypes.STRIKETHROUGH) {
                        sb.append(node.text)
                    }
                }

                GFMElementTypes.TABLE -> {
                    val alignment: List<String> = getTableAlignment(node)
                    var addedBody = false
                    sb.append("<table>")

                    for (child in node.children) {
                        if (child.type == GFMElementTypes.HEADER) {
                            sb.append("<thead>")
                            processTableRow(sb, child, "th", alignment)
                            sb.append("</thead>")
                        } else if (child.type == GFMElementTypes.ROW) {
                            if (!addedBody) {
                                sb.append("<tbody>")
                                addedBody = true
                            }

                            processTableRow(sb, child, "td", alignment)
                        }
                    }

                    if (addedBody) {
                        sb.append("</tbody>")
                    }
                    sb.append("</table>")
                }

                else -> {
                    processChildren()
                }
            }
        }
        return sb.toString().trimEnd()
    }

    private fun StringBuilder.trimEnd() {
        while (isNotEmpty() && this[length - 1] == ' ') {
            deleteCharAt(length - 1)
        }
    }

    private fun String.htmlEscape(): String = replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    private fun processTableRow(sb: StringBuilder, node: MarkdownNode, cellTag: String, alignment: List<String>) {
        sb.append("<tr>")
        for ((i, child) in node.children.filter { it.type == GFMTokenTypes.CELL }.withIndex()) {
            val alignValue = alignment.getOrElse(i) { "" }
            val alignTag = if (alignValue.isEmpty()) "" else " align=\"$alignValue\""
            sb.append("<$cellTag$alignTag>")
            sb.append(child.toHtml())
            sb.append("</$cellTag>")
        }
        sb.append("</tr>")
    }

    private fun getTableAlignment(node: MarkdownNode): List<String> {
        val separatorRow = node.child(GFMTokenTypes.TABLE_SEPARATOR)
            ?: return emptyList()

        return separatorRow.text.split('|').filterNot { it.isBlank() }.map {
            val trimmed = it.trim()
            val left = trimmed.startsWith(':')
            val right = trimmed.endsWith(':')
            if (left && right) "center"
            else if (right) "right"
            else if (left) "left"
            else ""
        }
    }

    /**
     * If highlighted links has the same color as highlighted inline code blocks they will be indistinguishable.
     * In this case we should change link color to standard hyperlink color which we believe is apriori different.
     */
    private fun tuneAttributesForLink(attributes: TextAttributes): TextAttributes {
        val globalScheme = EditorColorsManager.getInstance().globalScheme
        if (attributes.foregroundColor == globalScheme.getAttributes(HighlighterColors.TEXT).foregroundColor
            || attributes.foregroundColor == globalScheme.getAttributes(DefaultLanguageHighlighterColors.IDENTIFIER).foregroundColor
        ) {
            val tuned = attributes.clone()
            if (ApplicationManager.getApplication().isUnitTestMode) {
                tuned.foregroundColor = globalScheme.getAttributes(CodeInsightColors.HYPERLINK_ATTRIBUTES).foregroundColor
            } else {
                tuned.foregroundColor = globalScheme.getColor(DefaultLanguageHighlighterColors.DOC_COMMENT_LINK)
            }
            return tuned
        }
        return attributes
    }

    fun StringBuilder.appendHighlighted(
        value: String,
        project: Project,
        attributesBuilder: CangJieIdeDescriptorRendererHighlightingManager<CangJieIdeDescriptorRendererHighlightingManager.Companion.Attributes>.()
        -> CangJieIdeDescriptorRendererHighlightingManager.Companion.Attributes
    ) {
        with(createHighlightingManager(project)) {
            this@appendHighlighted.appendHighlighted(value, attributesBuilder())
        }
    }

    fun highlight(
        value: String,
        project: Project,
        attributesBuilder: CangJieIdeDescriptorRendererHighlightingManager<CangJieIdeDescriptorRendererHighlightingManager.Companion.Attributes>.()
        -> CangJieIdeDescriptorRendererHighlightingManager.Companion.Attributes
    ): String {
        return buildString { appendHighlighted(value, project, attributesBuilder) }
    }

    fun StringBuilder.appendCodeSnippetHighlightedByLexer(project: Project, codeSnippet: String) {
        with(createHighlightingManager(project)) {
            appendCodeSnippetHighlightedByLexer(codeSnippet)
        }
    }

    private data class TextAttributesAdapter(val attributes: TextAttributes) :
        CangJieIdeDescriptorRendererHighlightingManager.Companion.Attributes

    fun createHighlightingManager(project: Project): CangJieIdeDescriptorRendererHighlightingManager<CangJieIdeDescriptorRendererHighlightingManager.Companion.Attributes> {
        if (!DocumentationSettings.isHighlightingOfQuickDocSignaturesEnabled()) {
            return CangJieIdeDescriptorRendererHighlightingManager.NO_HIGHLIGHTING
        }
        return object : CangJieIdeDescriptorRendererHighlightingManager<TextAttributesAdapter> {
            override fun StringBuilder.appendHighlighted(
                value: String,
                attributes: TextAttributesAdapter
            ) {
                appendStyledFragment(value, attributes.attributes)
            }

            override fun StringBuilder.appendCodeSnippetHighlightedByLexer(codeSnippet: String) {
                appendStyledCodeFragment(project, CangJieLanguage, codeSnippet)
            }

            private fun resolveKey(key: TextAttributesKey): TextAttributesAdapter {
                return TextAttributesAdapter(
                    EditorColorsManager.getInstance().globalScheme.getAttributes(key)
                )
            }

            override val asError get() = resolveKey(CangJieHighlightingColors.RESOLVED_TO_ERROR)
            override val asInfo get() = resolveKey(CangJieHighlightingColors.BLOCK_COMMENT)
            override val asDot get() = resolveKey(CangJieHighlightingColors.DOT)
            override val asComma get() = resolveKey(CangJieHighlightingColors.COMMA)
            override val asColon get() = resolveKey(CangJieHighlightingColors.COLON)
            override val asLtColon: TextAttributesAdapter  = resolveKey(CangJieHighlightingColors.LT_COLON)
            override val asStatic: TextAttributesAdapter = resolveKey(CangJieHighlightingColors.STATIC)
            override val asDoubleColon get() = resolveKey(CangJieHighlightingColors.DOUBLE_COLON)
            override val asParentheses get() = resolveKey(CangJieHighlightingColors.PARENTHESIS)
            override val asArrow get() = resolveKey(CangJieHighlightingColors.ARROW)
            override val asBrackets get() = resolveKey(CangJieHighlightingColors.BRACKETS)
            override val asBraces get() = resolveKey(CangJieHighlightingColors.BRACES)
            override val asOperationSign get() = resolveKey(CangJieHighlightingColors.OPERATOR_SIGN)
            override val asNonNullAssertion get() = resolveKey(CangJieHighlightingColors.EXCLEXCL)
            override val asNullityMarker get() = resolveKey(CangJieHighlightingColors.QUEST)
            override val asKeyword get() = resolveKey(CangJieHighlightingColors.KEYWORD)
            override val asLet get() = resolveKey(CangJieHighlightingColors.LET_KEYWORD)
            override val asMut: TextAttributesAdapter = resolveKey(CangJieHighlightingColors.MUT_KEYWORD)
            override val asProp: TextAttributesAdapter = resolveKey(CangJieHighlightingColors.PROP_KEYWORD)
            override val asVar get() = resolveKey(CangJieHighlightingColors.VAR_KEYWORD)
            override val asAnnotationName get() = resolveKey(CangJieHighlightingColors.ANNOTATION)
            override val asAnnotationAttributeName get() = resolveKey(CangJieHighlightingColors.ANNOTATION_ATTRIBUTE_NAME_ATTRIBUTES)
            override val asClassName get() = resolveKey(CangJieHighlightingColors.CLASS)
            override val asPackageName get() = resolveKey(DefaultLanguageHighlighterColors.IDENTIFIER)
            override val asObjectName get() = resolveKey(CangJieHighlightingColors.OBJECT)
            override val asInstanceProperty get() = resolveKey(CangJieHighlightingColors.INSTANCE_PROPERTY)
            override val asTypeAlias get() = resolveKey(CangJieHighlightingColors.TYPE_ALIAS)
            override val asParameter get() = resolveKey(CangJieHighlightingColors.PARAMETER)
            override val asTypeParameterName get() = resolveKey(CangJieHighlightingColors.TYPE_PARAMETER)
            override val asLocalVarOrLet get() = resolveKey(CangJieHighlightingColors.LOCAL_VARIABLE)
            override val asFunDeclaration get() = resolveKey(CangJieHighlightingColors.FUNCTION_DECLARATION)
            override val asFunCall get() = resolveKey(CangJieHighlightingColors.FUNCTION_CALL)
        }
            .eraseTypeParameter()
    }
}
