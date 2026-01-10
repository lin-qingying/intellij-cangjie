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

import org.cangnova.cangjie.psi.CjVariable
import org.cangnova.cangjie.psi.stubs.CangJieVariableStub
import org.cangnova.cangjie.psi.stubs.PatternKind
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import org.cangnova.cangjie.psi.CjPatternVariable

/**
 * 变量声明 Stub 实现
 *
 * 变量声明是模式匹配的声明方式，支持绑定模式、元组模式、枚举模式、通配符模式。
 * 变量本身没有名称，名称信息来自模式匹配中的绑定模式子节点。
 * fqName 存储在子模式（CangJieBindingPatternStub）中，而不是变量本身。
 *
 * @param parent 父 Stub 元素
 * @param patternKind 模式类型
 * @param isVar 是否为 var 声明
 * @param isTopLevel 是否为顶层变量
 * @param hasInitializer 是否有初始化器
 * @param hasReturnTypeRef 是否有类型声明
 * @param origin Stub 来源信息
 */
class CangJieVariableStubImpl(
    parent: StubElement<out PsiElement>?,
    private val patternKind: PatternKind,
    private val isVar: Boolean,
    private val isTopLevel: Boolean,
    private val hasInitializer: Boolean,
    private val hasReturnTypeRef: Boolean,
    val origin: CangJieStubOrigin?,
) : CangJieStubBaseImpl<CjPatternVariable>(parent, CjStubElementTypes.VARIABLE), CangJieVariableStub {

    override fun getPatternKind(): PatternKind = patternKind
    override fun isVar(): Boolean = isVar
    override fun isTopLevel(): Boolean = isTopLevel
    override fun hasInitializer(): Boolean = hasInitializer
    override fun hasReturnTypeRef(): Boolean = hasReturnTypeRef
}
