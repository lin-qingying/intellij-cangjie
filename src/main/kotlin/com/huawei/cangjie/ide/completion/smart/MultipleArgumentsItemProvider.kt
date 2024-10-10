package com.huawei.cangjie.ide.completion.smart

import com.huawei.cangjie.descriptors.ValueParameterDescriptor
import com.huawei.cangjie.descriptors.VariableDescriptor
import com.huawei.cangjie.icon.CangJieIcons
import com.huawei.cangjie.ide.ArgumentPositionData
import com.huawei.cangjie.ide.CangJieDescriptorIconProvider
import com.huawei.cangjie.ide.ExpectedInfo
import com.huawei.cangjie.ide.Tail
import com.huawei.cangjie.ide.completion.SmartCastCalculator
import com.huawei.cangjie.ide.completion.tryGetOffset
import com.huawei.cangjie.incremental.components.NoLookupLocation
import com.huawei.cangjie.psi.Call
import com.huawei.cangjie.psi.CjExpression
import com.huawei.cangjie.renderer.render
import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.resolve.ResolutionFacade
import com.huawei.cangjie.resolve.calls.components.hasDefaultValue
import com.huawei.cangjie.resolve.isExtension
import com.huawei.cangjie.resolve.scopes.LexicalScope
import com.huawei.cangjie.resolve.scopes.findVariable
import com.huawei.cangjie.resolve.scopes.getResolutionScope
import com.huawei.cangjie.resolve.scopes.getVariableFromImplicitReceivers
import com.huawei.cangjie.types.checker.CangJieTypeChecker
import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.ui.LayeredIcon
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
