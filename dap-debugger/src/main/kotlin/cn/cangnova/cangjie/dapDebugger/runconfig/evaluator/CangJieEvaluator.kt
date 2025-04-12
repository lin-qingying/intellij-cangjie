package cn.cangnova.cangjie.dapDebugger.runconfig.evaluator

import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator
import cn.cangnova.cangjie.dapDebugger1.runconfig.stack.CangJieStackFrame
import cn.cangnova.cangjie.dapDebugger1.runconfig.variables.CangJieVariable
import org.eclipse.lsp4j.debug.EvaluateArguments
import org.eclipse.lsp4j.debug.EvaluateArgumentsContext
import org.eclipse.lsp4j.debug.Variable

class CangJieEvaluator(private val stackFrame: CangJieStackFrame) : XDebuggerEvaluator() {

    override fun evaluate(expression: String, callback: XEvaluationCallback, expressionPosition: XSourcePosition?) {
        try {
            stackFrame.debugProcess.getConnection().getServer().evaluate(EvaluateArguments().apply {
                this.expression = expression
                this.frameId = stackFrame.stackFrame.id
                this.context = EvaluateArgumentsContext.WATCH
            }).thenAccept { response ->
                val variable = Variable().apply {
                    name = expression
                    value = response.result
                    type = response.type
                    variablesReference = response.variablesReference
                }
                callback.evaluated(CangJieVariable(stackFrame.debugProcess, expression, variable))
            }.exceptionally { throwable ->
                callback.errorOccurred(throwable.message ?: "Evaluation failed")
                null
            }
        } catch (e: Exception) {
            callback.errorOccurred(e.message ?: "Evaluation failed")
        }
    }
}
