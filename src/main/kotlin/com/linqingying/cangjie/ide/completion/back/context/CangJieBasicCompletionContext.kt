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

package com.linqingying.cangjie.ide.completion.back.context

import com.linqingying.cangjie.analyzer.CjSymbolFromIndexProvider
import com.linqingying.cangjie.ide.completion.back.CangJieCompletionParameters
import com.linqingying.cangjie.ide.completion.back.ImportStrategyDetector
import com.linqingying.cangjie.ide.completion.back.LookupElementSink
import com.linqingying.cangjie.ide.completion.back.factories.CangJieLookupElementFactory
import com.linqingying.cangjie.psi.CjFile
import com.linqingying.cangjie.psi.psiUtil.parentOfType
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.PrefixMatcher
import com.intellij.openapi.project.Project

import com.linqingying.cangjie.psi.CjElement
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
