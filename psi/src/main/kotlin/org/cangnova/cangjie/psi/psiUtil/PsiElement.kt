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

package org.cangnova.cangjie.psi.psiUtil

import com.intellij.lang.ASTNode
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.*
import com.intellij.psi.impl.source.tree.LazyParseablePsiElement
import com.intellij.psi.impl.source.tree.TreeUtil
import com.intellij.psi.search.PsiSearchScopeUtil
import com.intellij.psi.search.SearchScope
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilCore
import com.intellij.psi.util.parentOfType
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.*

fun CjBlockStringTemplateEntry.dropCurlyBrackets(): CjSimpleNameStringTemplateEntry {
    val name = when (expression) {
        is CjThisExpression -> CjTokens.THIS_KEYWORD.value
        else -> (expression as CjNameReferenceExpression).referencedNameElement.text
    }

    val newEntry = CjPsiFactory(project).createSimpleNameStringTemplateEntry(name)
    return replaced(newEntry)
}
inline fun <reified I : PsiElement> psiElement(): PsiElementPattern.Capture<I> {
    return PlatformPatterns.psiElement(I::class.java)
}
fun PsiElement.isInsideAnnotationEntryArgumentList(): Boolean =
    parentOfType<CjValueArgumentList>()?.parent is CjAnnotation

fun CjBlockStringTemplateEntry.canDropCurlyBrackets(): Boolean {
    val expression = this.expression
    return (expression is CjNameReferenceExpression || (expression is CjThisExpression && expression.labelQualifier == null)) &&
        canPlaceAfterSimpleNameEntry(nextSibling)
}
fun PsiElement.isAncestorOf(child: PsiElement): Boolean =
    child.ancestors.contains(this)

val PsiElement.ancestors: Sequence<PsiElement>
    get() = generateSequence(this) {
        if (it is PsiFile) null else it.parent
    }
inline fun <reified T : PsiElement, reified V : PsiElement> PsiElement.getParentOfTypes2(): PsiElement? {
    return PsiTreeUtil.getParentOfType(this, T::class.java, V::class.java)
}

inline fun <reified T : PsiElement> PsiElement.collectDescendantsOfType(noinline predicate: (T) -> Boolean = { true }): List<T> {
    return collectDescendantsOfType({ true }, predicate)
}

inline fun <reified T : PsiElement> PsiElement.ancestorStrict(): T? =
    PsiTreeUtil.getParentOfType(this, T::class.java, /* strict */ true)

inline fun <reified T : PsiElement> PsiElement.collectDescendantsOfType(
    crossinline canGoInside: (PsiElement) -> Boolean,
    noinline predicate: (T) -> Boolean = { true },
): List<T> = collectDescendantsOfTypeTo(ArrayList(), canGoInside, predicate)

inline fun <reified T : PsiElement, C : MutableCollection<T>> PsiElement.collectDescendantsOfTypeTo(
    to: C,
    crossinline canGoInside: (PsiElement) -> Boolean,
    noinline predicate: (T) -> Boolean = { true },
): C {
    forEachDescendantOfType<T>(canGoInside) {
        if (predicate(it)) {
            to.add(it)
        }
    }
    return to
}

inline fun <reified T : PsiElement> PsiElement.replaced(newElement: T): T {
    if (this == newElement) {
        return newElement
    }

    return when (val result = replace(newElement)) {
        is T -> result
        else -> (result as CjParenthesizedExpression).expression as T
    }
}
inline fun <reified T : PsiElement> PsiElement.getParentOfType(strict: Boolean, vararg stopAt: Class<out PsiElement>): T? {
    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    return PsiTreeUtil.getParentOfType(this, T::class.java, strict, *stopAt)
}

inline fun <reified T : PsiElement> PsiElement.getParentOfType(strict: Boolean): T? {
    return PsiTreeUtil.getParentOfType(this, T::class.java, strict)
}

inline val PsiElement.identifier
    get() = when (this) {
        is CjSimpleNameExpression -> this.identifier

        else -> null
    }

inline fun <reified T : PsiElement> T.prevSiblingOfSameType() = PsiTreeUtil.getPrevSiblingOfType(this, T::class.java)

fun CjExpression.getBinaryWithTypeParent(): CjBinaryExpressionWithTypeRHS? {
    val callExpression = parent as? CjCallExpression ?: return null
    val possibleQualifiedExpression = callExpression.parent

    val targetExpression = if (possibleQualifiedExpression is CjQualifiedExpression) {
        if (possibleQualifiedExpression.selectorExpression != callExpression) return null
        possibleQualifiedExpression
    } else {
        callExpression
    }

    return targetExpression.topParenthesizedParentOrMe().parent as? CjBinaryExpressionWithTypeRHS
}

fun PsiElement?.unwrapParenthesesLabelsAndAnnotations(): PsiElement? {
    var unwrapped = this
    while (true) {
        unwrapped = when (unwrapped) {
            is CjParenthesizedExpression -> unwrapped.expression
//            is CjLabeledExpression -> unwrapped.baseExpression
//            is CjAnnotatedExpression -> unwrapped.baseExpression
            else -> return unwrapped
        }
    }
}

inline fun <reified T : PsiElement> PsiElement.findDescendantOfType(noinline predicate: (T) -> Boolean = { true }): T? {
    return findDescendantOfType({ true }, predicate)
}

fun CjExpression.topParenthesizedParentOrMe(): CjExpression {
    var result: CjExpression = this
    while (CjPsiUtil.deparenthesizeOnce(result.parent as? CjExpression) == result) {
        result = result.parent as? CjExpression ?: break
    }
    return result
}

fun Document.toPsiFile(project: Project): PsiFile? =
    PsiDocumentManager.getInstance(project).getPsiFile(this)

/**
 * Extracts node's element type
 */
val PsiElement.elementType: IElementType
    get() = elementTypeOrNull!!

operator fun SearchScope.contains(element: PsiElement): Boolean = PsiSearchScopeUtil.isInScope(this, element)

val PsiElement.elementTypeOrNull: IElementType?
    // XXX: be careful not to switch to AST
    get() = PsiUtilCore.getElementType(this)

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
// fun CjModifierList.hasSuspendModifier() = hasModifier(CjTokens.SUSPEND_KEYWORD)

fun LazyParseablePsiElement.getContainingCjFile(): CjFile {
    val file = this.containingFile

    if (file is CjFile) return file

    val fileString = if (file != null && file.isValid) file.text else ""
    throw IllegalStateException("CjElement not inside CjFile: $file with text \"$fileString\" for element $this of type ${this::class.java} node = ${this.node}")
}

fun PsiElement.getElementTextWithContext(): String = getElementTextWithContext(this)
inline fun <reified T : PsiElement> PsiElement.parentOfType(withSelf: Boolean = false): T? {
    return PsiTreeUtil.getParentOfType(this, T::class.java, !withSelf)
}

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
    noinline predicate: (T) -> Boolean = { true },
): Boolean {
    return findDescendantOfType(canGoInside, predicate) != null
}

inline fun <reified T : PsiElement> PsiElement.findDescendantOfType(
    crossinline canGoInside: (PsiElement) -> Boolean,
    noinline predicate: (T) -> Boolean = { true },
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

fun CjExpression.isEmptyBody(): Boolean = this is CjBlockExpression && statements.isEmpty()

/**
 * 获取return语句的上层元素
 * 可能是方法，也可能是lambda表达式
 */
val CjReturnExpression.returnTarget: CjDeclaration?
    get() {
        return parentOfType<CjFunctionLiteral>() ?: parentOfType<CjFunction>()
    }

val CjBlockExpression.returnTarget get() = parentOfType<CjFunctionLiteral>() ?: parentOfType<CjFunction>()


/**
 * 查找可以被求值的表达式
 *
 * 从给定的PSI元素开始，向上遍历PSI树，查找第一个可以被求值的表达式。
 * 支持：
 * - 简单变量引用（如 `x`）
 * - 属性访问（如 `obj.field`）
 * - 数组访问（如 `arr[0]`）
 * - 函数调用（如 `func()`）- 仅在 sideEffectsAllowed 为 true 时
 *
 * @param element PSI元素
 * @param sideEffectsAllowed 是否允许有副作用的表达式（如函数调用）
 * @return 可求值的表达式，如果没有则返回null
 */
  fun PsiElement.findEvaluatableExpression(

    sideEffectsAllowed: Boolean
): PsiElement? {
    var current: PsiElement? = this

    while (current != null) {
        // 检查是否是可以求值的表达式类型
        when (current) {
            // 简单变量名 - 总是允许
            is CjSimpleNameExpression -> return current

            // 引用表达式（属性访问）- 总是允许
            is CjReferenceExpression -> {
                // 排除函数调用（函数调用也是引用表达式）
                if (current !is CjCallExpression) {
                    return current
                }
            }

            // 函数调用 - 仅在允许副作用时
            is CjCallExpression -> {
                if (sideEffectsAllowed) {
                    return current
                }
            }

            // 数组访问 - 总是允许
            is CjArrayAccessExpression -> return current

            // 二元表达式（加减乘除等）- 总是允许
            is CjBinaryExpression -> return current

            // 常量表达式（字面量）- 总是允许
            is CjConstantExpression -> return current

            // 如果遇到语句级别的元素，停止向上查找
            is CjBlockExpression,
            is CjDeclaration -> return null
        }

        current = current.parent
    }

    return null
}