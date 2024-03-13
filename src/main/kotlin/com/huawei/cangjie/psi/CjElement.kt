package com.huawei.cangjie.psi


import com.huawei.cangjie.lang.CangJieLanguage
import com.huawei.cangjie.psi.psiUtil.parentSubstitute
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.lang.Language
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiReference


interface CjElement : NavigatablePsiElement, CjPureElement {

    fun <D> acceptChildren(visitor: CjVisitor<Void, D>, data: D)

    fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R

    @Deprecated("Don't use getReference() on CjElement for the choice is unpredictable")
    override fun getReference(): PsiReference?

}


open class CjElementImpl(node: ASTNode) : ASTWrapperPsiElement(node), CjElement {


    override fun toString(): String = node.elementType.toString()

    override fun <D> acceptChildren(visitor: CjVisitor<Void, D>, data: D) {
        CjPsiUtil.visitChildren<D>(this, visitor, data)
    }


    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is CjVisitor<*, *>) {
            accept(visitor, null)
        } else {
            visitor.visitElement(this)
        }
    }

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R = visitor.visitCjElement(this, data)

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

    override fun delete() {
        this.deleteSemicolon()
        super.delete()
    }


    override fun getReference(): PsiReference? {
        val references = references
        return if (references.size == 1) references[0] else null
    }

    override fun getReferences(): Array<PsiReference> {

//        return arrayOf()
        return CangJieReferenceProvidersService.getReferencesFromProviders(this)
    }

    override fun getParent(): PsiElement {
        val substitute: PsiElement? = this.parentSubstitute
        return substitute ?: super.getParent()
    }

    override fun getLanguage(): Language  = CangJieLanguage
}


//fun CjElement.findInScope(name: String, ns: Set<Namespace>): PsiElement? {
//    return pickFirstResolveVariant(name) {
//        processNestedScopesUpwards(this, ns, it)
//    }
//}
