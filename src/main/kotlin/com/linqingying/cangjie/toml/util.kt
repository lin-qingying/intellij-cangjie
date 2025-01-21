package com.linqingying.cangjie.toml

import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.notification.NotificationType
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.linqingying.cangjie.CangJieBundle
import com.linqingying.cangjie.ide.completion.getElementOfType
import com.linqingying.cangjie.ide.notifications.showBalloonWithoutProject
import com.linqingying.cangjie.psi.psiUtil.ancestorOrSelf
import com.linqingying.cangjie.psi.psiUtil.elementType
import com.linqingying.cangjie.psi.psiUtil.isAncestorOf
import org.toml.lang.psi.*

fun tomlPluginIsAbiCompatible(): Boolean = computeOnce
private inline fun <reified T : Any> load(): String = T::class.java.name

private val computeOnce: Boolean by lazy {
    try {
        load<TomlKeySegment>()
        true
    } catch (e: LinkageError) {
        showBalloonWithoutProject(
            CangJieBundle.message("notification.content.incompatible.toml.plugin.version.code.completion.for.cjpm.toml.not.available"),
            NotificationType.WARNING
        )
        false
    }
}
val TomlKeySegment.isDependencyKey: Boolean
    get() {
        val name = name
        return name == "dependencies" || name == "dev-dependencies" || name == "build-dependencies" || name == "test-dependencies"
    }

val TomlTableHeader.isDependencyListHeader: Boolean
    get() = key?.segments?.lastOrNull()?.isDependencyKey == true
fun getClosestKeyValueAncestor(position: PsiElement): TomlKeyValue? {
    val parent = position.parent ?: return null
    val keyValue = parent.ancestorOrSelf<TomlKeyValue>()
        ?: error("PsiElementPattern must not allow values outside of TomlKeyValues")
    // If a value is already present we should ensure that the value is a literal
    // and the caret is inside the value to forbid completion in cases like
    // `key = "" <caret>`
    val value = keyValue.value
    return when {
        value == null || !(value !is TomlLiteral || !value.isAncestorOf(position)) -> keyValue
        else -> null
    }
}

private val TomlLiteral.literalType: IElementType
    get() = children.first().elementType

/** Inserts `=` between key and value if missed and wraps inserted string with quotes if needed */
class StringValueInsertionHandler(private val keyValue: TomlKeyValue) : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        var startOffset = context.startOffset
        val value = context.getElementOfType<TomlValue>()
        val hasEq = keyValue.children.any { it.elementType == TomlElementTypes.EQ }
        val hasQuotes = value != null && (value !is TomlLiteral || value.literalType != TomlElementTypes.NUMBER)

        if (!hasEq) {
            context.document.insertString(startOffset - if (hasQuotes) 1 else 0, "= ")
            PsiDocumentManager.getInstance(context.project).commitDocument(context.document)
            startOffset += 2
        }

        if (!hasQuotes) {
            context.document.insertString(startOffset, "\"")
            context.document.insertString(context.selectionEndOffset, "\"")
        }
    }
}