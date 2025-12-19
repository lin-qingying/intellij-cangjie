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
     * 基本类型的超类型关系：
     * - 所有基本类型都继承自 Any
     * - Nothing 是所有类型的子类型
     */
    override fun computeSupertypes(): Collection<CangJieType> {
        return when (builtinsType) {


            // 其他基本类型都继承自 Any
            else -> listOf(builtIns.stdlibTypes.anyType)
        }
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
