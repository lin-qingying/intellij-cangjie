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

import org.cangnova.cangjie.psi.CjConstantExpression
import org.cangnova.cangjie.psi.stubs.CangJieConstantExpressionStub
import org.cangnova.cangjie.psi.stubs.ConstantValueKind
import org.cangnova.cangjie.psi.stubs.elements.CjConstantExpressionElementType
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef

class CangJieConstantExpressionStubImpl(
    parent: StubElement<out PsiElement>?,
    elementType: CjConstantExpressionElementType,
    private val kind: ConstantValueKind,
    private val value: StringRef,
) : CangJieStubBaseImpl<CjConstantExpression>(parent, elementType), CangJieConstantExpressionStub {
    override fun kind(): ConstantValueKind = kind
    override fun value(): String = StringRef.toString(value)
}
