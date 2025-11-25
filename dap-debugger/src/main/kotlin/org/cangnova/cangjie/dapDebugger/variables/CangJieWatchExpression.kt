/*
 * Copyright 2025 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.dapDebugger.variables

import com.intellij.xdebugger.frame.*
import com.intellij.xdebugger.frame.presentation.XValuePresentation
import org.cangnova.cangjie.dapDebugger.CangJieDebugProcess
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
