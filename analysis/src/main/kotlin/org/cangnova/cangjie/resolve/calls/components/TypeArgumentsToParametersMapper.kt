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

package org.cangnova.cangjie.resolve.calls.components

import org.cangnova.cangjie.descriptors.CallableDescriptor
import org.cangnova.cangjie.descriptors.ClassAndEnumDescriptor
import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.descriptors.EnumConstructorDescriptor
import org.cangnova.cangjie.descriptors.TypeParameterDescriptor
import org.cangnova.cangjie.resolve.calls.model.*
import org.cangnova.cangjie.types.CangJieType

/**
 * 类型参数到类型参数映射器
 *
 * 负责将函数调用中显式指定的类型参数映射到函数声明的类型参数。
 * 这是泛型函数调用解析的关键组件，处理类型参数的对应关系和验证。
 *
 * 主要功能：
 * 1. 处理显式类型参数（如 `func<Int, String>()`）
 * 2. 处理嵌套泛型（如枚举构造器的共享类型参数）
 * 3. 验证类型参数数量的匹配
 * 4. 收集类型参数相关的诊断信息
 *
 * 类型参数来源：
 * - **调用级类型参数**：直接在调用点指定的类型参数
 * - **顶层类型参数**：来自外层声明（如类或枚举）的类型参数
 * - **共享类型参数**：在多个声明中共享的类型参数（如枚举的类型参数）
 *
 * 示例：
 * ```
 * // 简单泛型调用
 * func<Int>()  // 类型参数 Int 映射到函数的类型参数 T
 *
 * // 枚举构造器的共享类型参数
 * enum Option<T> {
 *     Some(T)  // 构造器共享枚举的类型参数 T
 * }
 * ```
 */
class TypeArgumentsToParametersMapper {

    /**
     * 类型参数映射结果
     *
     * 封装类型参数映射的结果，包括映射关系和诊断信息。
     * 这是一个密封类，有两种实现：
     * - **NoExplicitArguments**：没有显式类型参数
     * - **TypeArgumentsMappingImpl**：有显式类型参数的完整映射
     *
     * 实现了 Iterable 接口，允许遍历所有映射的类型参数对。
     *
     * @property diagnostics 映射过程中产生的诊断信息列表
     */
    sealed class TypeArgumentsMapping(val diagnostics: List<CangJieCallDiagnostic>) :
        Iterable<Map.Entry<TypeParameterDescriptor, CangJieType?>> {

        /**
         * 获取类型参数对应的类型实参
         *
         * @param typeParameterDescriptor 类型参数描述符
         * @return 对应的类型实参，如果没有则返回占位符
         */
        abstract fun getTypeArgument(typeParameterDescriptor: TypeParameterDescriptor): TypeArgument

        /**
         * 无显式类型参数的映射
         *
         * 当函数调用没有显式指定类型参数时使用。
         * 所有类型参数查询都返回占位符，表示需要类型推断。
         *
         * 示例：
         * ```
         * func()  // 没有显式类型参数，使用类型推断
         * ```
         */
        object NoExplicitArguments : TypeArgumentsMapping(emptyList()) {
            /** 空迭代器，因为没有类型参数映射 */
            private val emptyIterator = mapOf<Nothing, Nothing>().iterator()

            /**
             * 所有类型参数都返回占位符
             *
             * @param typeParameterDescriptor 类型参数描述符
             * @return 类型参数占位符
             */
            override fun getTypeArgument(typeParameterDescriptor: TypeParameterDescriptor): TypeArgument =
                TypeArgumentPlaceholder

            override fun iterator() = emptyIterator
        }

        /**
         * 类型参数映射的完整实现
         *
         * 包含从类型参数描述符到类型实参的完整映射表。
         * 用于处理有显式类型参数的函数调用。
         *
         * 示例：
         * ```
         * func<Int, String>()  // 创建映射 T -> Int, U -> String
         * ```
         *
         * @property diagnostics 映射过程中产生的诊断信息
         * @property typeParameterToArgumentMap 类型参数到类型实参的映射表
         */
        class TypeArgumentsMappingImpl(
            diagnostics: List<CangJieCallDiagnostic>,
            private val typeParameterToArgumentMap: Map<TypeParameterDescriptor, TypeArgument>
        ) : TypeArgumentsMapping(diagnostics) {
            /**
             * 获取类型参数对应的类型实参
             *
             * @param typeParameterDescriptor 类型参数描述符
             * @return 映射表中的类型实参，如果不存在则返回占位符
             */
            override fun getTypeArgument(typeParameterDescriptor: TypeParameterDescriptor): TypeArgument =
                typeParameterToArgumentMap[typeParameterDescriptor] ?: TypeArgumentPlaceholder

            /**
             * 迭代所有类型参数映射
             *
             * 只返回简单类型参数（过滤掉占位符和其他特殊类型参数）。
             *
             * @return 类型参数描述符到仓颉类型的迭代器
             */
            override fun iterator() =
                typeParameterToArgumentMap.mapValues { (it.value as? SimpleTypeArgument)?.type }.iterator()
        }
    }

    /**
     * 映射类型参数
     *
     * 这是类型参数映射的主要方法，负责将调用中的类型实参映射到可调用对象的类型参数。
     *
     * **映射流程**：
     * 1. **检查是否有显式类型参数**：
     *    - 如果没有任何显式类型参数，返回 NoExplicitArguments
     *
     * 2. **处理顶层类型参数**：
     *    - 获取包含声明（如类或枚举）的类型参数
     *    - 检测共享类型参数（如枚举构造器共享枚举的类型参数）
     *
     * 3. **验证类型参数数量**：
     *    - 检查提供的类型实参数量是否与类型参数数量匹配
     *    - 数量不匹配时产生诊断信息
     *
     * 4. **创建映射**：
     *    - 将调用级类型实参与函数类型参数配对
     *    - 将顶层类型实参与顶层类型参数配对
     *    - 合并两个映射表
     *
     * **枚举构造器的特殊处理**：
     * 枚举构造器可能共享枚举的类型参数，例如：
     * ```
     * enum Option<T> {
     *     Some(T)  // Some 构造器共享 Option 的类型参数 T
     * }
     * Option<Int>.Some(42)  // 顶层类型参数 Int 映射到 T
     * ```
     *
     * @param call 仓颉调用，包含类型实参信息
     * @param descriptor 可调用描述符，包含类型参数声明
     * @return 类型参数映射结果，包含映射表和诊断信息
     */
    fun mapTypeArguments(call: CangJieCall, descriptor: CallableDescriptor): TypeArgumentsMapping {

        // 1. 如果没有显式类型参数，直接返回
        if (call.typeArguments.isEmpty() && call.topTypeArguments.isEmpty()) {
            return TypeArgumentsMapping.NoExplicitArguments
        }

        // 2. 初始化诊断和映射表
        val typeDiagnostics = mutableListOf<CangJieCallDiagnostic>()
        var typeParameterToArgumentMap = emptyMap<TypeParameterDescriptor, TypeArgument>()

        // 3. 枚举构造器的特殊处理
        // 编译器设计：枚举构造器没有自己的类型参数，类型参数从枚举限定符传递
        // 例如：A<String>.A1("111") 中，类型参数 String 应该在 A 上，而不是 A1 上
        if (descriptor is EnumConstructorDescriptor) {
            // 3.1 如果类型参数出现在枚举构造器后面，并且有显式接收者，这是错误的
            // 例如：A.B<Int>、A.A1<String>("111")、A<Int>.B<Int>
            // 但是 EnumSugar 语法允许：None<Int>、Some<String>("hello")（无显式接收者）
            if (call.typeArguments.isNotEmpty() && call.explicitReceiver != null) {
                val enumDescriptor = descriptor.constructedClass
                return TypeArgumentsMapping.TypeArgumentsMappingImpl(
                    listOf(TypeArgumentsAfterEnumEntry(descriptor, enumDescriptor)), emptyMap()
                )
            }

            // 3.2 使用枚举的类型参数
            val enumDescriptor = descriptor.constructedClass
            val enumTypeParameters = enumDescriptor.declaredTypeParameters

            // 3.3 确定类型参数来源：
            // - EnumSugar（无显式接收者）：使用 typeArguments（如 None<Int>）
            // - 完整写法（有显式接收者）：使用 topTypeArguments（如 Option<Int>.None）
            val effectiveTypeArguments = if (call.explicitReceiver == null) {
                call.typeArguments  // EnumSugar: None<Int>
            } else {
                call.topTypeArguments  // 完整写法: Option<Int>.None
            }

            // 3.4 验证类型参数数量
            if (effectiveTypeArguments.isNotEmpty() && effectiveTypeArguments.size != enumTypeParameters.size) {
                return TypeArgumentsMapping.TypeArgumentsMappingImpl(
                    listOf(WrongCountOfTypeArguments(descriptor, effectiveTypeArguments.size)), emptyMap()
                )
            }

            // 3.5 创建枚举类型参数映射
            if (effectiveTypeArguments.isNotEmpty()) {
                typeParameterToArgumentMap = enumTypeParameters.zip(effectiveTypeArguments).associate { it }
            }

            return TypeArgumentsMapping.TypeArgumentsMappingImpl(typeDiagnostics, typeParameterToArgumentMap)
        }

        // 4. 获取上层声明（如类或枚举）
        val topDescriptor = descriptor.containingDeclaration as? ClassAndEnumDescriptor

        // 5. 验证类型参数数量
        if (call.typeArguments.size != descriptor.typeParameters.size) {
            return TypeArgumentsMapping.TypeArgumentsMappingImpl(
                listOf(WrongCountOfTypeArguments(descriptor, call.typeArguments.size)), emptyMap()
            )
        } else {
            // 6. 创建顶层类型参数映射（如类的类型参数）
            val topTypeParameterToArgumentMap =
                topDescriptor?.declaredTypeParameters?.zip(call.topTypeArguments)?.associate { it } ?: emptyMap()

            // 7. 创建完整映射：调用级类型参数 + 顶层类型参数
            typeParameterToArgumentMap =
                descriptor.typeParameters.zip(call.typeArguments).associate { it } + topTypeParameterToArgumentMap
        }

        return TypeArgumentsMapping.TypeArgumentsMappingImpl(typeDiagnostics, typeParameterToArgumentMap)
    }

}

/**
 * 获取共享的类型参数
 *
 * 在某些场景下，多个声明会共享相同的类型参数。
 * 最典型的例子是枚举构造器共享枚举的类型参数。
 *
 * **共享类型参数的识别**：
 * 一个类型参数如果在多个声明描述符中都出现，则被认为是共享的。
 * 通过分组统计每个类型参数的出现次数来识别。
 *
 * **枚举示例**：
 * ```
 * enum Option<T> {
 *     Some(T),    // 构造器参数 T 与枚举类型参数 T 相同（共享）
 *     None        // 无参数构造器
 * }
 * ```
 * 在这个例子中：
 * - 枚举 `Option` 声明了类型参数 `T`
 * - 构造器 `Some` 的参数类型也是 `T`
 * - 这两个 `T` 是同一个类型参数（共享的）
 *
 * **为什么需要识别共享类型参数**：
 * 1. 避免重复映射：同一个类型参数不应该被映射多次
 * 2. 类型一致性：确保共享的类型参数在所有使用位置都有相同的类型实参
 * 3. 诊断信息：当类型参数冲突时，能够识别是否为共享参数导致的
 *
 * @param descriptor 声明描述符数组，可能包含 null 元素
 * @return 共享的类型参数集合
 */
fun getSharedTypeParametersByDeclarationDescriptor(vararg descriptor: DeclarationDescriptor?): Set<TypeParameterDescriptor> {
    // 收集所有声明的类型参数
    return descriptor.filterNotNull().flatMap {
        when (it) {
            is ClassAndEnumDescriptor -> it.declaredTypeParameters  // 类和枚举的类型参数
            is CallableDescriptor -> it.typeParameters               // 函数的类型参数
            else -> emptyList()
        }
    }.groupBy { it }              // 按类型参数分组
        .filter { it.value.size > 1 }  // 过滤出现次数大于1的（共享的）
        .keys                     // 取共享类型参数的集合
}

/**
 * 获取所有类型参数
 *
 * 从给定的声明描述符中提取所有类型参数（包括不共享的）。
 * 与 `getSharedTypeParametersByDeclarationDescriptor` 不同，
 * 此方法返回所有类型参数而不仅仅是共享的。
 *
 * **类型参数来源**：
 * - **ClassAndEnumDescriptor**：类或枚举声明的类型参数（如 `class Box<T>`、`enum Option<T>`）
 * - **CallableDescriptor**：函数或属性的类型参数（如 `func foo<T>()`）
 *
 * **去重处理**：
 * 使用 `groupBy` 和 `keys` 确保每个类型参数只出现一次，
 * 即使它在多个声明中被共享。
 *
 * @param descriptor 声明描述符数组，可能包含 null 元素
 * @return 所有类型参数的集合（已去重）
 */
fun getAllTypeParameter(vararg descriptor: DeclarationDescriptor?): Set<TypeParameterDescriptor> {
    return descriptor.filterNotNull().flatMap {
        when (it) {
            is ClassAndEnumDescriptor -> it.declaredTypeParameters  // 类和枚举的类型参数
            is CallableDescriptor -> it.typeParameters               // 函数的类型参数
            else -> emptyList()
        }
    }.groupBy { it }  // 分组去重
        .keys          // 取所有唯一的类型参数
}
