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

package org.cangnova.cangjie.resolve

import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.resolve.calls.inference.ConstraintSystemBuilderImpl
import org.cangnova.cangjie.resolve.calls.results.*
import org.cangnova.cangjie.types.ErrorUtils

class OverloadChecker(val specificityComparator: TypeSpecificityComparator) {

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
        // 检查两个声明是否一个有类型参数而另一个没有。如果有差异，认为可以重载。
        if (a.typeParameters.isEmpty() != b.typeParameters.isEmpty()) return true

        // 检查是否有错误类型参数。如果有，认为可以重载。
        if (a is FunctionDescriptor && ErrorUtils.containsErrorTypeInParameters(a) ||
            b is FunctionDescriptor && ErrorUtils.containsErrorTypeInParameters(b)
        ) return true

        // 创建两个描述符的扁平签名。
        val aSignature = FlatSignature.createFromCallableDescriptor(a)
        val bSignature = FlatSignature.createFromCallableDescriptor(b)

        // 检查 a 的签名是否不比 b 的签名更具体。
        val aIsNotLessSpecificThanB = ConstraintSystemBuilderImpl.forSpecificity()
            .isSignatureNotLessSpecific(
                aSignature,
                bSignature,
                OverloadabilitySpecificityCallbacks,
                specificityComparator
            )

        // 检查 b 的签名是否不比 a 的签名更具体。
        val bIsNotLessSpecificThanA = ConstraintSystemBuilderImpl.forSpecificity()
            .isSignatureNotLessSpecific(
                bSignature,
                aSignature,
                OverloadabilitySpecificityCallbacks,
                specificityComparator
            )

        // 如果 a 和 b 的签名都不比对方更具体，则认为不可以重载。
        return !(aIsNotLessSpecificThanB && bIsNotLessSpecificThanA)
    }


    private enum class DeclarationCategory {
        TYPE_OR_VALUE,
        FUNCTION,
        EXTENSION_PROPERTY
    }

    private fun getDeclarationCategory(a: DeclarationDescriptor): DeclarationCategory =
        when (a) {
            is PropertyDescriptor ->
//                if (a.isExtensionProperty)
//                    DeclarationCategory.EXTENSION_PROPERTY
//                else
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
