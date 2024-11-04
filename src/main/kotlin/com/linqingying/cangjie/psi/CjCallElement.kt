package com.linqingying.cangjie.psi

interface CjCallElement : CjElement {
    val calleeExpression: CjExpression?

    val valueArgumentList: CjValueArgumentList?

    val valueArguments: List<ValueArgument>

    val lambdaArguments: List<CjLambdaArgument>

    val typeArguments: List<CjTypeProjection>

    val typeArgumentList: CjTypeArgumentList?
}
