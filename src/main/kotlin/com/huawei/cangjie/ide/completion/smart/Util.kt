package com.huawei.cangjie.ide.completion.smart

import com.huawei.cangjie.descriptors.CallableDescriptor
import com.huawei.cangjie.descriptors.ClassDescriptor
import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.descriptors.VariableDescriptor
import com.huawei.cangjie.ide.ExpectedInfo
import com.huawei.cangjie.ide.completion.SmartCastCalculator
import com.huawei.cangjie.psi.NotNullableUserDataProperty
import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.resolve.ResolutionFacade
import com.huawei.cangjie.types.*
import com.huawei.cangjie.types.checker.SimpleClassicTypeSystemContext.isNothing
import com.huawei.cangjie.types.checker.SimpleClassicTypeSystemContext.isNullableNothing
import com.huawei.cangjie.types.util.TypeNullability
import com.huawei.cangjie.utils.CallTypeAndReceiver
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.util.Key

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
            returnType.type.isNullableNothing() ||
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
    } else if (this is ClassDescriptor && kind.isSingleton) {
        return listOf(defaultType.toFuzzyType(emptyList()))
    } else {
        return emptyList()
    }
}
fun Collection<FuzzyType>.matchExpectedInfo(expectedInfo: ExpectedInfo): ExpectedInfoMatch {
    val sequence = asSequence()
    val substitutor = sequence.map { expectedInfo.matchingSubstitutor(it) }.firstOrNull()
    if (substitutor != null) {
        return ExpectedInfoMatch.match(substitutor)
    }

    if (sequence.any { it.nullability() == TypeNullability.NULLABLE }) {
        val substitutor2 = sequence.map { expectedInfo.matchingSubstitutor(it.makeNotNullable()) }.firstOrNull()
        if (substitutor2 != null) {
            return ExpectedInfoMatch.ifNotNullMatch(substitutor2)
        }
    }

    return ExpectedInfoMatch.noMatch
}
