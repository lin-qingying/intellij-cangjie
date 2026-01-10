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

package org.cangnova.cangjie.psi.stubs.impl
import org.cangnova.cangjie.name.*

import org.cangnova.cangjie.psi.CjParameter
import org.cangnova.cangjie.psi.stubs.CangJieParameterStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef

class CangJieParameterStubImpl(
    parent: StubElement<out PsiElement>?,
    private val fqName: StringRef?,
    private val name: StringRef?,
    private val isMutable: Boolean,
    private val hasLetOrVar: Boolean,
    private val hasDefaultValue: Boolean,
    private val isNamed: Boolean = false,
    val functionTypeParameterName: String? = null,
) : CangJieStubBaseImpl<CjParameter>(parent, CjStubElementTypes.VALUE_PARAMETER), CangJieParameterStub {

    override fun getName(): String? {
        return StringRef.toString(name)
    }

    override fun getFqName(): FqName? {
        return if (fqName != null) FqName(fqName.string) else null
    }

    override fun isMutable() = isMutable
    override fun hasLetOrVar() = hasLetOrVar
    override fun hasDefaultValue() = hasDefaultValue
    override fun isNamed() = isNamed
}
