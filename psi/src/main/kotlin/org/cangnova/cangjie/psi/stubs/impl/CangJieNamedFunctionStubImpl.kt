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

import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjMainFunction
import org.cangnova.cangjie.psi.CjMacroDeclaration
import org.cangnova.cangjie.psi.stubs.CangJieNamedFunctionStub
import org.cangnova.cangjie.psi.stubs.CangJieMainFunctionStub
import org.cangnova.cangjie.psi.stubs.CangJieMacroStub
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.cangnova.cangjie.name.*

class CangJieNamedFunctionStubImpl(
    parent: StubElement<out PsiElement>?,
    element: IStubElementType<*, *>,
    private val nameRef: StringRef?,
    private val isTopLevel: Boolean,
    private val fqName: FqName?,
    private val isExtension: Boolean,
    private val hasBlockBody: Boolean,
    private val hasBody: Boolean,
    private val hasTypeParameterListBeforeFunctionName: Boolean,
    val origin: CangJieStubOrigin?,
) : CangJieStubBaseImpl<CjNamedFunction>(parent, element), CangJieNamedFunctionStub {
    init {
        if (isTopLevel && fqName == null) {
            throw IllegalArgumentException("fqName shouldn't be null for top level functions")
        }
    }

    override fun getFqName() = fqName

    override fun getName() = StringRef.toString(nameRef)
    override fun isTopLevel() = isTopLevel
    override fun isExtension() = isExtension
    override fun hasBlockBody() = hasBlockBody
    override fun hasBody() = hasBody
    override fun hasTypeParameterListBeforeFunctionName() = hasTypeParameterListBeforeFunctionName

    companion object
}

class CangJieMainFunctionStubImpl(
    parent: StubElement<out PsiElement>?,
    element: IStubElementType<*, *>,
    private val nameRef: StringRef?,

    private val fqName: FqName?,

    val origin: CangJieStubOrigin?,
) : CangJieStubBaseImpl<CjMainFunction>(parent, element), CangJieMainFunctionStub {


    override fun getFqName() = fqName

    override fun getName() = StringRef.toString(nameRef)
    override fun isTopLevel() = true
    override fun isExtension() = false
}

class CangJieMacroStubImpl(
    parent: StubElement<out PsiElement>?,
    element: IStubElementType<*, *>,
    private val nameRef: StringRef?,
    private val isTopLevel: Boolean,
    private val fqName: FqName?,
    private val isExtension: Boolean,
    private val hasBlockBody: Boolean,
    private val hasBody: Boolean,
    private val hasTypeParameterListBeforeFunctionName: Boolean,
    val origin: CangJieStubOrigin?,
) : CangJieStubBaseImpl<CjMacroDeclaration>(parent, element), CangJieMacroStub {
    init {
        if (isTopLevel && fqName == null) {
            throw IllegalArgumentException("fqName shouldn't be null for top level functions")
        }
    }

    override fun getFqName() = fqName

    override fun getName() = StringRef.toString(nameRef)
    override fun isTopLevel() = isTopLevel
    override fun isExtension() = isExtension
    override fun hasBlockBody() = hasBlockBody
    override fun hasBody() = hasBody
    override fun hasTypeParameterListBeforeFunctionName() = hasTypeParameterListBeforeFunctionName
}
