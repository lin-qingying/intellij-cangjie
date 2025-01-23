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

package com.linqingying.lsp.api.customization

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.icons.AllIcons
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.util.text.StringUtilRt
import org.eclipse.lsp4j.CompletionItem
import org.eclipse.lsp4j.CompletionItemKind
import org.eclipse.lsp4j.CompletionItemTag

import javax.swing.Icon

/**
 * 处理从 LSP 服务器接收的 [CompletionItem](https://microsoft.github.io/language-server-protocol/specification#completionItem) 对象。
 * 实现类可以对代码补全行为进行细化调整。
 * 例如，可以过滤掉不需要的补全项或调整补全项的显示装饰。
 */

open class LspCompletionSupport {
    /**
     * 在 IDE 准备启动代码补全会话时调用。
     * 例如，可以通过按快捷键或键入标识符触发。
     * 实现类可以返回 `false`，以表示不希望在当前位置使用基于 LSP 的代码补全。
     */
    open fun shouldRunCodeCompletion(parameters: CompletionParameters): Boolean = true

    /**
     * 将从 LSP 服务器接收到的 [CompletionItem](https://microsoft.github.io/language-server-protocol/specification#completionItem)
     * 对象转换为 [LookupElement] 对象。
     *
     * 注意，实现此函数时不应在此处操作补全项的展示方式。
     * 若需调整展示，可重写 [getIcon]、[isBold]、[isStrikeout]、[getTypeText] 或 [getTailText] 等函数。
     * 在高级用例中，插件可以重写 [renderLookupElement]。
     *
     * 一些实现的示例：
     * - 返回 `null`，以忽略此 [item]
     * - 调整补全项的优先级：
     *   ```kotlin
     *   PrioritizedLookupElement.withPriority(super.createLookupElement(parameters, item), priority)
     *   ```
     * - 使用 [parameters] 检查补全会话启动的上下文。
     *   注意 [parameters.originalFile][CompletionParameters.getOriginalFile] 可能是一个
     *   [注入](https://plugins.jetbrains.com/docs/intellij/language-injection.html)文件，
     *   而所有 `lsp4j` 实体（包括 [item]）始终处理宿主文件（也称顶层文件）。
     *   [InjectedLanguageManager] 有助于在注入文件和宿主文件之间映射偏移量。
     */
    open fun createLookupElement(parameters: CompletionParameters, item: CompletionItem): LookupElement? {
        val toUseForPrefixMatching = item.filterText?.let { StringUtilRt.convertLineSeparators(it) }
            ?: item.label
        val toInsertRaw = item.textEdit?.let { if (it.isLeft) it.left.newText else it.right.newText }
            ?: item.insertText
            ?: item.label
        val toInsert = StringUtilRt.convertLineSeparators(toInsertRaw)

        return LookupElementBuilder.create(item, toInsert)
            .let { if (toInsert == toUseForPrefixMatching) it else it.withLookupString(toUseForPrefixMatching) }
    }

    /**
     * 通常插件无需重写此函数，因为可以通过重写 [getIcon]、[isBold]、[isStrikeout]、[getTypeText] 或 [getTailText] 调整补全项的展示。
     *
     * 此函数通常被调用两次：一次用于初始 [CompletionItem][item]，另一次用于
     * [已解析](https://microsoft.github.io/language-server-protocol/specification/#completionItem_resolve)的 CompletionItem。
     */
    open fun renderLookupElement(item: CompletionItem, presentation: LookupElementPresentation) {
        presentation.itemText = item.label
        presentation.icon = getIcon(item)
        presentation.isItemTextBold = isBold(item)
        presentation.isStrikeout = isStrikeout(item)
        presentation.setTailText(getTailText(item), true)
        presentation.typeText = getTypeText(item)
        presentation.isTypeGrayed = true
    }

    protected open fun getIcon(item: CompletionItem): Icon? = when (item.kind) {
        CompletionItemKind.Text -> AllIcons.Nodes.Word
        CompletionItemKind.Method -> AllIcons.Nodes.Method
        CompletionItemKind.Function -> AllIcons.Nodes.Function
        CompletionItemKind.Constructor -> AllIcons.Nodes.Class // no special icon
        CompletionItemKind.Field -> AllIcons.Nodes.Field
        CompletionItemKind.Variable -> AllIcons.Nodes.Variable
        CompletionItemKind.Class -> AllIcons.Nodes.Class
        CompletionItemKind.Interface -> AllIcons.Nodes.Interface
        CompletionItemKind.Module -> null // 'module' may mean different things
        CompletionItemKind.Property -> AllIcons.Nodes.Property
        CompletionItemKind.Unit -> null // no standard icon
        CompletionItemKind.Value -> null // no standard icon
        CompletionItemKind.Enum -> AllIcons.Nodes.Enum
        CompletionItemKind.Keyword -> null // icon not needed
        CompletionItemKind.Snippet -> AllIcons.Nodes.Template
        CompletionItemKind.Color -> AllIcons.Actions.Colors
        CompletionItemKind.File -> AllIcons.FileTypes.Any_type
        CompletionItemKind.Reference -> null // no standard icon
        CompletionItemKind.Folder -> AllIcons.Nodes.Folder
        CompletionItemKind.EnumMember -> AllIcons.Nodes.Enum // the same as for Enum
        CompletionItemKind.Constant -> AllIcons.Nodes.Constant
        CompletionItemKind.Struct -> AllIcons.Json.Object // looks like `{}`
        CompletionItemKind.Event -> null // no standard icon
        CompletionItemKind.Operator -> null // no standard icon
        CompletionItemKind.TypeParameter -> AllIcons.Nodes.Type
        null -> null
    }

    /**
     * 判断补全项是否需要加粗显示。
     */
    protected open fun isBold(item: CompletionItem): Boolean = item.kind == CompletionItemKind.Keyword

    /**
     * 判断补全项是否需要使用删除线。
     * 注意旧的 LSP 服务器实现可能仍然使用已废弃的 `item.deprecated` 属性。
     */
    protected open fun isStrikeout(item: CompletionItem): Boolean {
        @Suppress("DEPRECATION") // old LSP server implementations may use deprecated property `item.deprecated`
        return item.deprecated == true || item.tags?.contains(CompletionItemTag.Deprecated) ?: false
    }

    /**
     * 获取补全项的尾部文本。
     */
    protected open fun getTailText(item: CompletionItem): String? = item.labelDetails?.detail

    /**
     * 获取补全项的类型文本。
     */
    protected open fun getTypeText(item: CompletionItem): String? = item.detail
}
