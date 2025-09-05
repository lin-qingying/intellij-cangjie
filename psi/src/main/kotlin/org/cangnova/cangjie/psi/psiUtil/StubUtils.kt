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

package org.cangnova.cangjie.psi.psiUtil

import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.psi.CjClassLikeDeclaration
import org.cangnova.cangjie.psi.CjEnumEntry
import org.cangnova.cangjie.psi.stubs.CangJieClassifierStub
import org.cangnova.cangjie.psi.stubs.CangJieFileStub
import org.cangnova.cangjie.psi.stubs.CangJiePlaceHolderStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream

object StubUtils {
    @JvmStatic
    fun deserializeClassId(dataStream: StubInputStream): ClassId? {
        val classId = dataStream.readName() ?: return null
        return ClassId.fromString(classId.string)
    }

    @JvmStatic
    fun serializeClassId(dataStream: StubOutputStream, classId: ClassId?) {
        dataStream.writeName(classId?.asString())
    }

//
    @JvmStatic
    fun createNestedClassId(parentStub: StubElement<*>, currentDeclaration: CjClassLikeDeclaration): ClassId? = when {
        parentStub is CangJieFileStub -> ClassId(parentStub.getPackageFqName(), currentDeclaration.nameAsSafeName)

        parentStub is CangJiePlaceHolderStub<*> && parentStub.stubType == CjStubElementTypes.CLASS_BODY -> {
            val containingClassStub = parentStub.parentStub as? CangJieClassifierStub
            if (containingClassStub != null && currentDeclaration !is CjEnumEntry) {
                containingClassStub.getClassId()?.createNestedClassId(currentDeclaration.nameAsSafeName)
            } else {
                null
            }
        }
        else -> null
    }
}
