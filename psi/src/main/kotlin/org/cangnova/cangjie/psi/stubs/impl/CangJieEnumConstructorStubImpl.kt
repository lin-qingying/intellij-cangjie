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
import org.cangnova.cangjie.name.*

import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.psi.CjEnumConstructor
import org.cangnova.cangjie.psi.stubs.CangJieEnumConstructorStub
import org.cangnova.cangjie.psi.stubs.elements.CjEnumConstructorElementType
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef

/**
 * 枚举构造器的 Stub 实现
 *
 * 根据仓颉语言规范，枚举条目是构造器，不是类型声明。
 * 此实现不再继承 CangJieStubBaseImpl<CjEnumConstructor>，而是直接实现接口。
 */
open class CangJieEnumConstructorStubImpl(
    type: CjEnumConstructorElementType,
    parent: StubElement<out PsiElement>?,
    private val fqName: StringRef?,           // 枚举条目的完全限定名 (例如: com.example.Color.Red)
    private val parentEnumFqName: StringRef?,  // 父枚举的完全限定名 (例如: com.example.Color)
    private val classId: ClassId?,
    private val name: StringRef?,
    private val parameterCount: Int,           // 参数数量(用于区分重载)
    private val parameterTypeNames: List<String>, // 参数类型名称列表
    private val isLocal: Boolean,
) : CangJieStubBaseImpl<CjEnumConstructor>(parent, type), CangJieEnumConstructorStub {

    override fun getFqName(): FqName? {
        val stringRef = StringRef.toString(fqName) ?: return null
        return FqName(stringRef)
    }

    override fun getParentEnumFqName(): FqName? {
        val stringRef = StringRef.toString(parentEnumFqName) ?: return null
        return FqName(stringRef)
    }

    override fun getParameterCount(): Int = parameterCount

    override fun getParameterTypeNames(): List<String> = parameterTypeNames

    override fun isLocal() = isLocal

    override fun getName() = StringRef.toString(name)

    override fun getClassId(): ClassId? = classId
}
