package com.linqingying.cangjie.dapDebugger1.runconfig.variables

import com.intellij.xdebugger.frame.*
import com.intellij.xdebugger.frame.presentation.XValuePresentation
import javax.swing.Icon

class CangJieScopeGroup(
    name: String,
    private val variables: List<CangJieVariable>
) : XNamedValue(name) {

    override fun computePresentation(node: XValueNode, place: XValuePlace) {
        node.setPresentation(
            null as Icon?,
            object : XValuePresentation() {
                override fun renderValue(renderer: XValueTextRenderer) {
                    renderer.renderValue("")
                }
                override fun getType(): String? = null
                override fun getSeparator(): String = ""
            },
            !variables.isEmpty()
        )
    }

    override fun computeChildren(node: XCompositeNode) {
        val children = XValueChildrenList()
        variables.forEach { variable -> 
            children.add(variable.name, variable)
        }
        node.addChildren(children, true)
    }
}
