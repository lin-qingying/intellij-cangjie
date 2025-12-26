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

import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.impl.LocalVariableDescriptor
import org.cangnova.cangjie.completion.SmartCastCalculator
import org.cangnova.cangjie.completion.handlers.WithExpressionPrefixInsertHandler
import org.cangnova.cangjie.completion.handlers.WithTailInsertHandler
import org.cangnova.cangjie.completion.suppressAutoInsertion
import org.cangnova.cangjie.psi.NotNullableUserDataProperty
import org.cangnova.cangjie.resolve.ResolutionFacade
import org.cangnova.cangjie.types.*
import org.cangnova.cangjie.types.checker.SimpleClassicTypeSystemContext.isNothing
import com.intellij.codeInsight.completion.PrefixMatcher
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.openapi.util.Key
import org.cangnova.cangjie.indices.ExpectedInfo
import org.cangnova.cangjie.indices.ItemOptions
import org.cangnova.cangjie.indices.ReturnValueAdditionalData
import org.cangnova.cangjie.indices.Tail
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.calls.util.CallTypeAndReceiver
import org.cangnova.cangjie.resolve.descriptorsEqualWithSubstitution

enum class SmartCompletionItemPriority {
    ARRAY_LITERAL_IN_ANNOTATION,
    MULTIPLE_ARGUMENTS_ITEM,
    LAMBDA_SIGNATURE,
    LAMBDA_SIGNATURE_EXPLICIT_PARAMETER_TYPES,
    IT,
    TRUE,
    FALSE,
    NAMED_ARGUMENT_TRUE,
    NAMED_ARGUMENT_FALSE,
    CLASS_LITERAL,
    THIS,
    DELEGATES_STATIC_MEMBER,
    ENUM_ENTRIES,
    DEFAULT,
    NULLABLE,
    INSTANTIATION,
    STATIC_MEMBER,
    ANONYMOUS_OBJECT,
    LAMBDA_NO_PARAMS,
    LAMBDA,
    CALLABLE_REFERENCE,
    NULL,
    NAMED_ARGUMENT_NULL,
    INHERITOR_INSTANTIATION
}

val SMART_COMPLETION_ITEM_PRIORITY_KEY = Key<SmartCompletionItemPriority>("SMART_COMPLETION_ITEM_PRIORITY_KEY")

class ExpectedInfoMatch
private constructor(
    val substitutor: TypeSubstitutor?,
    val makeNotNullable: Boolean
) {
    fun isMatch() = substitutor != null && !makeNotNullable

    companion object {
        val noMatch = ExpectedInfoMatch(null, false)
        fun match(substitutor: TypeSubstitutor) = ExpectedInfoMatch(substitutor, false)
        fun ifNotNullMatch(substitutor: TypeSubstitutor) = ExpectedInfoMatch(substitutor, true)
    }
}

internal var LookupElement.keywordProbability: KeywordProbability
        by NotNullableUserDataProperty(Key.create("KEYWORD_PROBABILITY_KEY"), KeywordProbability.DEFAULT)

/**
 * In some completion contexts, certain keywords are more probable than others. This enum together with
 * [keywordProbability] property are used to capture that. They should be considered when weighting completion
 * items.
 */
internal enum class KeywordProbability {
    HIGH,
    DEFAULT,
    LOW,
}

fun DeclarationDescriptor.fuzzyTypesForSmartCompletion(
    smartCastCalculator: SmartCastCalculator,
    callTypeAndReceiver: CallTypeAndReceiver<*, *>,
    resolutionFacade: ResolutionFacade,
    bindingContext: BindingContext,
): Collection<FuzzyType> {
//    if (callTypeAndReceiver is CallTypeAndReceiver.CALLABLE_REFERENCE) {
//        val lhs = callTypeAndReceiver.receiver?.let { bindingContext[BindingContext.DOUBLE_COLON_LHS, it] }
//        return listOfNotNull((this as? CallableDescriptor)?.callableReferenceType(resolutionFacade, lhs, callTypeAndReceiver.settings))
//    }

    if (this is CallableDescriptor) {
        val returnType = fuzzyReturnType() ?: return emptyList()

        // skip declarations of types Nothing, Nothing?, dynamic or of generic parameter type which has no real bounds
        if (returnType.type.isNothing() ||

            returnType.type.isDynamic() ||
            returnType.isAlmostEverything()
        ) {
            return emptyList()
        }

        return if (this is VariableDescriptor) { //TODO: generic properties!
            smartCastCalculator.types(this).map { it.toFuzzyType(emptyList()) }
        } else {
            listOf(returnType)
        }
    }  else {
        return emptyList()
    }
}

fun Collection<FuzzyType>.matchExpectedInfo(expectedInfo: ExpectedInfo): ExpectedInfoMatch {
    val sequence = asSequence()
    val substitutor = sequence.map { expectedInfo.matchingSubstitutor(it) }.firstOrNull()
    if (substitutor != null) {
        return ExpectedInfoMatch.match(substitutor)
    }

    if (sequence.any { it.optionality() == TypeOptionality.OPTIONAL }) {
        val substitutor2 = sequence.map { expectedInfo.matchingSubstitutor(it.makeNonOption()) }.firstOrNull()
        if (substitutor2 != null) {
            return ExpectedInfoMatch.ifNotNullMatch(substitutor2)
        }
    }

    return ExpectedInfoMatch.noMatch
}

fun LookupElement.withOptions(options: ItemOptions): LookupElement =
    if (options.starPrefix) {
        object : LookupElementDecorator<LookupElement>(this) {
            override fun renderElement(presentation: LookupElementPresentation) {
                super.renderElement(presentation)
                presentation.itemText = "*" + presentation.itemText
            }

            override fun getDelegateInsertHandler() = WithExpressionPrefixInsertHandler("*")
        }
    } else {
        this
    }

fun FuzzyType.matchExpectedInfo(expectedInfo: ExpectedInfo) = listOf(this).matchExpectedInfo(expectedInfo)
fun <TDescriptor : DeclarationDescriptor?> MutableCollection<LookupElement>.addLookupElements(
    descriptor: TDescriptor,
    expectedInfos: Collection<ExpectedInfo>,
    infoMatcher: (ExpectedInfo) -> ExpectedInfoMatch,
    noNameSimilarityForReturnItself: Boolean = false,
    lookupElementFactory: (TDescriptor) -> Collection<LookupElement>
) {
    class ItemData(val descriptor: TDescriptor, val itemOptions: ItemOptions) {
        @Suppress("UNCHECKED_CAST")
        override fun equals(other: Any?) = descriptorsEqualWithSubstitution(
            this.descriptor,
            (other as? ItemData)?.descriptor
        ) && itemOptions == (other as? ItemData)?.itemOptions

        override fun hashCode() = if (this.descriptor != null) this.descriptor.original.hashCode() else 0
    }

    fun ItemData.createLookupElements() = lookupElementFactory(this.descriptor).map { it.withOptions(this.itemOptions) }

    val matchedInfos = HashMap<ItemData, MutableList<ExpectedInfo>>()
    val makeNullableInfos = HashMap<ItemData, MutableList<ExpectedInfo>>()
    for (info in expectedInfos) {
        val classification = infoMatcher(info)
        if (classification.substitutor != null) {
            val substitutedDescriptor = descriptor.substituteFixed(classification.substitutor)
            val map = if (classification.makeNotNullable) makeNullableInfos else matchedInfos
            map.getOrPut(ItemData(substitutedDescriptor, info.itemOptions)) { ArrayList() }.add(info)
        }
    }

    if (matchedInfos.isNotEmpty()) {
        for ((itemData, infos) in matchedInfos) {
            val lookupElements = itemData.createLookupElements()
            val nameSimilarityInfos = if (noNameSimilarityForReturnItself && descriptor is CallableDescriptor) {
                infos.filter { (it.additionalData as? ReturnValueAdditionalData)?.callable != descriptor } // do not calculate name similarity with function itself in its return
            } else
                infos
            lookupElements.mapTo(this) { it.addTailAndNameSimilarity(infos, nameSimilarityInfos) }
        }
    } else {
        for ((itemData, infos) in makeNullableInfos) {
            addLookupElementsForNullable({ itemData.createLookupElements() }, infos)
        }
    }
}
fun shouldCompleteThisItems(prefixMatcher: PrefixMatcher): Boolean {
    val prefix = prefixMatcher.prefix
    val s = "this@"
    return prefix.startsWith(s) || s.startsWith(prefix)
}

private fun MutableCollection<LookupElement>.addLookupElementsForNullable(
    factory: () -> Collection<LookupElement>,
    matchedInfos: Collection<ExpectedInfo>
) {
    fun LookupElement.postProcess(): LookupElement {
        var element = this
        element = element.suppressAutoInsertion()
        element = element.assignSmartCompletionPriority(SmartCompletionItemPriority.NULLABLE)
        element = element.addTailAndNameSimilarity(matchedInfos)
        return element
    }

    factory().mapTo(this) {
        object : LookupElementDecorator<LookupElement>(it) {
            override fun renderElement(presentation: LookupElementPresentation) {
                super.renderElement(presentation)
                presentation.itemText = "!! " + presentation.itemText
            }

            override fun getDelegateInsertHandler() = WithTailInsertHandler("!!", spaceBefore = false, spaceAfter = false)
        }.postProcess()
    }

    factory().mapTo(this) {
        object : LookupElementDecorator<LookupElement>(it) {
            override fun renderElement(presentation: LookupElementPresentation) {
                super.renderElement(presentation)
                presentation.itemText = "?: " + presentation.itemText
            }

            override fun getDelegateInsertHandler() = WithTailInsertHandler("?:", spaceBefore = true, spaceAfter = true)
        }.postProcess()
    }
}

private fun <T : DeclarationDescriptor?> T.substituteFixed(substitutor: TypeSubstitutor): T {
    if (this is LocalVariableDescriptor || this is ValueParameterDescriptor || this !is Substitutable<*>) {
        return this
    }
    return this.substitute(substitutor) as T
}

fun LookupElement.addTailAndNameSimilarity(
    matchedExpectedInfos: Collection<ExpectedInfo>,
    nameSimilarityExpectedInfos: Collection<ExpectedInfo> = matchedExpectedInfos
): LookupElement {
    val lookupElement = addTail(mergeTails(matchedExpectedInfos.map { it.tail }))
    val similarity = calcNameSimilarity(lookupElement.lookupString, nameSimilarityExpectedInfos)
    if (similarity != 0) {
        lookupElement.putUserData(NAME_SIMILARITY_KEY, similarity)
    }
    return lookupElement
}

fun LookupElement.addTail(tail: Tail?): LookupElement = when (tail) {
    null -> this
    Tail.COMMA -> LookupElementDecorator.withDelegateInsertHandler(this, WithTailInsertHandler.COMMA)
    Tail.RPARENTH -> LookupElementDecorator.withDelegateInsertHandler(this, WithTailInsertHandler.RPARENTH)
    Tail.RBRACKET -> LookupElementDecorator.withDelegateInsertHandler(this, WithTailInsertHandler.RBRACKET)
    Tail.ELSE -> LookupElementDecorator.withDelegateInsertHandler(this, WithTailInsertHandler.ELSE)
    Tail.RBRACE -> LookupElementDecorator.withDelegateInsertHandler(this, WithTailInsertHandler.RBRACE)
}

fun mergeTails(tails: Collection<Tail?>): Tail? = tails.singleOrNull() ?: tails.toSet().singleOrNull()
fun LookupElement.assignSmartCompletionPriority(priority: SmartCompletionItemPriority): LookupElement {
    putUserData(SMART_COMPLETION_ITEM_PRIORITY_KEY, priority)
    return this
}
