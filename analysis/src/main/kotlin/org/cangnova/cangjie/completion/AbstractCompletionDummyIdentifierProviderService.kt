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

package org.cangnova.cangjie.completion

import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.*
import com.intellij.codeInsight.completion.CompletionInitializationContext
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.CompletionUtil
import com.intellij.codeInsight.completion.CompletionUtilCore
import com.intellij.psi.*
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import kotlin.math.max

/**
 * 代码补全虚拟标识符提供服务的抽象实现
 *
 * 此类是代码补全系统的核心组件，负责在代码补全过程中提供合适的"虚拟标识符"（Dummy Identifier）。
 * 虚拟标识符是一个临时插入到代码中光标位置的占位符，用于帮助解析器正确解析不完整的代码结构。
 *
 * ## 背景知识
 *
 * 在 IntelliJ 平台的代码补全机制中，当用户触发补全时，IDE 需要理解光标所在位置的语法上下文。
 * 由于用户正在输入的代码通常是不完整的，直接解析可能会失败。为解决这个问题，IDE 会在光标位置
 * 临时插入一个虚拟标识符，使代码在语法上变得完整，从而可以被正确解析。
 *
 * ## 虚拟标识符的作用
 *
 * 例如，当用户输入 `obj.` 并触发补全时：
 * - 原始代码：`obj.` （语法不完整，缺少成员名称）
 * - 插入虚拟标识符后：`obj.IntellijIdeaRulezzz`（语法完整，可以解析）
 *
 * 通过这种方式，IDE 可以：
 * 1. 正确识别 `obj` 是一个接收者表达式
 * 2. 确定需要补全的是成员访问
 * 3. 提供相应的补全建议
 *
 * ## 虚拟标识符的变体
 *
 * 不同的语法上下文需要不同的虚拟标识符：
 *
 * - **默认标识符** (`DUMMY_IDENTIFIER_TRIMMED + "$"`)：
 *   - 添加 `$` 后缀以忽略光标后的上下文
 *   - 适用于大多数表达式上下文
 *
 * - **简单标识符** (`DUMMY_IDENTIFIER_TRIMMED`)：
 *   - 不带 `$` 后缀
 *   - 用于类型参数列表、Lambda 签名等需要精确解析的场景
 *
 * - **带闭合符号的标识符**：
 *   - 如 `DUMMY_IDENTIFIER_TRIMMED + ">"`：用于未闭合的超类限定符
 *   - 如 `DUMMY_IDENTIFIER_TRIMMED + ">" + "$"`：用于未闭合的类型参数列表
 *
 * ## 主要功能
 *
 * ### 1. 字符串模板位置修正 [correctPositionForStringTemplateEntry]
 * 处理字符串模板中的简短形式（如 `"$name.xxx"`），将其转换为完整形式以便正确解析。
 *
 * ### 2. 参数位置修正 [correctPositionForParameter]
 * 调整函数参数声明中的替换范围，确保补全覆盖整个参数。
 *
 * ### 3. 虚拟标识符提供 [provideDummyIdentifier]
 * 根据光标位置的语法上下文，选择最合适的虚拟标识符。
 *
 * ## 特殊场景处理
 *
 * 此类处理多种特殊的语法场景：
 *
 * - **类头部**：类声明的继承列表、类型参数等
 * - **未闭合的超类限定符**：如 `super<Foo`
 * - **简单字符串模板**：如 `"$name"`
 * - **Lambda 签名**：Lambda 表达式的参数列表
 * - **扩展接收者**：扩展函数声明中的接收者类型
 * - **类型参数列表**：泛型类型参数
 * - **参数列表**：函数调用的参数
 * - **反引号名称**：使用反引号包围的标识符
 * - **二元表达式**：二元运算符表达式
 * - **注解入口**：注解声明
 *
 * @see CompletionDummyIdentifierProviderService
 * @see CompletionInitializationContext
 */
abstract class AbstractCompletionDummyIdentifierProviderService : CompletionDummyIdentifierProviderService {
    /**
     * 修正字符串模板中的补全位置
     *
     * 处理字符串模板中简短引用形式的补全，例如 `"$name.xxx"`。
     *
     * ## 问题场景
     *
     * 当用户在字符串模板的简短形式后输入点号时：
     * ```
     * val s = "$name.xxx"
     *              ^-- 光标在这里
     * ```
     *
     * 解析器会将 `.xxx` 视为普通字符串内容而非成员访问。
     * 此方法将其转换为块形式以便正确解析：
     * ```
     * val s = "${name.xxx}"
     * ```
     *
     * @param context 补全初始化上下文
     * @return 如果位置被修正则返回 true，否则返回 false
     */
    override fun correctPositionForStringTemplateEntry(context: CompletionInitializationContext): Boolean {
        val offset = context.startOffset
        val psiFile = context.file
        val tokenBefore = psiFile.findElementAt(max(0, offset - 1))

        if (offset > 0 && tokenBefore!!.node.elementType == CjTokens.REGULAR_STRING_PART && tokenBefore.text.startsWith(".")) {
            val prev = tokenBefore.parent.prevSibling
            if (prev != null && prev is CjSimpleNameStringTemplateEntry) {
                val expression = prev.expression
                if (expression != null) {
                    val prefix = tokenBefore.text.substring(0, offset - tokenBefore.startOffset)
                    context.dummyIdentifier = "{" + expression.text + prefix + CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED + "}"
                    context.offsetMap.addOffset(CompletionInitializationContext.START_OFFSET, expression.startOffset)
                    return true
                }
            }
        }
        return false
    }

    /**
     * 修正函数参数声明中的补全替换范围
     *
     * 当用户在参数声明中触发补全时，确保替换范围覆盖整个参数。
     *
     * ## 问题场景
     *
     * ```
     * func foo(f|oo: Foo)  // 光标在参数名中间
     * func foo(foo|: Foo)  // 光标在冒号前
     * ```
     *
     * 如果不修正范围，补全可能只替换光标前的部分，导致参数声明损坏。
     * 此方法将替换范围扩展到整个参数的结束位置。
     *
     * @param context 补全初始化上下文
     */
    override fun correctPositionForParameter(context: CompletionInitializationContext) {
        val offset = context.startOffset
        val psiFile = context.file
        val tokenAt = psiFile.findElementAt(max(0, offset)) ?: return

        // IDENTIFIER when 'f<caret>oo: Foo'
        // COLON when 'foo<caret>: Foo'
        if (tokenAt.node.elementType == CjTokens.IDENTIFIER || tokenAt.node.elementType == CjTokens.COLON) {
            val parameter = tokenAt.parent as? CjParameter
            if (parameter != null) {
                context.replacementOffset = parameter.endOffset
            }
        }
    }

    /**
     * 根据光标位置的语法上下文提供合适的虚拟标识符
     *
     * 这是代码补全系统的核心方法，根据不同的语法场景返回最合适的虚拟标识符。
     *
     * ## 返回值说明
     *
     * - **`DUMMY_IDENTIFIER_TRIMMED + "$"`**：默认情况，`$` 用于忽略光标后的上下文
     * - **`DUMMY_IDENTIFIER_TRIMMED`**：需要精确解析的场景（类型参数、Lambda 签名等）
     * - **`DUMMY_IDENTIFIER`**：类头部、二元表达式等需要完整标识符的场景
     * - **带闭合符号的变体**：用于修复未闭合的语法结构
     *
     * ## 处理优先级
     *
     * 1. 智能补全类型 → 默认标识符
     * 2. 类头部 → 简单标识符
     * 3. 未闭合的 super 限定符 → 带 `>` 的标识符
     * 4. 简单字符串模板 → 简单标识符
     * 5. 特殊场景链式检查（Lambda、扩展接收者、类型参数等）
     * 6. 默认处理或注解入口
     * 7. 最终默认标识符
     *
     * @param context 补全初始化上下文
     * @return 适合当前上下文的虚拟标识符字符串
     */
    override fun provideDummyIdentifier(context: CompletionInitializationContext): String {
        val psiFile = context.file
        if (psiFile !is CjFile) {
            error("CompletionDummyIdentifierProviderService.providerDummyIdentifier should not be called for non CjFile")
        }

        val offset = context.startOffset
        val tokenBefore = psiFile.findElementAt(max(0, offset - 1))

        return when {
            context.completionType == CompletionType.SMART -> DEFAULT_DUMMY_IDENTIFIER

            // TODO package completion

            isInClassHeader(tokenBefore) -> CompletionUtilCore.DUMMY_IDENTIFIER // do not add '$' to not interrupt class declaration parsing

            isInUnclosedSuperQualifier(tokenBefore) -> CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED + ">"

            isInSimpleStringTemplate(tokenBefore) -> CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED

            else -> specialLambdaSignatureDummyIdentifier(tokenBefore)
                ?: specialExtensionReceiverDummyIdentifier(tokenBefore)
                ?: specialInTypeArgsDummyIdentifier(tokenBefore)
                ?: specialInArgumentListDummyIdentifier(tokenBefore)
                ?: specialInNameWithQuotes(tokenBefore)
                ?: specialInBinaryExpressionDummyIdentifier(tokenBefore)
                ?: isInValueOrTypeParametersList(tokenBefore)
                ?: handleDefaultCase(context)
                ?: isInAnnotationEntry(tokenBefore)
                ?: DEFAULT_DUMMY_IDENTIFIER
        }
    }

    /**
     * 检查是否在注解入口中
     *
     * 注解中的类型引用需要使用完整的标识符以保持语法正确。
     *
     * @param tokenBefore 光标前的 token
     * @return 如果在注解中返回 `DUMMY_IDENTIFIER`，否则返回 null
     */
    private fun isInAnnotationEntry(tokenBefore: PsiElement?): String? {
        if (tokenBefore == null) return null

        val typeReference = tokenBefore.parentOfType<CjTypeReference>(true) ?: return null
        return if (typeReference.parentOfType<CjAnnotation>() != null) {
            CompletionUtilCore.DUMMY_IDENTIFIER
        } else {
            null
        }
    }

    /**
     * 子类可重写的默认情况处理钩子
     *
     * 允许子类在所有内置检查都不匹配时提供自定义的虚拟标识符。
     *
     * @param context 补全初始化上下文
     * @return 自定义的虚拟标识符，或 null 表示继续使用默认逻辑
     */
    protected open fun handleDefaultCase(context: CompletionInitializationContext): String? = null

    /**
     * 检查是否在值参数列表或类型参数列表中
     *
     * 参数列表中使用简单标识符以避免破坏列表解析。
     *
     * @param tokenBefore 光标前的 token
     * @return 如果在参数列表中返回 `DUMMY_IDENTIFIER_TRIMMED`，否则返回 null
     */
    private fun isInValueOrTypeParametersList(tokenBefore: PsiElement?): String? {
        if (tokenBefore == null) return null
        if (tokenBefore.parents.any { it is CjTypeParameterList || it is CjParameterList }) {
            return CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED
        }
        return null
    }

    /**
     * 处理 Lambda 表达式签名中的补全
     *
     * 当光标在 Lambda 的参数列表中时，使用简单标识符以保持参数列表解析正确。
     *
     * ## 示例
     *
     * ```
     * { x| -> ... }      // 光标在参数名后
     * { x, y| -> ... }   // 光标在第二个参数名后
     * ```
     *
     * @param tokenBefore 光标前的 token
     * @return 如果在 Lambda 参数列表中返回 `DUMMY_IDENTIFIER_TRIMMED`，否则返回 null
     */
    private fun specialLambdaSignatureDummyIdentifier(tokenBefore: PsiElement?): String? {
        var leaf = tokenBefore
        while (leaf is PsiWhiteSpace || leaf is PsiComment) {
            leaf = leaf.prevLeaf(true)
        }

        val lambda = leaf?.parents?.firstOrNull { it is CjFunctionLiteral } ?: return null

        val lambdaChild = leaf.parents.takeWhile { it != lambda }.lastOrNull()

        return if (lambdaChild is CjParameterList)
            CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED
        else
            null

    }

    /**
     * 检查光标是否在类头部（继承列表、类型参数等）
     *
     * 类头部需要使用不带 `$` 的标识符，以避免中断类声明的解析。
     *
     * ## 检查范围
     *
     * ```
     * class Foo<T>| : Base|, Interface| { ... }
     *          ↑      ↑        ↑
     *          └──────┴────────┴── 类头部区域
     * ```
     *
     * @param tokenBefore 光标前的 token
     * @return true 如果光标在类头部区域
     */
    private fun isInClassHeader(tokenBefore: PsiElement?): Boolean {
        val classOrObject = tokenBefore?.parents?.firstIsInstanceOrNull<CjTypeStatement>() ?: return false
        val name = classOrObject.nameIdentifier ?: return false
        val headerEnd = classOrObject.body?.startOffset ?: classOrObject.endOffset
        val offset = tokenBefore.startOffset
        return name.endOffset <= offset && offset <= headerEnd
    }

    /**
     * 处理二元表达式中的补全
     *
     * 当光标在二元表达式的标识符中时，使用完整的标识符以保持表达式解析正确。
     *
     * ## 示例
     *
     * ```
     * a + b|     // 光标在操作数后
     * x && y|    // 光标在布尔表达式操作数后
     * ```
     *
     * @param tokenBefore 光标前的 token
     * @return 如果在二元表达式中返回 `DUMMY_IDENTIFIER`，否则返回 null
     */
    private fun specialInBinaryExpressionDummyIdentifier(tokenBefore: PsiElement?): String? {
        if (tokenBefore.elementType == CjTokens.IDENTIFIER && tokenBefore?.context?.context is CjBinaryExpression)
            return CompletionUtilCore.DUMMY_IDENTIFIER
        return null
    }

    /**
     * 处理反引号包围的名称中的补全
     *
     * 仓颉语言支持使用反引号包围的标识符（如 `` `class` ``），当用户在反引号内输入时，
     * 需要添加闭合的反引号以保持语法正确。
     *
     * ## 示例
     *
     * ```
     * let `my|        // 需要补全并添加闭合反引号
     * let `nam|e`     // 光标在反引号名称中间
     * ```
     *
     * @param tokenBefore 光标前的 token
     * @return 如果在反引号名称中返回带闭合反引号的标识符，否则返回 null
     */
    private fun specialInNameWithQuotes(tokenBefore: PsiElement?): String? {
        val badCharacterBefore = when (tokenBefore?.elementType) {
            TokenType.BAD_CHARACTER -> tokenBefore
            CjTokens.IDENTIFIER -> tokenBefore?.prevLeaf(skipEmptyElements = true)?.takeIf { it.elementType == TokenType.BAD_CHARACTER }
            else -> null
        }
        val quote = "`"
        if (badCharacterBefore?.text == quote) return CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED + quote + "$"
        return null
    }

    /**
     * 检查是否在未闭合的 super 类型限定符中
     *
     * 处理类似 `super<Base` 的未闭合语法，需要添加 `>` 来闭合类型参数。
     *
     * ## 示例
     *
     * ```
     * super<Foo|      // 需要添加 > 来闭合
     * super<Foo.Bar|  // 多级限定，需要添加 > 来闭合
     * ```
     *
     * @param tokenBefore 光标前的 token
     * @return true 如果在未闭合的 super 限定符中
     */
    private fun isInUnclosedSuperQualifier(tokenBefore: PsiElement?): Boolean {
        if (tokenBefore == null) return false
        val tokensToSkip = TokenSet.orSet(TokenSet.create(CjTokens.IDENTIFIER, CjTokens.DOT), CjTokens.WHITE_SPACE_OR_COMMENT_BIT_SET)
        val tokens = generateSequence(tokenBefore) { it.prevLeaf() }
        val ltToken = tokens.firstOrNull { it.node.elementType !in tokensToSkip } ?: return false
        if (ltToken.node.elementType != CjTokens.LT) return false
        val superToken = ltToken.prevLeaf { it !is PsiWhiteSpace && it !is PsiComment }
        return superToken?.node?.elementType == CjTokens.SUPER_KEYWORD
    }

    /**
     * 检查是否在简单字符串模板中
     *
     * 简单字符串模板（如 `"$name"`）需要使用简单标识符。
     *
     * @param tokenBefore 光标前的 token
     * @return true 如果在简单字符串模板中
     */
    private fun isInSimpleStringTemplate(tokenBefore: PsiElement?): Boolean {
        return tokenBefore?.parents?.firstIsInstanceOrNull<CjStringTemplateExpression>()?.isPlain() ?: false
    }


    /**
     * 处理扩展函数接收者类型声明中的补全
     *
     * 当用户在扩展函数的接收者类型位置进行补全时，需要构造一个语法正确的虚拟标识符，
     * 以便解析器能正确识别这是一个扩展函数声明。
     *
     * ## 算法流程
     *
     * 1. 从光标位置向前扫描，收集已输入的 token
     * 2. 计算尖括号的平衡（`<` 和 `>` 的数量差）
     * 3. 找到声明关键字（`func`、`let`、`var`）
     * 4. 构造完整的声明语法并验证是否可解析
     * 5. 生成带有正确闭合符号的虚拟标识符
     *
     * ## 示例
     *
     * ```
     * func Foo|.bar() {}        // 光标在类型名后
     * func List<T|.bar() {}     // 光标在类型参数中
     * ```
     *
     * @param tokenBefore 光标前的 token
     * @return 适合扩展接收者位置的虚拟标识符，或 null
     */
    private fun specialExtensionReceiverDummyIdentifier(tokenBefore: PsiElement?): String? {
        var token = tokenBefore ?: return null
        var ltCount = 0
        var gtCount = 0
        val builder = StringBuilder()
        while (true) {
            val tokenType = token.node!!.elementType
            if (tokenType in declarationKeywords) {
                val balance = ltCount - gtCount
                if (balance < 0) return null
                builder.append(token.text!!.reversed())
                builder.reverse()

                var tail = "X" + ">".repeat(balance) + ".f"
                if (tokenType == CjTokens.FUNC_KEYWORD) {
                    tail += "()"
                }
                builder.append(tail)

                val text = builder.toString()
                val file = CjPsiFactory(tokenBefore.project).createFile(text)
                val declaration = file.declarations.singleOrNull() ?: return null
                if (declaration.textLength != text.length) return null
                val containsErrorElement = !PsiTreeUtil.processElements(file) { it !is PsiErrorElement }
                return if (containsErrorElement) null else "$tail$"
            }
            if (tokenType !in declarationTokens) return null
            if (tokenType == CjTokens.LT) ltCount++
            if (tokenType == CjTokens.GT) gtCount++
            builder.append(token.text!!.reversed())
            token = PsiTreeUtil.prevLeaf(token) ?: return null
        }
    }

    /**
     * 处理未闭合的类型参数列表中的补全
     *
     * 当用户在类型参数列表中进行补全时，如果参数列表未闭合，
     * 需要添加足够的 `>` 来闭合所有嵌套的类型参数。
     *
     * ## 算法流程
     *
     * 1. 检查是否已在已解析的类型参数列表内
     * 2. 向前扫描查找对应的名称引用
     * 3. 验证该名称引用是否指向函数或类
     * 4. 计算需要闭合的 `>` 数量
     * 5. 生成带正确数量闭合符号的标识符
     *
     * ## 示例
     *
     * ```
     * List<Map<String, |     // 需要 >>$ 来闭合
     * foo<T, |               // 需要 >$ 来闭合
     * ```
     *
     * @param tokenBefore 光标前的 token
     * @return 带闭合类型参数的虚拟标识符，或 null
     */
    private fun specialInTypeArgsDummyIdentifier(tokenBefore: PsiElement?): String? {
        if (tokenBefore == null) return null

        if (tokenBefore.getParentOfType<CjTypeArgumentList>(true) != null) { // already parsed inside type argument list
            return CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED // do not insert '$' to not break type argument list parsing
        }

        val pair = unclosedTypeArgListNameAndBalance(tokenBefore) ?: return null
        val (nameToken, balance) = pair
        assert(balance > 0)

        val nameRef = nameToken.parent as? CjNameReferenceExpression ?: return null
        return if (allTargetsAreFunctionsOrClasses(nameRef)) {
            CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED + ">".repeat(balance) + "$"
        } else {
            null
        }
    }

    /**
     * 子类必须实现：判断名称引用是否解析到函数或类
     *
     * 用于 [specialInTypeArgsDummyIdentifier]，确定 `<` 是类型参数列表的开始还是比较运算符。
     *
     * @param nameReferenceExpression 要检查的名称引用表达式
     * @return true 如果所有解析目标都是函数或类
     */
    protected abstract fun allTargetsAreFunctionsOrClasses(nameReferenceExpression: CjNameReferenceExpression): Boolean

    /**
     * 递归计算未闭合类型参数列表的名称和平衡数
     *
     * @param tokenBefore 起始 token
     * @return (名称 token, 未闭合的 < 数量) 的配对，或 null
     */
    private fun unclosedTypeArgListNameAndBalance(tokenBefore: PsiElement): Pair<PsiElement, Int>? {
        val nameToken = findCallNameTokenIfInTypeArgs(tokenBefore) ?: return null
        val pair = unclosedTypeArgListNameAndBalance(nameToken)
        return if (pair == null) {
            Pair(nameToken, 1)
        } else {
            Pair(pair.first, pair.second + 1)
        }
    }

    /** 类型参数列表中允许出现的 token 类型 */
    private val callTypeArgsTokens = TokenSet.orSet(
        TokenSet.create(
            CjTokens.IDENTIFIER, CjTokens.LT, CjTokens.GT,
            CjTokens.COMMA, CjTokens.DOT, CjTokens.QUEST, CjTokens.COLON,
            CjTokens.LPAR, CjTokens.RPAR, CjTokens.ARROW
        ),
        CjTokens.WHITE_SPACE_OR_COMMENT_BIT_SET
    )

    /**
     * 查找类型参数列表对应的调用名称 token
     *
     * 如果当前 token 可能位于某个调用的类型参数列表中，则返回该调用的名称 token。
     *
     * ## 算法
     *
     * 从当前 token 向前扫描，跳过类型参数列表中允许的 token，
     * 直到找到 `<`，然后验证 `<` 前是否是标识符。
     *
     * @param leaf 当前 token
     * @return 调用名称的 token，如果不在类型参数列表中则返回 null
     */
    private fun findCallNameTokenIfInTypeArgs(leaf: PsiElement): PsiElement? {
        var current = leaf
        while (true) {
            val tokenType = current.node!!.elementType
            if (tokenType !in callTypeArgsTokens) return null

            if (tokenType == CjTokens.LT) {
                val nameToken = current.prevLeaf(skipEmptyElements = true) ?: return null
                if (nameToken.node!!.elementType != CjTokens.IDENTIFIER) return null
                return nameToken
            }

            if (tokenType == CjTokens.GT) { // pass nested type argument list
                val prev = current.prevLeaf(skipEmptyElements = true) ?: return null
                val typeRef = findCallNameTokenIfInTypeArgs(prev) ?: return null
                current = typeRef
                continue
            }

            current = current.prevLeaf(skipEmptyElements = true) ?: return null
        }
    }


    /**
     * 处理函数调用参数列表中的补全
     *
     * 参数列表中的补全需要特殊处理，以避免破坏解析或产生歧义。
     *
     * ## 特殊情况
     *
     * 1. **委托调用**：构造函数委托（如 `super(...)`）需要使用简单标识符
     * 2. **命名参数**：参数列表中有 `=` 时，使用 `$,` 来避免被解析为赋值语句
     *
     * ## 示例
     *
     * ```
     * foo(a, |)          // 使用 $, 避免解析歧义
     * super(|)           // 使用简单标识符
     * foo(name = |)      // 使用 $, 避免被解析为赋值
     * ```
     *
     * @param tokenBefore 光标前的 token
     * @return 适合参数列表的虚拟标识符，或 null
     */
    private fun specialInArgumentListDummyIdentifier(tokenBefore: PsiElement?): String? {
        // If we insert `$` in the argument list of a delegation specifier, this will break parsing
        // and the following block will not be attached as a body to the constructor. Therefore
        // we need to use a regular identifier.
        val argumentList = tokenBefore?.getNonStrictParentOfType<CjValueArgumentList>() ?: return null
        if (argumentList.parent is CjConstructorDelegationCall) return CompletionUtil.DUMMY_IDENTIFIER_TRIMMED
        // If there is = in the argument list after caret, then breaking parsing with just $ prevents K2 from resolving function call,
        // i.e. `f ($ = )` is resolved to variable assignment and left part `f ($` is resolved to erroneous name reference,
        // so we need to use `$,` to avoid resolving to variable assignment
        return CompletionUtil.DUMMY_IDENTIFIER_TRIMMED + "$,"
    }

    /**
     * 伴生对象，包含常量定义
     */
    private companion object {
        /**
         * 默认虚拟标识符
         *
         * 使用 `$` 后缀来忽略光标后的上下文，防止后续文本干扰补全解析。
         */
        private const val DEFAULT_DUMMY_IDENTIFIER: String =
            CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED + "$"

        /** 声明关键字集合（func、let、var） */
        private val declarationKeywords = TokenSet.create(CjTokens.FUNC_KEYWORD, CjTokens.LET_KEYWORD, CjTokens.VAR_KEYWORD)

        /** 声明语法中允许出现的 token 类型 */
        private val declarationTokens = TokenSet.orSet(
            TokenSet.create(
                CjTokens.IDENTIFIER, CjTokens.LT, CjTokens.GT,
                CjTokens.COMMA, CjTokens.DOT, CjTokens.QUEST, CjTokens.COLON,
                CjTokens.IN_KEYWORD,
                CjTokens.LPAR, CjTokens.RPAR, CjTokens.ARROW,
                TokenType.ERROR_ELEMENT
            ),
            CjTokens.WHITE_SPACE_OR_COMMENT_BIT_SET
        )
    }
}
