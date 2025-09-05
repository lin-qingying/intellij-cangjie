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
 */

package org.cangnova.cangjie.descriptors.impl

import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.builtins.PrimitiveType
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.resolve.scopes.MemberScope
import org.cangnova.cangjie.resolve.scopes.PrimitiveMemberScope
import org.cangnova.cangjie.storage.StorageManager
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.PrimitiveTypeConstructor
import org.cangnova.cangjie.types.TypeConstructor
import org.cangnova.cangjie.types.checker.CangJieTypeRefiner

/**
 * 基本类型类描述符
 *
 * 专门为基本类型（Int32、Int64、Float64 等）设计的类描述符
 * 提供优化的成员解析和操作符支持
 */
class PrimitiveClassDescriptor(
    private val primitiveType: PrimitiveType,
    private val storageManager: StorageManager,
    containingModule: ModuleDescriptor,
    private val builtIns: CangJieBuiltIns
) : AbstractClassDescriptor(storageManager, primitiveType.typeName) {

    private val primitiveMemberScope by lazy {
        PrimitiveMemberScope(primitiveType, this, storageManager)
    }

    // 基本属性
    override val containingDeclaration: DeclarationDescriptor = containingModule
    override val annotations: Annotations = Annotations.EMPTY
    override val source: SourceElement = SourceElement.NO_SOURCE

    // 类的基本特征
    override val kind: ClassKind = ClassKind.BASIC
    override val modality: Modality = Modality.FINAL
    override val visibility: DescriptorVisibility = DescriptorVisibilities.PUBLIC

    // 类型参数和接收者
    override val declaredTypeParameters: List<TypeParameterDescriptor> = emptyList()
    override val contextReceivers: List<ReceiverParameterDescriptor> = emptyList()

    // 作用域
    override val staticScope: MemberScope = MemberScope.Empty

    // 构造函数相关
    override val constructors: Collection<ClassConstructorDescriptor> by lazy { createConstructors() }
    override val endConstructors: Collection<ClassConstructorDescriptor> = emptySet()
    override val unsubstitutedPrimaryConstructor: ClassConstructorDescriptor? = null

    // 类型构造器
    override val typeConstructor: TypeConstructor by lazy {
        PrimitiveTypeConstructor(primitiveType, this, storageManager, builtIns)
    }

    /**
     * 获取未替换的成员作用域（带类型精细器）
     */
    override fun getUnsubstitutedMemberScope(cangjieTypeRefiner: CangJieTypeRefiner): MemberScope {
        return primitiveMemberScope
    }

    /**
     * 获取密封子类（基本类型没有子类）
     */
    override val sealedSubclasses: Collection<ClassDescriptor>
        get() = emptyList()

    /**
     * 获取声明的可调用成员
     */
    fun getDeclaredCallableMembers(): List<CallableMemberDescriptor> {
        return primitiveMemberScope.getAllFunctions()
    }

    /**
     * 创建构造函数
     *
     * 为基本类型创建类型转换构造函数
     */
    private fun createConstructors(): Collection<ClassConstructorDescriptor> {
        val result = mutableSetOf<ClassConstructorDescriptor>()

        // 数值类型之间可以相互转换
        if (primitiveType.isNumeric()) {
            // 获取所有其他数值类型
            val numericTypes = PrimitiveType.values().filter {
                it.isNumeric() && it != primitiveType
            }

            for (sourceType in numericTypes) {
                result.add(createConversionConstructor(sourceType))
            }
        }

        return result
    }

    /**
     * 创建类型转换构造函数
     */
    private fun createConversionConstructor(sourceType: PrimitiveType): ClassConstructorDescriptor {
        return object : ClassConstructorDescriptorImpl(
            this@PrimitiveClassDescriptor,
            null,
            Annotations.EMPTY,
            false,
            CallableMemberDescriptor.Kind.DECLARATION,
            SourceElement.NO_SOURCE
        ) {
            init {
                val parameter = ValueParameterDescriptorImpl.createWithDestructuringDeclarations(
                    this, null, 0, Annotations.EMPTY, org.cangnova.cangjie.name.Name.identifier("value"),
                    false, createPrimitiveType(sourceType), false, SourceElement.NO_SOURCE, null
                )
                initialize(listOf(parameter))
                setReturnType(this@PrimitiveClassDescriptor.defaultType)
            }
        }
    }

    /**
     * 创建基本类型（辅助方法）
     */
    private fun createPrimitiveType(primitiveType: PrimitiveType): CangJieType {
        // 从 builtIns 获取对应的基本类型
        return builtIns.createPrimitiveClassDescriptor(primitiveType).defaultType
    }


    override fun toString(): String = "PrimitiveClassDescriptor(${primitiveType.typeName})"
}