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

package org.cangnova.cangjie.resolve

import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.resolve.calls.inference.ecs.ExistentialConstraintSystem
import org.cangnova.cangjie.resolve.calls.inference.ecs.OverloadabilityResult
import org.cangnova.cangjie.resolve.calls.results.*
import org.cangnova.cangjie.types.ErrorUtils

/**
 * 重载检查器
 *
 * 使用存在性约束系统 (ECS) 来判断两个声明是否可以重载。
 *
 * @property builtIns 内置类型系统
 * @property specificityComparator 类型特异性比较器
 */
class OverloadChecker(
    val builtIns: CangJieBuiltIns,
    val specificityComparator: TypeSpecificityComparator,
) {
    /** ECS 实例，用于重载检查 */
    private val ecs: ExistentialConstraintSystem by lazy {
        ExistentialConstraintSystem.create(builtIns, specificityComparator)
    }

    /**
     * 检查两个可调用描述符是否可以重载。
     *
     * 该函数用于判断两个给定的可调用描述符（[a] 和 [b]）表示的声明是否可以重载。
     * 主要通过比较两个描述符的签名和类型参数来决定它们是否可以重载。
     *
     * @param a 第一个可调用描述符，表示一个声明。
     * @param b 第二个可调用描述符，表示另一个声明。
     * @return 如果两个声明可以重载，返回 true；否则返回 false。
     */
    private fun checkOverloadability(a: CallableDescriptor, b: CallableDescriptor): Boolean {
        // 使用 ECS 进行重载检查
        val result = ecs.checkOverloadability(a, b)
        return result == OverloadabilityResult.OVERLOADABLE
    }




    private enum class DeclarationCategory {
        TYPE_OR_VALUE,
        FUNCTION,
        EXTENSION_PROPERTY
    }

    private fun getDeclarationCategory(a: DeclarationDescriptor): DeclarationCategory =
        when (a) {
            is PropertyDescriptor ->

                DeclarationCategory.TYPE_OR_VALUE

            is FunctionDescriptor ->
                DeclarationCategory.FUNCTION

            is VariableDescriptor ->
                DeclarationCategory.TYPE_OR_VALUE

            is ClassifierDescriptor ->
                DeclarationCategory.TYPE_OR_VALUE


            else ->
                error("Unexpected declaration kind: $a")
        }

    /**
     * Does not check names.
     */
    fun isOverloadable(a: DeclarationDescriptor, b: DeclarationDescriptor): Boolean {
        val aCategory = getDeclarationCategory(a)
        val bCategory = getDeclarationCategory(b)

        if (aCategory != bCategory) return false
        if (a !is CallableDescriptor || b !is CallableDescriptor) return false

        if (aCategory != DeclarationCategory.FUNCTION) return false
        return checkOverloadability(a, b)
    }


}
