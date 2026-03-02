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

import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.psi.CjExtend
import org.cangnova.cangjie.psi.stubs.CangJieExtendStub
import org.cangnova.cangjie.psi.stubs.elements.CjExtendElementType
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import java.util.ArrayList
import org.cangnova.cangjie.name.*

open class CangJieExtendStubImpl(
    type: CjExtendElementType,
    parent: StubElement<out PsiElement>?,
    private val qualifiedName: StringRef?,
    private val classId: ClassId?,
    private val name: StringRef?,

    private val superNames: Array<StringRef>,

    override val receiverTypeName: String?,

    ) : CangJieStubBaseImpl<CjExtend>(parent, type), CangJieExtendStub {

    override fun getFqName(): FqName? {
        val stringRef = StringRef.toString(qualifiedName) ?: return null
        return FqName(stringRef)
    }

    override val extendId: String
        get() = StringRef.toString(name)

    override fun getName(): String? = StringRef.toString(name)

    override fun isLocal(): Boolean = true

    override fun getSuperNames(): List<String> {
        val result = ArrayList<String>()
        for (ref in superNames) {
            result.add(ref.toString())
        }
        return result
    }

    override fun getClassId(): ClassId? = classId
}
