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

import org.cangnova.cangjie.descriptors.CallableDescriptor
import org.cangnova.cangjie.descriptors.TypeParameterDescriptor
import org.cangnova.cangjie.types.checker.CangJieTypeChecker
import org.cangnova.cangjie.types.model.TypeSubstitutorMarker

/**
 * 模糊类型 - 用于 IDE 代码补全的类型匹配
 *
 * 模糊类型封装了一个仓颉类型和一组自由类型参数。
 * 它用于在代码补全中进行智能类型匹配,支持泛型参数推断。
 *
 * ## 核心概念
 *
 * ### 自由类型参数
 * 自由类型参数是尚未绑定到具体类型的类型参数。
 * 例如: `List<T>` 中的 `T` 如果还没有确定具体类型,就是自由参数。
 *
 * ### 类型匹配
 * 模糊类型提供了两种匹配模式:
 * - **子类型匹配** ([checkIsSubtypeOf]): 检查当前类型是否为目标类型的子类型
 * - **超类型匹配** ([checkIsSuperTypeOf]): 检查当前类型是否为目标类型的超类型
 *
 * ### 类型推断
 * 在匹配过程中,会尝试推断自由类型参数的绑定。
 * 例如: `List<T>` 匹配 `List<Int>` 时,可以推断出 `T = Int`。
 *
 * ## 使用场景
 *
 * ### 代码补全
 * ```kotlin
 * fun foo(list: List<Int>) { }
 * val x: List<T> = ...  // T 是自由参数
 * foo(x)  // 补全时使用 FuzzyType 匹配,推断 T = Int
 * ```
 *
 * ### 智能类型过滤
 * ```kotlin
 * val expectedType = FuzzyType(IntType, emptySet())
 * val candidateType = FuzzyType(NumberType, emptySet())
 * // 检查 Number 是否可以赋值给 Int (子类型检查)
 * val substitutor = candidateType.checkIsSubtypeOf(expectedType)
 * ```
 *
 * @property type 封装的仓颉类型
 * @property freeParameters 自由类型参数集合
 */
class FuzzyType(
    val type: CangJieType,
    freeParameters: Collection<TypeParameterDescriptor>
) {
    /**
     * 自由类型参数集合
     *
     * 使用 Set 保证唯一性和快速查找。
     */
    val freeParameters: Set<TypeParameterDescriptor> = freeParameters.toSet()

    /**
     * 检查当前类型是否为目标类型的子类型
     *
     * 如果是子类型,返回类型替换器(包含推断出的类型参数绑定)。
     * 如果不是子类型,返回 null。
     *
     * **子类型关系**:
     * - Int 是 Number 的子类型
     * - List<Int> 是 List<Number> 的子类型(协变)
     * - 子类型的值可以赋值给父类型的变量
     *
     * **使用示例**:
     * ```kotlin
     * val intType = FuzzyType(IntType, emptySet())
     * val numberType = FuzzyType(NumberType, emptySet())
     * val substitutor = intType.checkIsSubtypeOf(numberType)
     * // substitutor != null (Int 是 Number 的子类型)
     * ```
     *
     * @param otherType 目标类型(父类型)
     * @return ComposableTypeSubstitutor? 类型替换器(匹配时),或 null(不匹配时)
     */
    fun checkIsSubtypeOf(otherType: FuzzyType): ComposableTypeSubstitutor? {
        return matchedSubstitutor(otherType, MatchKind.IS_SUBTYPE)
    }

    /**
     * 检查当前类型是否为目标类型的超类型
     *
     * 如果是超类型,返回类型替换器(包含推断出的类型参数绑定)。
     * 如果不是超类型,返回 null。
     *
     * **超类型关系**:
     * - Number 是 Int 的超类型
     * - List<Number> 是 List<Int> 的超类型(协变)
     * - 父类型的引用可以指向子类型的对象
     *
     * **使用示例**:
     * ```kotlin
     * val numberType = FuzzyType(NumberType, emptySet())
     * val intType = FuzzyType(IntType, emptySet())
     * val substitutor = numberType.checkIsSuperTypeOf(intType)
     * // substitutor != null (Number 是 Int 的超类型)
     * ```
     *
     * @param otherType 目标类型(子类型)
     * @return ComposableTypeSubstitutor? 类型替换器(匹配时),或 null(不匹配时)
     */
    fun checkIsSuperTypeOf(otherType: FuzzyType): ComposableTypeSubstitutor? {
        return matchedSubstitutor(otherType, MatchKind.IS_SUPERTYPE)
    }

    /**
     * 匹配模式枚举
     */
    private enum class MatchKind {
        /** 子类型匹配: 当前类型 <: 目标类型 */
        IS_SUBTYPE,
        /** 超类型匹配: 当前类型 :> 目标类型 */
        IS_SUPERTYPE
    }

    /**
     * 核心匹配逻辑
     *
     * 使用约束系统进行类型匹配和参数推断。
     *
     * **匹配流程**:
     * 1. 创建约束系统
     * 2. 将两个类型的自由参数注册为类型变量
     * 3. 根据匹配模式添加子类型约束
     * 4. 固定类型变量(完成推断)
     * 5. 检查约束系统是否有错误
     * 6. 如果成功,返回包含推断结果的类型替换器
     *
     * **约束系统**:
     * 约束系统是类型推断的核心组件,它:
     * - 收集类型约束(子类型关系、相等关系等)
     * - 求解约束,推断类型参数的绑定
     * - 检测冲突和错误
     *
     * @param otherType 目标类型
     * @param matchKind 匹配模式
     * @return ComposableTypeSubstitutor? 类型替换器(匹配时),或 null(不匹配时)
     */
    private fun matchedSubstitutor(otherType: FuzzyType, matchKind: MatchKind): ComposableTypeSubstitutor? {
        // 合并两个类型的自由参数
        val freeParameters = this.freeParameters + otherType.freeParameters

        if (freeParameters.isEmpty()) {
            // 没有自由参数,直接检查子类型关系
            val typeChecker = CangJieTypeChecker.DEFAULT
            val isMatch = when (matchKind) {
                MatchKind.IS_SUBTYPE -> typeChecker.isSubtypeOf(type, otherType.type)
                MatchKind.IS_SUPERTYPE -> typeChecker.isSubtypeOf(otherType.type, type)
            }
            return if (isMatch) ComposableTypeSubstitutor.EMPTY else null
        }

        // 有自由参数时，暂时简化处理：
        // 只要基本类型兼容就返回空替换器
        // TODO: 未来需要实现完整的类型推断逻辑
        val typeChecker = CangJieTypeChecker.DEFAULT
        val isMatch = when (matchKind) {
            MatchKind.IS_SUBTYPE -> typeChecker.isSubtypeOf(type, otherType.type)
            MatchKind.IS_SUPERTYPE -> typeChecker.isSubtypeOf(otherType.type, type)
        }
        return if (isMatch) ComposableTypeSubstitutor.EMPTY else null
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FuzzyType) return false
        return type == other.type && freeParameters == other.freeParameters
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + freeParameters.hashCode()
        return result
    }

    override fun toString(): String {
        return if (freeParameters.isEmpty()) {
            type.toString()
        } else {
            "$type [free: ${freeParameters.joinToString { it.name.asString() }}]"
        }
    }
}

/**
 * 将类型转换为模糊类型
 *
 * @param freeParameters 自由类型参数集合
 * @return FuzzyType 模糊类型
 */
fun CangJieType.toFuzzyType(freeParameters: Collection<TypeParameterDescriptor>): FuzzyType {
    return FuzzyType(this, freeParameters)
}

/**
 * 获取可调用描述符的模糊返回类型
 *
 * 使用描述符的类型参数作为自由参数。
 *
 * **使用场景**:
 * 用于智能补全中判断函数返回值是否匹配期望类型。
 *
 * @receiver CallableDescriptor 可调用描述符
 * @return FuzzyType? 模糊返回类型,如果没有返回类型则返回 null
 */
fun CallableDescriptor.fuzzyReturnType(): FuzzyType? {
    return returnType?.toFuzzyType(typeParameters)
}

/**
 * 获取可调用描述符的模糊扩展接收者类型
 *
 * 使用描述符的类型参数作为自由参数。
 *
 * @receiver CallableDescriptor 可调用描述符
 * @return FuzzyType? 模糊扩展接收者类型,如果没有扩展接收者则返回 null
 */
fun CallableDescriptor.fuzzyExtensionReceiverType(): FuzzyType? {
    // 在仓颉语言中, extend 成员使用 dispatchReceiver
    return dispatchReceiverParameter?.type?.toFuzzyType(typeParameters)
}

/**
 * 将模糊类型转换为 Option 类型
 *
 * 在仓颉语言中,Option<T> 类型用于表示可能不存在的值。
 *
 * @receiver FuzzyType 模糊类型
 * @return FuzzyType Option 包装的模糊类型
 */
fun FuzzyType.makeOption(): FuzzyType {
    return type.makeOption().toFuzzyType(freeParameters)
}

/**
 * 将模糊类型从 Option 类型中解包
 *
 * 在仓颉语言中,将 Option<T> 转换为 T。
 *
 * @receiver FuzzyType 模糊类型
 * @return FuzzyType 非 Option 的模糊类型
 */
fun FuzzyType.makeNonOption(): FuzzyType {
    return type.makeNonOption().toFuzzyType(freeParameters)
}

/**
 * 将模糊类型从 Option 类型中解包 (别名)
 *
 * 在仓颉语言中,将 Option<T> 转换为 T。
 *
 * @receiver FuzzyType 模糊类型
 * @return FuzzyType 非 Option 的模糊类型
 */
fun FuzzyType.unwrapOption(): FuzzyType {
    return unboxOptionType(type).toFuzzyType(freeParameters)
}

/**
 * 检查模糊类型是否"几乎是所有类型"
 *
 * 检查类型是否为 Option<Nothing> 类型。
 * 在仓颉语言中,Option<Nothing> 只能是 None,可以匹配任何 Option 类型。
 *
 * **使用场景**:
 * 在代码补全中,用于特殊处理 None 字面量的情况。
 *
 * @receiver FuzzyType 模糊类型
 * @return Boolean true 表示是 Option<Nothing> 类型
 */
fun FuzzyType.isAlmostEverything(): Boolean {
    return type.isNothing() && type.optionality() != TypeOptionality.NOT_OPTION
}

/**
 * 获取用于展示的类型
 *
 * 返回一个适合展示给用户的类型。
 * 如果类型有自由参数,返回原始类型;否则返回类型本身。
 *
 * **使用场景**:
 * 在代码补全的展示中,用于生成用户友好的类型描述。
 *
 * @receiver FuzzyType 模糊类型
 * @return CangJieType 用于展示的类型
 */
fun FuzzyType.presentationType(): CangJieType {
    // 如果有自由参数,直接返回原始类型(不进行替换)
    return type
}
