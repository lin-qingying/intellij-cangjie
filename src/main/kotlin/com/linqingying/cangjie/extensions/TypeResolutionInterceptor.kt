/*
 * Copyright 2024 LinQingYing. and contributors.
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

package com.linqingying.cangjie.extensions

import com.linqingying.cangjie.descriptors.impl.AnonymousFunctionDescriptor
import com.linqingying.cangjie.extensions.internal.InternalNonStableExtensionPoints
import com.linqingying.cangjie.extensions.internal.TypeResolutionInterceptorExtension
import com.linqingying.cangjie.psi.CjElement
import com.linqingying.cangjie.psi.CjLambdaExpression
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.expressions.ExpressionTypingContext
import com.intellij.openapi.project.Project



@OptIn(InternalNonStableExtensionPoints::class)
class TypeResolutionInterceptor(project: Project) {
    private val extensions = getInstances(project)

    fun interceptFunctionLiteralDescriptor(
        expression: CjLambdaExpression,
        context: ExpressionTypingContext,
        descriptor: AnonymousFunctionDescriptor
    ) = extensions.fold(descriptor) { it, extension ->
        extension.interceptFunctionLiteralDescriptor(expression, context, it)
    }

    fun interceptType(
        element: CjElement,
        context: ExpressionTypingContext,
        resultType: CangJieType?
    ): CangJieType? {
        // null means that source code has errors and in such scenarios shouldn't be passed into extension point
        if (resultType == null) return null

        return extensions.fold(resultType) { it, extension ->
            extension.interceptType(element, context, it)
        }
    }

    fun isEmpty() = extensions.isEmpty()

    companion object : ProjectExtensionDescriptor<TypeResolutionInterceptorExtension>(
        "org.jetbrains.CangJie.extensions.internal.typeResolutionInterceptorExtension",
        TypeResolutionInterceptorExtension::class.java
    )
}
