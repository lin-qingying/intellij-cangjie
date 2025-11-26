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

package org.cangnova.cangjie.debugger.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.ColoredTextContainer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator
import com.intellij.xdebugger.frame.*
import com.intellij.xdebugger.impl.XSourcePositionImpl
import kotlinx.coroutines.runBlocking
import org.cangnova.cangjie.debugger.core.StackFrameInfo
import org.cangnova.cangjie.debugger.core.ThreadInfo
import org.cangnova.cangjie.debugger.core.Variable

/**
 * 仓颉执行栈
 */
class CangJieExecutionStack(
    private val debugProcess: CangJieDebugProcess,
    private val thread: ThreadInfo,
    private val frames: List<StackFrameInfo>
) : XExecutionStack(thread.name) {

    override fun getTopFrame(): XStackFrame? {
        return frames.firstOrNull()?.let { frame ->
            CangJieStackFrame(debugProcess, thread.id, frame)
        }
    }

    override fun computeStackFrames(firstFrameIndex: Int, container: XStackFrameContainer) {
        val framesToShow = frames.drop(firstFrameIndex).map { frame ->
            CangJieStackFrame(debugProcess, thread.id, frame)
        }

        container.addStackFrames(framesToShow, true)
    }
}

/**
 * 仓颉栈帧
 */
class CangJieStackFrame(
    private val debugProcess: CangJieDebugProcess,
    private val threadId: Long,
    private val frameInfo: StackFrameInfo
) : XStackFrame() {

    companion object {
        private val LOG = Logger.getInstance(CangJieStackFrame::class.java)
    }

    @Volatile
    private var loadingVariables = false
    private val loadLock = Any()

    override fun getSourcePosition(): XSourcePosition? {
        val sourcePath = frameInfo.source?.path ?: return null
        val virtualFile = LocalFileSystem.getInstance().findFileByPath(sourcePath) ?: return null

        return XSourcePositionImpl.create(virtualFile, frameInfo.line - 1)
    }

    override fun customizePresentation(component: ColoredTextContainer) {
        val position = sourcePosition

        if (position != null) {
            component.append(position.file.name, SimpleTextAttributes.REGULAR_ATTRIBUTES)
            component.append(":${position.line + 1}", SimpleTextAttributes.REGULAR_ATTRIBUTES)
            component.setIcon(AllIcons.Debugger.Frame)
        } else {
            component.append(frameInfo.name, SimpleTextAttributes.GRAYED_ATTRIBUTES)
            component.setIcon(AllIcons.Debugger.Frame)
        }
    }

    override fun computeChildren(node: XCompositeNode) {
        synchronized(loadLock) {
            if (loadingVariables) {
                LOG.debug("Variables already loading for frame ${frameInfo.id}")
                return
            }
            loadingVariables = true
        }

        // 异步加载变量
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val scopes = runBlocking {
                    debugProcess.getVariableService()
                        .getScopes(frameInfo.id)
                        .getOrThrow()
                }

                val childrenList = XValueChildrenList()

                scopes.forEach { scope ->
                    val variables = runBlocking {
                        debugProcess.getVariableService()
                            .getVariables(scope.variablesReference)
                            .getOrThrow()
                    }

                    if (variables.isNotEmpty()) {
                        childrenList.addTopGroup(
                            CangJieScopeGroup(scope.name, variables, debugProcess)
                        )
                    }
                }

                node.addChildren(childrenList, true)
            } catch (e: Exception) {
                LOG.error("Failed to load variables", e)
                node.setErrorMessage("Failed to load variables: ${e.message}")
            } finally {
                loadingVariables = false
            }
        }
    }

    override fun getEvaluator(): XDebuggerEvaluator {
        return CangJieEvaluator(debugProcess, frameInfo.id, threadId)
    }
}

/**
 * 仓颉作用域组
 */
class CangJieScopeGroup(
    private val scopeName: String,
    private val variables: List<Variable>,
    private val debugProcess: CangJieDebugProcess
) : XValueGroup(scopeName) {

    override fun computeChildren(node: XCompositeNode) {
        val childrenList = XValueChildrenList()

        variables.forEach { variable ->
            childrenList.add(
                variable.name,
                CangJieVariable(debugProcess, variable)
            )
        }

        node.addChildren(childrenList, true)
    }
}

/**
 * 仓颉变量
 */
class CangJieVariable(
    private val debugProcess: CangJieDebugProcess,
    private val variable: Variable
) : XNamedValue(variable.name) {

    companion object {
        private val LOG = Logger.getInstance(CangJieVariable::class.java)
    }

    override fun computePresentation(node: XValueNode, place: XValuePlace) {
        node.setPresentation(
            AllIcons.Debugger.Value,
            variable.type,
            variable.value,
            variable.variablesReference > 0
        )
    }

    override fun computeChildren(node: XCompositeNode) {
        if (variable.variablesReference <= 0) {
            node.addChildren(XValueChildrenList.EMPTY, true)
            return
        }

        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val children = runBlocking {
                    debugProcess.getVariableService()
                        .getVariables(variable.variablesReference)
                        .getOrThrow()
                }

                val childrenList = XValueChildrenList()
                children.forEach { child ->
                    childrenList.add(child.name, CangJieVariable(debugProcess, child))
                }

                node.addChildren(childrenList, true)
            } catch (e: Exception) {
                LOG.error("Failed to load children", e)
                node.setErrorMessage("Failed to load children: ${e.message}")
            }
        }
    }
}