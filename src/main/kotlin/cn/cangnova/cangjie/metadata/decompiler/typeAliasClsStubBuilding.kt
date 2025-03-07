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

package cn.cangnova.cangjie.metadata.decompiler

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import cn.cangnova.cangjie.metadata.ProtoBuf
import cn.cangnova.cangjie.metadata.VISIBILITY
import cn.cangnova.cangjie.metadata.deserialization.Flags
import cn.cangnova.cangjie.metadata.deserialization.underlyingType
import cn.cangnova.cangjie.name.ClassId
import cn.cangnova.cangjie.psi.stubs.impl.CangJieTypeAliasStubImpl
import cn.cangnova.cangjie.serialization.deserialization.ProtoContainer
import cn.cangnova.cangjie.serialization.deserialization.getName
fun createTypeAliasStub(
    parent: StubElement<out PsiElement>,
    typeAliasProto: ProtoBuf.TypeAlias,
    protoContainer: ProtoContainer,
    outerContext: ClsStubBuilderContext
) {
    val c = outerContext.child(typeAliasProto.typeParameterList)
    val shortName = c.nameResolver.getName(typeAliasProto.name)

    val classId = when (protoContainer) {
        is ProtoContainer.Class -> protoContainer.classId.createNestedClassId(shortName)
        is ProtoContainer.Package -> ClassId.topLevel(protoContainer.fqName.child(shortName))
    }

    val typeAlias = CangJieTypeAliasStubImpl(
        parent, classId.shortClassName.ref(), classId.asSingleFqName().ref(), classId,
//        isTopLevel = !classId.isNestedClass
    )

    val modifierList = createModifierListStubForDeclaration(typeAlias, typeAliasProto.flags, arrayListOf(VISIBILITY), listOf())

    val typeStubBuilder = TypeClsStubBuilder(c)
    val restConstraints = typeStubBuilder.createTypeParameterListStub(typeAlias, typeAliasProto.typeParameterList)
    assert(restConstraints.isEmpty()) {
        "'where' constraints are not allowed for type aliases"
    }

    if (Flags.HAS_ANNOTATIONS.get(typeAliasProto.flags)) {
        createAnnotationStubs(
            typeAliasProto.annotationList.map {
                c.components.annotationLoader.loadAnnotation(it, c.nameResolver)
            },
            modifierList
        )
    }

    val typeAliasUnderlyingType = typeAliasProto.underlyingType(c.typeTable)
    typeStubBuilder.createTypeReferenceStub(typeAlias, typeAliasUnderlyingType)
}
