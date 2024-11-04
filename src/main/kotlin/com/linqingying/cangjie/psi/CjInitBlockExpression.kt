package com.linqingying.cangjie.psi

import com.linqingying.cangjie.CjNodeTypes.INIT_BLOCK
import com.intellij.openapi.diagnostic.Logger

class CjInitBlockExpression(text: CharSequence?) : CjBlockExpression(INIT_BLOCK, text) {
    companion object {
        val LOG = Logger.getInstance(CjInitBlockExpression::class.java)
    }


    fun replaceImplicitDelegationCallWithExplicit(isThis: Boolean): CjConstructorDelegationCall {
        val psiFactory = CjPsiFactory(project)
        val current = getDelegationCall()

        assert(current.isImplicit) { "Method should not be called with explicit delegation call: " + text }
        current.delete()
//        换行
        val whiteSpace = addAfter(psiFactory.createNewLine(), lBrace)

        val delegationName = if (isThis) "this" else "super"

        return addAfter(
            psiFactory.creareDelegatedSuperTypeEntry("$delegationName()"),
            whiteSpace.nextSibling
        ) as CjConstructorDelegationCall
    }


    fun getDelegationCall(): CjConstructorDelegationCall =
        getDelegationCallOrNull()!!

    fun getDelegationCallOrNull(): CjConstructorDelegationCall? =
        findChildByClass(CjConstructorDelegationCall::class.java)

}
