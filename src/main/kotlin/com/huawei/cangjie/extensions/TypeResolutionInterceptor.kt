package com.huawei.cangjie.extensions

import com.huawei.cangjie.descriptors.impl.AnonymousFunctionDescriptor
import com.huawei.cangjie.extensions.internal.InternalNonStableExtensionPoints
import com.huawei.cangjie.extensions.internal.TypeResolutionInterceptorExtension
import com.huawei.cangjie.psi.CjElement
import com.huawei.cangjie.psi.CjLambdaExpression
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.expressions.ExpressionTypingContext
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
