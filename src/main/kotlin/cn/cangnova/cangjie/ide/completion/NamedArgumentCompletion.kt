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

package cn.cangnova.cangjie.ide.completion

import cn.cangnova.cangjie.config.LanguageFeature
import cn.cangnova.cangjie.descriptors.CallableDescriptor
import cn.cangnova.cangjie.descriptors.ValueParameterDescriptor
import cn.cangnova.cangjie.icon.CangJieIcons
import cn.cangnova.cangjie.ide.ArgumentPositionData
import cn.cangnova.cangjie.ide.ExpectedInfo
import cn.cangnova.cangjie.ide.completion.handlers.NamedArgumentInsertHandler
import cn.cangnova.cangjie.ide.projectStructure.languageVersionSettings
import cn.cangnova.cangjie.name.Name
import cn.cangnova.cangjie.psi.*
import cn.cangnova.cangjie.psi.psiUtil.getStrictParentOfType
import cn.cangnova.cangjie.resolve.ResolutionFacade
import cn.cangnova.cangjie.resolve.caches.resolveToCall
import cn.cangnova.cangjie.resolve.calls.components.isVararg
import cn.cangnova.cangjie.resolve.calls.model.ResolvedCall
import cn.cangnova.cangjie.resolve.calls.util.getParameterForArgument
import cn.cangnova.cangjie.types.CangJieType
import cn.cangnova.cangjie.utils.CallType
import com.intellij.codeInsight.lookup.LookupElementBuilder

object NamedArgumentCompletion {
    fun isOnlyNamedArgumentExpected(nameExpression: CjSimpleNameExpression, resolutionFacade: ResolutionFacade): Boolean {
        val thisArgument = nameExpression.parent as? CjValueArgument ?: return false

        if (thisArgument.isNamed()) return false

        val resolvedCall = thisArgument.getStrictParentOfType<CjCallElement>()?.resolveToCall(resolutionFacade) ?: return false

        return !thisArgument.canBeUsedWithoutNameInCall(resolvedCall)
    }

    fun complete(collector: LookupElementsCollector, expectedInfos: Collection<ExpectedInfo>, callType: CallType<*>) {
        if (callType != CallType.DEFAULT) return

        val nameToParameterType = HashMap<Name, MutableSet<CangJieType>>()
        for (expectedInfo in expectedInfos) {
            val argumentData = expectedInfo.additionalData as? ArgumentPositionData.Positional ?: continue
            for (parameter in argumentData.namedArgumentCandidates) {
                nameToParameterType.getOrPut(parameter.name) { HashSet() }.add(parameter.type)
            }
        }

        for ((name, types) in nameToParameterType) {
            val typeText = types.singleOrNull()?.let { BasicLookupElementFactory.SHORT_NAMES_RENDERER.renderType(it) } ?: "..."
            val nameString = name.asString()
            val lookupElement = LookupElementBuilder.create("$nameString =")
                .withPresentableText("$nameString =")
                .withTailText(" $typeText")
                .withIcon(CangJieIcons.PARAMETER)
                .withInsertHandler(NamedArgumentInsertHandler(name))
            lookupElement.putUserData(SmartCompletionInBasicWeigher.NAMED_ARGUMENT_KEY, Unit)
            collector.addElement(lookupElement)
        }
    }
}

/**
 * Checks whether argument in the [resolvedCall] can be used without its name (as positional argument).
 */
fun CjValueArgument.canBeUsedWithoutNameInCall(resolvedCall: ResolvedCall<out CallableDescriptor>): Boolean {
    if (resolvedCall.resultingDescriptor.valueParameters.isEmpty()) return true



    val argumentsThatCanBeUsedWithoutName = collectAllArgumentsThatCanBeUsedWithoutName(resolvedCall).map { it.argument }
    if (argumentsThatCanBeUsedWithoutName.isEmpty() || argumentsThatCanBeUsedWithoutName.none { it == this }) return false

    val argumentsBeforeThis = argumentsThatCanBeUsedWithoutName.takeWhile { it != this }
    return if (languageVersionSettings.supportsFeature(LanguageFeature.MixedNamedArgumentsInTheirOwnPosition)) {
        argumentsBeforeThis.none { it.isNamed() && !it.placedOnItsOwnPositionInCall(resolvedCall) }
    } else {
        argumentsBeforeThis.none { it.isNamed() }
    }
}

data class ArgumentThatCanBeUsedWithoutName(
    val argument: CjValueArgument,
    /**
     * When we didn't manage to map an argument to the appropriate parameter then the parameter is `null`. It's useful for cases when we
     * want to analyze possibility for the argument to be used without name even when appropriate parameter doesn't yet exist
     * (it may start existing when user will create the parameter from usage with "Add parameter to function" refactoring)
     */
    val parameter: ValueParameterDescriptor?
)

fun collectAllArgumentsThatCanBeUsedWithoutName(
    resolvedCall: ResolvedCall<out CallableDescriptor>,
): List<ArgumentThatCanBeUsedWithoutName> {
    val arguments = resolvedCall.call.valueArguments.filterIsInstance<CjValueArgument>()
    val argumentAndParameters = arguments.map { argument ->
        val parameter = resolvedCall.getParameterForArgument(argument)
        argument to parameter
    }.sortedBy { (_, parameter) -> parameter?.index ?: Int.MAX_VALUE }

    val firstVarargArgumentIndex = argumentAndParameters.indexOfFirst { (_, parameter) -> parameter?.isVararg ?: false }
    val lastVarargArgumentIndex = argumentAndParameters.indexOfLast { (_, parameter) -> parameter?.isVararg ?: false }
    return argumentAndParameters
        .asSequence()
        .mapIndexed { argumentIndex, (argument, parameter) ->
            val parameterIndex = parameter?.index ?: argumentIndex
            val isAfterVararg = lastVarargArgumentIndex != -1 && argumentIndex > lastVarargArgumentIndex
            val isVarargArg = argumentIndex in firstVarargArgumentIndex..lastVarargArgumentIndex
            if (!isVarargArg && argumentIndex != parameterIndex ||
                isAfterVararg ||
                isVarargArg && argumentAndParameters.drop(lastVarargArgumentIndex + 1).any { (argument, _) -> !argument.isNamed() }
            ) {
                null
            } else {
                ArgumentThatCanBeUsedWithoutName(argument, parameter)
            }
        }
        .takeWhile { it != null } // When any argument can't be used without a name then all subsequent arguments must have a name too!
        .map { it ?: error("It cannot be null because of the previous takeWhile in the chain") }
        .toList()
}


fun ValueArgument.placedOnItsOwnPositionInCall(resolvedCall: ResolvedCall<out CallableDescriptor>): Boolean {
    return resolvedCall.getParameterForArgument(this)?.index == resolvedCall.call.valueArguments.indexOf(this)
}
