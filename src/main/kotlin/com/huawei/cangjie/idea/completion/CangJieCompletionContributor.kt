package com.huawei.cangjie.idea.completion

import com.huawei.cangjie.idea.completion.implCommon.StringTemplateCompletion
import com.huawei.cangjie.idea.completion.implCommon.stringTemplates.wrapLookupElementForStringTemplateAfterDotCompletion
import com.huawei.cangjie.psi.CjFile
import com.huawei.cangjie.psi.CjNameReferenceExpression
import com.huawei.cangjie.utils.getNonStrictParentOfType
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.completion.impl.CamelHumpMatcher
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.openapi.util.registry.Registry
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiComment
import com.intellij.util.ProcessingContext
import com.intellij.util.indexing.DumbModeAccessType


class CangJieCompletionContributor : CompletionContributor() , DumbAware {


//    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
//
//        val text = """abc \$\{1:str:String\}"""
//
//        result.addElement(LookupElementBuilder.create("Hello World"))
//        super.fillCompletionVariants(parameters, result)
//    }

//    companion object {
////        const val DEFAULT_DUMMY_IDENTIFIER: String = CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED + "$"
//          const val DEFAULT_DUMMY_IDENTIFIER: String = CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED
//    }
//
    init {


        val provider = object : CompletionProvider<CompletionParameters>() {
            override fun addCompletions(
                parameters: CompletionParameters,
                context: ProcessingContext,
                result: CompletionResultSet
            ) {
//                println("hello")
//
//                performCompletion(parameters, result)
                val insertText = "exampleFunction(\${1:param1}, \${2:param2}, \${3:param3})"

                // 创建自动补全项，并设置insertText

                // 创建自动补全项，并设置insertText
                val element = LookupElementBuilder.create(insertText)

                // 将自动补全项添加到结果集中

                // 将自动补全项添加到结果集中
                result.addElement(element)

            }
        }
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), provider)
        extend(CompletionType.SMART, PlatformPatterns.psiElement(), provider)
    }




//
    private fun shouldSuppressCompletion(parameters: CompletionParameters, prefixMatcher: PrefixMatcher): Boolean {
        val position = parameters.position
        val invocationCount = parameters.invocationCount

        if (prefixMatcher is CamelHumpMatcher && prefixMatcher.isTypoTolerant) return true




        if (invocationCount == 0 && Registry.`is`("cangjie.disable.auto.completion.inside.expression", false)) {
            val originalPosition = parameters.originalPosition
            val originalExpression = originalPosition?.getNonStrictParentOfType<CjNameReferenceExpression>()
            val expression = position.getNonStrictParentOfType<CjNameReferenceExpression>()

            if (expression != null && originalExpression != null &&
                !expression.getReferencedName().startsWith(originalExpression.getReferencedName())
            ) {
                return true
            }
        }

        return false
    }
//
    private fun doComplete(
        parameters: CompletionParameters,
        result: CompletionResultSet,
        lookupElementPostProcessor: ((LookupElement) -> LookupElement)? = null
    ) {
        val position = parameters.position
        if (position.getNonStrictParentOfType<PsiComment>() != null) {

            return
        }

        if (shouldSuppressCompletion(parameters, result.prefixMatcher)) {
            result.stopHere()
            return
        }




        for (extension in CangJieCompletionExtension.EP_NAME.extensionList) {
            if (extension.perform(parameters, result)) return
        }



        result.restartCompletionWhenNothingMatches()


    }

    private fun performCompletion(parameters: CompletionParameters, result: CompletionResultSet) {
        val position = parameters.position
        val parametersOriginFile = parameters.originalFile
        if (  parametersOriginFile !is CjFile) return


        StringTemplateCompletion.correctParametersForInStringTemplateCompletion(parameters)?.let { correctedParameters ->
            doComplete(correctedParameters, result, ::wrapLookupElementForStringTemplateAfterDotCompletion)
            return
        }
        DumbModeAccessType.RELIABLE_DATA_ONLY.ignoreDumbMode(ThrowableComputable {
            doComplete(parameters, result)
        })
    }
//
//
//
//    override fun beforeCompletion(context: CompletionInitializationContext) {
//
//
//
//
//
//        val offset = context.startOffset
//        val psiFile = context.file
//        val tokenBefore = psiFile.findElementAt(max(0, offset - 1))
////        val token = psiFile.findElementAt(offset)
//
////此代码将使替换偏移量“已修改”，并防止CompletionProgressIndicator中的代码对其进行更改
//        context.replacementOffset = context.replacementOffset
//
//        val dummyIdentifierCorrected =
//            CompletionDummyIdentifierProviderService.getInstance().correctPositionForStringTemplateEntry(context)
//        if (dummyIdentifierCorrected) {
//            return
//        }
//        context.dummyIdentifier = when {
//            context.completionType == CompletionType.SMART -> DEFAULT_DUMMY_IDENTIFIER
//
//            PackageDirectiveCompletion.ACTIVATION_PATTERN.accepts(tokenBefore) -> PackageDirectiveCompletion.DUMMY_IDENTIFIER
//
//            else -> CompletionDummyIdentifierProviderService.getInstance().provideDummyIdentifier(context)
//
//        }
//
//        val tokenAt = psiFile.findElementAt(max(0, offset))
//        if (tokenAt != null) {
////如果在行尾，不要使用父表达式-它可能被错误地解析
//            if (context.completionType == CompletionType.SMART && !isAtEndOfLine(offset, context.editor.document)) {
//                var parent = tokenAt.parent
//                if (parent is CjExpression && parent !is CjBlockExpression) {
////搜索要替换的表达式-当是父表达式的第一个子级时向上
//                    var expression: CjExpression = parent
//                    parent = expression.parent
//                    while (parent is CjExpression && parent.getFirstChild() == expression) {
//                        expression = parent
//                        parent = expression.parent
//                    }
//
//                    val suggestedReplacementOffset = replacementOffsetByExpression(expression)
//                    if (suggestedReplacementOffset > context.replacementOffset) {
//                        context.replacementOffset = suggestedReplacementOffset
//                    }
//
//                    context.offsetMap.addOffset(SmartCompletion.OLD_ARGUMENTS_REPLACEMENT_OFFSET, expression.endOffset)
//
//                    val argumentList = (expression.parent as? CjValueArgument)?.parent as? CjValueArgumentList
//                    if (argumentList != null) {
//                        context.offsetMap.addOffset(
//                            SmartCompletion.MULTIPLE_ARGUMENTS_REPLACEMENT_OFFSET,
//                            argumentList.rightParenthesis?.textRange?.startOffset ?: argumentList.endOffset
//                        )
//                    }
//                }
//            }
//            CompletionDummyIdentifierProviderService.getInstance().correctPositionForParameter(context)
//        }
//    }
//
//    private fun replacementOffsetByExpression(expression: CjExpression): Int {
//        when (expression) {
//            is CjCallExpression -> {
//                val calleeExpression = expression.calleeExpression
//                if (calleeExpression != null) {
//                    return calleeExpression.textRange!!.endOffset
//                }
//            }
//
//            is CjQualifiedExpression -> {
//                val selector = expression.selectorExpression
//                if (selector != null) {
//                    return replacementOffsetByExpression(selector)
//                }
//            }
//        }
//        return expression.textRange!!.endOffset
//    }
//
//    private fun isAtEndOfLine(offset: Int, document: Document): Boolean {
//        var i = offset
//        val chars = document.charsSequence
//        while (i < chars.length) {
//            val c = chars[i]
//            if (c == '\n') return true
//            if (!Character.isWhitespace(c)) return false
//            i++
//        }
//        return true
//    }


}
