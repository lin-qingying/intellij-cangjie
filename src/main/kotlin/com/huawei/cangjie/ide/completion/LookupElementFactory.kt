package com.huawei.cangjie.ide.completion

import com.huawei.cangjie.builtins.isFunctionType
import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.resolve.isExtension
import com.huawei.cangjie.resolve.overriddenTreeUniqueAsSequence
import com.huawei.cangjie.resolve.scopes.receivers.TransientReceiver
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.util.TypeUtils
import com.huawei.cangjie.types.util.isSubtypeOf
import com.huawei.cangjie.utils.CallType
import com.huawei.cangjie.utils.ReceiverType
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.openapi.editor.Editor

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
    override fun createStandardLookupElementsForDescriptor(
        descriptor: DeclarationDescriptor,
        useReceiverTypes: Boolean
    ): Collection<LookupElement> {
        TODO("Not yet implemented")
    }

    private fun LookupElement.boldIfImmediate(weight: CallableWeight?): LookupElement {
        val style = when (weight?.enum) {
            CallableWeightEnum.thisClassMember, CallableWeightEnum.thisTypeExtension -> Style.BOLD
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
    private fun CallableDescriptor.isExtensionForTypeParameter(): Boolean {
        val receiverParameter = original.extensionReceiverParameter ?: return false
        val typeParameter = receiverParameter.type.constructor.declarationDescriptor as? TypeParameterDescriptor ?: return false
        return typeParameter.containingDeclaration == original
    }
    private fun CallableDescriptor.callableWeightForReceiverType(
        receiverType: CangJieType,
        receiverParameterType: CangJieType
    ): CallableWeightEnum? = when {
        TypeUtils.equalTypes(receiverType, receiverParameterType) -> when {
            isExtensionForTypeParameter() -> CallableWeightEnum.typeParameterExtension
            isExtension -> CallableWeightEnum.thisTypeExtension
            else -> CallableWeightEnum.thisClassMember
        }
        receiverType.isSubtypeOf(receiverParameterType) -> if (isExtension) CallableWeightEnum.baseTypeExtension else CallableWeightEnum.baseClassMember
        else -> null
    }
    private fun CallableDescriptor.callableWeightBasedOnReceiver(
        receiverTypes: Collection<ReceiverType>,
        onReceiverTypeMismatch: CallableWeight?
    ): CallableWeight? {

        val bothReceivers = listOfNotNull(extensionReceiverParameter, dispatchReceiverParameter)

        val receiverTypesForFirstReceiver = receiverTypes.filterNot { it.implicit }.ifEmpty { receiverTypes }

        val weights = bothReceivers.zip(generateSequence(receiverTypesForFirstReceiver) { receiverTypes }.asIterable())
            .map { (receiverParameter, receiverTypes) ->
                callableWeightBasedOnReceiver(receiverTypes, onReceiverTypeMismatch, receiverParameter)
            }

        if (weights.any { it == onReceiverTypeMismatch }) return onReceiverTypeMismatch
        return weights.firstOrNull()
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
