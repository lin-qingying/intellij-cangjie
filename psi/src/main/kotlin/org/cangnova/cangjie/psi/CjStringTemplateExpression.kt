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

package org.cangnova.cangjie.psi

import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.CjExpressionImpl.Companion.replaceExpression
import org.cangnova.cangjie.psi.stubs.CangJiePlaceHolderStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.*
import com.intellij.psi.tree.TokenSet
import com.intellij.util.IncorrectOperationException
import java.util.regex.Pattern

class CjStringTemplateExpression :
    CjElementImplStub<CangJiePlaceHolderStub<CjStringTemplateExpression>>,
    CjExpression,
    PsiLanguageInjectionHost,
    ContributedReferenceHost {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJiePlaceHolderStub<CjStringTemplateExpression>) : super(
        stub,
        CjStubElementTypes.STRING_TEMPLATE,
    )

    @Throws(IncorrectOperationException::class)
    override fun replace(newElement: PsiElement): PsiElement {
        return replaceExpression(this, newElement, true) { newElement: PsiElement? ->
            super.replace(
                newElement!!,
            )
        }
    }

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitStringTemplateExpression(this, data)
    }

    val isDoubleQuote: Boolean
        /**
         * 是否双引号
         */
        get() = node.firstChildNode.text.startsWith("\"")

    val isMultiLine: Boolean
        /**
         * 是否是多行字符串
         */
        get() = node.firstChildNode.text == "\"\"\"" || node.firstChildNode
            .text == "'''"

    val isRawString: Boolean
        /**
         * 是否为原始字符串
         *
         * @return
         */
        get() = Pattern.matches("^#+[\"']", node.firstChildNode.text)

    val stringContent: String
        /**
         * 获取字符串内容
         *
         * @return
         */
        get() {
            val node = node
            val firstChild = node.firstChildNode
            val lastChild = node.lastChildNode

            if (firstChild == null || firstChild === lastChild) {
                return ""
            }

            val content = StringBuilder()
            var current = firstChild.treeNext
            while (current != null && current !== lastChild) {
                content.append(current.text)
                current = current.treeNext
            }

            return content.toString()
        }

    val entries: Array<CjStringTemplateEntry>
        get() = getStubOrPsiChildren(
            STRING_ENTRIES_TYPES,
            CjStringTemplateEntry.EMPTY_ARRAY,
        )

    override fun isValidHost(): Boolean {
        return node.getChildren(CLOSE_QUOTE_TOKEN_SET).isNotEmpty()
    }

    override fun updateText(text: String): PsiLanguageInjectionHost {
        val newExpression = CjPsiFactory(project).createExpressionIfPossible(text)
        if (newExpression is CjStringTemplateExpression) return replace(newExpression) as CjStringTemplateExpression
        return ElementManipulators.handleContentChange(this, text)
    }

    override fun createLiteralTextEscaper(): LiteralTextEscaper<out PsiLanguageInjectionHost> {
        return CangJieStringLiteralTextEscaper(this)
    }

    fun hasInterpolation(): Boolean {
        for (child in children) {
            if (child is CjSimpleNameStringTemplateEntry || child is CjBlockStringTemplateEntry) {
                return true
            }
        }

        return false
    }

    companion object {
        private val CLOSE_QUOTE_TOKEN_SET = TokenSet.create(CjTokens.CLOSING_QUOTE)
        private val STRING_ENTRIES_TYPES = TokenSet.create(
            CjStubElementTypes.LONG_STRING_TEMPLATE_ENTRY,
            CjStubElementTypes.SHORT_STRING_TEMPLATE_ENTRY,
            CjStubElementTypes.LITERAL_STRING_TEMPLATE_ENTRY,
            CjStubElementTypes.ESCAPE_STRING_TEMPLATE_ENTRY,
        )
    }
}
