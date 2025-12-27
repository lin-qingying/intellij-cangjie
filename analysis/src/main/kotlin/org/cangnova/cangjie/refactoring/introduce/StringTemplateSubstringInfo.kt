/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.refactoring.introduce

import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjPsiFactory
import org.cangnova.cangjie.psi.CjStringTemplateEntry
import org.cangnova.cangjie.psi.CjStringTemplateExpression
import org.cangnova.cangjie.psi.UserDataProperty
import org.cangnova.cangjie.psi.psiUtil.endOffset
import org.cangnova.cangjie.psi.psiUtil.nextSiblingOfSameType
import org.cangnova.cangjie.psi.psiUtil.startOffset
import org.cangnova.cangjie.utils.exceptions.CangJieExceptionWithAttachmentsImpl
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
/**
 * 存储与表达式关联的可提取子串信息。
 */
var CjExpression.extractableSubstringInfo: StringTemplateSubstringInfo? by UserDataProperty(Key.create("EXTRACTED_SUBSTRING_INFO"))

/**
 * 获取表达式的子串上下文，如果存在则返回模板，否则返回表达式本身。
 */
val CjExpression.substringContextOrThis: CjExpression
    get() = extractableSubstringInfo?.template ?: this

/**
 * 获取元素的子串上下文，如果元素是表达式且存在子串信息则返回模板，否则返回元素本身。
 */
val PsiElement.substringContextOrThis: PsiElement
    get() = (this as? CjExpression)?.extractableSubstringInfo?.template ?: this
/**
 * 表示字符串模板中可提取子串的信息。
 * 用于重构过程中，记录和管理从字符串模板中提取的子串的元数据，如起始位置、结束位置、前缀、后缀等。
 *
 * @property startEntry 子串的起始模板条目
 * @property endEntry 子串的结束模板条目
 * @property prefix 子串的前缀
 * @property suffix 子串的后缀
 */
abstract class StringTemplateSubstringInfo(
    val startEntry: CjStringTemplateEntry,
    val endEntry: CjStringTemplateEntry,
    val prefix: String,
    val suffix: String,
) {
    /**
     * 指示子串是否为纯字符串。
     */
    abstract val isString: Boolean

    /**
     * 获取子串所在的字符串模板表达式。
     */
    val template: CjStringTemplateExpression = startEntry.parent as CjStringTemplateExpression

    /**
     * 获取子串的实际内容（去除前缀和后缀）。
     */
    val content = with(entries.map { it.text }.joinToString(separator = "")) { substring(prefix.length, length - suffix.length) }

    /**
     * 获取子串在文档中的绝对范围。
     */
    val contentRange: TextRange
        get() = TextRange(startEntry.startOffset + prefix.length, endEntry.endOffset - suffix.length)

    /**
     * 获取子串相对于模板起始位置的偏移范围。
     */
    val relativeContentRange: TextRange
        get() = contentRange.shiftRight(-template.startOffset)

    /**
     * 获取子串包含的所有模板条目序列。
     */
    val entries: Sequence<CjStringTemplateEntry>
        get() = generateSequence(startEntry) { if (it != endEntry) it.nextSiblingOfSameType() else null }

    /**
     * 根据子串内容创建新的表达式。
     * 如果子串为纯字符串，则添加引号；否则直接使用内容。
     *
     * @return 新创建的表达式
     * @throws CangJieExceptionWithAttachmentsImpl 如果模板缺少引号
     */
    fun createExpression(): CjExpression {
        val quote = template.node.findChildByType(CjTokens.OPEN_QUOTE)?.text
            ?: throw CangJieExceptionWithAttachmentsImpl("Missing opening quote in a string template").withPsiAttachment("template", template)
        val literalValue = if (isString) "$quote$content$quote" else content
        return CjPsiFactory(template.project).createExpression(literalValue)
            .apply { extractableSubstringInfo = this@StringTemplateSubstringInfo }
    }

    /**
     * 复制当前子串信息到新的字符串模板中。
     *
     * @param newTemplate 新的字符串模板表达式
     * @return 新的子串信息
     * @throws CangJieExceptionWithAttachmentsImpl 如果新旧模板的条目不匹配
     */
    fun copy(newTemplate: CjStringTemplateExpression): StringTemplateSubstringInfo {
        val oldEntries = template.entries
        val newEntries = newTemplate.entries
        val startIndex = oldEntries.indexOf(startEntry)
        val endIndex = oldEntries.indexOf(endEntry)
        if (startIndex < 0 || startIndex >= newEntries.size || endIndex < 0 || endIndex >= newEntries.size) {
            throw CangJieExceptionWithAttachmentsImpl("Old template($startIndex..$endIndex): $template, new template: $newTemplate")
                .withPsiAttachment("template", template)
                .withPsiAttachment("newTemplate", newTemplate)
        }
        return copy(newEntries[startIndex], newEntries[endIndex])
    }

    /**
     * 抽象方法：复制当前子串信息到新的起始和结束条目。
     *
     * @param newStartEntry 新的起始模板条目
     * @param newEndEntry 新的结束模板条目
     * @return 新的子串信息
     */
    abstract fun copy(newStartEntry: CjStringTemplateEntry, newEndEntry: CjStringTemplateEntry): StringTemplateSubstringInfo
}

