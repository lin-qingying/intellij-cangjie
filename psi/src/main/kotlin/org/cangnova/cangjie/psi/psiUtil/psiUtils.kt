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

package org.cangnova.cangjie.psi.psiUtil

import org.cangnova.cangjie.psi.*
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.findParentInFile
import com.intellij.psi.util.isAncestor
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes

val PsiElement.cangjieFqName: FqName?
    get() = when (this) {

        is CjNamedDeclaration -> this.fqName
        else -> null
    }
fun TextRange.containsInside(offset: Int): Boolean = startOffset < offset && offset < endOffset
inline fun <reified T : PsiElement> PsiElement.getLastParentOfTypeInRow() =
    parents.takeWhile { it is T }.lastOrNull() as? T

fun PsiElement.isExtensionDeclaration(): Boolean {
    val callable: CjCallableDeclaration? = when (this) {
        is CjNamedFunction, is CjProperty   -> this as CjCallableDeclaration
        is CjPropertyAccessor -> getNonStrictParentOfType<CjProperty>()
        else -> null
    }

    return callable ?.getParentOfType<CjExtend>(false) != null
}

fun getElementTextWithContext(psiElement: PsiElement): String {
    if (!psiElement.isValid) return "<invalid element $psiElement>"

    @Suppress("LocalVariableName")
    val ELEMENT_TAG = "ELEMENT"
    val containingFile = psiElement.containingFile
    val context = psiElement.parentOfType("CjImportItem")
        ?: psiElement.parentOfType("CjPackageDirective")
        ?: psiElement.parentOfType("CjDeclarationWithBody")
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
    noinline action: (T) -> Unit,
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

inline fun <reified T : PsiElement> PsiElement.getParentOfTypeAndBranch(
    strict: Boolean = false,
    noinline branch: T.() -> PsiElement?
): T? {
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

inline fun <reified T : PsiElement, reified V : PsiElement, reified U : PsiElement> PsiElement.getParentOfTypes3(): PsiElement? {
    return PsiTreeUtil.getParentOfType(this, T::class.java, V::class.java, U::class.java)
}


fun CjElementImplStub<*>.getAllModifierLists(): Array<out CjDeclarationModifierList> =
    getStubOrPsiChildren(CjStubElementTypes.MODIFIER_LIST, CjStubElementTypes.MODIFIER_LIST.arrayFactory)
