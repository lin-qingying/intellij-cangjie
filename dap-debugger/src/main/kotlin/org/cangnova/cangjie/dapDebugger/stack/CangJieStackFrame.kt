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

package org.cangnova.cangjie.dapDebugger.stack

import com.intellij.icons.AllIcons
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.ColoredTextContainer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator
import com.intellij.xdebugger.frame.XCompositeNode
import com.intellij.xdebugger.frame.XStackFrame
import com.intellij.xdebugger.frame.XValueChildrenList
import com.intellij.xdebugger.impl.XSourcePositionImpl
import org.cangnova.cangjie.dapDebugger.CangJieDebugProcess
import org.cangnova.cangjie.dapDebugger.evaluator.CangJieEvaluator
import org.cangnova.cangjie.dapDebugger.variables.CangJieScopeGroup
import org.cangnova.cangjie.dapDebugger.variables.CangJieVariable
import org.eclipse.lsp4j.debug.ScopesArguments
import org.eclipse.lsp4j.debug.StackFrame
import org.eclipse.lsp4j.debug.VariablesArguments
import java.util.concurrent.CompletableFuture

class CangJieStackFrame(
    val debugProcess: CangJieDebugProcess,
    val threadId: Long,
    val stackFrame: StackFrame
) : XStackFrame() {

    private var loadingVariables = false

    override fun getSourcePosition(): XSourcePosition? {
        val sourcePath = stackFrame.source?.path
        val sourcePosition = if (sourcePath != null) {
            val virtualFile = LocalFileSystem.getInstance().findFileByPath(sourcePath)
            if (virtualFile != null) {
                XSourcePositionImpl.create(virtualFile, stackFrame.line - 1)
            } else null
        } else null

        return sourcePosition
    }

    override fun customizePresentation(component: ColoredTextContainer) {
        if (sourcePosition != null) {
            component.append(sourcePosition!!.file.name, SimpleTextAttributes.REGULAR_ATTRIBUTES)
            component.append(":" + (sourcePosition!!.line + 1), SimpleTextAttributes.REGULAR_ATTRIBUTES)
            component.setIcon(AllIcons.Debugger.Frame)
        } else {
            val frameName = stackFrame.name ?: "Frame ${stackFrame.id}"
            component.append(frameName, SimpleTextAttributes.GRAYED_ATTRIBUTES)
            component.setIcon(AllIcons.Debugger.Frame)
        }
    }

    override fun computeChildren(node: XCompositeNode) {
        if (loadingVariables) return
        loadingVariables = true

        // 显示加载中消息
//        node.setErrorMessage("Loading variables...")

        // 获取作用域
        debugProcess.getConnection().getServer().scopes(ScopesArguments().apply {
            frameId = stackFrame.id
        }).thenAccept { scopesResponse ->
            // 获取每个作用域的变量
            val futures = scopesResponse.scopes.map { scope ->
                debugProcess.getConnection().getServer().variables(VariablesArguments().apply {
                    variablesReference = scope.variablesReference
                }).thenApply { variablesResponse ->
                    scope to variablesResponse.variables
                }
            }

            // 等待所有变量加载完成
            CompletableFuture.allOf(*futures.toTypedArray()).thenAccept {
                val childrenList = XValueChildrenList()

                futures.forEach { future ->
                    val (scope, variables) = future.get()
                    if (variables.isNotEmpty()) {
                        // 直接添加作用域分组到 childrenList
                        childrenList.add(
                            scope.name,
                            CangJieScopeGroup(
                                scope.name,
                                variables.map { v -> CangJieVariable(debugProcess, v.name, v) }
                            )
                        )
                    }
                }

                node.addChildren(childrenList, true)
                loadingVariables = false
            }
        }.exceptionally { throwable ->
            node.setErrorMessage("Error loading variables: ${throwable.message}")
            loadingVariables = false
            null
        }
    }

    override fun getEqualityObject(): Any = stackFrame.id

    override fun getEvaluator(): XDebuggerEvaluator {
        return CangJieEvaluator(this)
    }

    companion object {
        fun create(debugProcess: CangJieDebugProcess, threadId: Long, stackFrame: StackFrame): CangJieStackFrame {
            return CangJieStackFrame(debugProcess, threadId, stackFrame)
        }
    }
}
