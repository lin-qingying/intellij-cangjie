package com.linqingying.lsp.api.customization
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.lang.injection.InjectedLanguageManager
import org.eclipse.lsp4j.CompletionItem
import org.eclipse.lsp4j.CompletionItemKind
import org.eclipse.lsp4j.CompletionItemTag
import org.jetbrains.annotations.ApiStatus
import javax.swing.Icon

/**
 * Handles [CompletionItem](https://microsoft.github.io/language-server-protocol/specification#completionItem) objects
 * received from the LSP server.
 * Implementations may fine-tune the code completion behavior.
 * For example, they may filter out unneeded completion items or tweak completion item decoration.
 */
@ApiStatus.Experimental
open class LspCompletionSupport {
    /**
     * Called when the IDE is going to run a code completion session.
     * It might be triggered, for example, by pressing a shortcut or by typing an identifier.
     * Implementations may return `false` if they don't want to have LSP-based code completion at the given location.
     */
    open fun shouldRunCodeCompletion(parameters: CompletionParameters): Boolean = true

    /**
     * Converts [CompletionItem](https://microsoft.github.io/language-server-protocol/specification#completionItem) object
     * received from the LSP server into [LookupElement] object.
     *
     * Some ideas that the overriding functions can implement:
     * - return `null` if they want to ignore this [item]
     *
     *
     * - tune completion item priority:
     *
     *
     *    PrioritizedLookupElement.withPriority(super.createLookupElement(parameters, item), priority)
     * - cast to [LookupElementBuilder] and call its `withFoo()` functions, for example:
     *
     *
     *    (super.createLookupElement(parameters, item) as LookupElementBuilder).withItemTextItalic(true)
     *
     * - use [parameters] if they need to check the context, in which this code completion session has started.
     *   Note that [parameters.originalFile][CompletionParameters.getOriginalFile] might be an
     *   [injected](https://plugins.jetbrains.com/docs/intellij/language-injection.html) file,
     *   while all `lsp4j` entities (including [item]) always deal with the host file (also known as top-level file).
     *   [InjectedLanguageManager] helps to map offsets between an injected and a host file.
     */
    open fun createLookupElement(parameters: CompletionParameters, item: CompletionItem): LookupElement? {
        val toShow = item.label
        val toUseForPrefixMatching = item.filterText ?: item.label
        val toInsert = item.textEdit?.let { if (it.isLeft) it.left.newText else it.right.newText } ?: item.insertText ?: item.label

        return LookupElementBuilder.create(item, toInsert)
            .withPresentableText(toShow)
            .let { if (toInsert == toUseForPrefixMatching) it else it.withLookupString(toUseForPrefixMatching) }
            .withIcon(getIcon(item))
            .withBoldness(isBold(item))
            .withStrikeoutness(isStrikeout(item))
            .withTailText(getTailText(item), true)
            .withTypeText(getTypeText(item), true)
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
