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

import com.github.weisj.jsvg.B
import org.cangnova.cangjie.builtins.BuiltinsType
import org.cangnova.cangjie.builtins.BuiltinsType.*
import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.builtins.PrimitiveType
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.scopes.BuiltInsMemberScope
import org.cangnova.cangjie.resolve.scopes.MemberScope
import org.cangnova.cangjie.resolve.scopes.PrimitiveMemberScope
import org.cangnova.cangjie.storage.StorageManager
import org.cangnova.cangjie.types.BuiltInsTypeConstructor
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.PrimitiveTypeConstructor
import org.cangnova.cangjie.types.TypeConstructor
import org.cangnova.cangjie.types.Variance
import org.cangnova.cangjie.types.checker.CangJieTypeRefiner

/**
 * 内置类型类描述符
 *
 * 提供优化的成员解析和操作符支持
 */
class BuiltinsClassDescriptor(
    private val builtinsType: BuiltinsType,
    private val storageManager: StorageManager,
    containingModule: ModuleDescriptor,
    private val builtIns: CangJieBuiltIns
) : AbstractClassDescriptor(storageManager, builtinsType.typeName) {

    private val builtinsMemberScope by lazy {
        BuiltInsMemberScope(builtinsType, this, storageManager)
    }

    // 基本属性
    override val containingDeclaration: DeclarationDescriptor = containingModule
    override val annotations: Annotations = Annotations.EMPTY
    override val source: SourceElement = SourceElement.NO_SOURCE

    // 类的基本特征
    override val kind: ClassKind = ClassKind.BUILTIN
    override val modality: Modality = Modality.FINAL
    override val visibility: DescriptorVisibility = DescriptorVisibilities.PUBLIC

    // 类型参数和接收者
    override val declaredTypeParameters: List<TypeParameterDescriptor>
        get() {
            return when (builtinsType) {
                CPOINTER -> {
                    listOf(
                        TypeParameterDescriptorImpl.createForFurtherModification(
                            this,
                            Annotations.EMPTY,
                            Variance.INVARIANT,
                            Name.identifier("T"),
                            1,
                            SourceElement.NO_SOURCE,
                            storageManager,
                        ).apply {
                            addUpperBound(builtIns.ctypeType)
                        }
                    )
                }


                else -> emptyList()
            }


        }
    override val contextReceivers: List<ReceiverParameterDescriptor> = emptyList()

    // 作用域
    override val staticScope: MemberScope = MemberScope.Empty

    // 构造函数相关
    override val constructors: Collection<ClassConstructorDescriptor> by lazy { createConstructors() }
    override val endConstructors: Collection<ClassConstructorDescriptor> = emptySet()
    override val unsubstitutedPrimaryConstructor: ClassConstructorDescriptor? = null

    // 类型构造器
    override val typeConstructor: TypeConstructor by lazy {
        BuiltInsTypeConstructor(builtinsType, this, storageManager, builtIns)
    }

    /**
     * 获取未替换的成员作用域（带类型精细器）
     */
    override fun getUnsubstitutedMemberScope(cangjieTypeRefiner: CangJieTypeRefiner): MemberScope {
        return builtinsMemberScope
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
        return builtinsMemberScope.getAllFunctions()
    }

    /**
     * 创建基本类型（辅助方法）
     */
    private fun createBuiltinsType(builtinsType: BuiltinsType): CangJieType {
        // 从 builtIns 获取对应的基本类型
        return builtIns.createBuiltInsClassDescriptor(builtinsType).defaultType
    }
    private fun getCStringConstructor():List<ClassConstructorDescriptor>{

        val result = mutableListOf<ClassConstructorDescriptor>()
        result.add(
            object : ClassConstructorDescriptorImpl(
                this@BuiltinsClassDescriptor,
                null,
                Annotations.EMPTY,
                false,
                CallableMemberDescriptor.Kind.DECLARATION,
                SourceElement.NO_SOURCE
            ) {
                init {
                    val parameter = ValueParameterDescriptorImpl.createWithDestructuringDeclarations(
                        this, null, 0, Annotations.EMPTY, Name.identifier("pointer"),
                        false, createBuiltinsType(BuiltinsType.CPOINTER), false, SourceElement.NO_SOURCE, null
                    )
                    initialize(listOf(parameter))
                    setReturnType(this@BuiltinsClassDescriptor.defaultType)
                }
            }
        )


        return result
    }
    private fun getCPointerConstructor(): List<ClassConstructorDescriptor> {

        val result = mutableListOf<ClassConstructorDescriptor>()
        result.add(
            object : ClassConstructorDescriptorImpl(
                this@BuiltinsClassDescriptor,
                null,
                Annotations.EMPTY,
                false,
                CallableMemberDescriptor.Kind.DECLARATION,
                SourceElement.NO_SOURCE
            ) {
                init {
                    val parameter = ValueParameterDescriptorImpl.createWithDestructuringDeclarations(
                        this, null, 0, Annotations.EMPTY, Name.identifier("pointer"),
                        false, createBuiltinsType(BuiltinsType.CPOINTER), false, SourceElement.NO_SOURCE, null
                    )
                    initialize(listOf(parameter))
                    setReturnType(this@BuiltinsClassDescriptor.defaultType)
                }
            }
        )


        return result
    }

    /**
     * 创建构造函数
     *
     */
    private fun createConstructors(): Collection<ClassConstructorDescriptor> {
        val result = mutableSetOf<ClassConstructorDescriptor>()

        when (builtinsType) {
            CPOINTER -> {
                getCPointerConstructor().forEach { result.add(it) }
            }

            CSTRING -> TODO()

        }



        return result
    }


    override fun toString(): String = "BuiltInsClassDescriptor(${builtinsType.typeName})"
}