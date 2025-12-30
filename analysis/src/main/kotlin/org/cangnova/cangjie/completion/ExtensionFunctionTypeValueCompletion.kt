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

import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.descriptors.FunctionDescriptor
import org.cangnova.cangjie.incremental.components.NoLookupLocation
import org.cangnova.cangjie.resolve.calls.tasks.createSynthesizedInvokes
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.codeInsight.lookup.LookupElementPresentation
import org.cangnova.cangjie.name.OperatorNameConventions
import org.cangnova.cangjie.resolve.calls.util.CallType
import org.cangnova.cangjie.resolve.calls.util.ReceiverType


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

            val invokes = variableType.memberScope.getContributedFunctions(OperatorNameConventions.INVOKE, NoLookupLocation.FROM_IDE)
            for (invoke in createSynthesizedInvokes(invokes)) {
                // 在仓颉语言中，invoke 操作符也是普通成员，不需要类型替换
                for (substituted in listOf(invoke)) {
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
