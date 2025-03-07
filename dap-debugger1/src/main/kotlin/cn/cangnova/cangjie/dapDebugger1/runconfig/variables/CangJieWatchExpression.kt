package cn.cangnova.cangjie.dapDebugger1.runconfig.variables

import com.intellij.xdebugger.frame.XCompositeNode
import com.intellij.xdebugger.frame.XValue
import com.intellij.xdebugger.frame.XValueNode
import com.intellij.xdebugger.frame.XValuePlace
import com.intellij.xdebugger.frame.presentation.XValuePresentation
import com.intellij.xdebugger.frame.XValueChildrenList
import cn.cangnova.cangjie.dapDebugger1.runconfig.CangJieDebugProcess
import org.eclipse.lsp4j.debug.Variable
import org.eclipse.lsp4j.debug.VariablesArguments
import javax.swing.Icon

class CangJieWatchExpression(
    private val debugProcess: CangJieDebugProcess,
    private val expression: String,
    private val variable: Variable
) : XValue() {

    private var loadingChildren = false

    override fun computePresentation(node: XValueNode, place: XValuePlace) {
        node.setPresentation(
            null as Icon?,
            object : XValuePresentation() {
                override fun renderValue(renderer: XValueTextRenderer) {
                    renderer.renderValue(variable.value ?: "null")
                }

                override fun getType(): String? = variable.type

                override fun getSeparator(): String = " = "
            },
            variable.variablesReference > 0
        )
    }

    override fun computeChildren(node: XCompositeNode) {
        if (variable.variablesReference <= 0 || loadingChildren) return
        loadingChildren = true

        debugProcess.getConnection().getServer().variables(VariablesArguments().apply {
            variablesReference = variable.variablesReference
        }).thenAccept { response ->
            val children = XValueChildrenList()
            response.variables.forEach { childVar ->
                children.add(
                    childVar.name,
                    CangJieVariable(debugProcess, childVar.name, childVar)
                )
            }
            node.addChildren(children, true)
            loadingChildren = false
        }.exceptionally { throwable ->
            node.setErrorMessage("Failed to load children: ${throwable.message}")
            loadingChildren = false
            null
        }
    }
}
