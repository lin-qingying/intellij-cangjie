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
package org.cangnova.cangjie.psi.stubs.elements

import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.CjDotQualifiedExpression
import com.intellij.lang.ASTNode
import org.jetbrains.annotations.NonNls

class CjDotQualifiedExpressionElementType(debugName: @NonNls String) :
    CjPlaceHolderStubElementType<CjDotQualifiedExpression>(debugName, CjDotQualifiedExpression::class.java) {
    override fun shouldCreateStub(node: ASTNode): Boolean {
        val treeParent = node.treeParent ?: return false

        val parentElementType = treeParent.elementType
        if (parentElementType === CjStubElementTypes.PACKAGE_DIRECTIVE || parentElementType === CjStubElementTypes.VALUE_ARGUMENT || parentElementType === CjStubElementTypes.DOT_QUALIFIED_EXPRESSION
        ) {
            return checkNodeTypesTraversal(node) && super.shouldCreateStub(node)
        }

        return false
    }

    companion object {
        private fun checkNodeTypesTraversal(node: ASTNode): Boolean {
            val type = node.elementType
            if (type !== CjStubElementTypes.DOT_QUALIFIED_EXPRESSION && type !== CjStubElementTypes.REFERENCE_EXPRESSION && type !== CjTokens.IDENTIFIER && type !== CjTokens.DOT
            ) {
                return false
            }

            var child = node.firstChildNode
            while (child != null) {
                if (!checkNodeTypesTraversal(child)) {
                    return false
                }
                child = child.treeNext
            }

            return true
        }
    }
}
