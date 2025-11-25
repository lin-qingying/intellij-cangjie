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
 */

package org.cangnova.cangjie.protodebugger.core

import com.intellij.xdebugger.frame.XSuspendContext
import com.intellij.xdebugger.frame.XExecutionStack
import org.cangnova.cangjie.protodebugger.data.LLDBThread
import org.cangnova.cangjie.protodebugger.data.LLDBFrame
import java.util.concurrent.ConcurrentHashMap

/**
 * 仓颉调试器暂停上下文
 *
 * 当调试器暂停时（如命中断点、单步执行完成等），提供当前暂停状态的上下文信息，
 * 包括执行线程、调用栈等信息。
 *
 * @param activeThread 当前暂停的执行线程（事件线程）
 * @param topFrame 当前停止位置的顶部栈帧
 * @param returnValue 函数的返回值（如果有）
 * @param facade 调试器驱动门面，用于获取调试信息
 */
class CangJieSuspendContext(
    private val activeThread: LLDBThread,
    private val topFrame: LLDBFrame? = null,

    private val facade: DebuggerDriverFacade
) : XSuspendContext() {

    // 执行栈缓存，避免重复创建
    private val executionStackCache: MutableMap<Long, CangJieExecutionStack> by lazy {
        ConcurrentHashMap()
    }

    // 活动执行栈的懒加载实现
    private val activeExecutionStack: CangJieExecutionStack by lazy {
        executionStackCache.getOrPut(activeThread.id) {
            activeThread.      toExecutionStack(
                topFrame = topFrame,

                facade = facade)

        }
    }

    /**
     * 计算所有线程的执行栈
     *
     * @param container 执行栈容器，用于添加计算出的执行栈
     */
    override fun computeExecutionStacks(container: XExecutionStackContainer) {
        facade.executeCommand {
            try {
                val allThreads = facade.sessionService.getThreads()

                // 将所有线程转换为执行栈
                val executionStacks = allThreads.map { thread ->
                    executionStackCache.getOrPut(thread.id) {
                        // 如果是活动线程，使用已有的执行栈（包含正确的 topFrame）
                        if (thread.id == activeThread.id) {
                            activeExecutionStack
                        } else {
                            // 非活动线程创建普通执行栈
                            CangJieExecutionStack(
                                thread = thread,
                                topFrame = null,

                                facade = facade
                            )
                        }
                    }
                }

                // 添加所有执行栈到容器，第一个是活动执行栈
                container.addExecutionStack(executionStacks, true)
            } catch (e: Exception) {
                container.errorOccurred("Failed to compute execution stacks: ${e.message}")
            }
        }
    }

    /**
     * 获取当前活动的执行栈
     *
     * @return 当前活动线程的执行栈
     */
    override fun getActiveExecutionStack(): XExecutionStack = activeExecutionStack
}

/**
 * 仓颉调试器执行栈
 *
 * 表示一个线程的执行栈，包含该线程的所有栈帧信息。
 *
 * @param thread 关联的执行线程
 * @param topFrame 已知的顶部栈帧（通常来自 DebugPausePoint）
 * @param returnValue 函数返回值（如果在函数返回点）
 * @param debuggerDriver 调试器驱动门面
 */
class CangJieExecutionStack(
    private val thread: LLDBThread,
    private val topFrame: LLDBFrame?,

    private val facade: DebuggerDriverFacade
) : XExecutionStack("Thread ${thread.id}${if (thread.name?.isNotEmpty() == true) " (${thread.name})" else ""}") {

    // 缓存顶部栈帧，避免重复创建
    private val cachedTopFrame: CangJieStackFrame? by lazy {
        topFrame?.let { frame ->
            frame.     toCangJieStackFrame       (

                thread = thread,
                facade = facade
            )

        }
    }

    /**
     * 获取栈帧列表
     *
     * @param firstFrameIndex 起始栈帧索引
     * @param container 栈帧容器，用于添加计算出的栈帧
     */
    override fun computeStackFrames(firstFrameIndex: Int, container: XStackFrameContainer) {
        facade.executeCommand {
            try {
                // 获取该线程的所有栈帧
                // 使用 getFrames 方法获取指定范围的栈帧
                // maxFrames 设置为较大值以获取所有剩余栈帧，实际可根据需要调整
                val frames = facade.sessionService.getFrames(
                    thread = thread,
                    startFrame = firstFrameIndex,
                    maxFrames = 100  // 可以根据实际需求调整，或设为 Int.MAX_VALUE
                )


                // 如果请求的是第一帧且我们有缓存的顶部栈帧，使用它
                val stackFrames = frames.drop(firstFrameIndex).mapIndexed { index, frame ->
                    val actualIndex = firstFrameIndex + index

                    // 如果是第一帧且有缓存，使用缓存的顶部栈帧
                    if (actualIndex == 0 && cachedTopFrame != null) {
                        cachedTopFrame!!
                    } else {
                        frame.     toCangJieStackFrame       (

                            thread = thread,
                            facade = facade
                        )
                    }
                }

                // 添加栈帧到容器
                container.addStackFrames(stackFrames, true)
            } catch (e: Exception) {
                container.errorOccurred("Failed to compute stack frames: ${e.message}")
            }
        }
    }

    /**
     * 获取顶部栈帧
     *
     * @return 顶部栈帧，如果没有则返回null
     */
    override fun getTopFrame(): CangJieStackFrame? = cachedTopFrame
}

/**
 * 将 LLDBThread 转换为 CangJieExecutionStack
 *
 * @param facade 调试器驱动门面
 * @return 创建的执行栈对象
 */
fun LLDBThread.toExecutionStack(
    facade: DebuggerDriverFacade,
    topFrame: LLDBFrame? = null
): CangJieExecutionStack = CangJieExecutionStack(
    thread = this,
    topFrame = topFrame,

    facade = facade
)



