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
import org.cangnova.cangjie.descriptors.EnumDescriptor
import org.cangnova.cangjie.descriptors.SupertypeLoopChecker
import org.cangnova.cangjie.descriptors.TypeParameterDescriptor
import org.cangnova.cangjie.resolve.builtIns
import org.cangnova.cangjie.storage.StorageManager
import org.cangnova.cangjie.types.checker.CangJieTypeRefiner

interface EnumTypeConstructor : TypeConstructor {

    override val declarationDescriptor: EnumDescriptor

}

/**
 * 枚举类型构造函数
 *
 * 表示枚举类型的构造函数，负责创建枚举类型实例。
 *
 * 特点：
 * - 包含枚举描述符
 * - 支持类型参数
 * - 支持类型参数声明
 * - 支持声明描述符
 *
 * 示例：
 * ```kotlin
 * val enumConstructor = EnumTypeConstructor(enumDescriptor)
 * val enumType = enumConstructor.createType(typeArguments)
 * ```
 */
class EnumTypeConstructorImpl(
     private val enumDescriptor: EnumDescriptor,
    parameters: List<TypeParameterDescriptor>,
    override val  supertypes: List<CangJieType>,
    storageManager: StorageManager
) :  AbstractClassTypeConstructor(storageManager), EnumTypeConstructor {

    /**
     * 声明描述符
     */
    override val declarationDescriptor: EnumDescriptor = enumDescriptor

    /**
     * 内置类型信息
     */
    override val builtIns: CangJieBuiltIns
        get() = enumDescriptor.builtIns

    
    override fun refine(cangjieTypeRefiner: CangJieTypeRefiner): TypeConstructor {
        return this
    }

    override fun computeSupertypes(): Collection<CangJieType> = supertypes


    override val supertypeLoopChecker: SupertypeLoopChecker = SupertypeLoopChecker.EMPTY

    override val parameters: List<TypeParameterDescriptor> = parameters.toList()


    /**
     * 是否最终
     */
    override val isFinal: Boolean = true

    /**
     * 是否拒绝
     */
    override val isDenotable: Boolean = true



    /**
     * 字符串表示
     *
     * @return 枚举类型构造函数的字符串表示
     */
    override fun toString(): String {
        return "EnumTypeConstructor(${enumDescriptor.name})"
    }

    /**
     * 相等性比较
     *
     * @param other 要比较的对象
     * @return true如果相等
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EnumTypeConstructor) return false

        return declarationDescriptor == other.declarationDescriptor
    }

    /**
     * 哈希码
     *
     * @return 哈希码
     */
    override fun hashCode(): Int {
        return enumDescriptor.hashCode()
    }
} 