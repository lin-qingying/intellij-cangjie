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

package com.linqingying.cangjie.resolve.extensions

import com.linqingying.cangjie.extensions.ProjectExtensionDescriptor
import com.linqingying.cangjie.extensions.internal.InternalNonStableExtensionPoints
import com.linqingying.cangjie.psi.CjBinaryExpression
import com.linqingying.cangjie.psi.CjExpression
import com.linqingying.cangjie.resolve.BindingContext
import com.linqingying.cangjie.resolve.scopes.LexicalWritableScope
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.expressions.ExpressionTypingComponents
import com.linqingying.cangjie.types.expressions.ExpressionTypingContext
import com.linqingying.cangjie.utils.exceptions.CangJieTypeInfo

@InternalNonStableExtensionPoints
interface AssignResolutionAltererExtension : AnnotationBasedExtension {
    companion object : ProjectExtensionDescriptor<AssignResolutionAltererExtension>(
        "com.linqingying.cangjie.assignResolutionAltererExtension",
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
