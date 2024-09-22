package com.huawei.cangjie.psi

import com.huawei.cangjie.doc.psi.CDoc
import com.huawei.cangjie.psi.psiUtil.findDocComment
import com.huawei.cangjie.psi.stubs.CangJieTypeStatementStub
import com.intellij.lang.ASTNode
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.util.PsiTreeUtil
import java.util.concurrent.atomic.AtomicLong

abstract class CjDeclarationStub<T : StubElement<*>> : CjModifierListOwnerStub<T>, CjDeclaration {
    private val modificationStamp: AtomicLong = AtomicLong()

    constructor(stub: T, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    constructor(node: ASTNode) : super(node)

    override val expression: CjExpression?
        get() = PsiTreeUtil.getStubChildOfType(
            this,
            CjExpression::class.java
        )

    override fun subtreeChanged() {
        super.subtreeChanged()
        modificationStamp.getAndIncrement()
    }

    fun getModificationStamp(): Long {
        return modificationStamp.get()
    }

    override val docComment: CDoc?
        get() {
            return findDocComment(this)
        }

    override fun getParent(): PsiElement {
        val stub = stub
        // we build stubs for local classes/objects too but they have wrong parent
        if (stub != null && !(stub is CangJieTypeStatementStub<*> && (stub as CangJieTypeStatementStub<*>).isLocal())) {
            return stub.parentStub.psi
        }
        return super.getParent()
    }


    override fun getOriginalElement(): PsiElement {
        val navigationPolicy: CangJieDeclarationNavigationPolicy? = ApplicationManager.getApplication().getService(
            CangJieDeclarationNavigationPolicy::class.java
        )
        return navigationPolicy?.getOriginalElement(this) ?: this
    }

    override fun getNavigationElement(): PsiElement {
        val navigationPolicy: CangJieDeclarationNavigationPolicy? = ApplicationManager.getApplication().getService(
            CangJieDeclarationNavigationPolicy::class.java
        )
        return navigationPolicy?.getNavigationElement(this) ?: this
    }
}
