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

package org.cangnova.cangjie.analysis.decompiler.stub

import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.descriptors.ClassKind
import org.cangnova.cangjie.metadata.deserialization.DeclTable
import org.cangnova.cangjie.metadata.deserialization.TypeTable
import org.cangnova.cangjie.metadata.model.wrapper.ClassDeclWrapper
import org.cangnova.cangjie.metadata.model.wrapper.TypeParameterWrapper
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.serialization.deserialization.ClassDataFinder

/**
 * Stub 构建器组件，用于存储构建 Stub 所需的各种组件和服务
 */
class ClsStubBuilderComponents(
    val classDataFinder: ClassDataFinder,
    val virtualFileForDebug: VirtualFile,
    val declTable: DeclTable,
    val typeTable: TypeTable
) {
    /**
     * 创建 Stub 构建上下文
     */
    fun createContext(
        packageFqName: FqName,
        typeTable: TypeTable
    ): ClsStubBuilderContext = ClsStubBuilderContext(
        this,
        packageFqName,
        EmptyTypeParameters,
        typeTable,
        protoContainer = null
    )
}

/**
 * 类型参数管理接口
 */
interface TypeParameters {
    operator fun get(id: Int): Name

    fun child(innerTypeParameters: List<TypeParameterWrapper>): TypeParameters =
        TypeParametersImpl(innerTypeParameters, parent = this)
}

/**
 * 空类型参数实现
 */
object EmptyTypeParameters : TypeParameters {
    override fun get(id: Int): Name = throw IllegalStateException("Unknown type parameter with id = $id")
}

/**
 * 类型参数实现
 */
class TypeParametersImpl(
    typeParameterWrappers: Collection<TypeParameterWrapper>,
    private val parent: TypeParameters
) : TypeParameters {
    private val typeParametersById = typeParameterWrappers.mapIndexed { index, wrapper ->
        Pair(index, wrapper.name)
    }.toMap()

    override fun get(id: Int): Name = typeParametersById[id] ?: parent[id]
}

/**
 * Stub 构建上下文，包含构建过程中所需的上下文信息
 */
class ClsStubBuilderContext(
    val components: ClsStubBuilderComponents,
    val containerFqName: FqName,
    val typeParameters: TypeParameters,
    val typeTable: TypeTable,
    val protoContainer: ProtoContainer?
)

/**
 * Proto 容器，表示类或包的容器
 */
sealed class ProtoContainer {
    abstract val fqName: FqName

    /**
     * 包容器
     */
    class Package(
        override val fqName: FqName,
        val typeTable: TypeTable
    ) : ProtoContainer()

    /**
     * 类容器
     */
    class Class(
        val classDecl: ClassDeclWrapper,
        val typeTable: TypeTable,
        val outerClass: Class?
    ) : ProtoContainer() {
        override val fqName: FqName = classDecl.classId.asSingleFqName()
        val classId: ClassId = classDecl.classId
        val kind: ClassKind = classDecl.kind
    }
}

/**
 * 创建子上下文
 */
internal fun ClsStubBuilderContext.child(
    typeParameterList: List<TypeParameterWrapper>,
    name: Name? = null,
    typeTable: TypeTable = this.typeTable,
    protoContainer: ProtoContainer? = this.protoContainer
): ClsStubBuilderContext = ClsStubBuilderContext(
    this.components,
    if (name != null) this.containerFqName.child(name) else this.containerFqName,
    this.typeParameters.child(typeParameterList),
    typeTable,
    protoContainer
)
