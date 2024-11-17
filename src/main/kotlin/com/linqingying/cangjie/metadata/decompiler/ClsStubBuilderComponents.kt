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

package com.linqingying.cangjie.metadata.decompiler

import com.intellij.openapi.vfs.VirtualFile
import com.linqingying.cangjie.descriptors.annotations.AnnotationUseSiteTarget
import com.linqingying.cangjie.metadata.ProtoBuf
import com.linqingying.cangjie.metadata.deserialization.NameResolver
import com.linqingying.cangjie.metadata.deserialization.TypeTable
import com.linqingying.cangjie.name.ClassId
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.resolve.constants.ConstantValue
import com.linqingying.cangjie.serialization.CangJieMetadataVersion
import com.linqingying.cangjie.serialization.SerializerExtensionProtocol
import com.linqingying.cangjie.serialization.deserialization.AnnotationLoader
import com.linqingying.cangjie.serialization.deserialization.ClassDataFinder
import com.linqingying.cangjie.serialization.deserialization.ProtoContainer
import com.linqingying.cangjie.serialization.deserialization.getName
data class AnnotationWithArgs(val classId: ClassId, val args: Map<Name, ConstantValue<*>>)

data class AnnotationWithTarget(val annotationWithArgs: AnnotationWithArgs, val target: AnnotationUseSiteTarget?)

class ClsStubBuilderComponents(
    val classDataFinder: ClassDataFinder,
    val annotationLoader: AnnotationLoader<AnnotationWithArgs>,
    val virtualFileForDebug: VirtualFile,
    val serializationProtocol: SerializerExtensionProtocol,
    val classFinder: CangJieClassFinder? = null,
    val metadataVersion: CangJieMetadataVersion? = null
) {
    fun createContext(
        nameResolver: NameResolver,
        packageFqName: FqName,
        typeTable: TypeTable
    ): ClsStubBuilderContext =
        ClsStubBuilderContext(this, nameResolver, packageFqName, EmptyTypeParameters, typeTable, protoContainer = null)
}

interface TypeParameters {
    operator fun get(id: Int): Name

    fun child(nameResolver: NameResolver, innerTypeParameters: List<ProtoBuf.TypeParameter>) =
        TypeParametersImpl(nameResolver, innerTypeParameters, parent = this)
}

object EmptyTypeParameters : TypeParameters {
    override fun get(id: Int): Name = throw IllegalStateException("Unknown type parameter with id = $id")
}

class TypeParametersImpl(
    nameResolver: NameResolver,
    typeParameterProtos: Collection<ProtoBuf.TypeParameter>,
    private val parent: TypeParameters
) : TypeParameters {
    private val typeParametersById = typeParameterProtos.map { Pair(it.id, nameResolver.getName(it.name)) }.toMap()

    override fun get(id: Int): Name = typeParametersById[id] ?: parent[id]
}

class ClsStubBuilderContext(
    val components: ClsStubBuilderComponents,
    val nameResolver: NameResolver,
    val containerFqName: FqName,
    val typeParameters: TypeParameters,
    val typeTable: TypeTable,
    val protoContainer: ProtoContainer.Class?
)

internal fun ClsStubBuilderContext.child(
    typeParameterList: List<ProtoBuf.TypeParameter>,
    name: Name? = null,
    nameResolver: NameResolver = this.nameResolver,
    typeTable: TypeTable = this.typeTable,
    protoContainer: ProtoContainer.Class? = this.protoContainer
): ClsStubBuilderContext = ClsStubBuilderContext(
    this.components,
    nameResolver,
    if (name != null) this.containerFqName.child(name) else this.containerFqName,
    this.typeParameters.child(nameResolver, typeParameterList),
    typeTable,
    protoContainer
)
