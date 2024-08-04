package com.linqingying.cangjie.psi.psiUtil

import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.psi.CjFile
import com.linqingying.cangjie.psi.CjModifierList
import com.intellij.lang.ASTNode
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.impl.source.tree.LazyParseablePsiElement
import com.intellij.psi.impl.source.tree.TreeUtil
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilCore

inline fun <reified T : PsiElement> PsiElement.getParentOfType(strict: Boolean): T? {
    return PsiTreeUtil.getParentOfType(this, T::class.java, strict)
}


fun Document.toPsiFile(project: Project): PsiFile? =
    PsiDocumentManager.getInstance(project).getPsiFile(this)
/**
 * Extracts node's element type
 */
val PsiElement.elementType: IElementType
    get() = elementTypeOrNull!!



val PsiElement.elementTypeOrNull: IElementType?
    // XXX: be careful not to switch to AST
    get() =   PsiUtilCore.getElementType(this)
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

fun PsiElement.nextLeaf(skipEmptyElements: Boolean = false): PsiElement? = PsiTreeUtil.nextLeaf(this, skipEmptyElements)
fun PsiElement.prevLeaf(filter: (PsiElement) -> Boolean): PsiElement? {
    var leaf = prevLeaf()
    while (leaf != null && !filter(leaf)) {
        leaf = leaf.prevLeaf()
    }
    return leaf
}
fun PsiElement.prevLeaf(skipEmptyElements: Boolean = false): PsiElement? = PsiTreeUtil.prevLeaf(this, skipEmptyElements)

fun PsiElement.nextLeaf(filter: (PsiElement) -> Boolean): PsiElement? {
    var leaf = nextLeaf()
    while (leaf != null && !filter(leaf)) {
        leaf = leaf.nextLeaf()
    }
    return leaf
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
fun PsiElement.getNextSiblingIgnoringWhitespaceAndComments(withItself: Boolean = false): PsiElement? {
    return siblings(withItself = withItself).filter { it !is PsiWhiteSpace && it !is PsiComment }.firstOrNull()
}
//fun CjModifierList.hasSuspendModifier() = hasModifier(CjTokens.SUSPEND_KEYWORD)

fun LazyParseablePsiElement.getContainingCjFile(): CjFile {

    val file = this.containingFile

    if (file is CjFile) return file

    val fileString = if (file != null && file.isValid) file.text else ""
    throw IllegalStateException("CjElement not inside CjFile: $file with text \"$fileString\" for element $this of type ${this::class.java} node = ${this.node}")
}

fun PsiElement.getElementTextWithContext(): String = com.linqingying.cangjie.utils.getElementTextWithContext(this)


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
inline fun <T : Any> T?.sure(message: () -> String): T = this ?: throw AssertionError(message())
inline fun <reified T : PsiElement> PsiElement.getChildrenOfType(): Array<T> {
    return PsiTreeUtil.getChildrenOfType(this, T::class.java) ?: arrayOf()
}
inline fun <reified T : PsiElement> PsiElement.getChildOfType(): T? {
    return PsiTreeUtil.getChildOfType(this, T::class.java)
}
inline fun <reified T : PsiElement> PsiElement.getStrictParentOfType(): T? {
    return PsiTreeUtil.getParentOfType(this, T::class.java, true)
}
fun ASTNode.children() = generateSequence(firstChildNode) { node -> node.treeNext }
fun ASTNode.siblings(forward: Boolean = true): Sequence<ASTNode> {
    if (forward) {
        return generateSequence(treeNext) { it.treeNext }
    } else {
        return generateSequence(treePrev) { it.treePrev }
    }
}
fun ASTNode.parents() = generateSequence(treeParent) { node -> node.treeParent }
fun ASTNode.leaves(forward: Boolean = true): Sequence<ASTNode> {
    if (forward) {
        return generateSequence(TreeUtil.nextLeaf(this)) { TreeUtil.nextLeaf(it) }
    } else {
        return generateSequence(TreeUtil.prevLeaf(this)) { TreeUtil.prevLeaf(it) }
    }
}

val PsiElement.startOffsetSkippingComments: Int
    get() {
        if (!startsWithComment()) return startOffset // fastpath
        val firstNonCommentChild = generateSequence(firstChild) { it.nextSibling }
            .firstOrNull { it !is PsiWhiteSpace && it !is PsiComment }
        return firstNonCommentChild?.startOffset ?: startOffset
    }
inline fun <reified T : PsiElement> T.nextSiblingOfSameType() = PsiTreeUtil.getNextSiblingOfType(this, T::class.java)

fun PsiElement.startsWithComment(): Boolean = firstChild is PsiComment

val PsiElement.textRangeWithoutComments: TextRange
    get() = if (!startsWithComment()) textRange else TextRange(startOffsetSkippingComments, endOffset)

fun PsiElement.getNextSiblingIgnoringWhitespace(withItself: Boolean = false): PsiElement? {
    return siblings(withItself = withItself).filter { it !is PsiWhiteSpace }.firstOrNull()
}
inline fun <reified T : PsiElement> PsiElement.anyDescendantOfType(
    crossinline canGoInside: (PsiElement) -> Boolean,
    noinline predicate: (T) -> Boolean = { true }
): Boolean {
    return findDescendantOfType(canGoInside, predicate) != null
}
inline fun <reified T : PsiElement> PsiElement.findDescendantOfType(
    crossinline canGoInside: (PsiElement) -> Boolean,
    noinline predicate: (T) -> Boolean = { true }
): T? {
    checkDecompiledText()
    var result: T? = null
    this.accept(object : PsiRecursiveElementWalkingVisitor() {
        override fun visitElement(element: PsiElement) {
            if (element is T && predicate(element)) {
                result = element
                stopWalking()
                return
            }

            if (canGoInside(element)) {
                super.visitElement(element)
            }
        }
    })
    return result
}
fun PsiElement.checkDecompiledText() {
    val file = containingFile
    if (file is CjFile && file.isCompiled && file.stub != null) {
        error("Attempt to load decompiled text, please use stubs instead. Decompile process might be slow and should be avoided")
    }
}
inline fun <reified T : PsiElement> PsiElement.ancestorOrSelf(): T? =
    PsiTreeUtil.getParentOfType(this, T::class.java, /* strict */ false)
