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

import org.cangnova.cangjie.lexer.CjSingleValueToken
import org.cangnova.cangjie.name.OperatorConventions
import org.cangnova.cangjie.parsing.CangJieExpressionParsing
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.TreeElement

class CjOperationReferenceExpression(node: ASTNode) : CjSimpleNameExpressionImpl(node) {

    override val referencedNameElement get() = CangJieExpressionParsing.ALL_OPERATIONS?.let {
        findChildByType<PsiElement>(
            it,
        )
    }
        ?: this

    fun isConventionOperator(): Boolean {
        val tokenType = operationSignTokenType ?: return false
        return OperatorConventions.getNameForOperationSymbol(tokenType) != null
    }
    val operationSignTokenType: CjSingleValueToken?
        get() = (firstChild as? TreeElement)?.elementType as? CjSingleValueToken
}
