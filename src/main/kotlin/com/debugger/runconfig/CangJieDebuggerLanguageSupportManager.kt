package com.debugger.runconfig

import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator

object CangJieDebuggerLanguageSupportManager {

    fun createEvaluator(frame: CangJieStackFrame): CangJieEvaluator {
        return CangJieEvaluator(frame)
    }

}


class CangJieEvaluator(val frame: CangJieStackFrame) : XDebuggerEvaluator() {
    override fun evaluate(expression: String, callback: XEvaluationCallback, expressionPosition: XSourcePosition?) {

    }
}
