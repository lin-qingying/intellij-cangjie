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

import org.cangnova.cangjie.psi.CjPropertyAccessor
import org.cangnova.cangjie.psi.stubs.CangJiePropertyAccessorStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.psi.stubs.StubElement

class CangJiePropertyAccessorStubImpl(
    parent: StubElement<*>?,
    private val isGetter: Boolean,
    private val hasBody: Boolean,
    private val hasBlockBody: Boolean,
) : CangJieStubBaseImpl<CjPropertyAccessor>(
    parent,
    CjStubElementTypes.PROPERTY_ACCESSOR,
),
    CangJiePropertyAccessorStub {
    override fun isGetter(): Boolean = isGetter

    override fun hasBody(): Boolean = hasBody

    override fun hasBlockBody(): Boolean = hasBlockBody
}
