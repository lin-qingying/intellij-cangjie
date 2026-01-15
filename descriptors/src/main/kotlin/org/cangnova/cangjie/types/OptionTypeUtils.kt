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

package org.cangnova.cangjie.types

import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.descriptors.ClassifierDescriptor
import org.cangnova.cangjie.descriptors.EnumDescriptor
import org.cangnova.cangjie.resolve.DescriptorUtils

/**
 * Option 类型工具类
 *
 * 提供 Option 类型的检查和构造方法。
 *
 * ## 重要说明
 *
 * 在仓颉语言中，Option<T> 是一个标准的泛型 enum 类型，定义如下：
 *
 * ```cangjie
 * enum Option<T> {
 *     | Some(T)
 *     | None
 * }
 * ```
 *
 * **关键点**：
 * - `Option<T>` 是一个独立的泛型类型，有自己的 TypeConstructor
 * - `?T` 只是 `Option<T>` 的语法糖
 * - `T` 和 `Option<T>` 是完全不同的类型，**没有子类型关系**
 * - 从 `T` 到 `Option<T>` 的转换是**隐式转换**（通过 Some 构造器），不是子类型关系
 *
 * ## 正确的设计
 *
 * ```kotlin
 * // ✅ 正确：Option 是一个具体的泛型类型
 * fun isOptionType(type: CangJieType): Boolean  // 检查是否是 Option 类型
 * fun createOptionType(elementType: CangJieType): CangJieType  // 构造 Option<T>
 * fun unwrapOptionType(type: CangJieType): CangJieType?  // 提取 T
 * ```
 *
 * ## 使用示例
 *
 * ```kotlin
 * val builtIns = module.builtIns
 * val int64Type = builtIns.int64Type
 *
 * // 构造 Option<Int64>
 * val optionInt64 = OptionTypeUtils.createOptionType(int64Type, builtIns)
 *
 * // 检查是否是 Option 类型
 * OptionTypeUtils.isOptionType(optionInt64)  // true
 * OptionTypeUtils.isOptionType(int64Type)  // false
 *
 * // 提取元素类型
 * OptionTypeUtils.unwrapOptionType(optionInt64)  // Int64
 * OptionTypeUtils.unwrapOptionType(int64Type)  // null
 * ```
 */
object OptionTypeUtils {

    /**
     * 检查类型是否是 Option 类型
     *
     * 通过检查类型的 TypeConstructor 是否对应 std.core.Option enum 来判断。
     *
     * **注意**：这是正确的判断方式，而不是检查某个 `isOption` 属性。
     *
     * @param type 要检查的类型
     * @return 如果类型是 Option<T> 则返回 true
     */
    @JvmStatic
    fun isOptionType(type: CangJieType): Boolean {
        val unwrapped = type.unwrap()
        if (unwrapped !is SimpleType) return false

        val descriptor = unwrapped.constructor.declarationDescriptor ?: return false
        return isOptionDescriptor(descriptor)
    }

    /**
     * 检查 ClassifierDescriptor 是否是 Option enum
     */
    @JvmStatic
    fun isOptionDescriptor(descriptor: ClassifierDescriptor): Boolean {
        if (descriptor !is EnumDescriptor) return false
        val fqName = DescriptorUtils.getFqName(descriptor)
        return fqName == StandardNames.FqNames.optionUFqName
    }

    /**
     * 构造 Option<T> 类型
     *
     * 使用标准的泛型类型构造方式创建 Option<T>。
     *
     * @param elementType Option 的元素类型 T
     * @param builtIns 内置类型提供者
     * @return Option<T> 类型
     */
    @JvmStatic
    fun createOptionType(elementType: CangJieType, builtIns: CangJieBuiltIns): SimpleType {
        val optionDescriptor = builtIns.stdlibTypes.option

        return CangJieTypeFactory.simpleType(
            attributes = TypeAttributes.Empty,
            constructor = optionDescriptor.typeConstructor,
            arguments = listOf(TypeArgumentImpl(elementType))
        )
    }

    /**
     * 从 Option<T> 中提取元素类型 T
     *
     * 如果类型不是 Option 类型，返回 null。
     *
     * @param type Option 类型
     * @return 元素类型 T，如果不是 Option 类型则返回 null
     */
    @JvmStatic
    fun unwrapOptionType(type: CangJieType): CangJieType? {
        if (!isOptionType(type)) return null

        val unwrapped = type.unwrap()
        if (unwrapped !is SimpleType) return null

        return unwrapped.arguments.firstOrNull()?.type
    }

    /**
     * 获取 Option 类型的嵌套层级
     *
     * 例如：
     * - `Int64` -> 0
     * - `Option<Int64>` -> 1
     * - `Option<Option<Int64>>` -> 2
     *
     * @param type 要检查的类型
     * @return 嵌套层级
     */
    @JvmStatic
    fun getOptionNestedLevel(type: CangJieType): Int {
        if (!isOptionType(type)) return 0

        val elementType = unwrapOptionType(type) ?: return 0
        return 1 + getOptionNestedLevel(elementType)
    }

    /**
     * 完全解包 Option 类型，移除所有 Option 包装
     *
     * 例如：
     * - `Int64` -> `Int64`
     * - `Option<Int64>` -> `Int64`
     * - `Option<Option<Int64>>` -> `Int64`
     *
     * @param type 要解包的类型
     * @return 完全解包后的类型
     */
    @JvmStatic
    fun fullyUnwrapOptionType(type: CangJieType): CangJieType {
        var current = type
        while (isOptionType(current)) {
            current = unwrapOptionType(current) ?: break
        }
        return current
    }

    /**
     * 检查是否可以从 source 隐式转换为 target
     *
     * 在仓颉语言中，`T` 可以隐式转换为 `Option<T>`（通过 Some 构造器），
     * 但这**不是子类型关系**。
     *
     * 示例：
     * ```kotlin
     * canImplicitlyConvert(Int64, Option<Int64>)  // true（隐式转换）
     * canImplicitlyConvert(Option<Int64>, Int64)  // false
     * ```
     *
     * @param source 源类型
     * @param target 目标类型
     * @return 如果可以隐式转换则返回 true
     */
    @JvmStatic
    fun canImplicitlyConvertToOption(source: CangJieType, target: CangJieType): Boolean {
        // 检查是否是 T -> Option<T> 的隐式转换
        if (isOptionType(target) && !isOptionType(source)) {
            val targetElementType = unwrapOptionType(target) ?: return false
            // 检查 source 是否等于 targetElementType
            return TypeUtils.equalTypes(source, targetElementType)
        }

        return false
    }

    /**
     * 根据需要将类型包装为 Option 类型
     *
     * 如果 shouldBeOption 为 true 且类型不是 Option 类型，则包装为 Option<T>。
     * 否则返回原类型。
     *
     * @param type 要处理的类型
     * @param shouldBeOption 是否应该是 Option 类型
     * @return 处理后的类型
     */
    @JvmStatic
    fun makeOptionalIfNeeded(type: CangJieType, shouldBeOption: Boolean): CangJieType {
        if (!shouldBeOption) return type
        if (isOptionType(type)) return type
        return createOptionType(type, type.builtIns)
    }

    /**
     * SimpleType 版本的 makeOptionalIfNeeded
     */
    @JvmStatic
    fun makeOptionalIfNeeded(type: SimpleType, shouldBeOption: Boolean): SimpleType {
        if (!shouldBeOption) return type
        if (isOptionType(type)) return type
        return createOptionType(type, type.builtIns)
    }




}

/**
 * 扩展函数：检查类型是否是 Option 类型
 */
fun CangJieType.isOptionType(): Boolean = OptionTypeUtils.isOptionType(this)

/**
 * 扩展函数：从 Option<T> 中提取元素类型 T
 */
fun CangJieType.unwrapOptionType(): CangJieType? = OptionTypeUtils.unwrapOptionType(this)

/**
 * 扩展函数：获取 Option 嵌套层级
 */
fun CangJieType.optionNestedLevel(): Int = OptionTypeUtils.getOptionNestedLevel(this)

/**
 * 扩展函数：完全解包 Option 类型
 */
fun CangJieType.fullyUnwrapOption(): CangJieType = OptionTypeUtils.fullyUnwrapOptionType(this)

/**
 * 扩展函数：将类型包装为 Option<T>
 */
fun CangJieType.makeOption(): CangJieType {
    if (this.isOptionType()) return this
    return OptionTypeUtils.createOptionType(this, this.builtIns)
}

/**
 * 扩展函数：从 Option<T> 中解包为 T
 */
fun CangJieType.makeNonOption(): CangJieType {
    return OptionTypeUtils.unwrapOptionType(this) ?: this
}

/**
 * 扩展函数：根据需要将类型包装为 Option 类型
 */
fun CangJieType.makeOptionalAsSpecified(shouldBeOption: Boolean): CangJieType {
    return OptionTypeUtils.makeOptionalIfNeeded(this, shouldBeOption)
}

/**
 * 扩展函数：根据需要将类型包装为 Option 类型 (别名)
 */
fun CangJieType.makeOptionAsSpecified(shouldBeOption: Boolean): CangJieType {
    return makeOptionalAsSpecified(shouldBeOption)
}

/**
 * SimpleType 版本的 makeOption
 */
fun SimpleType.makeOption(): SimpleType {

    return OptionTypeUtils.createOptionType(this, this.builtIns)
}

/**
 * SimpleType 版本的 makeNonOption
 */
fun SimpleType.makeNonOption(): SimpleType {
    return OptionTypeUtils.unwrapOptionType(this) as? SimpleType ?: this
}

/**
 * SimpleType 版本的 makeOptionalAsSpecified
 */
fun SimpleType.makeOptionalAsSpecified(shouldBeOption: Boolean): SimpleType {
    return OptionTypeUtils.makeOptionalIfNeeded(this, shouldBeOption)
}

/**
 * SimpleType 版本的 makeOptionAsSpecified
 */
fun SimpleType.makeOptionAsSpecified(shouldBeOption: Boolean): SimpleType {
    return makeOptionalAsSpecified(shouldBeOption)
}
