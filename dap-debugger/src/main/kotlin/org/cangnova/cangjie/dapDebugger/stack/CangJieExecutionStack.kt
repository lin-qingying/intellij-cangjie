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

import com.intellij.xdebugger.frame.XExecutionStack
import com.intellij.xdebugger.frame.XStackFrame
import org.cangnova.cangjie.dapDebugger.CangJieDebugProcess
import org.eclipse.lsp4j.debug.StackFrame
import org.eclipse.lsp4j.debug.StackTraceArguments
import org.eclipse.lsp4j.debug.Thread

class CangJieExecutionStack(
    private val debugProcess: CangJieDebugProcess,
    private val thread: Thread,
    private var frames: List<StackFrame>? = null
) : XExecutionStack(thread.name ?: "Thread #${thread.id}") {

    private var myTopFrame: XStackFrame? = null
    private val stackFrames = mutableListOf<XStackFrame>()
    private var loading = false

    init {
        // 初始化顶部帧
        frames?.firstOrNull()?.let { frame ->
            myTopFrame = CangJieStackFrame.create(debugProcess, thread.id.toLong(), frame)
        }
    }

    override fun getTopFrame(): XStackFrame? {
        if (myTopFrame == null && frames == null   ) {
            // 如果没有帧数据，需要从调试器获取
            debugProcess.getConnection().getServer().stackTrace(StackTraceArguments().apply {
                threadId = thread.id
            }).thenAccept { response ->
                frames = response.stackFrames.toList()
                frames?.firstOrNull()?.let { frame ->
                    myTopFrame = CangJieStackFrame.create(debugProcess, thread.id.toLong(), frame)
                }
            }
        }
        return myTopFrame
    }

    override fun computeStackFrames(firstFrameIndex: Int, container: XStackFrameContainer) {
        if (loading) return
        loading = true

        if (frames == null || frames!!.isEmpty()) {
            // 如果还没有帧数据，需要从调试器获取
            debugProcess.getConnection().getServer().stackTrace(StackTraceArguments().apply {
                threadId = thread.id
            }).thenAccept { response ->
                frames = response.stackFrames.toList()
                computeAndAddFrames(firstFrameIndex, container)
                loading = false
            }.exceptionally { throwable ->
                container.errorOccurred("Failed to load frames: ${throwable.message}")
                loading = false
                null
            }
        } else {
            computeAndAddFrames(firstFrameIndex, container)
            loading = false
        }
    }

    private fun computeAndAddFrames(firstFrameIndex: Int, container: XStackFrameContainer) {
        if (stackFrames.isEmpty() && frames != null) {
            frames?.forEach { frame ->
                CangJieStackFrame.create(debugProcess, thread.id.toLong(), frame).let {
                    stackFrames.add(it)
                }
            }
        }

        if (firstFrameIndex < stackFrames.size) {
            container.addStackFrames(
                stackFrames.subList(firstFrameIndex, stackFrames.size),
                true
            )
        }
    }
}
