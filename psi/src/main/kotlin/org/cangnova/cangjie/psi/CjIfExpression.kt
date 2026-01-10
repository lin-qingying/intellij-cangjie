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
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement

class CjIfExpression(node: ASTNode) : CjExpressionImpl(node), CjPatternEntryBlock {
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitIfExpression(this, data)
    }

    @get: IfNotParsed
    val condition: CjExpression?
        get() = findExpressionUnder(CjNodeTypes.CONDITION)

    val letExpression get() = findChildByType<CjLetExpression>(CjNodeTypes.LET_EXPRESSION)

    @get:IfNotParsed
    val leftParenthesis: PsiElement?
        get() = findChildByType(CjTokens.LPAR)

    @get:IfNotParsed
    val rightParenthesis: PsiElement?
        get() = findChildByType(CjTokens.RPAR)
    val then: CjExpression?
        get() = findExpressionUnder(CjNodeTypes.THEN)
    val `else`: CjExpression?
        get() = findExpressionUnder(CjNodeTypes.ELSE)
    val elseKeyword: PsiElement?
        get() = findChildByType(CjTokens.ELSE_KEYWORD)
    val ifKeyword: PsiElement
        get() = findChildByType(CjTokens.IF_KEYWORD)!!
}
