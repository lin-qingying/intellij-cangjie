package com.huawei.cangjie.resolve.extensions

import com.huawei.cangjie.extensions.ProjectExtensionDescriptor
import com.huawei.cangjie.extensions.internal.InternalNonStableExtensionPoints
import com.huawei.cangjie.psi.CjBinaryExpression
import com.huawei.cangjie.psi.CjExpression
import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.resolve.scopes.LexicalWritableScope
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.expressions.ExpressionTypingComponents
import com.huawei.cangjie.types.expressions.ExpressionTypingContext
import com.huawei.cangjie.utils.exceptions.CangJieTypeInfo

@InternalNonStableExtensionPoints
interface AssignResolutionAltererExtension : AnnotationBasedExtension {
    companion object : ProjectExtensionDescriptor<AssignResolutionAltererExtension>(
        "org.jetbrains.kotlin.assignResolutionAltererExtension",
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
