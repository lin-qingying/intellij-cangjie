package com.huawei.cangjie.psi

import com.huawei.cangjie.psi.psiUtil.unpackFunctionLiteral
import com.huawei.cangjie.psi.stubs.CangJieValueArgumentStub
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode


class CjLambdaArgument : CjValueArgument, LambdaArgument {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJieValueArgumentStub<CjLambdaArgument>) : super(stub, CjStubElementTypes.LAMBDA_ARGUMENT)

    override fun getLambdaExpression(): CjLambdaExpression? = getArgumentExpression()?.unpackFunctionLiteral()
}
