package com.huawei.cangjie.ide.completion.back.context

import com.huawei.cangjie.analyzer.CjSymbolFromIndexProvider
import com.huawei.cangjie.ide.completion.back.CangJieCompletionParameters
import com.huawei.cangjie.ide.completion.back.ImportStrategyDetector
import com.huawei.cangjie.ide.completion.back.LookupElementSink
import com.huawei.cangjie.ide.completion.back.factories.CangJieLookupElementFactory
import com.huawei.cangjie.psi.CjFile
import com.huawei.cangjie.psi.psiUtil.parentOfType
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.PrefixMatcher
import com.intellij.openapi.project.Project

import com.huawei.cangjie.psi.CjElement
class CangJieBasicCompletionContext(
    val parameters: CompletionParameters,
    val sink: LookupElementSink,
    val prefixMatcher: PrefixMatcher,
    val originalCjFile: CjFile,
    val fakeCjFile: CjFile,
    val project: Project,

    val symbolFromIndexProvider: CjSymbolFromIndexProvider,
    val importStrategyDetector: ImportStrategyDetector,
    val lookupElementFactory: CangJieLookupElementFactory = CangJieLookupElementFactory(),
)
{


    companion object {
        fun createFromParameters(firParameters: CangJieCompletionParameters, result: CompletionResultSet): CangJieBasicCompletionContext? {
            val prefixMatcher = result.prefixMatcher
            val parameters = firParameters.ijParameters
            val originalCjFile = parameters.originalFile as? CjFile ?: return null
            val fakeCjFile = parameters.position.containingFile as? CjFile ?: return null
            val useSiteCjElement = parameters.position.parentOfType<CjElement>(withSelf = true) ?: return null

            val project = originalCjFile.project

            return CangJieBasicCompletionContext(
                parameters,
                LookupElementSink(result, firParameters),
                prefixMatcher,
                originalCjFile,
                fakeCjFile,
                project,
//                targetPlatform,
                CjSymbolFromIndexProvider.createForElement(useSiteCjElement),
                ImportStrategyDetector(originalCjFile, project),
            )
        }
    }
}
