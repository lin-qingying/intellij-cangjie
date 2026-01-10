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

package org.cangnova.cangjie.types.checker

import org.cangnova.cangjie.types.BasicType
import org.cangnova.cangjie.types.SimpleType

/**
 * 基本类型子类型检查器
 *
 * 处理仓颉语言中的基本类型（原始类型）：
 * - 整数类型：Int8, Int16, Int32, Int64, UInt8, UInt16, UInt32, UInt64
 * - 浮点类型：Float16, Float32, Float64
 * - 布尔类型：Bool
 * - 字符类型：Rune
 * - 单元类型：Unit
 *
 * 注意：仓颉语言中基本类型之间没有隐式转换，
 * 只有相同类型才是子类型关系
 */
internal object PrimitiveTypeChecker {

    // 整数类型名称
    private val INTEGER_TYPES = setOf(
        "Int8", "Int16", "Int32", "Int64",
        "UInt8", "UInt16", "UInt32", "UInt64",
        "IntNative", "UIntNative"
    )

    // 浮点类型名称
    private val FLOAT_TYPES = setOf(
        "Float16", "Float32", "Float64"
    )

    // 所有数值类型
    private val NUMERIC_TYPES = INTEGER_TYPES + FLOAT_TYPES

    // 所有基本类型
    private val PRIMITIVE_TYPES = NUMERIC_TYPES + setOf("Bool", "Rune", "Unit")

    /**
     * 检查基本类型的子类型关系
     *
     * 仓颉语言中基本类型之间没有隐式转换，
     * 只有相同类型才满足子类型关系
     *
     * @param subType 子类型
     * @param superType 父类型
     * @return 如果 subType 是 superType 的子类型返回 true
     */
    fun isSubtype(subType: SimpleType, superType: SimpleType): Boolean {
        // Option 状态检查
        if (subType.isOption != superType.isOption) {
            // 非 Option 可以赋值给 Option
            if (!subType.isOption && superType.isOption) {
                val unwrappedSuper = superType.makeOptionAsSpecified(false)
                return isSubtype(subType, unwrappedSuper)
            }
            return false
        }

        // 基本类型只有相同类型才是子类型
        return CangJieSubtypeChecker.areEqualTypeConstructors(subType.constructor, superType.constructor)
    }

    /**
     * 检查类型是否是基本类型
     *
     * @param type 待检查的类型
     * @return 如果是基本类型返回 true
     */
    fun isPrimitiveType(type: SimpleType): Boolean {
        if (type !is BasicType) return false
        val typeName = getTypeName(type)
        return typeName in PRIMITIVE_TYPES
    }

    /**
     * 检查类型是否是整数类型
     *
     * @param type 待检查的类型
     * @return 如果是整数类型返回 true
     */
    fun isIntegerType(type: SimpleType): Boolean {
        if (type !is BasicType) return false
        val typeName = getTypeName(type)
        return typeName in INTEGER_TYPES
    }

    /**
     * 检查类型是否是有符号整数类型
     *
     * @param type 待检查的类型
     * @return 如果是有符号整数类型返回 true
     */
    fun isSignedIntegerType(type: SimpleType): Boolean {
        if (type !is BasicType) return false
        val typeName = getTypeName(type)
        return typeName in setOf("Int8", "Int16", "Int32", "Int64", "IntNative")
    }

    /**
     * 检查类型是否是无符号整数类型
     *
     * @param type 待检查的类型
     * @return 如果是无符号整数类型返回 true
     */
    fun isUnsignedIntegerType(type: SimpleType): Boolean {
        if (type !is BasicType) return false
        val typeName = getTypeName(type)
        return typeName in setOf("UInt8", "UInt16", "UInt32", "UInt64", "UIntNative")
    }

    /**
     * 检查类型是否是浮点类型
     *
     * @param type 待检查的类型
     * @return 如果是浮点类型返回 true
     */
    fun isFloatType(type: SimpleType): Boolean {
        if (type !is BasicType) return false
        val typeName = getTypeName(type)
        return typeName in FLOAT_TYPES
    }

    /**
     * 检查类型是否是数值类型（整数或浮点）
     *
     * @param type 待检查的类型
     * @return 如果是数值类型返回 true
     */
    fun isNumericType(type: SimpleType): Boolean {
        if (type !is BasicType) return false
        val typeName = getTypeName(type)
        return typeName in NUMERIC_TYPES
    }

    /**
     * 检查类型是否是布尔类型
     *
     * @param type 待检查的类型
     * @return 如果是布尔类型返回 true
     */
    fun isBoolType(type: SimpleType): Boolean {
        if (type !is BasicType) return false
        return getTypeName(type) == "Bool"
    }

    /**
     * 检查类型是否是字符类型
     *
     * @param type 待检查的类型
     * @return 如果是字符类型返回 true
     */
    fun isRuneType(type: SimpleType): Boolean {
        if (type !is BasicType) return false
        return getTypeName(type) == "Rune"
    }

    /**
     * 检查类型是否是 Unit 类型
     *
     * @param type 待检查的类型
     * @return 如果是 Unit 类型返回 true
     */
    fun isUnitType(type: SimpleType): Boolean {
        if (type !is BasicType) return false
        return getTypeName(type) == "Unit"
    }

    /**
     * 获取基本类型的名称
     */
    private fun getTypeName(type: SimpleType): String {
        return type.constructor.declarationDescriptor?.name?.toString() ?: ""
    }

    /**
     * 获取整数类型的位宽
     *
     * @param type 整数类型
     * @return 位宽，如果不是整数类型返回 -1
     */
    fun getIntegerBitWidth(type: SimpleType): Int {
        if (!isIntegerType(type)) return -1

        return when (getTypeName(type)) {
            "Int8", "UInt8" -> 8
            "Int16", "UInt16" -> 16
            "Int32", "UInt32" -> 32
            "Int64", "UInt64" -> 64
            "IntNative", "UIntNative" -> -1  // 平台相关
            else -> -1
        }
    }

    /**
     * 获取浮点类型的位宽
     *
     * @param type 浮点类型
     * @return 位宽，如果不是浮点类型返回 -1
     */
    fun getFloatBitWidth(type: SimpleType): Int {
        if (!isFloatType(type)) return -1

        return when (getTypeName(type)) {
            "Float16" -> 16
            "Float32" -> 32
            "Float64" -> 64
            else -> -1
        }
    }

    /**
     * 检查两个基本类型是否相等
     *
     * @param a 第一个类型
     * @param b 第二个类型
     * @return 两个类型相等时返回 true
     */
    fun areEqual(a: SimpleType, b: SimpleType): Boolean {
        if (a.isOption != b.isOption) return false
        return CangJieSubtypeChecker.areEqualTypeConstructors(a.constructor, b.constructor)
    }

    /**
     * 获取类型的默认值表示
     *
     * @param type 基本类型
     * @return 默认值的字符串表示
     */
    fun getDefaultValueRepresentation(type: SimpleType): String? {
        if (!isPrimitiveType(type)) return null

        return when (getTypeName(type)) {
            "Bool" -> "false"
            "Rune" -> "'\\0'"
            "Unit" -> "()"
            in INTEGER_TYPES -> "0"
            in FLOAT_TYPES -> "0.0"
            else -> null
        }
    }

    /**
     * 获取数值类型的最小值
     *
     * @param type 数值类型
     * @return 最小值，如果不是数值类型返回 null
     */
    fun getMinValue(type: SimpleType): Number? {
        if (!isNumericType(type)) return null

        return when (getTypeName(type)) {
            "Int8" -> Byte.MIN_VALUE
            "Int16" -> Short.MIN_VALUE
            "Int32" -> Int.MIN_VALUE
            "Int64" -> Long.MIN_VALUE
            "UInt8", "UInt16", "UInt32", "UInt64" -> 0
            "Float16", "Float32" -> Float.MIN_VALUE
            "Float64" -> Double.MIN_VALUE
            else -> null
        }
    }

    /**
     * 获取数值类型的最大值
     *
     * @param type 数值类型
     * @return 最大值，如果不是数值类型返回 null
     */
    fun getMaxValue(type: SimpleType): Number? {
        if (!isNumericType(type)) return null

        return when (getTypeName(type)) {
            "Int8" -> Byte.MAX_VALUE
            "Int16" -> Short.MAX_VALUE
            "Int32" -> Int.MAX_VALUE
            "Int64" -> Long.MAX_VALUE
            "UInt8" -> 255
            "UInt16" -> 65535
            "UInt32" -> 4294967295L
            "UInt64" -> Long.MAX_VALUE  // 实际是 2^64 - 1
            "Float16", "Float32" -> Float.MAX_VALUE
            "Float64" -> Double.MAX_VALUE
            else -> null
        }
    }
}
