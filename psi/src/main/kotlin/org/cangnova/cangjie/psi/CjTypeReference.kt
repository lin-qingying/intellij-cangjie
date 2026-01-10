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
import org.cangnova.cangjie.psi.psiUtil.CjStubbedPsiUtil
import org.cangnova.cangjie.psi.stubs.CangJiePlaceHolderStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import org.cangnova.cangjie.psi.stubs.elements.CjTokenSets
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement

/**
 *键入Reference Element。
 *底层令牌为[org.cangnova.cangjie.psi.CjNodeTypes.TYPE_REFERENCE]
 */
class CjTypeReference :
    CjModifierListOwnerStub<CangJiePlaceHolderStub<CjTypeReference>>,
    CjElement {

    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJiePlaceHolderStub<CjTypeReference>) : super(stub, CjStubElementTypes.TYPE_REFERENCE)

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitTypeReference(this, data)
    }


    val isPlaceholder: Boolean
        get() = ((typeElement as? CjUserType)?.referenceExpression as? CjNameReferenceExpression)?.isPlaceholder == true

    val typeElement: CjTypeElement?
        get() {
            return CjStubbedPsiUtil.getStubOrPsiChild(this, CjTokenSets.TYPE_ELEMENT_TYPES, CjTypeElement.ARRAY_FACTORY)

             /*   ?: if (children.isNotEmpty() && children[0].elementType == CjNodeTypes.BASIC_TYPE) {
                    children[0] as CjTypeElement
                } else {
                    null
                }*/
        }

    fun hasParentheses(): Boolean {
        return findChildByType<PsiElement>(CjTokens.LPAR) != null && findChildByType<PsiElement>(CjTokens.RPAR) != null
    }

    fun nameForReceiverLabel() = (typeElement as? CjUserType)?.referencedName

    fun getTypeText(): String {
        return stub?.let { getTypeText(typeElement) } ?: text
    }

    private fun getQualifiedName(userType: CjUserType): String? {
        val qualifier = userType.qualifier ?: return userType.referencedName
        return getQualifiedName(qualifier) + "." + userType.referencedName
    }

    private fun getTypeText(typeElement: CjTypeElement?): String? {
        return when (typeElement) {
            is CjUserType -> buildString {
                append(getQualifiedName(typeElement))
//                val args = typeElement.typeArguments
//                if (args.isNotEmpty()) {
//                    append(args.joinToString(", ", "<", ">") {
//                        val projection = when (it.projectionKind) {
//                            CjProjectionKind.IN -> "in "
//
//                            CjProjectionKind.STAR -> "*"
//                            CjProjectionKind.NONE -> ""
//                        }
//                        projection + (getTypeText(it.typeReference?.typeElement) ?: "")
//                    })
//                }
            }

            is CjBasicType -> buildString {
                append(typeElement.text)
            }

            is CjFunctionType -> buildString {
                typeElement.receiverTypeReference?.let { append(getTypeText(it.typeElement)) }
                append(
                    typeElement.parameters.joinToString(", ", "(", ")") { param ->
                        param.name?.let { "$it: " }.orEmpty() + param.typeReference?.getTypeText().orEmpty()
                    },
                )
                typeElement.returnTypeReference?.let { returnType ->
                    append(" -> ")
                    append(getTypeText(returnType.typeElement))
                }
            }

            null -> null
            else -> error("Unsupported type $typeElement")
        }
    }
}
