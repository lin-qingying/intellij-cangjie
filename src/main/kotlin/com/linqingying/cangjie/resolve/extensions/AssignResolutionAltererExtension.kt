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
