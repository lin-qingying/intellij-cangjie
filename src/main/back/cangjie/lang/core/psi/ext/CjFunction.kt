package com.huawei.cangjie.lang.core.psi.ext


import com.huawei.cangjie.lang.core.psi.CjBlock
import com.huawei.cangjie.lang.core.psi.CjFunction
import com.huawei.cangjie.lang.core.psi.CjPsiImplUtil
import com.huawei.cangjie.lang.core.stubs.CjFunctionStub
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.SimpleModificationTracker
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.util.PsiTreeUtil


abstract class CjFunctionImplMixin : CjStubbedNamedElementImpl<CjFunctionStub>, CjFunction,CjModificationTrackerOwner {
    constructor(node: ASTNode) : super(node)
    override val modificationTracker: SimpleModificationTracker =
        SimpleModificationTracker()
    constructor(stub: CjFunctionStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)
//
//    override val crateRelativePath: String?
//        get() =  CjPsiImplUtil.crateRelativePath(this)

    override fun incModificationCount(element: PsiElement): Boolean {
        val shouldInc = block?.isAncestorOf(element) == true && PsiTreeUtil.findChildOfAnyType(
            element,
            false,
            CjItemElement::class.java

        ) == null
        if (shouldInc) modificationTracker.incModificationCount()
        return shouldInc
    }
}

val CjFunction.functionName: String?
    get() = (this as CjFunctionImplMixin).functionName


val CjFunction.block: CjBlock? get() = PsiTreeUtil.getChildOfType(this, CjBlock::class.java)


