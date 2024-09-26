
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
import org.jetbrains.annotations.ApiStatus
import javax.swing.Icon

/**
 * Handles [CompletionItem](https://microsoft.github.io/language-server-protocol/specification#initialCompletionItem) objects
 * received from the LSP server.
 * Implementations may fine-tune the code completion behavior.
 * For example, they may filter out unneeded completion items or tweak completion item decoration.
 */

open class LspCompletionSupport {
  /**
   * Called when the IDE is going to run a code completion session.
   * It might be triggered, for example, by pressing a shortcut or by typing an identifier.
   * Implementations may return `false` if they don't want to have LSP-based code completion at the given location.
   */
  open fun shouldRunCodeCompletion(parameters: CompletionParameters): Boolean = true

  /**
   * Converts [CompletionItem](https://microsoft.github.io/language-server-protocol/specification#initialCompletionItem) object
   * received from the LSP server into [LookupElement] object.
   *
   * Note that implementations shouldn't manipulate the completion item presentation in this function.
   * To tune the presentation, override functions like [getIcon], [isBold], [isStrikeout], [getTypeText], or [getTailText].
   * In advanced use cases, plugins can override [renderLookupElement].
   *
   * Some ideas that the overriding functions can implement:
   * - return `null` if they want to ignore this [item]
   * - tune completion item priority:
   *   ```
   *   PrioritizedLookupElement.withPriority(super.createLookupElement(parameters, item), priority)
   *   ```
   * - use [parameters] if they need to check the context, in which this code completion session has started.
   *   Note that [parameters.originalFile][CompletionParameters.getOriginalFile] might be an
   *   [injected](https://plugins.jetbrains.com/docs/intellij/language-injection.html) file,
   *   while all `lsp4j` entities (including [item]) always deal with the host file (also known as top-level file).
   *   [InjectedLanguageManager] helps to map offsets between an injected and a host file.
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
   * Typically, plugins don't need to override this function as they can tune the completion item presentation
   * by overriding [getIcon], [isBold], [isStrikeout], [getTypeText], or [getTailText].
   *
   * This function is usually called twice: first, for the initial [CompletionItem][item],
   * and later for the [resolved](https://microsoft.github.io/language-server-protocol/specification/#completionItem_resolve) one.
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
    else -> null
  }

  protected open fun isBold(item: CompletionItem): Boolean = item.kind == CompletionItemKind.Keyword

  protected open fun isStrikeout(item: CompletionItem): Boolean {
    @Suppress("DEPRECATION") // old LSP server implementations may use deprecated property `item.deprecated`
    return item.deprecated == true || item.tags?.contains(CompletionItemTag.Deprecated) ?: false
  }

  protected open fun getTailText(item: CompletionItem): String? = item.labelDetails?.detail

  protected open fun getTypeText(item: CompletionItem): String? = item.detail
}
