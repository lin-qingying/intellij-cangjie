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

package org.cangnova.cangjie.resolve.lazy

import com.intellij.openapi.progress.ProgressManager
import org.cangnova.cangjie.descriptors.CallableDescriptor
import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.descriptors.TypeAliasDescriptor
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.resolve.DescriptorUtils
import org.cangnova.cangjie.resolve.scopes.MemberScope
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.TypeConstructor
import org.cangnova.cangjie.types.TypeUtils.NO_EXPECTED_TYPE
import org.cangnova.cangjie.types.asFlexibleType
import org.cangnova.cangjie.types.isFlexible

/**
 * 强制解析工具类
 *
 * ForceResolveUtil 提供了一组工具方法，用于强制解析延迟（lazy）描述符和类型的所有内容。
 * 在仓颉语言的编译过程中，许多描述符和类型采用延迟解析策略以提高性能，
 * 只有在真正需要时才会解析具体内容。
 *
 * 然而，在某些场景下（如序列化、测试、调试等），需要确保所有内容都已完全解析。
 * 本工具类提供的方法会递归地触发所有延迟解析逻辑，确保描述符树的完整性。
 *
 * **使用场景**:
 * - 序列化描述符前确保所有内容已解析
 * - 测试中验证描述符的完整性
 * - 调试时检查描述符的所有细节
 * - 在需要完整类型信息的分析阶段
 *
 * **注意**:
 * - 强制解析会触发大量计算，应谨慎使用
 * - 解析过程中会检查取消状态（ProgressManager.checkCanceled()）
 * - 对于大型作用域，解析可能需要较长时间
 *
 * @see LazyEntity
 * @see MemberScope
 * @see DeclarationDescriptor
 */
object ForceResolveUtil {

    /**
     * 强制解析作用域中的所有内容
     *
     * 此方法会获取指定作用域中的所有描述符，并递归解析它们的所有内容。
     * 这包括嵌套的类、函数、属性及其类型信息等。
     *
     * @param scope 要解析的成员作用域
     *
     * @see MemberScope
     * @see DescriptorUtils.getAllDescriptors
     */
    fun forceResolveAllContents(scope: MemberScope) {
        forceResolveAllContents(
            DescriptorUtils.getAllDescriptors(
                scope
            )
        )
    }

    /**
     * 强制解析描述符集合中的所有内容
     *
     * 遍历给定的描述符集合，对每个描述符递归调用 [forceResolveAllContents] 方法。
     *
     * @param descriptors 要解析的描述符集合
     *
     * @see DeclarationDescriptor
     */
    fun forceResolveAllContents(descriptors: Iterable<DeclarationDescriptor>) {
        for (descriptor in descriptors) {
            forceResolveAllContents(
                descriptor
            )
        }
    }

    /**
     * 执行强制解析的核心逻辑
     *
     * 此私有方法根据对象的类型执行不同的解析策略：
     *
     * 1. **LazyEntity**: 调用其 [LazyEntity.forceResolveAllContents] 方法触发延迟解析
     * 2. **CallableDescriptor**: 递归解析其值参数、类型参数、返回类型和注解
     * 3. **TypeAliasDescriptor**: 解析其底层类型
     *
     * 在解析过程开始时会检查取消状态，确保长时间运行的解析可以被中断。
     *
     * @param any 要解析的对象（可以是描述符、类型等）
     *
     * @see LazyEntity
     * @see CallableDescriptor
     * @see TypeAliasDescriptor
     * @see ProgressManager.checkCanceled
     */
    private fun doForceResolveAllContents(any: Any) {
        ProgressManager.checkCanceled()

        when (any) {
            is LazyEntity -> {
                val lazyEntity: LazyEntity =
                    any
                lazyEntity.forceResolveAllContents()
            }

            is CallableDescriptor -> {



                for (parameterDescriptor in any.valueParameters) {
                    forceResolveAllContents(
                        parameterDescriptor
                    )
                }
                for (typeParameterDescriptor in any.typeParameters) {
                    forceResolveAllContents(typeParameterDescriptor.upperBounds)
                }
                forceResolveAllContents(any.returnType)
                forceResolveAllContents(any.annotations)
            }

            is TypeAliasDescriptor -> {
                val typeAliasDescriptor: TypeAliasDescriptor =
                    any
                forceResolveAllContents(typeAliasDescriptor.underlyingType)
            }
        }
    }

    /**
     * 强制解析单个对象并返回
     *
     * 这是泛型包装方法，用于在强制解析后返回原始对象。
     * 这使得该方法可以在链式调用中使用。
     *
     * 示例:
     * ```kotlin
     * val descriptor = forceResolveAllContents(lazyDescriptor)
     * // descriptor 的所有内容已完全解析
     * ```
     *
     * @param T 对象的类型
     * @param descriptor 要解析的对象
     * @return 解析后的同一对象（用于链式调用）
     */
    fun <T : Any> forceResolveAllContents(descriptor: T): T {
        doForceResolveAllContents(descriptor)
        return descriptor
    }

    /**
     * 强制解析类型构造器
     *
     * 解析类型构造器的所有内容，包括其声明描述符和类型参数。
     *
     * @param typeConstructor 要解析的类型构造器
     *
     * @see TypeConstructor
     */
    fun forceResolveAllContents(typeConstructor: TypeConstructor) {
        doForceResolveAllContents(typeConstructor)
    }

    /**
     * 强制解析仓颉类型
     *
     * 递归解析类型的所有组成部分：
     * - 类型注解
     * - 对于灵活类型（flexible types）：解析其上界和下界
     * - 对于普通类型：解析类型构造器和所有类型参数
     *
     * 灵活类型是用于表示平台类型（如 Java 互操作）的特殊类型，
     * 它有一个上界和下界来表示类型的可能范围。
     *
     * @param type 要解析的类型，可以为 null 或 NO_EXPECTED_TYPE
     * @return 解析后的类型，如果输入为 null 或 NO_EXPECTED_TYPE 则返回 null
     *
     * @see CangJieType
     * @see CangJieType.isFlexible
     * @see NO_EXPECTED_TYPE
     */
    fun forceResolveAllContents(type: CangJieType?): CangJieType? {
        if (type == null || type == NO_EXPECTED_TYPE) return null

        forceResolveAllContents(type.annotations)
        if (type.isFlexible()) {
            forceResolveAllContents(type.asFlexibleType().lowerBound)
            forceResolveAllContents(type.asFlexibleType().upperBound)
        } else {
            forceResolveAllContents(type.constructor)
            for (projection in type.arguments) {

                forceResolveAllContents(projection.type)

            }
        }
        return type
    }


    /**
     * 强制解析注解集合
     *
     * 递归解析注解集合本身以及集合中的每个注解。
     * 注解可能包含复杂的参数和类型信息，这些都需要被解析。
     *
     * @param annotations 要解析的注解集合
     *
     * @see Annotations
     */
    fun forceResolveAllContents(annotations: Annotations) {
        doForceResolveAllContents(annotations)
        for (annotation in annotations) {
            doForceResolveAllContents(annotation)
        }
    }


}
