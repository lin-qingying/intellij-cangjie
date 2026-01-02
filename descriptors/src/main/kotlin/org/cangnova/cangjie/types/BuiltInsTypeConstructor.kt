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

import org.cangnova.cangjie.builtins.BuiltinsType
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
class BuiltInsTypeConstructor(
    private val builtinsType: BuiltinsType,
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
     * 内置类型的超类型关系：
     * - 内置类型不应该有显式的超类型（与编译器实现一致）
     * - 内置类型与 Any 的子类型关系通过类型检查器的 implicitBoxed 机制处理
     * - Nothing 是所有类型的子类型，但自身没有超类型
     *
     * ## 编译器实现参考
     *
     * 根据 cangjie_compiler/src/Sema/TypeManager.cpp:1004-1014，内置类型满足以下条件：
     * 1. 在类型定义层面：内置类型没有显式父类型
     * 2. 在子类型判断时：当 implicitBoxed=true（默认值）时，内置类型可以作为 Any 的子类型
     * 3. 这种设计将"默认实现 Any"的语义从类型定义层面移到了类型检查层面
     *
     * ## 为什么不继承 Any
     *
     * ```
     * // 错误的实现（旧版本）：
     * String -> supertypes = [Any]  // 显式继承
     *
     * // 正确的实现（当前版本）：
     * String -> supertypes = []     // 无显式继承
     *
     * // 但在类型检查时：
     * isSubtypeOf(String, Any) == true  // 通过 implicitBoxed 机制
     * ```
     *
     * 这样可以保持与编译器的一致性，避免在类型层次结构和子类型判断中的双重关系。
     */
    override fun computeSupertypes(): Collection<CangJieType> {
        // 所有内置类型（包括 Nothing）都没有显式的超类型
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
        return builtinsType.hashCode()
    }

    /**
     * 相等性检查
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BuiltInsTypeConstructor) return false
        return builtinsType == other.builtinsType
    }

    /**
     * 字符串表示
     */
    override fun toString(): String {
        return "PrimitiveTypeConstructor(${builtinsType.typeName.asString()})"
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
