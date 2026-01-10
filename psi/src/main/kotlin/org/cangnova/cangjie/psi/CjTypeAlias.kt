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
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.psi.psiUtil.ClassIdCalculator
import org.cangnova.cangjie.psi.stubs.CangJiePlaceHolderStub
import org.cangnova.cangjie.psi.stubs.CangJieTypeAliasStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentationProviders
import com.intellij.psi.PsiElement

class CjTypeAlias : CjTypeParameterListOwnerStub<CangJieTypeAliasStub>, CjNamedDeclaration, CjClassLikeDeclaration {
    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJieTypeAliasStub) : super(stub, CjStubElementTypes.TYPEALIAS)

    override fun getClassId(): ClassId? {
        stub?.let { return it.getClassId() }
        return ClassIdCalculator.calculateClassId(this)
    }

    override fun toString(): String {
        return super.toString()
    }
    override fun getPresentation() = ItemPresentationProviders.getItemPresentation(this)

    @IfNotParsed
    fun getTypeAliasKeyword(): PsiElement? =
        findChildByType(CjTokens.TYPE_KEYWORD)

    override fun <R : Any?, D : Any?> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitTypeAlias(this, data)
    }

    @IfNotParsed
    fun getTypeReference(): CjTypeReference? {
        return if (stub != null) {
            val typeReferences =
                getStubOrPsiChildrenAsList<CjTypeReference, CangJiePlaceHolderStub<CjTypeReference>>(CjStubElementTypes.TYPE_REFERENCE)
            typeReferences[0]
        } else {
            findChildByType(CjNodeTypes.TYPE_REFERENCE)
        }
    }
}
