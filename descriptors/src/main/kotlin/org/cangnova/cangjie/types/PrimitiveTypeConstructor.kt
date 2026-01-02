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
import org.cangnova.cangjie.builtins.PrimitiveType
import org.cangnova.cangjie.descriptors.ClassDescriptor
import org.cangnova.cangjie.descriptors.SupertypeLoopChecker
import org.cangnova.cangjie.descriptors.TypeParameterDescriptor
import org.cangnova.cangjie.storage.StorageManager

/**
 * 基本类型构造器
 *
 * 专门为基本类型设计的类型构造器，提供优化的性能和内存使用
 */
class PrimitiveTypeConstructor(
    private val primitiveType: PrimitiveType,
    private val classDescriptor: ClassDescriptor,
    private val storageManager: StorageManager,
    override val builtIns: CangJieBuiltIns
) : AbstractClassTypeConstructor(storageManager) {

    /**
     * 获取声明描述符
     */
    override val declarationDescriptor: ClassDescriptor = classDescriptor

    /**
     * 获取类型参数（基本类型没有类型参数）
     */
    override val parameters: List<TypeParameterDescriptor> = emptyList()

    /**
     * 计算超类型
     *
     * 基本类型的超类型关系：
     * - 基本类型不应该有显式的超类型（与编译器实现一致）
     * - 基本类型与 Any 的子类型关系通过类型检查器的 implicitBoxed 机制处理
     * - Nothing 是所有类型的子类型，但自身没有超类型
     *
     * ## 编译器实现参考
     *
     * 根据 cangjie_compiler/src/Sema/TypeManager.cpp:1004-1014，基本类型满足以下条件：
     * 1. 在类型定义层面：基本类型没有显式父类型
     * 2. 在子类型判断时：当 implicitBoxed=true（默认值）时，基本类型可以作为 Any 的子类型
     * 3. 这种设计将"默认实现 Any"的语义从类型定义层面移到了类型检查层面
     *
     * ## 为什么不继承 Any
     *
     * ```
     * // 错误的实现（旧版本）：
     * Int32 -> supertypes = [Any]  // 显式继承
     *
     * // 正确的实现（当前版本）：
     * Int32 -> supertypes = []     // 无显式继承
     *
     * // 但在类型检查时：
     * isSubtypeOf(Int32, Any) == true  // 通过 implicitBoxed 机制
     * ```
     *
     * 这样可以保持与编译器的一致性，避免在类型层次结构和子类型判断中的双重关系。
     */
    override fun computeSupertypes(): Collection<CangJieType> {
        // 所有基本类型（包括 Nothing）都没有显式的超类型
        // 与 Any 的关系通过类型检查器的 implicitBoxed 机制处理
        return emptyList()
    }




    /**
     * 超类型循环检查器（基本类型不会有循环）
     */
    override val supertypeLoopChecker: SupertypeLoopChecker = SupertypeLoopChecker.EMPTY


    override val isDenotable: Boolean = true

    /**
     * 哈希码
     */
    override fun hashCode(): Int {
        return primitiveType.hashCode()
    }

    /**
     * 相等性检查
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PrimitiveTypeConstructor) return false
        return primitiveType == other.primitiveType
    }

    /**
     * 字符串表示
     */
    override fun toString(): String {
        return primitiveType.typeName.asString()
    }

    /**
     * 检查是否为数值类型
     */
    fun isNumericType(): Boolean {
        return primitiveType.isNumeric()
    }

    /**
     * 检查是否为整数类型
     */
    fun isIntegerType(): Boolean {
        return primitiveType in listOf(
            PrimitiveType.INT8, PrimitiveType.INT16, PrimitiveType.INT32,
            PrimitiveType.INT64, PrimitiveType.INTNATIVE
        )
    }

    /**
     * 检查是否为无符号整数类型
     */
    fun isUnsignedIntegerType(): Boolean {
        return primitiveType in listOf(
            PrimitiveType.UINT8, PrimitiveType.UINT16, PrimitiveType.UINT32,
            PrimitiveType.UINT64, PrimitiveType.UINTNATIVE
        )
    }

    /**
     * 检查是否为浮点类型
     */
    fun isFloatType(): Boolean {
        return primitiveType in listOf(
            PrimitiveType.FLOAT16, PrimitiveType.FLOAT32, PrimitiveType.FLOAT64
        )
    }

    /**
     * 获取类型的位宽（如果适用）
     */
    fun getBitWidth(): Int? {
        return when (primitiveType) {
            PrimitiveType.INT8, PrimitiveType.UINT8 -> 8
            PrimitiveType.INT16, PrimitiveType.UINT16, PrimitiveType.FLOAT16 -> 16
            PrimitiveType.INT32, PrimitiveType.UINT32, PrimitiveType.FLOAT32 -> 32
            PrimitiveType.INT64, PrimitiveType.UINT64, PrimitiveType.FLOAT64 -> 64
            PrimitiveType.BOOL -> 1
            PrimitiveType.Rune -> 32 // Unicode 代码点
            else -> null // 本机大小或不适用
        }
    }

    /**
     * 检查是否可以隐式转换到另一个类型
     */
    fun canImplicitlyConvertTo(targetType: PrimitiveType): Boolean {
        // 基本的隐式转换规则
        return when {
            // 相同类型
            primitiveType == targetType -> true

            // 小整数向大整数转换
            primitiveType == PrimitiveType.INT8 && targetType in listOf(
                PrimitiveType.INT16, PrimitiveType.INT32, PrimitiveType.INT64
            ) -> true

            primitiveType == PrimitiveType.INT16 && targetType in listOf(
                PrimitiveType.INT32, PrimitiveType.INT64
            ) -> true

            primitiveType == PrimitiveType.INT32 && targetType == PrimitiveType.INT64 -> true

            // 整数向浮点转换
            isIntegerType() && targetType in listOf(
                PrimitiveType.FLOAT32, PrimitiveType.FLOAT64
            ) -> true

            // 小浮点向大浮点转换
            primitiveType == PrimitiveType.FLOAT16 && targetType in listOf(
                PrimitiveType.FLOAT32, PrimitiveType.FLOAT64
            ) -> true

            primitiveType == PrimitiveType.FLOAT32 && targetType == PrimitiveType.FLOAT64 -> true

            // 其他情况需要显式转换
            else -> false
        }
    }

    /**
     * 获取与另一个类型的公共类型
     */
    fun getCommonTypeWith(otherType: PrimitiveType): PrimitiveType? {
        if (primitiveType == otherType) return primitiveType

        // 数值类型的公共类型规则
        if (isNumericType() && PrimitiveType.values().find { it == otherType }?.isNumeric() == true) {
            return when {
                // 如果有浮点类型，选择精度更高的浮点类型
                isFloatType() || PrimitiveType.values().find { it == otherType }?.let {
                    it in listOf(PrimitiveType.FLOAT16, PrimitiveType.FLOAT32, PrimitiveType.FLOAT64)
                } == true -> {
                    val floatTypes = listOf(PrimitiveType.FLOAT64, PrimitiveType.FLOAT32, PrimitiveType.FLOAT16)
                    floatTypes.find { it == primitiveType || it == otherType }
                }

                // 都是整数类型，选择较大的
                else -> {
                    val integerTypes = listOf(
                        PrimitiveType.INT64, PrimitiveType.INT32, PrimitiveType.INT16, PrimitiveType.INT8,
                        PrimitiveType.UINT64, PrimitiveType.UINT32, PrimitiveType.UINT16, PrimitiveType.UINT8
                    )
                    integerTypes.find { it == primitiveType || it == otherType }
                }
            }
        }

        return null
    }

    companion object {
        /**
         * 创建基本类型构造器
         */
        fun create(
            primitiveType: PrimitiveType,
            classDescriptor: ClassDescriptor,
            storageManager: StorageManager,
            builtIns: CangJieBuiltIns
        ): PrimitiveTypeConstructor {
            return PrimitiveTypeConstructor(primitiveType, classDescriptor, storageManager, builtIns)
        }
    }
}

/**
 * 基本类型构造器工厂
 *
 * 负责创建和缓存基本类型构造器，避免重复创建
 */
class PrimitiveTypeConstructorFactory(
    private val storageManager: StorageManager,
    private val builtIns: CangJieBuiltIns
) {
    /**
     * 类型构造器缓存
     */
    private val constructorCache = mutableMapOf<PrimitiveType, PrimitiveTypeConstructor>()

    /**
     * 获取或创建基本类型构造器
     */
    fun getOrCreateConstructor(
        primitiveType: PrimitiveType,
        classDescriptor: ClassDescriptor
    ): PrimitiveTypeConstructor {
        return constructorCache.getOrPut(primitiveType) {
            PrimitiveTypeConstructor.create(primitiveType, classDescriptor, storageManager, builtIns)
        }
    }

    /**
     * 清除缓存
     */
    fun clearCache() {
        constructorCache.clear()
    }

    /**
     * 获取缓存的构造器数量
     */
    fun getCacheSize(): Int = constructorCache.size
}
