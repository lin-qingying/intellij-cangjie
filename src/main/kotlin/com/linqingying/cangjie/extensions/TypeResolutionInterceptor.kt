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
