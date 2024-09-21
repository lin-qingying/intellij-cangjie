package com.huawei.cangjie.types.expressions

import com.huawei.cangjie.config.LanguageFeature.ContextReceivers
import com.huawei.cangjie.descriptors.BindingTrace
import com.huawei.cangjie.descriptors.ClassDescriptor
import com.huawei.cangjie.descriptors.FunctionDescriptor
import com.huawei.cangjie.descriptors.ReceiverParameterDescriptor
import com.huawei.cangjie.diagnostics.Errors.*
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.psi.psiUtil.parents
import com.huawei.cangjie.resolve.BindingContext.*
import com.huawei.cangjie.resolve.BindingContextUtils
import com.huawei.cangjie.resolve.DescriptorResolver
import com.huawei.cangjie.resolve.DescriptorToSourceUtils
import com.huawei.cangjie.resolve.calls.context.ResolutionContext
import com.huawei.cangjie.resolve.scopes.getDeclarationsByLabel
import com.huawei.cangjie.utils.addIfNotNull
import com.intellij.psi.PsiElement

object LabelResolver {
    private fun getLabelForFunctionalExpression(element: CjExpression): Name? {
        return when (val parent = element.parent) {
//            is CjLabeledExpression -> getLabelNamesIfAny(parent, false).singleOrNull()
            is CjBinaryExpression -> parent.operationReference.getReferencedNameAsName()
            else -> getCallerName(element)
        }
    }

    private fun getContainingCallExpression(expression: CjExpression): CjCallExpression? {
        val parent = expression.parent
        if (parent is CjLambdaArgument) {
            // f {}
            val call = parent.parent
            if (call is CjCallExpression) {
                return call
            }
        }

        if (parent is CjValueArgument) {
            // f ({}) or f(p = {}) or f (fun () {})
            val argList = parent.parent ?: return null
            val call = argList.parent
            if (call is CjCallExpression) {
                return call
            }
        }
        return null
    }

    private fun getCallerName(expression: CjExpression): Name? {
        val callExpression = getContainingCallExpression(expression) ?: return null
        val calleeExpression = callExpression.calleeExpression as? CjSimpleNameExpression
        return calleeExpression?.getReferencedNameAsName()

    }

    fun getLabelNamesIfAny(element: PsiElement, addClassNameLabels: Boolean): List<Name> {
        val result = mutableListOf<Name>()
        when (element) {
//            is CjLabeledExpression -> result.addIfNotNull(element.getLabelNameAsName())
            // TODO: Support context receivers in function literals
            is CjFunctionLiteral -> return getLabelNamesIfAny(element.parent!!, false)
            is CjLambdaExpression -> result.addIfNotNull(getLabelForFunctionalExpression(element))
        }

        if (element is CjClass) {
            element.contextReceivers
                .mapNotNullTo(result) { it.name()?.let { s -> Name.identifier(s) } }
        }

        val functionOrProperty = when (element) {
            is CjNamedFunction -> {
                result.addIfNotNull(element.nameAsName ?: getLabelForFunctionalExpression(element))
                element
            }

            is CjPropertyAccessor -> element.property
            else -> return result
        }
        if (addClassNameLabels) {
            functionOrProperty.receiverTypeReference?.nameForReceiverLabel()?.let { result.add(Name.identifier(it)) }
            functionOrProperty.contextReceivers
                .mapNotNullTo(result) { it.name()?.let { s -> Name.identifier(s) } }
        }
        return result
    }

    private fun getElementsByLabelName(
        labelName: Name,
        labelExpression: CjSimpleNameExpression,
        classNameLabelsEnabled: Boolean
    ): Pair<LinkedHashSet<CjElement>, CjCallableDeclaration?> {
        val elements = linkedSetOf<CjElement>()
        var typedElement: CjCallableDeclaration? = null
        var parent: PsiElement? = labelExpression.parent
        while (parent != null) {
            val names = getLabelNamesIfAny(parent, classNameLabelsEnabled)
            if (names.contains(labelName)) {
                elements.add(getExpressionUnderLabel(parent as CjExpression))
            } else if (parent is CjCallableDeclaration && typedElement == null) {
                val receiverTypeReference = parent.receiverTypeReference
                val nameForReceiverLabel = receiverTypeReference?.nameForReceiverLabel()
                if (nameForReceiverLabel == labelName.asString()) {
                    typedElement = parent
                }
            }
            parent = if (parent is CjCodeFragment) parent.context else parent.parent
        }
        return elements to typedElement
    }

    private fun getExpressionUnderLabel(labeledExpression: CjExpression): CjExpression {
        val expression = CjPsiUtil.safeDeparenthesize(labeledExpression)
        return if (expression is CjLambdaExpression) expression.functionLiteral else expression
    }

    fun resolveThisOrSuperLabel(
        expression: CjInstanceExpressionWithLabel,
        context: ResolutionContext<*>,
        labelName: Name
    ): LabeledReceiverResolutionResult {
        val referenceExpression = expression.instanceReference
        val targetLabelExpression = expression.getTargetLabel() ?: error(expression)

        val scope = context.scope
        val declarationsByLabel = scope.getDeclarationsByLabel(labelName)
        val (elementsByLabel, typedElement) = getElementsByLabelName(
            labelName, targetLabelExpression,
            classNameLabelsEnabled = expression is CjThisExpression && context.languageVersionSettings.supportsFeature(
                ContextReceivers
            )
        )
        val trace = context.trace
        when (declarationsByLabel.size) {
            1 -> {
                val declarationDescriptor = declarationsByLabel.single()
                val thisReceiver = when (declarationDescriptor) {
                    is ClassDescriptor -> declarationDescriptor.thisAsReceiverParameter
                    is FunctionDescriptor -> declarationDescriptor.extensionReceiverParameter
//                    is PropertyDescriptor -> declarationDescriptor.extensionReceiverParameter
                    else -> throw UnsupportedOperationException("Unsupported descriptor: $declarationDescriptor") // TODO
                }

                val declarationElement = DescriptorToSourceUtils.descriptorToDeclaration(declarationDescriptor)
                    ?: error("No PSI element for descriptor: $declarationDescriptor")
                trace.record(LABEL_TARGET, targetLabelExpression, declarationElement)
                trace.record(REFERENCE_TARGET, referenceExpression, declarationDescriptor)
                val closestElement = elementsByLabel.firstOrNull()
                if (closestElement != null && declarationElement in closestElement.parents) {
                    reportLabelResolveWillChange(
                        trace, targetLabelExpression, declarationElement, closestElement, isForExtensionReceiver = false
                    )
                } else if (typedElement != null && declarationElement in typedElement.parents) {
                    reportLabelResolveWillChange(
                        trace, targetLabelExpression, declarationElement, typedElement, isForExtensionReceiver = true
                    )
                }

                if (declarationDescriptor is ClassDescriptor) {
                    if (!DescriptorResolver.checkHasOuterClassInstance(
                            scope, trace, targetLabelExpression, declarationDescriptor
                        )
                    ) {
                        return LabeledReceiverResolutionResult.labelResolutionFailed()
                    }
                }

                return LabeledReceiverResolutionResult.labelResolutionSuccess(thisReceiver)
            }

            0 -> {
                if (elementsByLabel.size > 1) {
                    trace.report(LABEL_NAME_CLASH.on(targetLabelExpression))
                }
                val element = elementsByLabel.firstOrNull()?.also {
                    trace.record(LABEL_TARGET, targetLabelExpression, it)
                }
                val declarationDescriptor = trace.bindingContext[DECLARATION_TO_DESCRIPTOR, element]
                if (declarationDescriptor is FunctionDescriptor || declarationDescriptor is ClassDescriptor) {
                    val labelNameToReceiverMap = trace.bindingContext[
                        DESCRIPTOR_TO_CONTEXT_RECEIVER_MAP,
                        /* if (declarationDescriptor is PropertyAccessorDescriptor) declarationDescriptor.correspondingProperty else */declarationDescriptor
                    ]
                    val thisReceivers = labelNameToReceiverMap?.get(labelName.identifier)
                    val thisReceiver = when {
                        thisReceivers.isNullOrEmpty() ->
                            (declarationDescriptor as? FunctionDescriptor)?.extensionReceiverParameter

                        thisReceivers.size == 1 -> thisReceivers.single()
                        else -> {
                            BindingContextUtils.reportAmbiguousLabel(trace, targetLabelExpression, declarationsByLabel)
                            return LabeledReceiverResolutionResult.labelResolutionFailed()
                        }
                    }?.also {
                        trace.record(LABEL_TARGET, targetLabelExpression, element)
                        trace.record(REFERENCE_TARGET, referenceExpression, declarationDescriptor)
                    }
                    return LabeledReceiverResolutionResult.labelResolutionSuccess(thisReceiver)
                } else {
                    trace.report(UNRESOLVED_REFERENCE.on(targetLabelExpression, targetLabelExpression))
                }
            }

            else -> BindingContextUtils.reportAmbiguousLabel(trace, targetLabelExpression, declarationsByLabel)
        }
        return LabeledReceiverResolutionResult.labelResolutionFailed()
    }

    private fun reportLabelResolveWillChange(
        trace: BindingTrace,
        target: CjSimpleNameExpression,
        declarationElement: PsiElement,
        closestElement: CjElement,
        isForExtensionReceiver: Boolean
    ) {
        fun suffix() = if (isForExtensionReceiver) "extension receiver" else "context receiver"

        val closestDescription = when (closestElement) {
            is CjFunctionLiteral -> "anonymous function"
            is CjNamedFunction -> "function ${closestElement.name} ${suffix()}"
            is CjPropertyAccessor -> "property ${closestElement.property.name} ${suffix()}"
            else -> "???"
        }
        val declarationDescription = when (declarationElement) {
            is CjClass -> "class ${declarationElement.name}"
            is CjNamedFunction -> "function ${declarationElement.name}"
            is CjProperty -> "property ${declarationElement.name}"
            is CjNamedDeclaration -> "declaration with name ${declarationElement.name}"
            else -> "unknown declaration"
        }
        trace.report(LABEL_RESOLVE_WILL_CHANGE.on(target, declarationDescription, closestDescription))
    }

    class LabeledReceiverResolutionResult private constructor(
        val code: Code,
        private val receiverParameterDescriptor: ReceiverParameterDescriptor?
    ) {
        enum class Code {
            LABEL_RESOLUTION_ERROR,
            NO_THIS,
            SUCCESS
        }

        fun success(): Boolean {
            return code == Code.SUCCESS
        }

        fun getReceiverParameterDescriptor(): ReceiverParameterDescriptor? {
            assert(success()) { "Don't try to obtain the receiver when resolution failed with $code" }
            return receiverParameterDescriptor
        }

        companion object {
            fun labelResolutionSuccess(receiverParameterDescriptor: ReceiverParameterDescriptor?): LabeledReceiverResolutionResult {
                if (receiverParameterDescriptor == null) {
                    return LabeledReceiverResolutionResult(Code.NO_THIS, null)
                }
                return LabeledReceiverResolutionResult(Code.SUCCESS, receiverParameterDescriptor)
            }

            fun labelResolutionFailed(): LabeledReceiverResolutionResult {
                return LabeledReceiverResolutionResult(Code.LABEL_RESOLUTION_ERROR, null)
            }
        }
    }
}
