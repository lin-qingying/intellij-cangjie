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

import org.cangnova.cangjie.psi.stubs.CangJiePlaceHolderStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ArrayFactory

open class CjSuperTypeListEntry : CjElementImplStub<CangJiePlaceHolderStub<out CjSuperTypeListEntry>> {
    constructor(node: ASTNode) : super(node)

    constructor(
        stub: CangJiePlaceHolderStub<out CjSuperTypeListEntry>,
        nodeType: IStubElementType<*, *>,
    ) : super(stub, nodeType)

    val parentDeclaration: PsiElement?
        get() = PsiTreeUtil.getParentOfType(
            this,
            CjTypeStatement::class.java,
        )

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitSuperTypeListEntry(this, data)
    }

    override fun toString(): String {
        return node.elementType.toString()
    }

    open val typeReference: CjTypeReference?
        get() = getStubOrPsiChild(CjStubElementTypes.TYPE_REFERENCE)

    val typeAsUserType: CjUserType?
        get() {
            val reference = typeReference
            if (reference != null) {
                val element = reference.typeElement
                if (element is CjUserType) {
                    return element
                }
            }
            return null
        }

    companion object {
        private val EMPTY_ARRAY = arrayOfNulls<CjSuperTypeListEntry>(0)

        var ARRAY_FACTORY: ArrayFactory<CjSuperTypeListEntry> =
            ArrayFactory { count: Int -> if (count == 0) EMPTY_ARRAY else arrayOfNulls(count) }
    }
}
