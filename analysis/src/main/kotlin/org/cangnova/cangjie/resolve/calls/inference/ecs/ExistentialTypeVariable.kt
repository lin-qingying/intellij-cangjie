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

package org.cangnova.cangjie.resolve.calls.inference.ecs

import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.descriptors.ClassifierDescriptor
import org.cangnova.cangjie.descriptors.TypeParameterDescriptor
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.CangJieTypeFactory
import org.cangnova.cangjie.types.SimpleType
import org.cangnova.cangjie.types.TypeAttributes
import org.cangnova.cangjie.types.TypeConstructor
import org.cangnova.cangjie.types.checker.CangJieTypeRefiner
import org.cangnova.cangjie.types.model.TypeParameterMarker
import org.cangnova.cangjie.types.model.TypeVariableMarker
import org.cangnova.cangjie.types.model.TypeVariableTypeConstructorMarker

/**
 * ECS 专用的类型变量类型构造器
 *
 * 轻量级实现，专门用于存在性约束判断。
 * 与完整的 [TypeVariableTypeConstructor] 不同，这个实现：
 * - 不追踪方差位置信息
 * - 不支持延迟推导
 * - 不关联到具体的声明位置
 *
 * @property builtIns 内置类型系统
 * @property name 类型变量名称（用于调试）
 * @property originalTypeParameter 原始类型参数（如果有）
 */
class ExistentialTypeVariableConstructor(
    override val builtIns: CangJieBuiltIns,
    val name: String,
    val originalTypeParameter: TypeParameterDescriptor?
) : TypeConstructor, TypeVariableTypeConstructorMarker {

    override val supertypes: Collection<CangJieType>
        get() = emptyList()

    override val parameters: List<TypeParameterDescriptor>
        get() = emptyList()

    override val isFinal: Boolean
        get() = false

    override val isDenotable: Boolean
        get() = false

    override val declarationDescriptor: ClassifierDescriptor?
        get() = null

    override fun refine(cangjieTypeRefiner: CangJieTypeRefiner): TypeConstructor = this

    override fun toString(): String = "∃$name"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ExistentialTypeVariableConstructor) return false
        // 使用引用相等，每个实例都是唯一的
        return false
    }

    override fun hashCode(): Int = System.identityHashCode(this)
}

/**
 * ECS 专用的类型变量
 *
 * 轻量级类型变量，只用于存在性判断。
 * 特点：
 * - 无状态，创建后不可变
 * - 不支持 OnlyInputTypes 等高级特性
 * - 通过 [freshTypeConstructor] 唯一标识
 *
 * ## 使用示例
 *
 * ```kotlin
 * // 从类型参数创建
 * val typeVar = ExistentialTypeVariable.fromTypeParameter(typeParam, builtIns)
 *
 * // 获取默认类型用于约束构建
 * val type = typeVar.defaultType
 * ```
 *
 * @property freshTypeConstructor 类型构造器，唯一标识此类型变量
 * @property defaultType 默认类型，用于在约束中引用此类型变量
 */
class ExistentialTypeVariable private constructor(
    val freshTypeConstructor: ExistentialTypeVariableConstructor
) : TypeVariableMarker {

    /**
     * 类型变量的默认类型表示
     */
    val defaultType: SimpleType = CangJieTypeFactory.simpleTypeWithNonTrivialMemberScope(
        TypeAttributes.Empty,
        freshTypeConstructor,
        emptyList(),
        false,
        freshTypeConstructor.builtIns.stdlibTypes.any.unsubstitutedMemberScope
    )

    /**
     * 获取原始类型参数
     */
    val originalTypeParameter: TypeParameterDescriptor?
        get() = freshTypeConstructor.originalTypeParameter

    /**
     * 类型变量名称
     */
    val name: String
        get() = freshTypeConstructor.name

    override fun toString(): String = freshTypeConstructor.toString()

    companion object {
        /**
         * 从类型参数描述符创建存在性类型变量
         *
         * @param typeParameter 原始类型参数
         * @param builtIns 内置类型系统
         * @return 新创建的存在性类型变量
         */
        fun fromTypeParameter(
            typeParameter: TypeParameterDescriptor,
            builtIns: CangJieBuiltIns
        ): ExistentialTypeVariable {
            val constructor = ExistentialTypeVariableConstructor(
                builtIns,
                typeParameter.name.asString(),
                typeParameter
            )
            return ExistentialTypeVariable(constructor)
        }

        /**
         * 从类型参数标记创建存在性类型变量
         *
         * @param typeParameter 类型参数标记
         * @param builtIns 内置类型系统
         * @return 新创建的存在性类型变量
         */
        fun fromTypeParameterMarker(
            typeParameter: TypeParameterMarker,
            builtIns: CangJieBuiltIns
        ): ExistentialTypeVariable {
            val name = when (typeParameter) {
                is TypeParameterDescriptor -> typeParameter.name.asString()
                else -> "T${typeParameter.hashCode()}"
            }
            val originalParam = typeParameter as? TypeParameterDescriptor
            val constructor = ExistentialTypeVariableConstructor(builtIns, name, originalParam)
            return ExistentialTypeVariable(constructor)
        }

        /**
         * 创建匿名类型变量
         *
         * @param name 变量名称
         * @param builtIns 内置类型系统
         * @return 新创建的匿名存在性类型变量
         */
        fun anonymous(name: String, builtIns: CangJieBuiltIns): ExistentialTypeVariable {
            val constructor = ExistentialTypeVariableConstructor(builtIns, name, null)
            return ExistentialTypeVariable(constructor)
        }
    }
}

/**
 * 类型变量注册表
 *
 * 管理 ECS 中所有注册的类型变量，提供查找和替换功能。
 */
class ExistentialTypeVariableRegistry(
    private val builtIns: CangJieBuiltIns
) {
    /** 类型构造器到类型变量的映射 */
    private val variablesByConstructor = mutableMapOf<TypeConstructor, ExistentialTypeVariable>()

    /** 原始类型参数到类型变量的映射 */
    private val variablesByParameter = mutableMapOf<TypeParameterDescriptor, ExistentialTypeVariable>()

    /**
     * 注册类型参数，返回对应的存在性类型变量
     *
     * 如果该类型参数已注册，返回已有的类型变量。
     */
    fun registerTypeParameter(typeParameter: TypeParameterDescriptor): ExistentialTypeVariable {
        return variablesByParameter.getOrPut(typeParameter) {
            val variable = ExistentialTypeVariable.fromTypeParameter(typeParameter, builtIns)
            variablesByConstructor[variable.freshTypeConstructor] = variable
            variable
        }
    }

    /**
     * 批量注册类型参数
     */
    fun registerTypeParameters(typeParameters: Collection<TypeParameterDescriptor>): List<ExistentialTypeVariable> {
        return typeParameters.map { registerTypeParameter(it) }
    }

    /**
     * 检查类型构造器是否是已注册的类型变量
     */
    fun isTypeVariable(typeConstructor: TypeConstructor): Boolean {
        return typeConstructor in variablesByConstructor
    }

    /**
     * 获取类型构造器对应的类型变量
     */
    fun getTypeVariable(typeConstructor: TypeConstructor): ExistentialTypeVariable? {
        return variablesByConstructor[typeConstructor]
    }

    /**
     * 获取所有已注册的类型变量
     */
    fun getAllVariables(): Collection<ExistentialTypeVariable> {
        return variablesByConstructor.values
    }

    /**
     * 清空注册表
     */
    fun clear() {
        variablesByConstructor.clear()
        variablesByParameter.clear()
    }
}
