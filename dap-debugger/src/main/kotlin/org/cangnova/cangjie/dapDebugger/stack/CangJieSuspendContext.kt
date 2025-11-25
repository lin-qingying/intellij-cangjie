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
import com.intellij.xdebugger.frame.XSuspendContext
import org.cangnova.cangjie.dapDebugger.CangJieDebugProcess
import org.eclipse.lsp4j.debug.StackFrame
import org.eclipse.lsp4j.debug.Thread

class CangJieSuspendContext(
    private val debugProcess: CangJieDebugProcess,
    val activeThreadId: Long,
    threads: List<Thread>,
    frames: List<StackFrame>
) : XSuspendContext() {

    private val executionStacks: MutableMap<Long, CangJieExecutionStack> = mutableMapOf()

    init {
        // 为活动线程创建执行栈
        val activeThread = threads.find { it.id.toLong() == activeThreadId }
        if (activeThread != null) {
            executionStacks[activeThreadId] = CangJieExecutionStack(debugProcess, activeThread, frames)
        }

        // 为其他线程创建执行栈（暂时用空的帧列表）
        threads.filter { it.id.toLong() != activeThreadId }.forEach { thread ->
            executionStacks[thread.id.toLong()] = CangJieExecutionStack(debugProcess, thread, emptyList())
        }
    }

    override fun getActiveExecutionStack(): XExecutionStack? {
        return executionStacks[activeThreadId]
    }

    override fun getExecutionStacks(): Array<XExecutionStack> {
        return executionStacks.values.toTypedArray()
    }
}
