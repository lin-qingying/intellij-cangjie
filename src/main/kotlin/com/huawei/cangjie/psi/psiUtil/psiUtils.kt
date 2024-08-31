package com.huawei.cangjie.psi.psiUtil

import com.huawei.cangjie.ide.highlighter.namedUnwrappedElement
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.psi.CjNamedDeclaration
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.findParentInFile
import com.intellij.psi.util.findParentOfType
import com.intellij.psi.util.isAncestor

fun getElementTextWithContext(psiElement: PsiElement): String {
    if (!psiElement.isValid) return "<invalid element $psiElement>"

    @Suppress("LocalVariableName") val ELEMENT_TAG = "ELEMENT"
    val containingFile = psiElement.containingFile
    val context = psiElement.parentOfType("CjImportDirective")
        ?: psiElement.parentOfType("CjPackageDirective")
        ?: psiElement.parentOfType("CjDeclarationWithBody"  )
        ?: psiElement.parentOfType("CjProperty")
        ?: containingFile
    val elementTextInContext = buildString {
        context.accept(object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element === psiElement) append("<$ELEMENT_TAG>")
                if (element is LeafPsiElement) {
                    append(element.text)
                } else {
                    element.acceptChildren(this)
                }
                if (element === psiElement) append("</$ELEMENT_TAG>")
            }
        })
    }.trimIndent().trim()

    return buildString {
        appendLine("<File name: ${containingFile.name}, Physical: ${containingFile.isPhysical}>")
        append(elementTextInContext)
    }
}
inline fun <reified T : PsiElement> PsiElement.getNonStrictParentOfType(): T? {
    return PsiTreeUtil.getParentOfType(this, T::class.java, false)
}
val PsiElement.parents: Sequence<PsiElement>
    get() = parentsWithSelf.drop(1)
val PsiElement.parentsWithSelf: Sequence<PsiElement>
    get() = generateSequence(this) { if (it is PsiFile) null else it.parent }

inline fun <reified T : PsiElement> PsiElement.anyDescendantOfType(noinline predicate: (T) -> Boolean = { true }): Boolean {
    return findDescendantOfType(predicate) != null
}
inline fun <reified T : PsiElement> PsiElement.forEachDescendantOfType(noinline action: (T) -> Unit) {
    forEachDescendantOfType({ true }, action)
}

inline fun <reified T : PsiElement> PsiElement.findParentOfType(strict: Boolean = true): T? {
    return findParentInFile(!strict) { it is T } as? T
}

inline fun <reified T : PsiElement> PsiElement.forEachDescendantOfType(
    crossinline canGoInside: (PsiElement) -> Boolean,
    noinline action: (T) -> Unit
) {
    checkDecompiledText()
    this.accept(object : PsiRecursiveElementVisitor() {
        override fun visitElement(element: PsiElement) {
            if (canGoInside(element)) {
                super.visitElement(element)
            }

            if (element is T) {
                action(element)
            }
        }
    })
}
inline fun <reified T : PsiElement> PsiElement.getParentOfTypeAndBranch(strict: Boolean = false, noinline branch: T.() -> PsiElement?): T? {
    return getParentOfType<T>(strict)?.getIfChildIsInBranch(this, branch)
}


fun <T : PsiElement> T.getIfChildIsInBranch(element: PsiElement, branch: T.() -> PsiElement?): T? {
    return if (branch().isAncestor(element)) this else null
}
fun PsiElement.getPrevSiblingIgnoringWhitespace(withItself: Boolean = false): PsiElement? {
    return siblings(withItself = withItself, forward = false).filter { it !is PsiWhiteSpace }.firstOrNull()
}
val PsiChildRange.textRange: TextRange?
    get() {
        if (isEmpty) return null
        return TextRange(first!!.startOffset, last!!.endOffset)
    }
fun PsiElement.getStartOffsetIn(ancestor: PsiElement): Int {
    var offset = 0
    var parent = this
    while (parent != ancestor) {
        offset += parent.startOffsetInParent
        parent = parent.parent
    }
    return offset
}
val PsiElement.cangjieFqName: FqName?
    get() = when (val element = namedUnwrappedElement) {

        is CjNamedDeclaration -> element.fqName
        else -> null
    }
