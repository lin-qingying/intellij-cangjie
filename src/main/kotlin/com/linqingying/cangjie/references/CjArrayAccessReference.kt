package com.linqingying.cangjie.references

import com.linqingying.cangjie.descriptors.DeclarationDescriptor
import com.linqingying.cangjie.lexer.CjToken
import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.psi.CjArrayAccessExpression
import com.linqingying.cangjie.resolve.BindingContext
import com.linqingying.cangjie.utils.OperatorNameConventions
import com.intellij.psi.MultiRangeReference

class CjArrayAccessReference(
    expression: CjArrayAccessExpression

) : CjSimpleReference<CjArrayAccessExpression>(expression), MultiRangeReference {
    override val resolvesByNames: Collection<Name>
        get() = NAMES

    override fun getRangeInElement() = element.textRange.shiftRight(-element.textOffset)

    private fun getBracketRange(bracketToken: CjToken) =
        expression.indicesNode.node.findChildByType(bracketToken)?.textRange?.shiftRight(-expression.textOffset)


    override fun getTargetDescriptors(context: BindingContext): Collection<DeclarationDescriptor> {
        val getFunctionDescriptor = context[BindingContext.INDEXED_LVALUE_GET, expression]?.candidateDescriptor
        val setFunctionDescriptor = context[BindingContext.INDEXED_LVALUE_SET, expression]?.candidateDescriptor
        return listOfNotNull(getFunctionDescriptor, setFunctionDescriptor)
    }

    override fun getRanges() = listOfNotNull(getBracketRange(CjTokens.LBRACKET), getBracketRange(CjTokens.RBRACKET))

    override fun canRename() = true

    companion object {
        private val NAMES = listOf(OperatorNameConventions.GET, OperatorNameConventions.SET)
    }
}
