/*
 * Copyright 2026 LinQingYing. and contributors.
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

@file:OptIn(
    org.cangnova.cangjie.analysis.api.CaNonPublicApi::class,
    org.cangnova.cangjie.psi.CjNonPublicApi::class,
)

package org.cangnova.cangjie.ide.documentation

import com.intellij.codeInsight.documentation.DocumentationManagerUtil
import com.intellij.codeInsight.navigation.targetPresentation
import com.intellij.lang.documentation.DocumentationSettings
import com.intellij.model.Pointer
import com.intellij.openapi.util.text.HtmlChunk
import com.intellij.openapi.util.text.StringUtil
import com.intellij.platform.backend.documentation.DocumentationResult
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.presentation.TargetPresentation
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiElement
import com.intellij.psi.createSmartPointer
import org.cangnova.cangjie.analysis.api.CaNonPublicApi
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.analyze
import org.cangnova.cangjie.analysis.api.components.containingDeclaration
import org.cangnova.cangjie.analysis.api.components.findCDoc
import org.cangnova.cangjie.analysis.api.components.render
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaNamedFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaValueParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.symbol
import org.cangnova.cangjie.highlighter.CDocRenderer
import org.cangnova.cangjie.highlighter.CDocRenderer.createHighlightingManager
import org.cangnova.cangjie.highlighter.CDocRenderer.renderCDoc
import org.cangnova.cangjie.highlighter.CangJieIdeDeclarationRenderer
import org.cangnova.cangjie.highlighter.findCDocByPsi
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.lexer.cdoc.CDocContent
import org.cangnova.cangjie.lexer.cdoc.CDocTemplate
import org.cangnova.cangjie.lexer.cdoc.insert
import org.cangnova.cangjie.lexer.cdoc.psi.api.CDocCommentDescriptor
import org.cangnova.cangjie.lexer.cdoc.psi.impl.CDocSection
import org.cangnova.cangjie.psi.CjCallExpression
import org.cangnova.cangjie.psi.CjConstructor
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjFunction
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjNonPublicApi
import org.cangnova.cangjie.psi.CjSecondaryConstructor
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.cangnova.cangjie.psi.CjValueArgumentList
import org.cangnova.cangjie.references.mainReference
import org.jetbrains.annotations.Nls

/**
 * 对齐 CangJie K2 `DocumentationTarget` 的仓颉实现。
 *
 * 这里保持 IntelliJ 文档后端 target 的入口结构与 CangJie 一致，
 * 但只保留仓颉中真实存在的概念；已确认不存在的概念分支不引入、不兜底。
 *
 * 文档定义头和正文统一复用前端公开能力：
 * - `CaSymbol.render()`
 * - `CaDeclarationSymbol.findCDoc()`
 */
internal class CangJieDocumentationTarget(
    val element: PsiElement,
    private val originalElement: PsiElement?,
) : DocumentationTarget {
    override fun createPointer(): Pointer<out DocumentationTarget> {
        val elementPtr = element.createSmartPointer()
        val originalElementPtr = originalElement?.createSmartPointer()
        return Pointer {
            val element = elementPtr.dereference() ?: return@Pointer null
            CangJieDocumentationTarget(element, originalElementPtr?.dereference())
        }
    }

    override fun computePresentation(): TargetPresentation {
        return targetPresentation(element)
    }

    override fun computeDocumentationHint(): String? {
        return computeLocalDocumentation(element, originalElement, true)
    }

    override val navigatable: Navigatable?
        get() = element as? Navigatable

    override fun computeDocumentation(): DocumentationResult? {
        val html = computeLocalDocumentation(element, originalElement, false) ?: return null
        return DocumentationResult.documentation(html)
    }
}

private fun computeLocalDocumentation(element: PsiElement, originalElement: PsiElement?, quickNavigation: Boolean): String? {
    when {
        element is CjDeclaration -> {
            return buildString {
                renderCangJieDeclaration(element, quickNavigation)
            }
        }

        element is CjValueArgumentList -> {
            val referenceExpression = element.prevSibling as? CjSimpleNameExpression ?: return null
            val calledElement = referenceExpression.mainReference.resolve()
            if (calledElement is CjNamedFunction || calledElement is CjConstructor<*>) {
                return computeLocalDocumentation(calledElement as CjExpression, element, quickNavigation)
            }
        }

        element is CjCallExpression -> {
            val calledElement = element.referenceExpression?.mainReference?.resolve() ?: return null
            return computeLocalDocumentation(calledElement, originalElement, quickNavigation)
        }

        element.isModifier() -> {
            return null
        }
    }
    return null
}

internal fun PsiElement?.isModifier(): Boolean =
    this != null &&
        parent is org.cangnova.cangjie.psi.CjModifierList &&
        CjTokens.MODIFIER_KEYWORDS_ARRAY.firstOrNull { it.value == text } != null

private fun @receiver:Nls StringBuilder.renderCangJieDeclaration(
    declaration: CjDeclaration,
    onlyDefinition: Boolean,
    symbolFinder: CaSession.(CaSymbol) -> CaSymbol? = { it },
    preBuild: CDocTemplate.() -> Unit = {},
) {
    analyze(declaration) {
        val symbol = symbolFinder(declaration.symbol)
        if (symbol !is CaDeclarationSymbol) return

        renderCangJieSymbol(symbol, declaration, onlyDefinition, true, preBuild)
    }
}
context(_: CaSession)
private fun findCDoc(symbol: CaSymbol): CDocContent? {
    val declarationSymbol = symbol as? CaDeclarationSymbol ?: return null
    val descriptor = declarationSymbol.findCDoc() ?: return null
    return CDocContent(descriptor.primaryTag, descriptor.additionalSections)
}
context(_: CaSession)
private fun renderCDoc(
    symbol: CaSymbol,
    stringBuilder: StringBuilder,
) {
    val declaration = symbol.psi?.navigationElement as? CjElement
    val cDoc = findCDoc(symbol)
    if (cDoc != null) {
        stringBuilder.renderCDoc(cDoc.contentTag, cDoc.sections)
        return
    }
    if (declaration is CjSecondaryConstructor) {
        declaration.getContainingTypeStatement().findCDocByPsi()?.let {
            stringBuilder.renderCDoc(it.contentTag, it.sections)
        }
    }
}

context(_: CaSession)
private fun getContainerInfo(cjDeclaration: CjDeclaration): HtmlChunk {
    val containingSymbol = cjDeclaration.symbol.containingDeclaration
    val fqName = (containingSymbol as? CaClassLikeSymbol)?.classId?.asFqNameString()
        ?: (cjDeclaration.containingFile as? CjFile)?.packageFqName?.takeIf { !it.isRoot }?.asString()

    val fqNameSection = fqName?.let {
        @Nls val link = StringBuilder()
        val highlighted = if (DocumentationSettings.isSemanticHighlightingOfLinksEnabled()) {
            CDocRenderer.highlight(it, cjDeclaration.project) { asClassName }
        } else {
            it
        }

        DocumentationManagerUtil.createHyperlink(link, it, highlighted, false)
        HtmlChunk.fragment(
            HtmlChunk.tag("icon").attr(
                "src",
                "CangJieBaseResourcesIcons.ClassCangJie"
            ),
            HtmlChunk.nbsp(),
            HtmlChunk.raw(link.toString()),
            HtmlChunk.br()
        )
    } ?: HtmlChunk.empty()

    val fileNameSection = cjDeclaration.navigationElement.containingFile
        ?.name

        ?.let {
            HtmlChunk.fragment(
                HtmlChunk.tag("icon").attr("src", "CangJieBaseResourcesIcons.CangJie_file"),
                HtmlChunk.nbsp(),
                HtmlChunk.text(it),
                HtmlChunk.br()
            )
        }
        ?: HtmlChunk.empty()

    return HtmlChunk.fragment(fqNameSection, fileNameSection)
}

context(_: CaSession)
private fun @receiver:Nls StringBuilder.renderCangJieSymbol(
    symbol: CaDeclarationSymbol,
    declaration: CjDeclaration,
    onlyDefinition: Boolean,
    passContainerInfo: Boolean = true,
    preBuild: CDocTemplate.() -> Unit = {},
) {

    insert(CDocTemplate()) {
        definition {
            append(symbol.render(CangJieIdeDeclarationRenderer(createHighlightingManager(declaration.project), symbol).renderer))

        }

        if (!onlyDefinition) {
            description {
                renderCDoc(symbol, this)
            }
        }

        if (passContainerInfo) {
            getContainerInfo(declaration).toString().takeIf { it.isNotBlank() }?.let { info ->
                containerInfo {
                    append(info)
                }
            }
        }

        preBuild()
    }
}

internal data class CangJieRenderedDocumentation(
    val signature: String?,
    val body: String?,
)

internal fun renderDocumentation(declaration: CjDeclaration): CangJieRenderedDocumentation? {
    val rendered = analyze(declaration) {
        val symbol = declaration.symbol
        CangJieRenderedDocumentation(
            signature = symbol.render().takeIf { it.isNotBlank() },
            body = (symbol as? CaDeclarationSymbol)
                ?.findCDoc()
                ?.renderToDocumentationString()
                ?.takeIf { it.isNotBlank() },
        )
    }

    return rendered.takeIf { it.signature != null || it.body != null }
}

private fun String.asDocumentationHtml(): String {
    return StringUtil.escapeXmlEntities(this)
        .replace("\r\n", "\n")
        .replace('\r', '\n')
        .replace("\n", "<br/>")
}

private fun CDocCommentDescriptor.renderToDocumentationString(): String? {
    val rendered = buildString {
        val primaryContent = primaryTag.getContent().trim()
        if (primaryContent.isNotEmpty()) {
            append(primaryContent)
        }

        additionalSections
            .filterNot { section -> section == primaryTag }
            .forEach { section ->
                val renderedSection = section.renderSectionLine()
                if (renderedSection.isNotEmpty()) {
                    if (isNotEmpty()) appendLine()
                    append(renderedSection)
                }
            }
    }

    return rendered.ifBlank { null }
}

private fun CDocSection.renderSectionLine(): String {
    val tagName = name ?: return ""
    val content = getContent().trim()
    val subjectName = getSubjectName()

    return buildString {
        append("@")
        append(tagName)
        if (!subjectName.isNullOrBlank()) {
            append(" ")
            append(subjectName)
        }
        if (content.isNotEmpty()) {
            append(" ")
            append(content)
        }
    }
}
