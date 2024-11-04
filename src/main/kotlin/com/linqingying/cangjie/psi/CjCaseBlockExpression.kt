package com.linqingying.cangjie.psi

import com.linqingying.cangjie.CjNodeTypes.CASE_BLOCK
import com.linqingying.cangjie.CjNodeTypes.INIT_BLOCK
import com.intellij.openapi.diagnostic.Logger

class CjCaseBlockExpression(text: CharSequence?) : CjBlockExpression(CASE_BLOCK, text) {
    companion object {
        val LOG = Logger.getInstance(CjCaseBlockExpression::class.java)
    }




}
