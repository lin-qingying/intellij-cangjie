package com.huawei.cangjie.psi

import com.huawei.cangjie.lang.CangJieLanguage
import com.huawei.cangjie.psi.CangJieReferenceProvidersService.Companion.getReferencesFromProviders
import com.huawei.cangjie.psi.stubs.elements.CjStubElementType
import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.ASTNode
import com.intellij.lang.Language
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiReference
import com.intellij.psi.StubBasedPsiElement
import com.intellij.psi.impl.source.PsiFileImpl
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement

open class CjElementImplStub<T : StubElement<*>> :
    StubBasedPsiElementBase<T>, CjElement, StubBasedPsiElement<T> {
    constructor(stub: T, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    constructor(stub: T, nodeType: IStubElementType<*, *>, node: ASTNode?) : super(stub, nodeType, node)

    constructor(node: ASTNode) : super(node)

    override fun getPsiOrParent(): CjElement {
        return this
    }


    override fun toString(): String {
        return node.elementType.toString()
    }


    override fun getReference(): PsiReference? {
        val references = references
        return if ((references.isNotEmpty())) references[0] else null
    }

    override fun getReferences(): Array<PsiReference> {
        return getReferencesFromProviders(this)
    }

    override fun getContainingCjFile(): CjFile {
        val file = containingFile
        if (file !is CjFile) {
            var fileString = ""
            if (file.isValid) {
                try {
                    fileString = " " + file.text
                } catch (_: Exception) {
                }
            }
            // getNode() will fail if getContainingFile() returns not PsiFileImpl instance
            val nodeString = (if (file is PsiFileImpl) (" node = $node") else "")

            throw IllegalStateException(
                "CjElement not inside CjFile: " +
                        file + fileString + " of type " + file.javaClass +
                        " for element " + this + " of type " + this.javaClass + nodeString
            )
        }
        return file
    }


    override fun getLanguage(): Language {
        return CangJieLanguage
    }


    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is CjVisitor<*, *>) {
            accept(visitor, null)
        } else {
            visitor.visitElement(this)
        }
    }

    override fun <D> acceptChildren(visitor: CjVisitor<Void, D>, data: D) {
        var child = firstChild
        while (child != null) {
            if (child is CjElement) {
                child.accept(visitor, data)
            }
            child = child.nextSibling
        }
    }

    fun <PsiT : CjElementImplStub<*>, StubT : StubElement<*>> getStubOrPsiChildrenAsList(
        elementType: CjStubElementType<StubT, PsiT>
    ): List<PsiT> {
        return listOf(*getStubOrPsiChildren(elementType, elementType.arrayFactory))
    }

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitCjElement(this, data)
    }
}
