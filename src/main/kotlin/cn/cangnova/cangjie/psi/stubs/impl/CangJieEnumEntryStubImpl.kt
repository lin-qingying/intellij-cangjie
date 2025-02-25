/*
 * Copyright 2024 LinQingYing. and contributors.
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

package cn.cangnova.cangjie.psi.stubs.impl

import cn.cangnova.cangjie.name.ClassId
import cn.cangnova.cangjie.name.FqName
import cn.cangnova.cangjie.psi.CjEnumEntry
import cn.cangnova.cangjie.psi.stubs.CangJieEnumEntryStub
import cn.cangnova.cangjie.psi.stubs.elements.CjEnumEntryElementType
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef


open class CangJieEnumEntryStubImpl(
    type: CjEnumEntryElementType,
    parent: StubElement<out PsiElement>?,
      val qualifiedNameByParent: StringRef?,  //枚举值对于enum声明的名称
      val qualifiedNameByPackage:StringRef?, //枚举值对于包声明的名称
    private val classId: ClassId?,
    private val name: StringRef?,

//    private val isInterface: Boolean,

    private val isLocal: Boolean,
//    private val isTopLevel: Boolean,
) : CangJieStubBaseImpl<CjEnumEntry>(parent, type), CangJieEnumEntryStub {

    override fun getFqName(): FqName? {
        val stringRef = StringRef.toString(qualifiedNameByParent) ?: return null
        return FqName(stringRef)
    }
    override val fqNameByPackage: FqName?get() {
        val stringRef = StringRef.toString(qualifiedNameByPackage) ?: return null
        return FqName(stringRef)
    }

//    override fun isInterface() = isInterface

    override fun isLocal() = isLocal
    override fun getName() = StringRef.toString(name)

    override fun getSuperNames(): List<String>  = emptyList()
    override fun getClassId(): ClassId? = classId

//    override fun isTopLevel() = isTopLevel
}
