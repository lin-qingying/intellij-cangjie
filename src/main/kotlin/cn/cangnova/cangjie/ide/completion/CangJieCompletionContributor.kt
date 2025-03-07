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

import com.intellij.codeInsight.completion.*

import com.intellij.codeInsight.completion.impl.CamelHumpMatcher
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.openapi.util.registry.Registry
import com.intellij.patterns.PlatformPatterns.elementType
import com.intellij.patterns.PlatformPatterns.psiElement
import cn.cangnova.cangjie.ide.completion.turboComplete.KindCollector
import cn.cangnova.cangjie.ide.completion.turboComplete.KindVariety
import cn.cangnova.cangjie.ide.completion.turboComplete.SmartPipelineRunner
import cn.cangnova.cangjie.ide.completion.turboComplete.SuggestionGeneratorExecutor


import com.intellij.psi.PsiComment
import com.intellij.util.indexing.DumbModeAccessType
import cn.cangnova.cangjie.configurable.services.CangJieLanguageServerServices
import cn.cangnova.cangjie.configurable.services.Feature
import cn.cangnova.cangjie.ide.completion.addingPolicy.PolicyController

import cn.cangnova.cangjie.ide.completion.smart.SmartCompletion
import cn.cangnova.cangjie.ide.completion.stringTemplates.StringTemplateCompletion
import cn.cangnova.cangjie.ide.completion.stringTemplates.wrapLookupElementForStringTemplateAfterDotCompletion

import cn.cangnova.cangjie.lexer.CjTokens
import cn.cangnova.cangjie.psi.*
import cn.cangnova.cangjie.psi.psiUtil.endOffset
import cn.cangnova.cangjie.psi.psiUtil.getNonStrictParentOfType
import kotlin.math.max


// 定义一个抽象类，用于执行特定类型的补全操作
abstract class CangJieKindExecutingCompletionContributor : CompletionContributor(), KindCollector {
    // 重写fillCompletionVariants方法，使用智能管道运行器来执行补全操作
    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        SmartPipelineRunner.getOneOrDefault().runPipeline(this, parameters, result)
    }
}

// 具体的补全贡献者类，继承自CangJieKindExecutingCompletionContributor
class CangJieCompletionContributor : CangJieKindExecutingCompletionContributor() {

    // 定义补全种类的多样性
    override val kindVariety: KindVariety = CangJieKindVariety

    // 定义在数字字面量之后的 PSI 元素条件
    private val AFTER_NUMBER_LITERAL = psiElement().afterLeafSkipping(
        psiElement().withText(""),
        psiElement().withElementType(elementType().oneOf(CjTokens.FLOAT_LITERAL, CjTokens.INTEGER_LITERAL))
    )

    // 定义在整数字面量和小数点之后的 PSI 元素条件
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

        // 如果前缀匹配器是 CamelHumpMatcher 并且容忍拼写错误，则抑制补全 TODO 由于isTypoTolerant是内部API，后续寻找方法替代
//        if (prefixMatcher is CamelHumpMatcher && prefixMatcher.isTypoTolerant) return true

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

    /**
     * 在代码补全开始前进行预处理。
     *
     * 该方法负责设置补全过程所需的初始信息，包括调整替换偏移量、确定虚拟标识符以及处理表达式的替换位置。
     *
     * @param context 补全初始化的上下文环境，提供了文件、偏移量等信息
     */
    override fun beforeCompletion(context: CompletionInitializationContext) {
        val offset = context.startOffset
        val psiFile = context.file
        val tokenBefore = psiFile.findElementAt(max(0, offset - 1))

        // 标记替换偏移量为已修改，防止后续代码改变它
        context.markReplacementOffsetAsModified()

        // 校正字符串模板条目的位置，如果成功则直接返回
        val dummyIdentifierCorrected =
            CompletionDummyIdentifierProviderService.getInstance().correctPositionForStringTemplateEntry(context)
        if (dummyIdentifierCorrected) {
            return
        }

        // 根据不同的条件设置虚拟标识符
        context.dummyIdentifier = when {
            context.completionType == CompletionType.SMART -> DEFAULT_DUMMY_IDENTIFIER
            PackageDirectiveCompletion.ACTIVATION_PATTERN.accepts(tokenBefore) -> PackageDirectiveCompletion.DUMMY_IDENTIFIER
            else -> CompletionDummyIdentifierProviderService.getInstance().provideDummyIdentifier(context)
        }

        val tokenAt = psiFile.findElementAt(max(0, offset))
        if (tokenAt != null) {
            // 如果是智能补全且不在行尾，则查找父表达式并调整替换偏移量
            if (context.completionType == CompletionType.SMART && !isAtEndOfLine(offset, context.editor.document)) {
                var parent = tokenAt.parent
                if (parent is CjExpression && parent !is CjBlockExpression) {
                    // 查找要替换的表达式，沿父级向上遍历直到不再是第一个子节点
                    var expression: CjExpression = parent
                    parent = expression.parent
                    while (parent is CjExpression && parent.firstChild == expression) {
                        expression = parent
                        parent = expression.parent
                    }

                    // 更新替换偏移量为建议的值
                    val suggestedReplacementOffset = replacementOffsetByExpression(expression)
                    if (suggestedReplacementOffset > context.replacementOffset) {
                        context.replacementOffset = suggestedReplacementOffset
                    }

                    // 添加旧参数的替换偏移量
                    context.offsetMap.addOffset(SmartCompletion.OLD_ARGUMENTS_REPLACEMENT_OFFSET, expression.endOffset)

                    // 处理函数调用中的参数列表，添加多个参数的替换偏移量
                    val argumentList = (expression.parent as? CjValueArgument)?.parent as? CjValueArgumentList
                    if (argumentList != null) {
                        context.offsetMap.addOffset(
                            SmartCompletion.MULTIPLE_ARGUMENTS_REPLACEMENT_OFFSET,
                            argumentList.rightParenthesis?.textRange?.startOffset ?: argumentList.endOffset
                        )
                    }
                }
            }

            // 校正参数的位置
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
        } else {
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



