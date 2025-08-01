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

package cn.cangnova.cangjie.references

import cn.cangnova.cangjie.descriptors.DeclarationDescriptor
import cn.cangnova.cangjie.lexer.CjToken
import cn.cangnova.cangjie.lexer.CjTokens
import cn.cangnova.cangjie.psi.CjArrayAccessExpression
import cn.cangnova.cangjie.resolve.BindingContext
import cn.cangnova.cangjie.utils.OperatorNameConventions
import com.intellij.psi.MultiRangeReference

class CjArrayAccessReference(
    expression: CjArrayAccessExpression

) : CjSimpleReference<CjArrayAccessExpression>(expression), MultiRangeReference {
    override val resolvesByNames: Collection<Name>
        get() = NAMES

    override fun getRangeInElement() = element.textRange.shiftRight(-element.textOffset)

    private fun getBracketRange(bracketToken: CjToken) =
        expression.indicesNode.node.findChildByType(bracketToken)?.textRange?.shiftRight(-expression.textOffset)


    override fun getTargetDescriptors(context: BindingContext): Collection<DeclarationDescriptor> {
        val getFunctionDescriptor = context[BindingContext.INDEXED_LVALUE_GET, expression]?.candidateDescriptor
        val setFunctionDescriptor = context[BindingContext.INDEXED_LVALUE_SET, expression]?.candidateDescriptor
        return listOfNotNull(getFunctionDescriptor, setFunctionDescriptor)
    }

    override fun getRanges() = listOfNotNull(getBracketRange(CjTokens.LBRACKET), getBracketRange(CjTokens.RBRACKET))

    override fun canRename() = true

    companion object {
        private val NAMES = listOf(OperatorNameConventions.GET, OperatorNameConventions.SET)
    }
}
