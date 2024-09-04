package com.huawei.cangjie.resolve.calls.components

import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.resolve.calls.inference.components.ConstraintSystemCompletionMode
import com.huawei.cangjie.resolve.calls.model.CangJieDiagnosticsHolder
import com.huawei.cangjie.resolve.calls.model.ResolvedAtom
import com.huawei.cangjie.resolve.calls.model.ResolvedCallableReferenceArgumentAtom

class PostponedArgumentsAnalyzer(
    private val callableReferenceArgumentResolver: CallableReferenceArgumentResolver,
    private val languageVersionSettings: LanguageVersionSettings
)
{

    fun analyze(
        c: PostponedArgumentsAnalyzerContext,
        resolutionCallbacks: CangJieResolutionCallbacks,
        argument: ResolvedAtom,
        completionMode: ConstraintSystemCompletionMode,
        diagnosticsHolder: CangJieDiagnosticsHolder
    ) {
        when (argument) {
//            is ResolvedLambdaAtom ->
//                analyzeLambda(c, resolutionCallbacks, argument, completionMode, diagnosticsHolder)
//
//            is LambdaWithTypeVariableAsExpectedTypeAtom ->
//                analyzeLambda(
//                    c,
//                    resolutionCallbacks,
//                    argument.transformToResolvedLambda(c.getBuilder(), diagnosticsHolder),
//                    completionMode,
//                    diagnosticsHolder
//                )

            is ResolvedCallableReferenceArgumentAtom ->
                callableReferenceArgumentResolver.processCallableReferenceArgument(
                    c.getBuilder(), argument, diagnosticsHolder, resolutionCallbacks
                )

//            is ResolvedCollectionLiteralAtom -> TODO("Not supported")

            else -> error("Unexpected resolved primitive: ${argument.javaClass.canonicalName}")
        }
    }
}
