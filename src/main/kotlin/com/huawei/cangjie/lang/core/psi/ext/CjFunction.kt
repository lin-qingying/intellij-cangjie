package com.huawei.cangjie.lang.core.psi.ext

import com.huawei.cangjie.lang.core.psi.CangJieBlock
import com.huawei.cangjie.lang.core.psi.CangJieFunction
import com.huawei.cangjie.lang.core.stubs.CjFunctionStub
import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.util.PsiTreeUtil


abstract class CjFunctionImplMixin : CjStubbedNamedElementImpl<CjFunctionStub>, CangJieFunction {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CjFunctionStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

}

val CangJieFunction.functionName: String?
    get() = (this as CjFunctionImplMixin).functionName
val CangJieFunction.block: CangJieBlock? get() = PsiTreeUtil.getChildOfType(this, CangJieBlock::class.java)


