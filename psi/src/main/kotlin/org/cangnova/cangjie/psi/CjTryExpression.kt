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

class CjTryExpression(node: ASTNode) : CjExpressionImpl(node) {
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitTryExpression(this, data)
    }

    val tryResourceList: CjTryResourceList? get() = findChildByClass(CjTryResourceList::class.java)
    val catchBody: CjExpression?
        get() = findChildByClass(CjExpression::class.java)
    val tryBlock: CjBlockExpression
        get() = findChildByType<PsiElement>(CjNodeTypes.BLOCK) as CjBlockExpression
    val catchClauses: List<CjCatchClause>
        get() = findChildrenByType(CjNodeTypes.CATCH)
    val finallyBlock: CjFinallySection?
        get() = findChildByType<PsiElement>(CjNodeTypes.FINALLY) as CjFinallySection?
    val tryKeyword: PsiElement?
        get() = findChildByType(CjTokens.TRY_KEYWORD)
}
