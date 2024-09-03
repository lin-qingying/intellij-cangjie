package com.huawei.cangjie.resolve

import com.huawei.cangjie.config.LanguageFeature
import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.descriptors.BindingTrace
import com.huawei.cangjie.descriptors.Errors
import com.huawei.cangjie.psi.CjContextReceiverList
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.checker.NewCangJieTypeChecker
import com.huawei.cangjie.types.util.isTypeParameter
import com.huawei.cangjie.types.util.supertypes

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

