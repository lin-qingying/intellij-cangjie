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

import org.cangnova.cangjie.psi.CjNodeTypes.FUNCTION_LITERAL
import org.cangnova.cangjie.psi.CjNodeTypes.LAMBDA_EXPRESSION
import org.cangnova.cangjie.lexer.CjTokens.LBRACE
import org.cangnova.cangjie.lexer.CjTokens.RBRACE
import org.cangnova.cangjie.psi.psiUtil.getContainingCjFile
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.impl.source.tree.LazyParseablePsiElement

class CjLambdaExpression(text: CharSequence?) :
    LazyParseablePsiElement(LAMBDA_EXPRESSION, text),
    CjExpression {

    val functionLiteral: CjFunctionLiteral
        get() = findChildByType(FUNCTION_LITERAL)?.getPsi(CjFunctionLiteral::class.java)!!

    val valueParameters: List<CjParameter>
        get() = functionLiteral.valueParameters

    val parameterList: CjParameterList?
        get() = functionLiteral.valueParameterList
    val bodyExpression: CjBlockExpression?
        get() = functionLiteral.bodyExpression

    fun hasDeclaredReturnType(): Boolean {
        return functionLiteral.typeReference != null
    }

    fun asElement(): CjElement {
        return this
    }

    val leftCurlyBrace: ASTNode
        get() = functionLiteral.node.findChildByType(LBRACE)!!

    val rightCurlyBrace: ASTNode?
        get() = functionLiteral.node.findChildByType(RBRACE)

    override fun <D> acceptChildren(visitor: CjVisitor<Unit, D>, data: D) {
        CjPsiUtil.visitChildren<D>(this, visitor, data)
    }

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitLambdaExpression(this, data)
    }

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is CjVisitor<*, *>) {
            @Suppress("UNCHECKED_CAST")
            accept(visitor as CjVisitor<Any?, Any?>, null as Any?)
        } else {
            visitor.visitElement(this)
        }
    }

    override fun toString(): String {
        return node.elementType.toString()
    }

    override fun getPsiOrParent(): CjElement {
        return this
    }

    override fun getContainingCjFile(): CjFile {
        return getContainingCjFile(this)
    }

    @Suppress("unused") // keep for compatibility with potential plugins
    fun shouldChangeModificationCount(place: PsiElement?): Boolean {
        return false
    }
}
