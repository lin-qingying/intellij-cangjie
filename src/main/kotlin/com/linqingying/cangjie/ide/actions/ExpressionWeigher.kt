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

package com.linqingying.cangjie.ide.actions
import com.linqingying.cangjie.resolve.BindingContext
import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.ide.imports.importableFqName
import com.linqingying.cangjie.ide.quickfix.ImportFixHelper
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.psi.*
import com.linqingying.cangjie.psi.psiUtil.getParentOfType
import com.linqingying.cangjie.resolve.caches.analyze
import com.linqingying.cangjie.resolve.caches.resolveToDescriptorIfAny
import com.linqingying.cangjie.resolve.caches.safeAnalyze
import com.linqingying.cangjie.resolve.calls.components.isVararg
import com.linqingying.cangjie.resolve.calls.util.getType
import com.linqingying.cangjie.resolve.calls.util.receiverType

import com.linqingying.cangjie.resolve.lazy.BodyResolveMode
import com.linqingying.cangjie.resolve.source.getPsi
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.checker.CangJieTypeChecker
import com.linqingying.cangjie.types.util.makeNotNullable
import com.linqingying.cangjie.utils.exceptions.OperatorConventions
import com.intellij.psi.PsiElement

interface ExpressionWeigher {

    fun weigh(descriptor: DeclarationDescriptor): Int

    companion object {
        fun createWeigher(element: PsiElement?): ExpressionWeigher =
            when (element) {
                is CjNameReferenceExpression -> CallExpressionWeigher(element)
                is CjOperationReferenceExpression -> OperatorExpressionWeigher(element)
                else -> EmptyExpressionWeigher
            }
    }
}

internal object EmptyExpressionWeigher: ExpressionWeigher {
    override fun weigh(descriptor: DeclarationDescriptor): Int = 0

}

internal abstract class AbstractExpressionWeigher: ExpressionWeigher {
    override fun weigh(descriptor: DeclarationDescriptor): Int {
        val base = descriptor.importableFqName?.let { fqName ->
            ImportFixHelper.calculateWeightBasedOnFqName(fqName, (descriptor as? DeclarationDescriptorWithSource)?.source?.getPsi())
        } ?: 0

        return base + ownWeigh(descriptor)
    }

    protected abstract fun ownWeigh(descriptor: DeclarationDescriptor): Int

    protected fun CangJieType.weight(weight: Int, cangjieType: CangJieType?): Int? {
        if (cangjieType == null) return null
        val typeMarkedNullable = cangjieType.isMarkedOption
        val markedNullable = isMarkedOption

        val adjustedType: CangJieType
        val nullablesWeight = if (typeMarkedNullable == markedNullable) {
            adjustedType = this
            2
        } else {
            adjustedType = if (markedNullable) makeNotNullable() else this
            // no reason to make `cangjieType` not nullable as `T` is a subtype of `T?`
            0
        }

        return if (CangJieTypeChecker.DEFAULT.isSubtypeOf(adjustedType, cangjieType)) {
            100 * weight + 10 + nullablesWeight
        } else {
            null
        }
    }

}

internal class CallExpressionWeigher(element: CjNameReferenceExpression?): AbstractExpressionWeigher() {

    private val argumentCangJieTypes: List<CangJieType>
    private val valueArgumentsSize: Int
    private val receiverType: CangJieType?

    init {
        val callExpression = element?.getParentOfType<CjCallElement>(false)
        val receiverExpression = element?.getParentOfType<CjQualifiedExpression>(false)?.receiverExpression ?: element?.getParentOfType<CjLambdaExpression>(false)
        receiverType = if (receiverExpression != null) {
            val context = receiverExpression.analyze(BodyResolveMode.PARTIAL)
            val type = if (receiverExpression is CjLambdaExpression) {
                val functionDescriptor = context[BindingContext.FUNCTION, receiverExpression.functionLiteral]
                functionDescriptor?.extensionReceiverParameter?.type ?: receiverExpression.getParentOfType<CjTypeStatement>(false)
                    ?.resolveToDescriptorIfAny()?.defaultType
            } else {
                receiverExpression.getType(context)
            }
            // use non-nullable type if safe call is used i.e `val value: T? = ...; value?.smth()`
            if (receiverExpression.parent is CjSafeQualifiedExpression) {
                type?.makeNotNullable()
            } else {
                type
            }
        } else {
            null
        }

        val valueArgumentList = callExpression?.valueArgumentList
        argumentCangJieTypes = if (callExpression != null && valueArgumentList != null) {
            val valueArguments = callExpression.valueArguments
            valueArgumentsSize = valueArguments.size

            val types = ArrayList<CangJieType>(valueArgumentsSize)
            val bindingContext = valueArgumentList.analyze(BodyResolveMode.PARTIAL)

            for (valueArgument in valueArguments) {
                val argumentExpression = valueArgument.getArgumentExpression() ?: break
                types += argumentExpression.getType(bindingContext) ?: break
            }
            types
        } else {
            valueArgumentsSize = 0
            emptyList()
        }
    }

    override fun ownWeigh(descriptor: DeclarationDescriptor): Int =
        when (descriptor) {
            is CallableMemberDescriptor -> calculateWeight(descriptor, argumentCangJieTypes)
            // TODO: some constructors could be not visible
            is ClassDescriptor -> {
                descriptor.constructors.maxOfOrNull { calculateWeight(it, argumentCangJieTypes) } ?: 0
            }

            else -> 0
        }

    private fun calculateWeight(
        callableMemberDescriptor: CallableMemberDescriptor?,
        cangjieTypes: List<CangJieType>
    ): Int {
        if (callableMemberDescriptor == null) return 0

        var weight = 0
        receiverType?.let {
            val receiverValueType = callableMemberDescriptor.extensionReceiverParameter?.value?.type
            weight = it.weight(weight, receiverValueType) ?: weight
        }
        val valueParameters = callableMemberDescriptor.valueParameters
        val descriptorParameters = valueParameters.size
        val descriptorHasVarargParameter = valueParameters.any { it?.isVararg == true }

        weight += if (descriptorParameters >= valueArgumentsSize || descriptorHasVarargParameter) {
            // same number of arguments is better than when more arguments
            if (descriptorParameters == valueArgumentsSize || descriptorHasVarargParameter) 1 else 0
        } else {
            // apply only base weigh if target has fewer parameters than expected
            return weight
        }

        val valueParameterDescriptorIterator: MutableIterator<ValueParameterDescriptor> = valueParameters.iterator()
        var valueParameterDescriptor: ValueParameterDescriptor? = null

        // TODO: it does not cover following cases:
        //  - named parameters
        //  - default value, e.g. `param: Int = ""`
        //  - functional types, e.g. `Int.() -> Unit`
        //  - functional references, e.g. `::foo`

        for (cangjieType in cangjieTypes) {
            if (!valueParameterDescriptorIterator.hasNext()) {
                break
            }
            if (valueParameterDescriptor == null || !valueParameterDescriptor.isVararg) {
                // vararg could be only the last parameter, there is no parameters left
                valueParameterDescriptor = valueParameterDescriptorIterator.next()
            }

            // replace `<T>` but `<*>` if needed, otherwise `<T>` has no subtypes
            val returnType = valueParameterDescriptor.returnType
//            val vararg = valueParameterDescriptor.isVararg
//            val valueParameterType = if (vararg) {
//                // `vararg a: Int` has type `IntArray`
//                returnType?.getArrayElementType()
//            } else {
//                returnType
//            }

            weight = cangjieType.weight(weight, returnType) ?: cangjieType.weight(weight, returnType) ?: break
        }
        return weight
    }

}

internal class OperatorExpressionWeigher(element: CjOperationReferenceExpression): AbstractExpressionWeigher() {

    private val leftType: CangJieType?
    private val rightType: CangJieType?
    private val operatorName: Name?

    init {
        operatorName = element.operationSignTokenType?.let { operationSignTokenType ->
            OperatorConventions.getNameForOperationSymbol(operationSignTokenType, false, true)
        }
        val parent = element.parent
        if (parent is CjBinaryExpression) {
            val context = parent.safeAnalyze(BodyResolveMode.PARTIAL)
            leftType = parent.left?.getType(context)
            rightType = parent.right?.getType(context)
        } else {
            leftType = null
            rightType = null
        }
    }

    override fun weigh(descriptor: DeclarationDescriptor): Int {
        val functionDescriptor = (descriptor as? FunctionDescriptor)?.takeIf { it.isOperator } ?: return 0

        return super.weigh(functionDescriptor)
    }

    override fun ownWeigh(descriptor: DeclarationDescriptor): Int {
        val functionDescriptor = descriptor as FunctionDescriptor

        val name = functionDescriptor.name
        var weight = 0
        if (name == operatorName) {
            weight += 8
        }
        (descriptor as? CallableDescriptor)?.let {
            val receiverType = it.receiverType()
            weight = leftType?.weight(weight, receiverType) ?: weight

            val valueParameterDescriptor = it.valueParameters.firstOrNull()
            val valueParameterType = valueParameterDescriptor?.returnType
            if (valueParameterType != null) {
                weight = rightType?.weight(weight, valueParameterType) ?: weight
            }
        }

        return weight
    }

}
