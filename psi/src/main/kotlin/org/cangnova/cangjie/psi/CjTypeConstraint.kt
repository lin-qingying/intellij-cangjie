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

class CjTypeConstraint : CjElementImplStub<CangJiePlaceHolderStub<CjTypeConstraint>>, CjElement {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJiePlaceHolderStub<CjTypeConstraint>) : super(stub, CjStubElementTypes.TYPE_CONSTRAINT)

    override fun toString(): String {
        return node.elementType.toString()
    }

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitTypeConstraint(this, data)
    }

    @get:IfNotParsed
    val subjectTypeParameterName: CjSimpleNameExpression?
        get() = getStubOrPsiChild(CjStubElementTypes.REFERENCE_EXPRESSION)

    @get:IfNotParsed
    val boundTypeReference: CjTypeReference?
        get() = getStubOrPsiChild(CjStubElementTypes.TYPE_REFERENCE)

    @get:IfNotParsed
    val boundTypeReferences: List<CjTypeReference>
        get() = getStubOrPsiChildrenAsList(
            CjStubElementTypes.TYPE_REFERENCE,
        )
}
