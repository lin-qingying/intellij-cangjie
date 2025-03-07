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

import cn.cangnova.cangjie.doc.parser.CDocKnownTag
import cn.cangnova.cangjie.doc.psi.CDoc
import cn.cangnova.cangjie.psi.CjDeclaration
import cn.cangnova.cangjie.psi.CjNamedFunction
import cn.cangnova.cangjie.psi.CjTypeStatement
import cn.cangnova.cangjie.psi.psiUtil.getNonStrictParentOfType
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionUtil
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.StandardPatterns
import com.intellij.util.ProcessingContext
import java.util.*


object CDocTagCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
        // findIdentifierPrefix() requires identifier part characters to be a superset of identifier start characters
        val prefix = CompletionUtil.findIdentifierPrefix(
            parameters.position.containingFile,
            parameters.offset,
            StandardPatterns.character().javaIdentifierPart() or singleCharPattern('@'),
            StandardPatterns.character().javaIdentifierStart() or singleCharPattern('@')
        )

        if (parameters.isAutoPopup && prefix.isEmpty()) return
        if (prefix.isNotEmpty() && !prefix.startsWith('@')) {
            return
        }
        val cdocOwner = parameters.position.getNonStrictParentOfType<CDoc>()?.getOwner()
        val resultWithPrefix = result.withPrefixMatcher(prefix)
        CDocKnownTag.entries.forEach {
            if (cdocOwner == null || it.isApplicable(cdocOwner)) {
                resultWithPrefix.addElement(LookupElementBuilder.create("@" + it.name.lowercase(Locale.US)))
            }
        }
    }

    private fun CDocKnownTag.isApplicable(declaration: CjDeclaration) = when (this) {
        CDocKnownTag.CONSTRUCTOR,
        CDocKnownTag.PROPERTY -> declaration is CjTypeStatement

        CDocKnownTag.RETURN -> declaration is CjNamedFunction

        CDocKnownTag.RECEIVER -> declaration is CjNamedFunction && declaration.receiverTypeReference != null

        CDocKnownTag.AUTHOR,
        CDocKnownTag.THROWS,
        CDocKnownTag.EXCEPTION,
        CDocKnownTag.PARAM,
        CDocKnownTag.SEE,
        CDocKnownTag.SINCE,
        CDocKnownTag.SAMPLE,
        CDocKnownTag.SUPPRESS -> true
    }
}
