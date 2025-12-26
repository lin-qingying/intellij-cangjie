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

package org.cangnova.cangjie.completion.stringTemplates

import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.CjBlockStringTemplateEntry
import org.cangnova.cangjie.psi.CjDotQualifiedExpression
import org.cangnova.cangjie.psi.CjNameReferenceExpression
import org.cangnova.cangjie.psi.psiUtil.endOffset
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionUtilCore

object StringTemplateCompletion {
    fun correctParametersForInStringTemplateCompletion(parameters: CompletionParameters): CompletionParameters? {
        val position = parameters.position
        if (position.node.elementType == CjTokens.LONG_TEMPLATE_ENTRY_START) {
            val expression = (position.parent as? CjBlockStringTemplateEntry)?.expression
            if (expression is CjDotQualifiedExpression) {
                val correctedPosition = (expression.selectorExpression as? CjNameReferenceExpression)?.firstChild
                if (correctedPosition != null) {
                    // ex:
                    // expression: some.IntellijIdeaRulezzz
                    // correctedOffset: ^
                    // expression: some.funcIntellijIdeaRulezzz
                    // correctedOffset      ^
                    val correctedOffset = correctedPosition.endOffset - CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED.length
                    return parameters.withPosition(correctedPosition, correctedOffset)
                }
            }
        }
        return null
    }

}
