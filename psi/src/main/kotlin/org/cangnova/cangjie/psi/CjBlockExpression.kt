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

import com.intellij.lang.Language
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.tree.CompositeElement
import com.intellij.psi.impl.source.tree.LazyParseablePsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiUtilCore
import com.intellij.util.IncorrectOperationException
import org.cangnova.cangjie.lang.CangJieLanguage
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.psiUtil.deleteSemicolon
import org.cangnova.cangjie.psi.psiUtil.getContainingCjFile
import org.cangnova.cangjie.psi.psiUtil.parentSubstitute
import java.util.*

open class CjBlockExpression : LazyParseablePsiElement, CjElement, CjExpression, CjStatementExpression {
    constructor(type: IElementType, text: CharSequence?) : super(type, text)
    constructor(text: CharSequence?) : super(CjNodeTypes.BLOCK, text)

    override fun getLanguage(): Language {
        return CangJieLanguage
    }

    override fun toString(): String {
        return node.elementType.toString()
    }
    override fun getContainingCjFile(): CjFile {
        return getContainingCjFile(this)

    }
    override fun getContainingFile(): PsiFile {
        return super.getContainingFile()
    }

    override fun <T : PsiElement> getPsi(clazz: Class<T>): T {
        return super.getPsi(clazz)
    }

    override fun <D> acceptChildren(visitor: CjVisitor<Unit, D>, data: D) {
        CjPsiUtil.visitChildren(this, visitor, data)
    }

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitBlockExpression(this, data)
    }

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is CjVisitor<*, *>) {
            @Suppress("UNCHECKED_CAST")
            accept(visitor as CjVisitor<Any?, Any?>, null as Any?)
        } else {
            visitor.visitElement(this)
        }
    }

    @Throws(IncorrectOperationException::class)
    override fun delete() {
        this.deleteSemicolon()
        super.delete()
    }

    override fun getChildren(): Array<PsiElement> {
        var psiChild = firstChild

        var result: MutableList<PsiElement>? = null
        while (psiChild != null) {
            if (psiChild.node is CompositeElement) {
                if (result == null) result = ArrayList()
                result.add(psiChild)
            }
            psiChild = psiChild.nextSibling
        }
        return if (result == null) PsiElement.EMPTY_ARRAY else PsiUtilCore.toPsiElementArray(result)
    }

    override fun getPsiOrParent(): CjElement {
        return this
    }

    override fun getParent(): PsiElement {
        val substitute = this.parentSubstitute
        return substitute ?: super.getParent()
    }



    val firstStatement: CjExpression?
        get() = findChildByClass(CjExpression::class.java)

    val lastStatement: CjExpression?
        get() {
            val statement = statements

            if (!statement.isEmpty()) {
                return statement[statement.size - 1]
            }

            return null
        }

    open val statements: List<CjExpression>
        get() = Arrays.asList(*findChildrenByClass(CjExpression::class.java))

    val statementsWithoutReturnKeyword: Set<CjExpression>
        /**
         * 没有return关键字的语句
         *
         * @return
         */
        get() {
            val returns =
                findChildrenByClass(CjReturnExpression::class.java)

            val result: MutableSet<CjExpression> = HashSet()

            for (statement in returns) {
                statement.returnedExpression?.let { result.add(it) }
            }

            val lastStatement = lastStatement

            if (lastStatement != null) {
                if (lastStatement !is CjReturnExpression && lastStatement !is CjDeclaration) {
                    result.add(lastStatement)
                }
            }
            return result
        }

    val returnStatements: Set<CjExpression>
        get() {
            val returns: MutableSet<CjExpression> = HashSet(
                java.util.List.of(
                    *findChildrenByClass(
                        CjReturnExpression::class.java,
                    ),
                ),
            )

            val lastStatement = lastStatement
            if (lastStatement != null) {
                returns.add(lastStatement)
            }
            return returns
        }

    val lastBracketRange: TextRange?
        get() {
            val rBrace = rBrace
            return rBrace?.textRange
        }

    val rBrace: PsiElement?
        get() = findPsiChildByType(CjTokens.RBRACE)

    val lBrace: PsiElement?
        get() = findPsiChildByType(CjTokens.LBRACE)
}
