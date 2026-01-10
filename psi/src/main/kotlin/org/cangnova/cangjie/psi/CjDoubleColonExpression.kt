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

interface CjDoubleColonExpression : CjExpression {
    val receiverExpression: CjExpression?
        get() = node.firstChildNode.psi as? CjExpression

    val hasQuestionMarks: Boolean
        get() {
            for (element in generateSequence(node.firstChildNode, ASTNode::getTreeNext)) {
                when (element.elementType) {
                    CjTokens.QUEST -> return true
//                    CjTokens.COLONCOLON -> return false
                }
            }
            error("Double colon expression must have '::': $text")
        }

    fun findColonColon(): PsiElement?

    val doubleColonTokenReference: PsiElement
        get() = findColonColon()!!

    val lhs: PsiElement?
        get() = doubleColonTokenReference.prevSibling

    fun setReceiverExpression(newReceiverExpression: CjExpression) {
        val oldReceiverExpression = this.receiverExpression
        oldReceiverExpression?.replace(newReceiverExpression) ?: addBefore(newReceiverExpression, doubleColonTokenReference)
    }

    val isEmptyLHS: Boolean
        get() = lhs == null

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitDoubleColonExpression(this, data)
    }
}
