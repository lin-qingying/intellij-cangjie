package com.huawei.cangjie.psi


import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiReference


interface CjElement : NavigatablePsiElement, CjPureElement {

    fun <D> acceptChildren(visitor: CjVisitor<Void, D>, data: D)

    fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R

    @Deprecated("Don't use getReference() on CjElement for the choice is unpredictable")
    override fun getReference(): PsiReference?
}


open class CjElementImpl(node: ASTNode) : ASTWrapperPsiElement(node), CjElement {


    override fun toString(): String = node.elementType.toString()

    override fun <D> acceptChildren(visitor: CjVisitor<Void, D>, data: D) {
        CjPsiUtil.visitChildren<D>(this, visitor, data)
    }

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R = visitor.visitCjElement(this, data)

    override fun getPsiOrParent(): CjElement = this
    override fun getContainingCjFile(): CjFile {
        val file = containingFile
        if (file !is CjFile) {
            val fileString = if (file != null && file.isValid) " " + file.text else ""
            throw IllegalStateException(
                "CjElement not inside CjFile: " + file + fileString +
                        " for element " + this + " of type " + this.javaClass + " node = " + node
            )
        }
        return file
    }

}
