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

package org.cangnova.cangjie.psi.stubs.impl

import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjVariable
import org.cangnova.cangjie.psi.stubs.CangJieVariableStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef

class CangJieVariableStubImpl @JvmOverloads constructor(
    parent: StubElement<out PsiElement>?,
    private val name: StringRef?,
    private val isVar: Boolean,
    private val isTopLevel: Boolean,

    private val hasInitializer: Boolean,
    private val isExtension: Boolean,
    private val hasReturnTypeRef: Boolean,
    private val fqName: FqName?,
    override val childNamesByPattern: List<CangJieVariableStub.ChildInfo> = emptyList(),

    val origin: CangJieStubOrigin?,
) : CangJieStubBaseImpl<CjVariable>(parent, CjStubElementTypes.VARIABLE), CangJieVariableStub {

    init {
        if (isTopLevel && fqName == null) {
            throw IllegalArgumentException("fqName shouldn't be null for top level properties")
        }
    }

    override fun getFqName() = fqName
    override fun isVar() = isVar
    override fun isTopLevel() = isTopLevel

    override fun hasInitializer() = hasInitializer
    override fun isExtension() = isExtension
    override fun hasReturnTypeRef() = hasReturnTypeRef
    override fun getName() = StringRef.toString(name)
}
