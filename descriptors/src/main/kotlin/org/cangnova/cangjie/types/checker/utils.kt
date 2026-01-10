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

import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.descriptors.TypeParameterDescriptor
import org.cangnova.cangjie.renderer.DescriptorRenderer
import org.cangnova.cangjie.types.*
import java.util.*

/**
 * 变量构造器接口
 *
 * 表示类型推断中的类型变量
 */
interface TypeVariableConstructor : TypeConstructor {
    /** 原始的类型参数描述符 */
    val originalTypeParameter: TypeParameterDescriptor?
}

/**
 * 子类型路径节点
 *
 * 用于在类型层次结构中追踪从子类型到父类型的路径
 *
 * @param type 当前类型
 * @param previous 路径中的前一个节点
 */
private class SubtypePathNode(val type: CangJieType, val previous: SubtypePathNode?)

/**
 * 近似捕获类型
 *
 * 将捕获类型转换为其上界的近似类型
 */
private fun CangJieType.approximate() = approximateCapturedTypes(this).upper

/**
 * 查找对应的父类型
 *
 * 在子类型的类型层次结构中查找与给定父类型构造器匹配的类型，
 * 并应用适当的类型替换以返回完全具体化的父类型。
 *
 * 示例：
 * - subtype = ArrayList<String>, supertype = List<T>
 * - 返回：List<String>
 *
 * 算法：
 * 1. 使用广度优先搜索遍历子类型的父类型层次结构
 * 2. 找到匹配的类型构造器后，沿路径向上回溯
 * 3. 应用每一步的类型替换，处理型变和捕获类型
 * 4. 保留路径上的可空性标记
 *
 * @param subtype 子类型
 * @param supertype 要查找的父类型
 * @param typeCheckingProcedureCallbacks 类型检查回调（用于自定义类型构造器比较）
 * @return 对应的父类型，如果不存在则返回 null
 */
fun findCorrespondingSupertype(
    subtype: CangJieType, supertype: CangJieType,
    typeCheckingProcedureCallbacks: TypeCheckingProcedureCallbacks = TypeCheckerProcedureCallbacksImpl()
): CangJieType? {
    // 使用广度优先搜索队列
    val queue = ArrayDeque<SubtypePathNode>()
    queue.add(SubtypePathNode(subtype, null))

    val supertypeConstructor = supertype.constructor

    while (!queue.isEmpty()) {
        val lastPathNode = queue.poll()
        val currentSubtype = lastPathNode.type
        val constructor = currentSubtype.constructor

        // 检查是否找到匹配的类型构造器
        if (typeCheckingProcedureCallbacks.assertEqualTypeConstructors(constructor, supertypeConstructor)) {
            var substituted = currentSubtype
            var isAnyMarkedNullable = currentSubtype.isOption

            var currentPathNode = lastPathNode.previous

            // 沿路径向上回溯，应用类型替换
            while (currentPathNode != null) {
                val currentType = currentPathNode.type
                // 仓颉语言所有类型参数都是不变的（invariant），不需要捕获替换
                substituted = TypeConstructorSubstitution.create(currentType)
                    .buildSubstitutor()
                    .safeSubstitute(substituted)

                // 保留可空性标记
                isAnyMarkedNullable = isAnyMarkedNullable || currentType.isOption

                currentPathNode = currentPathNode.previous
            }

            // 验证替换后的类型构造器是否仍然匹配
            val substitutedConstructor = substituted.constructor
            if (!typeCheckingProcedureCallbacks.assertEqualTypeConstructors(substitutedConstructor, supertypeConstructor)) {
                throw AssertionError("Type constructors should be equals!\n" +
                        "substitutedSuperType: ${substitutedConstructor.debugInfo()}, \n\n" +
                        "supertype: ${supertypeConstructor.debugInfo()} \n" +
                        typeCheckingProcedureCallbacks.assertEqualTypeConstructors(substitutedConstructor, supertypeConstructor))
            }

            return TypeUtils.makeOptionalAsSpecified(substituted, isAnyMarkedNullable)
        }

        // 将所有直接父类型加入队列
        for (immediateSupertype in constructor.supertypes) {
            queue.add(SubtypePathNode(immediateSupertype, lastPathNode))
        }
    }

    return null
}

/**
 * 获取类型构造器的调试信息
 *
 * 包含类型、哈希码、Java 类名和完全限定名等详细信息
 */
private fun TypeConstructor.debugInfo() = buildString {
    operator fun String.unaryPlus() = appendLine(this)

    + "type: ${this@debugInfo}"
    + "hashCode: ${this@debugInfo.hashCode()}"
    + "javaClass: ${this@debugInfo::class.java.canonicalName}"
    var declarationDescriptor: DeclarationDescriptor? = declarationDescriptor
    while (declarationDescriptor != null) {

        + "fqName: ${DescriptorRenderer.FQ_NAMES_IN_TYPES.render(declarationDescriptor)}"
        + "javaClass: ${declarationDescriptor::class.java.canonicalName}"

        declarationDescriptor = declarationDescriptor.containingDeclaration
    }
}
