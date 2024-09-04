package com.huawei.cangjie.diagnostics.rendering

import com.huawei.cangjie.descriptors.Diagnostic
import com.linqingying.utils.Config


class SimpleDiagnosticRendererByFunction(private val message: () -> String) :
    DiagnosticRenderer<Diagnostic> {
    override fun render(diagnostic: Diagnostic): String {
        return message()
    }

    override fun renderParameters(diagnostic: Diagnostic): Array<Any?> {
        return arrayOfNulls(0)
    }
}

class SimpleDiagnosticRendererByAstMsgData(private val message: AstMsgData) :
    DiagnosticRenderer<Diagnostic> {
    override fun render(diagnostic: Diagnostic): String {
        return message.getMessage(Config.astMsgType)
    }

    override fun renderParameters(diagnostic: Diagnostic): Array<Any?> {
        return arrayOfNulls(0)
    }
}
