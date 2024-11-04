package com.linqingying.cangjie.psi.psiUtil

import com.linqingying.cangjie.psi.CjDeclaration
import com.linqingying.cangjie.psi.CjElement
import com.linqingying.cangjie.psi.CjElementImplStub
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ArrayFactory

object CjStubbedPsiUtil {
    @JvmStatic
    fun getContainingDeclaration(element: PsiElement): CjDeclaration? {
        return getPsiOrStubParent(element, CjDeclaration::class.java, true)
    }
    @JvmStatic
    fun <T : CjDeclaration> getContainingDeclaration(element: PsiElement, declarationClass: Class<T>): T? {
        return getPsiOrStubParent(element, declarationClass, true)
    }

    @JvmStatic
    fun <T : CjElement> getPsiOrStubParent(
        element: PsiElement,
        declarationClass: Class<T>,
        strict: Boolean
    ): T? {
        if (!strict && declarationClass.isInstance(element)) {
            return element as T
        }
        if (element is CjElementImplStub<*>) {
            val stub = element.stub
            if (stub != null) {
                return stub.getParentStubOfType(declarationClass)
            }
        }
        return PsiTreeUtil.getParentOfType(element, declarationClass, strict)
    }
@JvmStatic
    fun <T : CjElement> getStubOrPsiChild(
    element: CjElementImplStub<*>,
    types: TokenSet,
    factory: ArrayFactory<T?>
    ): T? {
        val typeElements = element.getStubOrPsiChildren(types, factory)
        if (typeElements.isEmpty()) {
            return null
        }
        return typeElements[0]
    }
}
