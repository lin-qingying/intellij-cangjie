package com.huawei.cangjie.ide.completion

import com.huawei.cangjie.doc.parser.CDocKnownTag
import com.huawei.cangjie.doc.psi.CDoc
import com.huawei.cangjie.ide.completion.back.or
import com.huawei.cangjie.ide.completion.back.singleCharPattern
import com.huawei.cangjie.psi.CjDeclaration
import com.huawei.cangjie.psi.CjNamedFunction
import com.huawei.cangjie.psi.CjTypeStatement
import com.huawei.cangjie.psi.psiUtil.getNonStrictParentOfType
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionUtil
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.StandardPatterns
import com.intellij.util.ProcessingContext
import java.util.*

class CDocCompletion {
}

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
        val kdocOwner = parameters.position.getNonStrictParentOfType<CDoc>()?.getOwner()
        val resultWithPrefix = result.withPrefixMatcher(prefix)
        CDocKnownTag.values().forEach {
            if (kdocOwner == null || it.isApplicable(kdocOwner)) {
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
