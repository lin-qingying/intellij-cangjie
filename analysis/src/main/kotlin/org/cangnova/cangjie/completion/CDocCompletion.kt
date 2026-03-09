/*
 * Copyright 2026 LinQingYing. and contributors.
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

import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjTypeStatement
import org.cangnova.cangjie.psi.psiUtil.getNonStrictParentOfType
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionUtil
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.StandardPatterns
import com.intellij.util.ProcessingContext
import org.cangnova.cangjie.lexer.cdoc.parser.CDocKnownTag
import org.cangnova.cangjie.lexer.cdoc.psi.CDoc
import java.util.*


/**
 * CDoc 文档注释标签补全提供者
 *
 * 在 CDoc 注释（/** ... */）中输入 @ 时，
 * 提供可用的文档标签补全，如 @param、@return、@throws 等。
 *
 * 补全触发条件：
 * - 光标在 CDoc 注释块内
 * - 前缀为空（手动触发）或以 @ 开头
 *
 * 上下文感知：
 * - 根据 CDoc 所附着的声明类型过滤可用标签
 *   例如：@return 只在函数上可用，@property 只在类上可用
 */
object CDocTagCompletionProvider : CompletionProvider<CompletionParameters>() {

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        // 提取当前光标处的前缀
        // 注意：findIdentifierPrefix 要求"标识符组成字符"是"标识符起始字符"的超集
        // 这里将 @ 也纳入识别范围，使 @param 这样的前缀能被整体识别
        val prefix = CompletionUtil.findIdentifierPrefix(
            parameters.position.containingFile,
            parameters.offset,
            // 标识符组成字符：Java 标识符字符 或 @
            StandardPatterns.character().javaIdentifierPart() or singleCharPattern('@'),
            // 标识符起始字符：Java 标识符起始字符 或 @
            StandardPatterns.character().javaIdentifierStart() or singleCharPattern('@')
        )

        // 自动弹出时前缀为空则不触发（避免在注释中随意弹出）
        if (parameters.isAutoPopup && prefix.isEmpty()) return

        // 前缀非空但不以 @ 开头，说明用户在输入普通文本，不是标签，跳过
        if (prefix.isNotEmpty() && !prefix.startsWith('@')) return

        // 获取当前 CDoc 注释所附着的声明（如函数、类等）
        // 为 null 表示 CDoc 没有明确的归属声明，此时所有标签都可用
        val cdocOwner = parameters.position.getNonStrictParentOfType<CDoc>()?.getOwner()

        // 使用实际前缀（含 @）替换默认前缀匹配器，确保 @param 等能正确匹配
        val resultWithPrefix = result.withPrefixMatcher(prefix)

        // 遍历所有已知 CDoc 标签，过滤出当前上下文可用的标签并加入补全列表
        CDocKnownTag.entries.forEach { tag ->
            if (cdocOwner == null || tag.isApplicable(cdocOwner)) {
                // 标签名统一转为小写（@Param → @param）
                resultWithPrefix.addElement(
                    LookupElementBuilder.create("@" + tag.name.lowercase(Locale.US))
                )
            }
        }
    }

    /**
     * 判断某个 CDoc 标签是否适用于指定的声明。
     *
     * 过滤规则：
     * - @constructor、@property → 只适用于类/枚举等类型声明
     * - @return               → 只适用于函数声明
     * - 其余标签              → 所有声明类型均适用
     *
     * @param declaration CDoc 注释所附着的声明节点
     * @return true 表示此标签在该声明上有意义，应出现在补全列表中
     */
    private fun CDocKnownTag.isApplicable(declaration: CjDeclaration) = when (this) {
        // 类型声明专属标签
        CDocKnownTag.CONSTRUCTOR,
        CDocKnownTag.PROPERTY -> declaration is CjTypeStatement

        // 函数声明专属标签
        CDocKnownTag.RETURN -> declaration is CjNamedFunction

        // 通用标签：适用于所有声明类型
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