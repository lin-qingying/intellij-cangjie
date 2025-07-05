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

package cn.cangnova.cangjie.psi

import cn.cangnova.cangjie.lexer.CjTokens
import cn.cangnova.cangjie.psi.stubs.CangJieTypeParameterStub
import cn.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.util.PsiTreeUtil

class CjTypeParameter : CjNamedDeclarationStub<CangJieTypeParameterStub> {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJieTypeParameterStub) : super(stub, CjStubElementTypes.TYPE_PARAMETER)

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R? {
        return visitor.visitTypeParameter(this, data)
    }

    override fun toString(): String {
        return node.elementType.toString()
    }

    fun setExtendsBound(typeReference: CjTypeReference?): CjTypeReference? {
        val currentExtendsBound = extendsBound
        if (currentExtendsBound != null) {
            if (typeReference == null) {
                val colon = findChildByType<PsiElement>(CjTokens.COLON)
                colon?.delete()
                currentExtendsBound.delete()
                return null
            }
            return currentExtendsBound.replace(typeReference) as CjTypeReference
        }

        if (typeReference != null) {
            val colon = addAfter(CjPsiFactory(project).createColon(), nameIdentifier)
            return addAfter(typeReference, colon) as CjTypeReference
        }

        return null
    }

    val extendsBound: CjTypeReference?
        get() = getStubOrPsiChild(CjStubElementTypes.TYPE_REFERENCE)
    val extendsBounds: List<CjTypeReference>
        get() = getStubOrPsiChildrenAsList(
            CjStubElementTypes.TYPE_REFERENCE,
        )

    override fun getUseScope(): SearchScope {
        val owner = PsiTreeUtil.getParentOfType(
            this,
            CjTypeParameterListOwner::class.java,
        )
        return LocalSearchScope(owner ?: this)
    }
}
