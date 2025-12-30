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

package org.cangnova.cangjie.completion

import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.completion.handlers.CangJieFunctionInsertHandler
import org.cangnova.cangjie.completion.handlers.GenerateLambdaInfo
import org.cangnova.cangjie.completion.handlers.InsertHandlerProvider
import org.cangnova.cangjie.completion.handlers.createNormalFunctionInsertHandler
import org.cangnova.cangjie.renderer.DescriptorRenderer
import org.cangnova.cangjie.renderer.render
import org.cangnova.cangjie.resolve.calls.components.hasDefaultValue
import org.cangnova.cangjie.resolve.calls.util.getValueParametersCountFromFunctionType
import org.cangnova.cangjie.resolve.findOriginalTopMostOverriddenDescriptors
import org.cangnova.cangjie.resolve.scopes.receivers.TransientReceiver
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.toFuzzyType
import org.cangnova.cangjie.utils.addIfNotNull
import com.intellij.codeInsight.completion.CompositeDeclarativeInsertHandler
import com.intellij.codeInsight.completion.DeclarativeInsertHandler
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.openapi.editor.Editor
import com.intellij.util.SmartList
import org.cangnova.cangjie.name.OperatorNameConventions
import org.cangnova.cangjie.resolve.calls.util.CallType
import org.cangnova.cangjie.resolve.calls.util.ReceiverType
import org.cangnova.cangjie.resolve.overriddenTreeUniqueAsSequence
import org.cangnova.cangjie.resolve.parentsWithSelf
import org.cangnova.cangjie.types.TypeUtils
import org.cangnova.cangjie.types.isBuiltinFunctionalType
import org.cangnova.cangjie.types.isFunctionType
import org.cangnova.cangjie.types.isSubtypeOf
import org.cangnova.cangjie.utils.isExtension

interface AbstractLookupElementFactory {
    fun createStandardLookupElementsForDescriptor(
        descriptor: DeclarationDescriptor,
        useReceiverTypes: Boolean
    ): Collection<LookupElement>

    fun createLookupElement(
        descriptor: DeclarationDescriptor,
        useReceiverTypes: Boolean,
        qualifyNestedClasses: Boolean = false,
        includeClassTypeArguments: Boolean = true,
        parametersAndTypeGrayed: Boolean = false
    ): LookupElement?
}

data /* we need copy() */
class LookupElementFactory(
    val basicFactory: BasicLookupElementFactory,
    private val editor: Editor,
    private val receiverTypes: Collection<ReceiverType>?,
    private val callType: CallType<*>,
    private val inDescriptor: DeclarationDescriptor,
    private val contextVariablesProvider: ContextVariablesProvider,
    private val standardLookupElementsPostProcessor: (LookupElement) -> LookupElement = { it }
) : AbstractLookupElementFactory {
    private fun createFunctionCallElementWithLambda(
        descriptor: FunctionDescriptor,
        parameterType: CangJieType,
        useReceiverTypes: Boolean,
        explicitLambdaParameters: Boolean
    ): LookupElement {
        var lookupElement = createLookupElement(descriptor, useReceiverTypes)
        val lambdaInfo = GenerateLambdaInfo(parameterType, explicitLambdaParameters)
        val lambdaPresentation = if (explicitLambdaParameters)
            LambdaSignatureTemplates.lambdaPresentation(parameterType, LambdaSignatureTemplates.SignaturePresentation.NAMES_OR_TYPES)
        else
            LambdaSignatureTemplates.DEFAULT_LAMBDA_PRESENTATION

        // render only the last parameter because all other should be optional and will be omitted
        var parametersRenderer = BasicLookupElementFactory.SHORT_NAMES_RENDERER
        if (descriptor.valueParameters.size > 1) {
            parametersRenderer = parametersRenderer.withOptions {
                valueParametersHandler = object : DescriptorRenderer.ValueParametersHandler by this.valueParametersHandler {
                    override fun appendBeforeValueParameter(
                        parameter: ValueParameterDescriptor,
                        parameterIndex: Int,
                        parameterCount: Int,
                        builder: StringBuilder
                    ) {
                        builder.append("..., ")
                    }
                }
            }
        }

        val parametersPresentation =
            parametersRenderer.renderValueParameters(listOf(descriptor.valueParameters.last()), descriptor.hasSynthesizedParameterNames())

        val handler = createNormalFunctionInsertHandler(
            editor,
            callType,
            descriptor.name,
            insertHandlerProvider.needTypeArguments(descriptor),
            inputValueArguments = false,
            lambdaInfo = lambdaInfo,
        )

        lookupElement = object : LookupElementDecorator<LookupElement>(lookupElement) {
            override fun renderElement(presentation: LookupElementPresentation) {
                super.renderElement(presentation)

                presentation.clearTail()
                presentation.appendTailText(" $lambdaPresentation ", false)
                presentation.appendTailText(parametersPresentation, true)
                basicFactory.appendContainerAndReceiverInformation(descriptor) { presentation.appendTailText(it, true) }
            }

            override fun getDelegateInsertHandler(): InsertHandler<LookupElement> = handler
        }

        return lookupElement
    }

    private fun MutableCollection<LookupElement>.addSpecialFunctionCallElements(descriptor: FunctionDescriptor, useReceiverTypes: Boolean) {
        // check that all parameters except for the last one are optional
        val lastParameter = descriptor.valueParameters.lastOrNull() ?: return
        if (!descriptor.valueParameters.all { it == lastParameter || it.hasDefaultValue() }) return

        if (lastParameter.original.type.isBuiltinFunctionalType) {
            val isSingleParameter = descriptor.valueParameters.size == 1
            val parameterType = lastParameter.type

            if (!InsertHandlerProvider.isCangJieLambda(descriptor, callType)) {
                val functionParameterCount = getValueParametersCountFromFunctionType(parameterType)
                add(
                    createFunctionCallElementWithLambda(
                        descriptor,
                        parameterType,
                        useReceiverTypes,
                        explicitLambdaParameters = functionParameterCount > 1
                    )
                )
            }

            if (isSingleParameter) {
                //TODO: also ::function? at least for local functions
                //TODO: order for them
                val fuzzyParameterType = parameterType.toFuzzyType(descriptor.typeParameters)
                for ((variable, substitutor) in contextVariablesProvider.functionTypeVariables(fuzzyParameterType)) {
                    val substitutedDescriptor = descriptor.substitute(substitutor) ?: continue
                    add(createFunctionCallElementWithArguments(substitutedDescriptor, variable.name.render(), useReceiverTypes))
                }
            }
        }
    }
    private inner class FunctionCallWithArgumentsLookupElement(
        originalLookupElement: LookupElement,
        private val descriptor: FunctionDescriptor,
        private val argumentText: String,
        private val needTypeArguments: Boolean
    ) : LookupElementDecorator<LookupElement>(originalLookupElement) {

        override fun equals(other: Any?) =
            other is FunctionCallWithArgumentsLookupElement && delegate == other.delegate && argumentText == other.argumentText

        override fun hashCode() = delegate.hashCode() * 17 + argumentText.hashCode()

        override fun renderElement(presentation: LookupElementPresentation) {
            super.renderElement(presentation)

            presentation.clearTail()
            presentation.appendTailText("($argumentText)", false)
            basicFactory.appendContainerAndReceiverInformation(descriptor) { presentation.appendTailText(it, true) }
        }

        override fun getDecoratorInsertHandler() = CangJieFunctionInsertHandler.Normal(
            callType,
            inputTypeArguments = needTypeArguments,
            inputValueArguments = false,
            argumentText = argumentText,
        )
    }
    private fun createFunctionCallElementWithArguments(
        descriptor: FunctionDescriptor,
        argumentText: String,
        useReceiverTypes: Boolean
    ): LookupElement {
        val lookupElement = createLookupElement(descriptor, useReceiverTypes)
        return FunctionCallWithArgumentsLookupElement(
            lookupElement,
            descriptor,
            argumentText,
            insertHandlerProvider.needTypeArguments(descriptor),
        )
    }

    private val superFunctions: Set<FunctionDescriptor> by lazy {
        inDescriptor.parentsWithSelf.takeWhile { it !is ClassDescriptor }.filterIsInstance<FunctionDescriptor>().toList()
            .flatMap { it.findOriginalTopMostOverriddenDescriptors() }.toSet()
    }
    override fun createStandardLookupElementsForDescriptor(
        descriptor: DeclarationDescriptor,
        useReceiverTypes: Boolean
    ): Collection<LookupElement> {
        val result = SmartList<LookupElement>()

        val isNormalCall =
            callType == CallType.DEFAULT || callType == CallType.DOT || callType == CallType.SAFE || callType == CallType.SUPER_MEMBERS

        result.add(createLookupElement(descriptor, useReceiverTypes, parametersAndTypeGrayed = !isNormalCall  ))

        // add special item for function with one argument of function type with more than one parameter
        if (descriptor is FunctionDescriptor && isNormalCall) {
            if (callType != CallType.SUPER_MEMBERS) {
                result.addSpecialFunctionCallElements(descriptor, useReceiverTypes)
            } else if (useReceiverTypes) {
                result.addIfNotNull(createSuperFunctionCallWithArguments(descriptor))
            }
        }

        // special "[]" item for get-operator
        if (callType == CallType.DOT && descriptor is FunctionDescriptor && descriptor.isOperator && descriptor.name == OperatorNameConventions.GET) {
            val brackets = "[]"

            val insertHandler = CompositeDeclarativeInsertHandler.withUniversalHandler(
                "\n\t",
                DeclarativeInsertHandler.Builder()
                    .addOperation(offsetFrom = -1 - brackets.length, offsetTo = -brackets.length, newText = "")
                    .withOffsetToPutCaret(-1)
                    .withPopupOptions(DeclarativeInsertHandler.PopupOptions.ParameterInfo)
                    .build(),
            )

            val baseLookupElement = createLookupElement(descriptor, useReceiverTypes)
            val lookupElement = object : LookupElementDecorator<LookupElement>(baseLookupElement) {
                override fun getLookupString() = brackets
                override fun getAllLookupStrings() = setOf(lookupString)
                override fun renderElement(presentation: LookupElementPresentation) {
                    super.renderElement(presentation)
                    presentation.itemText = lookupString
                }

                override fun getDelegateInsertHandler() = insertHandler
            }


            lookupElement.assignPriority(ItemPriority.GET_OPERATOR)
            result += lookupElement
        }

        return result.map(standardLookupElementsPostProcessor)
    }
    private fun createSuperFunctionCallWithArguments(descriptor: FunctionDescriptor): LookupElement? {
        if (descriptor.valueParameters.isEmpty()) return null
        if (descriptor.findOriginalTopMostOverriddenDescriptors().none { it in superFunctions }) return null

        val argumentText = descriptor.valueParameters.joinToString(", ") {
            (if (it.varargElementType != null) "*" else "") + it.name.render()
        } //TODO: use code formatting settings

        val lookupElement = createFunctionCallElementWithArguments(descriptor, argumentText, true)
        lookupElement.assignPriority(ItemPriority.SUPER_METHOD_WITH_ARGUMENTS)
        lookupElement.suppressItemSelectionByCharsOnTyping = true
        return lookupElement
    }
    val insertHandlerProvider = basicFactory.insertHandlerProvider

    companion object {
        fun hasSingleFunctionTypeParameter(descriptor: FunctionDescriptor): Boolean {
            val parameter = descriptor.original.valueParameters.singleOrNull() ?: return false
            return parameter.type.isBuiltinFunctionalType
        }
    }

    private fun LookupElement.boldIfImmediate(weight: CallableWeight?): LookupElement {
        val style = when (weight?.enum) {
            // 在仓颉语言中，类成员和 extend 成员都应该加粗显示
            CallableWeightEnum.thisClassMember,
            CallableWeightEnum.thisTypeExtension -> Style.BOLD
            CallableWeightEnum.receiverCastRequired -> Style.GRAYED
            else -> Style.NORMAL
        }
        return if (style != Style.NORMAL) {
            object : LookupElementDecorator<LookupElement>(this) {
                override fun renderElement(presentation: LookupElementPresentation) {
                    super.renderElement(presentation)
                    if (style == Style.BOLD) {
                        presentation.isItemTextBold = true
                    } else {
                        presentation.itemTextForeground = CANGJIE_CAST_REQUIRED_COLOR
                        // gray all tail fragments too:
                        val fragments = presentation.tailFragments
                        presentation.clearTail()
                        for (fragment in fragments) {
                            presentation.appendTailText(fragment.text, true)
                        }
                    }
                }
            }
        } else {
            this
        }
    }

    private enum class Style {
        NORMAL,
        BOLD,
        GRAYED
    }

    private fun callableWeightBasic(
        descriptor: CallableDescriptor,
        receiverTypes: Collection<ReceiverType>
    ): CallableWeight {
        descriptor.callableWeightBasedOnReceiver(receiverTypes, CallableWeight.receiverCastRequired)?.let { return it }

        return when (descriptor.containingDeclaration) {
            is PackageFragmentDescriptor, is ClassifierDescriptor -> CallableWeight.globalOrStatic
            else -> CallableWeight.local
        }
    }

    private fun CallableDescriptor.callableWeightBasedOnReceiver(
        receiverTypes: Collection<ReceiverType>,
        onReceiverTypeMismatch: CallableWeight?,
        receiverParameter: ReceiverParameterDescriptor
    ): CallableWeight? {
        if ((receiverParameter.value as? TransientReceiver)?.type?.isFunctionType == true) return null

        val matchingReceiverIndices = HashSet<Int>()
        var bestReceiverType: ReceiverType? = null
        var bestWeight: CallableWeightEnum? = null
        for (receiverType in receiverTypes) {
                val weight = callableWeightForReceiverType(receiverType.type, receiverParameter.type)
            if (weight != null) {
                if (bestWeight == null || weight < bestWeight) {
                    bestWeight = weight
                    bestReceiverType = receiverType
                }
                matchingReceiverIndices.add(receiverType.receiverIndex)
            }
        }

        if (bestWeight == null) return onReceiverTypeMismatch

        val receiverIndex = bestReceiverType!!.receiverIndex

        var receiverIndexToUse: Int? = receiverIndex
        val maxReceiverIndex = receiverTypes.maxOf { it.receiverIndex }
        if (maxReceiverIndex > 0) {
            val matchesAllReceivers = (0..maxReceiverIndex).all { it in matchingReceiverIndices }
            if (matchesAllReceivers) { // if descriptor is matching all receivers then use null as receiverIndex - otherwise e.g. all members of Any would have too high priority
                receiverIndexToUse = null
            }
        }

        return CallableWeight(bestWeight, receiverIndexToUse)
    }

    private fun CallableDescriptor.callableWeightForReceiverType(
        receiverType: CangJieType,
        receiverParameterType: CangJieType
    ): CallableWeightEnum? = when {
        TypeUtils.equalTypes(receiverType, receiverParameterType) -> when {
            // 区分是否在 extend 中声明
            isExtension -> CallableWeightEnum.thisTypeExtension
            else -> CallableWeightEnum.thisClassMember
        }
        receiverType.isSubtypeOf(receiverParameterType) -> when {
            isExtension -> CallableWeightEnum.baseTypeExtension
            else -> CallableWeightEnum.baseClassMember
        }
        else -> null
    }
    private fun CallableDescriptor.callableWeightBasedOnReceiver(
        receiverTypes: Collection<ReceiverType>,
        onReceiverTypeMismatch: CallableWeight?
    ): CallableWeight? {
        // 仓颉语言只有 dispatchReceiverParameter（类成员的 this）
        val receiver = dispatchReceiverParameter ?: return null

        return callableWeightBasedOnReceiver(receiverTypes, onReceiverTypeMismatch, receiver)
    }

    private fun callableWeight(descriptor: DeclarationDescriptor): CallableWeight? {
        if (receiverTypes == null) return null
        if (descriptor !is CallableDescriptor) return null

//        if (descriptor is SamAdapterExtensionFunctionDescriptor) {
//            return callableWeight(descriptor.baseDescriptorForSynthetic)
//        }

        if (descriptor.overriddenDescriptors.isNotEmpty()) {
            // Optimization: when one of direct overridden fits, then nothing can fit better
            descriptor.overriddenDescriptors.mapNotNull {
                it.callableWeightBasedOnReceiver(
                    receiverTypes,
                    onReceiverTypeMismatch = null
                )
            }
                .minByOrNull { it.enum }?.let { return it }

            val overridden = descriptor.overriddenTreeUniqueAsSequence(useOriginal = false)
            return overridden.map { callableWeightBasic(it, receiverTypes) }.minByOrNull { it.enum }!!
        }

        return callableWeightBasic(descriptor, receiverTypes)
    }

    override fun createLookupElement(
        descriptor: DeclarationDescriptor,
        useReceiverTypes: Boolean,
        qualifyNestedClasses: Boolean,
        includeClassTypeArguments: Boolean,
        parametersAndTypeGrayed: Boolean
    ): LookupElement {
        var element = basicFactory.createLookupElement(
            descriptor,
            qualifyNestedClasses,
            includeClassTypeArguments,
            parametersAndTypeGrayed
        )

        if (useReceiverTypes) {
            val weight = callableWeight(descriptor)
            if (weight != null) {
                element.putUserData(CALLABLE_WEIGHT_KEY, weight) // store for use in lookup elements sorting
            }

            element = element.boldIfImmediate(weight)
        }
        return element
    }
}
