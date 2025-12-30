/*
 * Copyright 2025 LinQingYing. and contributors.
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

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import org.cangnova.cangjie.lexer.CjKeywordToken
import org.cangnova.cangjie.psi.psiUtil.addModifier
import org.cangnova.cangjie.psi.psiUtil.removeModifier
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes

open class CjModifierListOwnerStub<T : StubElement<*>> :
    CjElementImplStub<T>,
    CjModifierListOwner {
    constructor(node: ASTNode) : super(node)

    constructor(stub: T, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    override val annotations: CjAnnotations?
        get() = getStubOrPsiChild(CjStubElementTypes.ANNOTATIONS)

    override val annotationEntries: List<CjAnnotation>
        get() = annotations?.entries ?: emptyList()

    override val modifierList: CjModifierList?
        get() = getStubOrPsiChild(CjStubElementTypes.MODIFIER_LIST)

    override fun hasModifier(modifier: CjKeywordToken): Boolean {
        val modifierList = modifierList
        return modifierList != null && modifierList.hasModifier(modifier)
    }


    override fun addModifier(modifier: CjKeywordToken) {
        addModifier(this, modifier)
    }

    override fun removeModifier(modifier: CjKeywordToken) {
        removeModifier(this, modifier)
    }
}
