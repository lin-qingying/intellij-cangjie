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

package org.cangnova.cangjie.ide.documentation

import com.intellij.openapi.util.TextRange
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.documentation.DocumentationTargetProvider
import com.intellij.platform.backend.documentation.InlineDocumentation
import com.intellij.platform.backend.documentation.InlineDocumentationProvider
import com.intellij.platform.backend.documentation.PsiDocumentationTargetProvider
import com.intellij.psi.PsiDocCommentBase
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.lang.CangJieLanguage
import org.cangnova.cangjie.lexer.cdoc.psi.CDoc
import org.cangnova.cangjie.psi.CjDeclaration

/**
 * 仓颉 PSI 到文档 target 的主入口。
 *
 * 该入口与 Kotlin K2 一样走 `platform.backend.documentation.*`，
 * 让 IDE 文档系统统一基于 target 工作，而不是继续挂接旧式
 * `lang.documentationProvider` 单点接口。
 */
class CangJiePsiDocumentationTargetProvider : PsiDocumentationTargetProvider {
    override fun documentationTarget(element: PsiElement, originalElement: PsiElement?): DocumentationTarget? {
        return if (element.language.`is`(CangJieLanguage)) {
            CangJieDocumentationTarget(element, originalElement)
        } else {
            null
        }
    }
}

/**
 * `DocumentationTargetProvider` 在这里仅负责 file/offset 侧的补充入口。
 *
 * 常规标识符与引用走 `PsiDocumentationTargetProvider` 即可；
 * file/offset 入口主要补齐修饰符这类“有 offset，但不一定有直接目标元素”的场景。
 */
class CangJieDocumentationTargetProvider : DocumentationTargetProvider {
    override fun documentationTargets(file: PsiFile, offset: Int): List<DocumentationTarget> {
        val element = file.findElementAt(offset) ?: return emptyList()
        return if (element.isModifier()) {
            arrayListOf(CangJieDocumentationTarget(element, element))
        } else {
            emptyList()
        }
    }
}

private class CangJieInlineDocumentation(
    private val comment: CDoc,
    private val declaration: CjDeclaration,
) : InlineDocumentation {
    override fun getDocumentationRange(): TextRange = comment.textRange

    override fun getDocumentationOwnerRange(): TextRange? = declaration.textRange

    override fun renderText(): String? = renderDocumentation(declaration)?.body

    override fun getOwnerTarget(): DocumentationTarget = CangJieDocumentationTarget(declaration, declaration)
}

/**
 * inline documentation 与 Quick Documentation 共用同一条声明恢复与 Analysis API 文档主线，
 * 避免“悬浮文档”和“行内文档”各自维护一套渲染规则。
 */
class CangJieInlineDocumentationProvider : InlineDocumentationProvider {
    override fun inlineDocumentationItems(file: PsiFile?): Collection<InlineDocumentation> {
        if (file?.language?.`is`(CangJieLanguage) != true) {
            return emptyList()
        }

        val items = mutableListOf<InlineDocumentation>()
        PsiTreeUtil.processElements(file) { element ->
            val declaration = element as? CjDeclaration ?: return@processElements true
            val comment = declaration.docComment ?: return@processElements true
            items += CangJieInlineDocumentation(comment, declaration)
            true
        }
        return items
    }

    override fun findInlineDocumentation(file: PsiFile, textRange: TextRange): InlineDocumentation? {
        if (!file.language.`is`(CangJieLanguage)) {
            return null
        }

        val comment = PsiTreeUtil.getParentOfType(
            file.findElementAt(textRange.startOffset),
            PsiDocCommentBase::class.java,
            false,
        ) as? CDoc ?: return null

        if (comment.textRange != textRange) {
            return null
        }

        val declaration = comment.owner ?: return null
        return CangJieInlineDocumentation(comment, declaration)
    }
}
