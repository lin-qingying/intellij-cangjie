package com.huawei.cangjie1.psi.psiUtil

import com.huawei.cangjie1.psi.CjFile
import com.intellij.psi.*
import com.intellij.psi.impl.source.tree.LazyParseablePsiElement
import java.util.NoSuchElementException

fun PsiElement.siblings(forward: Boolean = true, withItself: Boolean = true): Sequence<PsiElement> {
    return object : Sequence<PsiElement> {
        override fun iterator(): Iterator<PsiElement> {
            var next: PsiElement? = this@siblings
            return object : Iterator<PsiElement> {
                init {
                    if (!withItself) next()
                }

                override fun hasNext(): Boolean = next != null
                override fun next(): PsiElement {
                    val result = next ?: throw NoSuchElementException()
                    next = if (forward) result.nextSibling else result.prevSibling
                    return result
                }
            }
        }
    }
}

val PsiElement.endOffset: Int
    get() = textRange.endOffset

val PsiElement.startOffset: Int
    get() = textRange.startOffset
val PsiElement.parentsWithSelf: Sequence<PsiElement>
    get() = generateSequence(this) { if (it is PsiFile) null else it.parent }

val PsiElement.allChildren: PsiChildRange
    get() {
        val first = firstChild
        return if (first != null) PsiChildRange(first, lastChild) else PsiChildRange.EMPTY
    }

fun PsiElement.getPrevSiblingIgnoringWhitespaceAndComments(withItself: Boolean = false): PsiElement? {
    return siblings(withItself = withItself, forward = false).filter { it !is PsiWhiteSpace && it !is PsiComment }
        .firstOrNull()
}

fun LazyParseablePsiElement.getContainingCjFile(): CjFile {

    val file = this.containingFile

    if (file is CjFile) return file

    val fileString = if (file != null && file.isValid) file.text else ""
    throw IllegalStateException("CjElement not inside CjFile: $file with text \"$fileString\" for element $this of type ${this::class.java} node = ${this.node}")
}

fun PsiElement.getElementTextWithContext(): String = com.huawei.cangjie1.utils.getElementTextWithContext(this)


fun PsiElement.parentOfType(vararg psiClassNames: String): PsiElement? {
    fun acceptsClass(javaClass: Class<*>): Boolean {
        if (javaClass.simpleName in psiClassNames) return true
        javaClass.superclass?.let { if (acceptsClass(it)) return true }
        for (superInterface in javaClass.interfaces) {
            if (acceptsClass(superInterface)) return true
        }
        return false
    }
    return generateSequence(this) { it.parent }
        .filter { it !is PsiFile }
        .firstOrNull { acceptsClass(it::class.java) }
}
