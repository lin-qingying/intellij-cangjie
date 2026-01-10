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

package org.cangnova.cangjie.types

import com.google.common.collect.LinkedHashMultimap
import com.google.common.collect.Multimap
import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.descriptors.TypeParameterDescriptor

/**
 * 类型替换工具
 *
 * 提供类型参数替换相关的工具函数，主要用于泛型类型的实例化和类型推断。
 *
 * 类型替换是泛型编程的核心操作，它将泛型类型参数替换为具体的类型实参。
 * 例如：List<T> 替换为 List<String>，其中 T -> String
 *
 * 此工具支持深度替换，即递归地处理类型层次结构中的所有类型参数。
 */
object SubstitutionUtils {
    /**
     * 填充深度替换器
     *
     * 递归地遍历类型层次结构，为每个类型参数建立到其实参的映射。
     * 此方法会沿着继承链向上遍历，收集所有超类型中的类型参数映射。
     *
     * 工作原理：
     * 1. 获取当前类型的类型参数和类型实参
     * 2. 建立参数到实参的映射
     * 3. 递归处理所有超类型，使用替换器转换它们的类型参数
     * 4. 如果遇到 Nothing 类型则停止递归（Nothing 没有超类型）
     *
     * 注意：此方法利用了 substitution 映射的可变性，在递归过程中不断更新映射
     *
     * @param context 当前要处理的类型
     * @param substitutor 类型替换器，用于替换类型参数
     * @param substitution 类型构造器到类型投影的映射（可变，会被更新）
     * @param typeParameterMapping 类型参数到类型投影的多值映射（可选，用于跟踪所有映射）
     */
    private fun fillInDeepSubstitutor(
        context: CangJieType,
        substitutor: DefaultTypeSubstitutor,
        substitution: MutableMap<TypeConstructor, TypeArgument>,
        typeParameterMapping: Multimap<TypeParameterDescriptor, TypeArgument>?
    ) {
        // 获取类型构造器的类型参数列表
        val parameters: List<TypeParameterDescriptor> =
            context.constructor.parameters
        // 获取类型的类型实参列表
        val arguments: List<TypeArgument> = context.arguments

        // 类型参数和类型实参数量必须匹配
        check(parameters.size == arguments.size)

        // 为每个类型参数建立到类型实参的映射
        for (i in arguments.indices) {
            val argument: TypeArgument = arguments[i]
            val parameter: TypeParameterDescriptor = parameters[i]

            // 使用替换器替换类型实参（处理嵌套的类型参数）
            val substitute: TypeArgument = checkNotNull(substitutor.substitute(argument))
            // 将映射添加到替换映射中
            substitution[parameter.typeConstructor] = substitute
            // 如果提供了类型参数映射，也记录在其中
            typeParameterMapping?.put(parameter, substitute)
        }

        // Nothing 类型没有超类型，停止递归
        if (CangJieBuiltIns.isNothing(context)) return

        // 递归处理所有超类型
        for (supertype in context.constructor.supertypes) {
            fillInDeepSubstitutor(supertype, substitutor, substitution, typeParameterMapping)
        }
    }

    /**
     * 构建深度替换多值映射
     *
     * 为给定类型及其所有超类型中的类型参数建立到类型实参的映射。
     * 这个映射反映了整个类型层次结构中的类型参数实例化情况。
     *
     * 使用场景：
     * - 类型推断：确定泛型方法调用时的类型参数
     * - 类型检查：验证类型兼容性时需要知道类型参数的具体实例
     * - 成员解析：在泛型类中查找成员时需要替换类型参数
     *
     * 示例说明（伪代码）：
     * ```
     * trait Iterable<T>
     * trait Collection<E>: Iterable<E>
     * trait MyFooCollection<F>: Collection<Foo<F>>
     *
     * 对于类型 MyFooCollection<out CharSequence>，返回的映射为：
     * - T (定义在 Iterable 中) -> Foo<out CharSequence>
     * - E (定义在 Collection 中) -> Foo<out CharSequence>
     * - F (定义在 MyFooCollection 中) -> out CharSequence
     * ```
     *
     * 多值映射的原因：
     * 在复杂的类型层次结构中，同一个类型参数可能通过不同的继承路径
     * 得到不同的实例化结果，因此需要使用多值映射来记录所有可能的映射。
     *
     * @param type 要分析的类型
     * @return 类型参数到其所有可能的类型实参的多值映射
     */
    fun buildDeepSubstitutionMultimap(type: CangJieType): Multimap<TypeParameterDescriptor, TypeArgument> {
        // 创建多值映射，用于存储最终结果
        val fullSubstitution: Multimap<TypeParameterDescriptor, TypeArgument> =
            LinkedHashMultimap.create<TypeParameterDescriptor, TypeArgument>()

        // 创建替换映射，用于在递归过程中累积类型参数映射
        val substitution =
            HashMap<TypeConstructor, TypeArgument>()

        // 创建类型替换器，基于上面的可变映射
        val typeSubstitutor: DefaultTypeSubstitutor =
            DefaultTypeSubstitutor.create(substitution)

        // 递归地填充替换映射
        // 注意：这里利用了 substitution 映射的可变性，
        // 在递归过程中会不断更新映射内容
        fillInDeepSubstitutor(
            type,
            typeSubstitutor,
            substitution,
            fullSubstitution
        )

        return fullSubstitution
    }

}
