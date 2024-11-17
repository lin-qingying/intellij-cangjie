package com.linqingying.cangjie.psi

import com.intellij.lang.ASTNode
import com.linqingying.cangjie.CjNodeTypes
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.name.Name.Companion.identifier
import com.linqingying.cangjie.psi.stubs.CangJieMacroExpressionStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes

class CjMacroExpression : CjElementImplStub<CangJieMacroExpressionStub>, CjCallElement {

    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJieMacroExpressionStub) : super(stub, CjStubElementTypes.MACRO_EXPRESSION)

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitMacroExpression(this, data)
    }

    val referenceExpression: CjNameReferenceExpression? get() = findChildByType(CjNodeTypes.REFERENCE_EXPRESSION)
    val shortName: Name?
        get() {
            val stub = stub
            if (stub != null) {
                val shortName = stub.getShortName()
                if (shortName != null) {
                    return identifier(shortName)
                }
                return null
            }
            if (referenceExpression != null) {
                return referenceExpression!!.getReferencedNameAsName()
            }

            return null
        }
    val input: CjMacroInput? get() = findChildByType(CjNodeTypes.MACRO_INPUT)
    val attr: CjMacroAttr? get() = findChildByType(CjNodeTypes.MACRO_ATTR)


    override val calleeExpression: CjExpression?
        get() = null
    override val valueArgumentList: CjValueArgumentList?
        get() = null
    override val valueArguments: List<ValueArgument>
        get() = emptyList()
    override val lambdaArguments: List<CjLambdaArgument>
        get() = emptyList()
    override val typeArguments: List<CjTypeProjection>
        get() = emptyList()
    override val typeArgumentList: CjTypeArgumentList?
        get() = null

}

