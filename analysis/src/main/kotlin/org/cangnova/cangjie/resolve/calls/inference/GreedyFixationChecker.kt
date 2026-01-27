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

package org.cangnova.cangjie.resolve.calls.inference

import org.cangnova.cangjie.descriptors.ClassAndEnumDescriptor
import org.cangnova.cangjie.descriptors.ClassDescriptor
import org.cangnova.cangjie.descriptors.TypeParameterDescriptor
import org.cangnova.cangjie.descriptors.isFinalClass
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.TypeUtils
import org.cangnova.cangjie.types.isAny
import org.cangnova.cangjie.types.isNothing
import org.cangnova.cangjie.types.model.TypeVariableMarker


/**
 * 贪婪固定判断器
 *
 * 对应编译器的 IsGreedySolution 函数。
 * 判断类型变量是否可以立即固定，无需等待更多信息。
 *
 * 简化实现：使用现有的类型系统 API 进行判断。
 */
object GreedyFixationChecker {

    /**
     * 检查是否可以贪婪固定
     *
     * @param typeVariable 类型变量
     * @param bound 约束类型
     * @param isUpperBound 是否为上界约束
     * @return 是否可以贪婪固定
     */
    fun isGreedySolution(
        typeVariable: TypeVariableMarker,
        bound: CangJieType,
        isUpperBound: Boolean
    ): Boolean {
        // 条件 1: 类型参数（泛型参数）
        val isTypeParam = TypeUtils.isTypeParameter(bound)

        // 条件 2: Final 类型（不可继承）- 使用简化判断
        val isFinalType = when {
            isUpperBound -> !isInheritableClass(bound)
            else -> !isTypeParam && !isClassLike(bound) && !bound.isAny()
        }

        // 条件 3: Any（作为下界）或 Nothing（作为上界）
        val isAnyOrNothing = (bound.isAny() && !isUpperBound) ||
            (bound.isNothing() && isUpperBound)

        return isTypeParam || isFinalType || isAnyOrNothing
    }

    /**
     * 检查类型是否类似于类（有类描述符）
     */
    private fun isClassLike(type: CangJieType): Boolean {
        return type.constructor.declarationDescriptor is ClassDescriptor
    }

    /**
     * 检查类型是否可继承
     */
    private fun isInheritableClass(type: CangJieType): Boolean {
        val classifier = type.constructor.declarationDescriptor
        return when (classifier) {
            is ClassAndEnumDescriptor -> !classifier.isFinalClass && !classifier.isSealed
            is ClassDescriptor -> !classifier.isFinalClass && !classifier.isSealed
            else -> false
        }
    }
}
