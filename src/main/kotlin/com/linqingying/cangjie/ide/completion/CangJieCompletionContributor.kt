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

package com.linqingying.cangjie.ide.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.completion.addingPolicy.PolicyController
import com.intellij.codeInsight.completion.impl.CamelHumpMatcher
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.openapi.util.registry.Registry
import com.intellij.patterns.PlatformPatterns.elementType
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.platform.ml.impl.turboComplete.KindCollector
import com.intellij.platform.ml.impl.turboComplete.KindVariety
import com.intellij.platform.ml.impl.turboComplete.SmartPipelineRunner
import com.intellij.platform.ml.impl.turboComplete.SuggestionGeneratorExecutor
import com.intellij.psi.PsiComment
import com.intellij.util.indexing.DumbModeAccessType
import com.linqingying.cangjie.configurable.services.CangJieLanguageServerServices
import com.linqingying.cangjie.configurable.services.Feature
import com.linqingying.cangjie.ide.completion.smart.SmartCompletion
import com.linqingying.cangjie.ide.completion.stringTemplates.StringTemplateCompletion
import com.linqingying.cangjie.ide.completion.stringTemplates.wrapLookupElementForStringTemplateAfterDotCompletion
import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.psi.*
import com.linqingying.cangjie.psi.psiUtil.endOffset
import com.linqingying.cangjie.psi.psiUtil.getNonStrictParentOfType
import kotlin.math.max


abstract class CangJieKindExecutingCompletionContributor : CompletionContributor(), KindCollector {
    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        SmartPipelineRunner.getOneOrDefault().runPipeline(this, parameters, result)
    }
}

class CangJieCompletionContributor : CangJieKindExecutingCompletionContributor() {

    override val kindVariety: KindVariety = CangJieKindVariety
    private val AFTER_NUMBER_LITERAL = psiElement().afterLeafSkipping(
        psiElement().withText(""),
        psiElement().withElementType(elementType().oneOf(CjTokens.FLOAT_LITERAL, CjTokens.INTEGER_LITERAL))
    )
    private val AFTER_INTEGER_LITERAL_AND_DOT = psiElement().afterLeafSkipping(
        psiElement().withText("."),
        psiElement().withElementType(elementType().oneOf(CjTokens.INTEGER_LITERAL))
    )

    /**
     * 收集补全类型
     *
     * 该函数负责根据给定的参数收集补全类型，并将结果添加到指定的补全结果集中
     * 它首先尝试处理字符串模板中的补全，如果适用，则使用修正后的参数进行补全
     * 如果不适用于字符串模板补全，则在正常模式下进行补全
     *
     * @param parameters 补全参数，包含了补全所需的各种信息
     * @param generatorExecutor 用于执行建议生成器的执行器
     * @param result 用于存储补全结果的对象
     */
    override fun collectKinds(
        parameters: CompletionParameters,
        generatorExecutor: SuggestionGeneratorExecutor,
        result: CompletionResultSet,
    ) {
        // 尝试修正参数以适应字符串模板中的补全，如果成功，则使用修正后的参数进行补全
        StringTemplateCompletion.correctParametersForInStringTemplateCompletion(parameters)
            ?.let { correctedParameters ->
                generateCompletionKinds(
                    correctedParameters,
                    generatorExecutor,
                    result,
                    ::wrapLookupElementForStringTemplateAfterDotCompletion
                )
                return
            }

        // 如果上述尝试失败，则在正常模式下进行补全，确保只使用可靠的数据进行补全
        DumbModeAccessType.RELIABLE_DATA_ONLY.ignoreDumbMode(ThrowableComputable {
            generateCompletionKinds(parameters, generatorExecutor, result, null)
        })
    }

    /**
     * 判断是否应该调用补全功能。
     *
     * 该函数根据特定条件决定是否应调用补全功能。主要检查 SEMANTIC_TOKENS 功能是否启用，
     * 以及当前文件和参数来源文件是否为 CjFile 类型。
     *
     * @param parameters 补全参数，包含位置和原始文件信息
     * @return 如果 SEMANTIC_TOKENS 功能已启用或文件类型不符合要求，则返回 false；否则返回 true
     */
    override fun shouldBeCalled(parameters: CompletionParameters): Boolean {

        if (!CangJieLanguageServerServices.getInstance().astConfig.isFeatureEnabled(Feature.AUTO_COMPLETE))
            return false

        val position = parameters.position
        val parametersOriginFile = parameters.originalFile

        // 检查当前文件和参数来源文件是否为 CjFile 类型
        return position.containingFile is CjFile && parametersOriginFile is CjFile
    }


    /**
     * 判断是否应根据给定的参数和前缀匹配规则抑制代码补全。
     *
     * @param parameters 补全参数，包含当前补全上下文的信息。
     * @param prefixMatcher 前缀匹配器，用于确定补全前缀是否匹配。
     * @return 如果应抑制补全返回 true；否则返回 false。
     */
    private fun shouldSuppressCompletion(parameters: CompletionParameters, prefixMatcher: PrefixMatcher): Boolean {
        val position = parameters.position
        val invocationCount = parameters.invocationCount

        // 如果前缀匹配器是 CamelHumpMatcher 并且容忍拼写错误，则抑制补全
        if (prefixMatcher is CamelHumpMatcher && prefixMatcher.isTypoTolerant) return true

        // 在数字字面量内部不提供补全
        if (AFTER_NUMBER_LITERAL.accepts(position)) return true

        // 在整数字面量和小数点之后不自动弹出补全
        if (invocationCount == 0 && prefixMatcher.prefix.isEmpty() && AFTER_INTEGER_LITERAL_AND_DOT.accepts(position)) return true

        // 检查是否在表达式内部禁用自动补全
        if (invocationCount == 0 && Registry.`is`("cangjie.disable.auto.completion.inside.expression", false)) {
            val originalPosition = parameters.originalPosition
            val originalExpression = originalPosition?.getNonStrictParentOfType<CjNameReferenceExpression>()
            val expression = position.getNonStrictParentOfType<CjNameReferenceExpression>()

            // 如果当前表达式的引用名称不是原始表达式引用名称的前缀，则抑制补全
            if (expression != null && originalExpression != null &&
                !expression.referencedName.startsWith(originalExpression.referencedName)
            ) {
                return true
            }
        }

        return false
    }


    companion object {
        // add '$' to ignore context after the caret
        const val DEFAULT_DUMMY_IDENTIFIER: String = CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED + "$"
    }

    private fun replacementOffsetByExpression(expression: CjExpression): Int {
        when (expression) {
            is CjCallExpression -> {
                val calleeExpression = expression.calleeExpression
                if (calleeExpression != null) {
                    return calleeExpression.textRange!!.endOffset
                }
            }

            is CjQualifiedExpression -> {
                val selector = expression.selectorExpression
                if (selector != null) {
                    return replacementOffsetByExpression(selector)
                }
            }
        }
        return expression.textRange!!.endOffset
    }

    override fun beforeCompletion(context: CompletionInitializationContext) {
        val offset = context.startOffset
        val psiFile = context.file
        val tokenBefore = psiFile.findElementAt(max(0, offset - 1))

        // this code will make replacement offset "modified" and prevents altering it by the code in CompletionProgressIndicator
        context.markReplacementOffsetAsModified()

        val dummyIdentifierCorrected =
            CompletionDummyIdentifierProviderService.getInstance().correctPositionForStringTemplateEntry(context)
        if (dummyIdentifierCorrected) {
            return
        }
        context.dummyIdentifier = when {
            context.completionType == CompletionType.SMART -> DEFAULT_DUMMY_IDENTIFIER

            PackageDirectiveCompletion.ACTIVATION_PATTERN.accepts(tokenBefore) -> PackageDirectiveCompletion.DUMMY_IDENTIFIER

            else -> CompletionDummyIdentifierProviderService.getInstance().provideDummyIdentifier(context)
        }

        val tokenAt = psiFile.findElementAt(max(0, offset))
        if (tokenAt != null) {
            /* do not use parent expression if we are at the end of line - it's probably parsed incorrectly */
            if (context.completionType == CompletionType.SMART && !isAtEndOfLine(offset, context.editor.document)) {
                var parent = tokenAt.parent
                if (parent is CjExpression && parent !is CjBlockExpression) {
                    // search expression to be replaced - go up while we are the first child of parent expression
                    var expression: CjExpression = parent
                    parent = expression.parent
                    while (parent is CjExpression && parent.getFirstChild() == expression) {
                        expression = parent
                        parent = expression.parent
                    }

                    val suggestedReplacementOffset = replacementOffsetByExpression(expression)
                    if (suggestedReplacementOffset > context.replacementOffset) {
                        context.replacementOffset = suggestedReplacementOffset
                    }

                    context.offsetMap.addOffset(SmartCompletion.OLD_ARGUMENTS_REPLACEMENT_OFFSET, expression.endOffset)

                    val argumentList = (expression.parent as? CjValueArgument)?.parent as? CjValueArgumentList
                    if (argumentList != null) {
                        context.offsetMap.addOffset(
                            SmartCompletion.MULTIPLE_ARGUMENTS_REPLACEMENT_OFFSET,
                            argumentList.rightParenthesis?.textRange?.startOffset ?: argumentList.endOffset
                        )
                    }
                }
            }
            CompletionDummyIdentifierProviderService.getInstance().correctPositionForParameter(context)
        }
    }

    private fun isAtEndOfLine(offset: Int, document: Document): Boolean {
        var i = offset
        val chars = document.charsSequence
        while (i < chars.length) {
            val c = chars[i]
            if (c == '\n') return true
            if (!Character.isWhitespace(c)) return false
            i++
        }
        return true
    }

    private fun generateCompletionKinds(
        parameters: CompletionParameters,
        suggestionGeneratorExecutor: SuggestionGeneratorExecutor,
        result: CompletionResultSet,
        lookupElementPostProcessor: ((LookupElement) -> LookupElement)?,
    ) {
        val position = parameters.position
        if (position.getNonStrictParentOfType<PsiComment>() != null) {
            // don't stop here, allow other contributors to run
            return
        }

        if (shouldSuppressCompletion(parameters, result.prefixMatcher)) {
            result.stopHere()
            return
        }

        if (PackageDirectiveCompletion.perform(parameters, result)) {
            result.stopHere()
            return
        }

        fun addPostProcessor(session: CompletionSession) {
            if (lookupElementPostProcessor != null) {
                session.addLookupElementPostProcessor(lookupElementPostProcessor)
            }
        }

        result.restartCompletionWhenNothingMatches()

        val resultPolicyController = PolicyController(result)

        val configuration = CompletionSessionConfiguration(parameters)
        if (parameters.completionType == CompletionType.BASIC) {
            val session =
                BasicCompletionSession(configuration, parameters, resultPolicyController, suggestionGeneratorExecutor)
            addPostProcessor(session)

            if (parameters.isAutoPopup && session.shouldDisableAutoPopup()) {
                result.stopHere()
                return
            }

            session.complete()
            suggestionGeneratorExecutor.executeAll()

            if (session.isNothingAddedToResult && parameters.invocationCount < 2) {
                // Rerun completion if nothing was found
                val newConfiguration = CompletionSessionConfiguration(
                    useBetterPrefixMatcherForNonImportedClasses = false,
                    nonAccessibleDeclarations = false,

                    staticMembers = parameters.invocationCount > 0,
                    //                    dataClassComponentFunctions = true,
                    //                    excludeEnumEntries = configuration.excludeEnumEntries,
                )

                val newSession = BasicCompletionSession(
                    newConfiguration, parameters, resultPolicyController, suggestionGeneratorExecutor
                )

                addPostProcessor(newSession)
                newSession.complete()
            }
        }
        else {
            val session = SmartCompletionSession(configuration, parameters, result)
            addPostProcessor(session)
            session.complete()
        }


    }

}

fun CompletionInitializationContext.markReplacementOffsetAsModified() {
    // set replacement offset explicitly to mark it as modified
    replacementOffset = replacementOffset
}
