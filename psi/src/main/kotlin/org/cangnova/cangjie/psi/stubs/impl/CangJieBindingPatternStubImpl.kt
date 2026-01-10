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

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjBindingPattern
import org.cangnova.cangjie.psi.stubs.CangJieBindingPatternStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes

/**
 * 绑定模式 Stub 实现
 *
 * 存储绑定变量的名称和 fqName。
 * 如 `let a = 1` 中的 `a`，fqName 为 `package.a`。
 *
 * @param parent 父 Stub 元素
 * @param nameRef 绑定变量的名称（StringRef）
 * @param fqName 完全限定名（仅顶层变量有效）
 */
class CangJieBindingPatternStubImpl(
    parent: StubElement<out PsiElement>?,
    private val nameRef: StringRef?,
    override val fqName: FqName? = null,
) : CangJieStubBaseImpl<CjBindingPattern>(parent, CjStubElementTypes.BINDING_PATTERN),
    CangJieBindingPatternStub {

    override fun getName(): String? = StringRef.toString(nameRef)

}
