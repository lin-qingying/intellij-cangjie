package com.linqingying.cangjie.resolve

import com.linqingying.cangjie.config.LanguageFeature
import com.linqingying.cangjie.config.LanguageVersionSettings
import com.linqingying.cangjie.descriptors.BindingTrace
import com.linqingying.cangjie.diagnostics.Errors
import com.linqingying.cangjie.psi.CjContextReceiverList
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.checker.NewCangJieTypeChecker
import com.linqingying.cangjie.types.util.isTypeParameter
import com.linqingying.cangjie.types.util.supertypes

fun checkContextReceiversAreEnabled(
    trace: BindingTrace,
    languageVersionSettings: LanguageVersionSettings,
    contextReceiverList: CjContextReceiverList
) {
    if (!languageVersionSettings.supportsFeature(LanguageFeature.ContextReceivers)) {
        trace.report(
            Errors.UNSUPPORTED_FEATURE.on(
                contextReceiverList,
                LanguageFeature.ContextReceivers to languageVersionSettings
            )
        )
    }
}
fun checkSubtypingBetweenContextReceivers(
    trace: BindingTrace,
    contextReceiverList: CjContextReceiverList,
    contextReceiverTypes: List<CangJieType>
) {
    fun CangJieType.prepared(): CangJieType = when {
        isTypeParameter() -> supertypes().first()
//        containsTypeParameter() -> replaceArgumentsWithStarProjections()
        else -> this
    }
    for (i in 0 until contextReceiverTypes.lastIndex) {
        val contextReceiverType = contextReceiverTypes[i].prepared()
        for (j in (i + 1) until contextReceiverTypes.size) {
            val anotherContextReceiverType = contextReceiverTypes[j].prepared()
            if (NewCangJieTypeChecker.Default.isSubtypeOf(contextReceiverType, anotherContextReceiverType) ||
                NewCangJieTypeChecker.Default.isSubtypeOf(anotherContextReceiverType, contextReceiverType)
            ) {
                trace.report(Errors.SUBTYPING_BETWEEN_CONTEXT_RECEIVERS.on(contextReceiverList))
                return
            }
        }
    }
}

