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

package org.cangnova.cangjie.psi.stubs.elements

import org.cangnova.cangjie.psi.CjConstructorElementType
import org.cangnova.cangjie.psi.CjEndSecondaryConstructor
import org.cangnova.cangjie.psi.CjSecondaryConstructor
import org.cangnova.cangjie.psi.stubs.CangJieConstructorStub
import org.cangnova.cangjie.psi.stubs.impl.CangJieConstructorStubImpl
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef

class CjEndSecondaryConstructorElementType(debugName: String) :
    CjConstructorElementType<CjEndSecondaryConstructor>(debugName, CjEndSecondaryConstructor::class.java, CangJieConstructorStub::class.java) {
    override fun newStub(
        parentStub: StubElement<*>,
        nameRef: StringRef?,
        hasBody: Boolean,
        isPrimary: Boolean,
        isDelegatedCallToThis: Boolean,
    ): CangJieConstructorStub<CjEndSecondaryConstructor> {
        return CangJieConstructorStubImpl(
            parentStub,
            CjStubElementTypes.END_SECONDARY_CONSTRUCTOR,
            nameRef,
            hasBody,
            isPrimary,
            isDelegatedCallToThis,
        )
    }

    override fun isDelegatedCallToThis(constructor: CjEndSecondaryConstructor): Boolean {
        return constructor.getDelegationCallOrNull()?.isCallToThis ?: true
    }
}

class CjSecondaryConstructorElementType(debugName: String) :
    CjConstructorElementType<CjSecondaryConstructor>(debugName, CjSecondaryConstructor::class.java, CangJieConstructorStub::class.java) {
    override fun newStub(
        parentStub: StubElement<*>,
        nameRef: StringRef?,
        hasBody: Boolean,
        isPrimary: Boolean,
        isDelegatedCallToThis: Boolean,
    ): CangJieConstructorStub<CjSecondaryConstructor> {
        return CangJieConstructorStubImpl(
            parentStub,
            CjStubElementTypes.SECONDARY_CONSTRUCTOR,
            nameRef,
            hasBody,
            isPrimary,
            isDelegatedCallToThis,
        )
    }

    override fun isDelegatedCallToThis(constructor: CjSecondaryConstructor): Boolean {
        return constructor.getDelegationCallOrNull()?.isCallToThis ?: true
    }
}
