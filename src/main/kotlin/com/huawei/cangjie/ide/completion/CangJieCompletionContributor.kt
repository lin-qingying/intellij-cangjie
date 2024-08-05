package com.huawei.cangjie.ide.completion

import com.huawei.cangjie.ide.completion.back.implCommon.StringTemplateCompletion
import com.huawei.cangjie.psi.CjFile
import com.intellij.codeInsight.completion.*
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.platform.ml.impl.turboComplete.KindExecutingCompletionContributor
import com.intellij.platform.ml.impl.turboComplete.KindVariety
import com.intellij.platform.ml.impl.turboComplete.SuggestionGeneratorExecutor
import com.intellij.util.indexing.DumbModeAccessType


class CangJieCompletionContributor :  KindExecutingCompletionContributor() {
    override val kindVariety: KindVariety = CangJieKindVariety

    override fun collectKinds(
        parameters: CompletionParameters,
        generatorExecutor: SuggestionGeneratorExecutor,
        result: CompletionResultSet
    ) {
        StringTemplateCompletion.correctParametersForInStringTemplateCompletion(parameters)?.let { correctedParameters ->
//            generateCompletionKinds(correctedParameters, generatorExecutor, result, ::wrapLookupElementForStringTemplateAfterDotCompletion)
            return
        }

        DumbModeAccessType.RELIABLE_DATA_ONLY.ignoreDumbMode(ThrowableComputable {
//            generateCompletionKinds(parameters, generatorExecutor, result, null)
        })
    }

    override fun shouldBeCalled(parameters: CompletionParameters): Boolean {
        val position = parameters.position
        val parametersOriginFile = parameters.originalFile
        return position.containingFile is CjFile && parametersOriginFile is CjFile
    }


}
