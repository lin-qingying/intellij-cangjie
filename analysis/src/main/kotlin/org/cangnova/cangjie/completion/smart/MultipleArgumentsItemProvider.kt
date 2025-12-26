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

package org.cangnova.cangjie.completion.smart

import org.cangnova.cangjie.descriptors.ValueParameterDescriptor
import org.cangnova.cangjie.descriptors.VariableDescriptor
import org.cangnova.cangjie.icon.CangJieIcons
import org.cangnova.cangjie.indices.ArgumentPositionData
import org.cangnova.cangjie.indices.ExpectedInfo
import org.cangnova.cangjie.indices.Tail
import org.cangnova.cangjie.completion.SmartCastCalculator
import org.cangnova.cangjie.completion.tryGetOffset
import org.cangnova.cangjie.incremental.components.NoLookupLocation
import org.cangnova.cangjie.psi.Call
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.renderer.render
import org.cangnova.cangjie.resolve.ResolutionFacade
import org.cangnova.cangjie.resolve.calls.components.hasDefaultValue
import org.cangnova.cangjie.resolve.scopes.LexicalScope
import org.cangnova.cangjie.resolve.scopes.findVariable
import org.cangnova.cangjie.resolve.scopes.getResolutionScope
import org.cangnova.cangjie.resolve.scopes.getVariableFromImplicitReceivers
import org.cangnova.cangjie.types.checker.CangJieTypeChecker
import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.ui.LayeredIcon
import org.cangnova.cangjie.icon.CangJieDescriptorIconProvider
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.utils.isExtension
import java.util.ArrayList
import java.util.HashSet

class MultipleArgumentsItemProvider(
    private val bindingContext: BindingContext,
    private val smartCastCalculator: SmartCastCalculator,
    private val resolutionFacade: ResolutionFacade
) {

    fun addToCollection(
        collection: MutableCollection<LookupElement>,
        expectedInfos: Collection<ExpectedInfo>,
        context: CjExpression
    ) {
        val resolutionScope = context.getResolutionScope(bindingContext, resolutionFacade)

        val added = HashSet<String>()
        for (expectedInfo in expectedInfos) {
            val additionalData = expectedInfo.additionalData
            if (additionalData is ArgumentPositionData.Positional) {
                val parameters = additionalData.function.valueParameters.drop(additionalData.argumentIndex)
                if (parameters.size > 1) {
                    val tail = when (additionalData.callType) {
                        Call.CallType.ARRAY_GET_METHOD, Call.CallType.ARRAY_SET_METHOD -> Tail.RBRACKET
                        else -> Tail.RPARENTH
                    }
                    val variables = ArrayList<VariableDescriptor>()
                    for ((i, parameter) in parameters.withIndex()) {
                        variables.add(variableInScope(parameter, resolutionScope) ?: break)

                        // this is the last parameter or all others have default values
                        if (i > 0 && parameters.asSequence().drop(i + 1).all { it.hasDefaultValue() }) {
                            val lookupElement = createParametersLookupElement(variables, tail)
                            if (added.add(lookupElement.lookupString)) { // check that we don't already have item with the same text
                                collection.add(lookupElement)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun createParametersLookupElement(variables: List<VariableDescriptor>, tail: Tail): LookupElement {
        val compoundIcon = LayeredIcon(2)
        val firstIcon = CangJieDescriptorIconProvider.getIcon(variables.first(), null, 0) ?: CangJieIcons.PARAMETER
        val lastIcon = CangJieDescriptorIconProvider.getIcon(variables.last(), null, 0) ?: CangJieIcons.PARAMETER
        compoundIcon.setIcon(lastIcon, 0, 2 * firstIcon.iconWidth / 5, 0)
        compoundIcon.setIcon(firstIcon, 1, 0, 0)

        return LookupElementBuilder.create(variables.joinToString(", ") { it.name.render() }) //TODO: use code formatting settings
            .withInsertHandler { context, _ ->
                if (context.completionChar == Lookup.REPLACE_SELECT_CHAR) {
                    val offset = context.offsetMap.tryGetOffset(SmartCompletion.MULTIPLE_ARGUMENTS_REPLACEMENT_OFFSET)
                    if (offset != null) {
                        context.document.deleteString(context.tailOffset, offset)
                    }
                }

            }
            .withIcon(compoundIcon)
            .addTail(tail)
            .assignSmartCompletionPriority(SmartCompletionItemPriority.MULTIPLE_ARGUMENTS_ITEM)
    }

    private fun variableInScope(parameter: ValueParameterDescriptor, scope: LexicalScope): VariableDescriptor? {
        val name = parameter.name
        //TODO: there can be more than one property with such name in scope and we should be able to select one (but we need API for this)
        val variable = scope.findVariable(name, NoLookupLocation.FROM_IDE) { !it.isExtension }
            ?: scope.getVariableFromImplicitReceivers(name) ?: return null
        return if (smartCastCalculator.types(variable).any { CangJieTypeChecker.DEFAULT.isSubtypeOf(it, parameter.type) })
            variable
        else
            null
    }
}
