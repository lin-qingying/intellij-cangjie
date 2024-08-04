package com.linqingying.cangjie.psi

import com.linqingying.cangjie.psi.psiUtil.unpackFunctionLiteral
import com.linqingying.cangjie.psi.stubs.CangJieValueArgumentStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode


class CjLambdaArgument : CjValueArgument, LambdaArgument {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJieValueArgumentStub<CjLambdaArgument>) : super(stub, CjStubElementTypes.LAMBDA_ARGUMENT) {}

    override fun getLambdaExpression(): CjLambdaExpression? = getArgumentExpression()?.unpackFunctionLiteral()
}
