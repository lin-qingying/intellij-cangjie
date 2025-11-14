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

package org.cangnova.cangjie.protodebugger.core

import com.intellij.xdebugger.frame.XSuspendContext
import com.intellij.xdebugger.frame.XExecutionStack
import kotlinx.coroutines.launch
import org.cangnova.cangjie.protodebugger.data.LLThread

/**
 * 仓颉调试器暂停上下文
 *
 * 当调试器暂停时（如命中断点、单步执行完成等），提供当前暂停状态的上下文信息，
 * 包括执行线程、调用栈等信息。
 *
 * @param thread 当前暂停的执行线程
 * @param debuggerDriver 调试器驱动门面，用于获取调试信息
 */
class CangJieSuspendContext(
    val thread: LLThread,
    private val debuggerDriver: DebuggerDriverFacade
) : XSuspendContext() {

    /**
     * 获取执行栈
     *
     * @return 执行栈数组，通常包含一个主执行栈
     */
    override fun getExecutionStacks(): Array<XExecutionStack> {
        return arrayOf(CangJieExecutionStack(thread, debuggerDriver))
    }

    /**
     * 获取活动执行栈
     *
     * @return 当前的活动执行栈
     */
    override fun getActiveExecutionStack(): XExecutionStack {
        return getExecutionStacks()[0]
    }
}

/**
 * 仓颉调试器执行栈
 *
 * 表示一个线程的执行栈，包含该线程的所有栈帧信息。
 *
 * @param thread 关联的执行线程
 * @param debuggerDriver 调试器驱动门面
 */
class CangJieExecutionStack(
    private val thread: LLThread,
    private val debuggerDriver: DebuggerDriverFacade
) : XExecutionStack("Thread ${thread.id}") {

    /**
     * 获取栈帧列表
     *
     * @param callback 栈帧获取完成的回调函数
     */
    override fun computeStackFrames(firstFrameIndex: Int, container: XStackFrameContainer) {
        // 异步获取栈帧信息
        try {

        } catch (e: Exception) {
            container.errorOccurred("Failed to launch stack frame retrieval: ${e.message}")
        }
    }

    /**
     * 获取顶部栈帧
     *
     * @return 顶部栈帧，如果没有则返回null
     */
    override fun getTopFrame(): CangJieStackFrame? {
        return CangJieStackFrame(0, "main", thread, debuggerDriver)
    }
}