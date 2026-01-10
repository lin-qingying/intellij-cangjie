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

import org.cangnova.cangjie.lexer.CjToken
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.stubs.CangJiePlaceHolderStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import com.google.common.collect.Lists
import com.intellij.lang.ASTNode

class CjFunctionType : CjElementImplStub<CangJiePlaceHolderStub<CjFunctionType>>, CjTypeElement {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJiePlaceHolderStub<CjFunctionType>) : super(stub, CjStubElementTypes.FUNCTION_TYPE)

    override val typeArgumentsAsTypes: List<CjTypeReference>
        get() {
            val result =
                Lists.newArrayList<CjTypeReference>()

            val receiverTypeRef = receiverTypeReference
            if (receiverTypeRef != null) {
                result.add(receiverTypeRef)
            }
            for (cjParameter in parameters) {
                cjParameter.typeReference?.let{
                    result.add(it)

                }
            }
            val returnTypeRef = returnTypeReference
            if (returnTypeRef != null) {
                result.add(returnTypeRef)
            }
            return result
        }

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitFunctionType(this, data)
    }

    val parameterList: CjParameterList?
        get() = getStubOrPsiChild(CjStubElementTypes.VALUE_PARAMETER_LIST)

    val parameters: List<CjParameter>
        get() {
            val list = parameterList
            return list?.parameters ?: emptyList()
        }

    val receiver: CjFunctionTypeReceiver?
        get() = getStubOrPsiChild(CjStubElementTypes.FUNCTION_TYPE_RECEIVER)

    val receiverTypeReference: CjTypeReference?
        get() {
            val receiverDeclaration = receiver ?: return null
            return receiverDeclaration.typeReference
        }



    val returnTypeReference: CjTypeReference?
        get() = getStubOrPsiChild(CjStubElementTypes.TYPE_REFERENCE)



    companion object {
        val RETURN_TYPE_SEPARATOR: CjToken = CjTokens.ARROW
    }
}
