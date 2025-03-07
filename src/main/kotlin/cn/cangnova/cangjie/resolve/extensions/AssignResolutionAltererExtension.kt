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

package cn.cangnova.cangjie.resolve.extensions

import cn.cangnova.cangjie.extensions.ProjectExtensionDescriptor
import cn.cangnova.cangjie.extensions.internal.InternalNonStableExtensionPoints
import cn.cangnova.cangjie.psi.CjBinaryExpression
import cn.cangnova.cangjie.psi.CjExpression
import cn.cangnova.cangjie.resolve.BindingContext
import cn.cangnova.cangjie.resolve.scopes.LexicalWritableScope
import cn.cangnova.cangjie.types.CangJieType
import cn.cangnova.cangjie.types.expressions.ExpressionTypingComponents
import cn.cangnova.cangjie.types.expressions.ExpressionTypingContext
import cn.cangnova.cangjie.utils.exceptions.CangJieTypeInfo

@InternalNonStableExtensionPoints
interface AssignResolutionAltererExtension : AnnotationBasedExtension {
    companion object : ProjectExtensionDescriptor<AssignResolutionAltererExtension>(
        "cn.cangnova.cangjie.assignResolutionAltererExtension",
        AssignResolutionAltererExtension::class.java
    )

    fun needOverloadAssign(expression: CjBinaryExpression, leftType: CangJieType?, bindingContext: BindingContext): Boolean

    fun resolveAssign(
        bindingContext: BindingContext,
        expression: CjBinaryExpression,
        leftOperand: CjExpression,
        left: CjExpression,
        leftInfo: CangJieTypeInfo,
        context: ExpressionTypingContext,
        components: ExpressionTypingComponents,
        scope: LexicalWritableScope
    ): CangJieTypeInfo?
}
