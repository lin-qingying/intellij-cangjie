package com.huawei.cangjie.psi

import com.huawei.cangjie.CjNodeTypes.CASE_BLOCK
import com.huawei.cangjie.CjNodeTypes.INIT_BLOCK
import com.intellij.openapi.diagnostic.Logger

class CjCaseBlockExpression(text: CharSequence?) : CjBlockExpression(CASE_BLOCK, text) {
    companion object {
        val LOG = Logger.getInstance(CjCaseBlockExpression::class.java)
    }




}
