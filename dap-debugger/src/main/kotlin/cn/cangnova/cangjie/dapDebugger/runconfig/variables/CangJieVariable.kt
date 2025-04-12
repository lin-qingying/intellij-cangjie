package cn.cangnova.cangjie.dapDebugger.runconfig.variables

import com.intellij.xdebugger.frame.*
import com.intellij.xdebugger.frame.presentation.XValuePresentation
import cn.cangnova.cangjie.dapDebugger1.runconfig.CangJieDebugProcess
import org.eclipse.lsp4j.debug.Variable
import org.eclipse.lsp4j.debug.VariablesArguments
import javax.swing.Icon


//abstract class AbstractCangJieVariable
//{
//    open fun computeChildren(node: XCompositeNode){
//
//    }
//
//}
class CangJieVariable(
    private val debugProcess: CangJieDebugProcess,
    private val name: String,
    private val variable: Variable
) : XNamedValue(name) {

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

        // 显示加载中消息
//        node.setErrorMessage("Loading...")

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
