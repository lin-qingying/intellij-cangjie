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

package org.cangnova.cangjie.decompiler.stub

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
 *
 * @property classDataFinder 类数据查找器，用于查找类的元数据
 * @property virtualFileForDebug 用于调试的虚拟文件
 * @property declTable 声明表，存储所有声明的元数据
 * @property typeTable 类型表，存储所有类型的元数据
 */
class ClsStubBuilderComponents(
    val classDataFinder: ClassDataFinder,
    val virtualFileForDebug: VirtualFile,
    val declTable: DeclTable,
    val typeTable: TypeTable
) {
    /**
     * 创建 Stub 构建上下文
     *
     * @param packageFqName 包的完全限定名
     * @param typeTable 类型表
     * @return 新的 Stub 构建上下文
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
 *
 * 用于在 Stub 构建过程中管理和查找类型参数。支持嵌套的类型参数上下文。
 */
interface TypeParameters {
    /**
     * 根据 ID 获取类型参数名称
     */
    operator fun get(id: Int): Name

    /**
     * 创建一个包含内部类型参数的子上下文
     */
    fun child(innerTypeParameters: List<TypeParameterWrapper>): TypeParameters =
        TypeParametersImpl(innerTypeParameters, parent = this)
}

/**
 * 空类型参数实现
 *
 * 用于没有类型参数的上下文，访问任何类型参数都会抛出异常。
 */
object EmptyTypeParameters : TypeParameters {
    override fun get(id: Int): Name = throw IllegalStateException("Unknown type parameter with id = $id")
}

/**
 * 类型参数实现
 *
 * 维护一个类型参数 ID 到名称的映射，如果本地没有找到则委托给父上下文。
 *
 * @property typeParameterWrappers 类型参数包装器集合
 * @property parent 父类型参数上下文
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
 *
 * @property components Stub 构建器组件
 * @property containerFqName 当前容器的完全限定名
 * @property typeParameters 类型参数管理器
 * @property typeTable 类型表
 * @property protoContainer Proto 容器（包或类）
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
 *
 * 用于在 Stub 构建过程中跟踪当前正在处理的声明所在的容器。
 */
sealed class ProtoContainer {
    /**
     * 容器的完全限定名
     */
    abstract val fqName: FqName

    /**
     * 包容器
     *
     * @property fqName 包的完全限定名
     * @property typeTable 类型表
     */
    class Package(
        override val fqName: FqName,
        val typeTable: TypeTable
    ) : ProtoContainer()

    /**
     * 类容器
     *
     * @property classDecl 类声明包装器
     * @property typeTable 类型表
     * @property outerClass 外部类容器（如果是嵌套类）
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
 *
 * 用于处理嵌套的声明（如类成员或嵌套类）时创建新的上下文。
 *
 * @param typeParameterList 类型参数列表
 * @param name 子容器的名称（如果有）
 * @param typeTable 类型表
 * @param protoContainer Proto 容器
 * @return 新的 Stub 构建上下文
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
