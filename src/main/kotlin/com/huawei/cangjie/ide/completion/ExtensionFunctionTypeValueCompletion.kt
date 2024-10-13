package com.huawei.cangjie.ide.completion

import com.huawei.cangjie.builtins.isExtensionFunctionType
import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.descriptors.FunctionDescriptor
import com.huawei.cangjie.ide.util.substituteExtensionIfCallable
import com.huawei.cangjie.incremental.components.NoLookupLocation
import com.huawei.cangjie.resolve.calls.tasks.createSynthesizedInvokes
import com.huawei.cangjie.utils.CallType
import com.huawei.cangjie.utils.OperatorNameConventions
import com.huawei.cangjie.utils.ReceiverType
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.codeInsight.lookup.LookupElementPresentation


class ExtensionFunctionTypeValueCompletion(
    private val receiverTypes: Collection<ReceiverType>,
    private val callType: CallType<*>,
    private val lookupElementFactory: LookupElementFactory
) {
    data class Result(val invokeDescriptor: FunctionDescriptor, val factory: AbstractLookupElementFactory)

    fun processVariables(variablesProvider: RealContextVariablesProvider): Collection<Result> {
        if (callType != CallType.DOT && callType != CallType.SAFE) return emptyList()

        val results = ArrayList<Result>()

        for (variable in variablesProvider.allFunctionTypeVariables) {
            val variableType = variable.type
            if (!variableType.isExtensionFunctionType) continue

            val invokes = variableType.memberScope.getContributedFunctions(OperatorNameConventions.INVOKE, NoLookupLocation.FROM_IDE)
            for (invoke in createSynthesizedInvokes(invokes)) {
                for (substituted in invoke.substituteExtensionIfCallable(receiverTypes.map { it.type }, callType)) {
                    val factory = object : AbstractLookupElementFactory {
                        override fun createStandardLookupElementsForDescriptor(
                            descriptor: DeclarationDescriptor,
                            useReceiverTypes: Boolean
                        ): Collection<LookupElement> {
                            if (!useReceiverTypes) return emptyList()
                            descriptor as FunctionDescriptor // should be descriptor for "invoke"

                            val invokeLookupElement = lookupElementFactory.createLookupElement(substituted, useReceiverTypes = true)
                            val variableLookupElement = lookupElementFactory.createLookupElement(variable, useReceiverTypes = false)
                            val insertHandler = lookupElementFactory.insertHandlerProvider.insertHandler(invoke)

                            val lookupElement = object : LookupElementDecorator<LookupElement>(variableLookupElement) {
                                override fun renderElement(presentation: LookupElementPresentation) {
                                    invokeLookupElement.renderElement(presentation)

                                    presentation.itemText = variable.name.asString()

                                    val parameterTail = presentation.tailFragments.first()
                                    presentation.clearTail()
                                    presentation.appendTailText(parameterTail.text, false)

                                    lookupElementFactory.basicFactory.appendContainerAndReceiverInformation(variable) {
                                        presentation.appendTailText(it, true)
                                    }
                                }

                                override fun getDecoratorInsertHandler() = insertHandler
                            }

                            return listOf(lookupElement)
                        }

                        override fun createLookupElement(
                            descriptor: DeclarationDescriptor,
                            useReceiverTypes: Boolean,
                            qualifyNestedClasses: Boolean,
                            includeClassTypeArguments: Boolean,
                            parametersAndTypeGrayed: Boolean
                        ): LookupElement? = null
                    }

                    results.add(Result(substituted, factory))
                }
            }
        }

        return results
    }
}
