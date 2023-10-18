package com.huawei.cangjie.lang.core.psi.ext

import com.huawei.cangjie.lang.core.psi.CjFile
import com.huawei.cangjie.lang.core.stubs.CjFileStub
import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.StubBasedPsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilCore


inline fun <reified T : PsiElement> PsiElement.contextStrict(): T? =
    PsiTreeUtil.getContextOfType(this, T::class.java, /* strict */ true)
val PsiElement.containingCjFileSkippingCodeFragments: CjFile?
    get() {
        var containingFile = containingFile.originalFile

        while (containingFile !is CjFile) {
            containingFile = containingFile.context?.containingFile?.originalFile ?: break
        }
        return containingFile as? CjFile
    }
inline fun <reified T : PsiElement> PsiElement.descendantOfTypeStrict(): T? =
    PsiTreeUtil.findChildOfType(this, T::class.java, /* strict */ true)

@Suppress("UNCHECKED_CAST")
inline val <T : StubElement<*>> StubBasedPsiElement<T>.greenStub: T?
    get() = (this as? StubBasedPsiElementBase<T>)?.greenStub

inline fun <reified T : PsiElement> PsiElement.childrenOfType(): List<T> =
    PsiTreeUtil.getChildrenOfTypeAsList(this, T::class.java)
inline fun <reified T : PsiElement> PsiElement.childOfType(): T? =
    PsiTreeUtil.getChildOfType(this, T::class.java)
val PsiElement.elementType: IElementType
    get() = elementTypeOrNull!!
val PsiElement.elementTypeOrNull: IElementType?
    // XXX: be careful not to switch to AST
    get() = if (this is CjFile) CjFileStub.Type else PsiUtilCore.getElementType(this)

inline fun <reified T : PsiElement> PsiElement.ancestorStrict(): T? =
    PsiTreeUtil.getParentOfType(this, T::class.java, /* strict */ true)

fun PsiElement?.getPrevNonWhitespaceSibling(): PsiElement? =
    PsiTreeUtil.skipWhitespacesBackward(this)



val PsiElement.contexts: Sequence<PsiElement>
    get() = generateSequence(this) {
        if (it is PsiFile) null else it.context
    }
val PsiElement.stubAncestors: Sequence<PsiElement>
    get() = generateSequence(this) {
        if (it is PsiFile) null else it.stubParent
    }

val PsiElement.stubParent: PsiElement?
    get() {
        if (this is StubBasedPsiElement<*>) {
            val stub = this.greenStub
            if (stub != null) return stub.parentStub?.psi
        }
        return parent
    }
fun PsiElement.isAncestorOf(child: PsiElement): Boolean =
    child.ancestors.contains(this)
val PsiElement.ancestors: Sequence<PsiElement>
    get() = generateSequence(this) {
        if (it is PsiFile) null else it.parent
    }
