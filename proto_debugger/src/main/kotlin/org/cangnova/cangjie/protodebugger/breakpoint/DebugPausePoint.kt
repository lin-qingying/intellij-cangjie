package org.cangnova.cangjie.protodebugger.breakpoint

import org.cangnova.cangjie.protodebugger.core.CangJieSuspendContext
import org.cangnova.cangjie.protodebugger.core.DebuggerDriverFacade
import org.cangnova.cangjie.protodebugger.data.LLDBFrame
import org.cangnova.cangjie.protodebugger.data.LLDBThread
import org.cangnova.cangjie.protodebugger.data.LLDBVariable

/**
 * 调试器暂停点（DebugPausePoint）
 *
 * 表示调试器执行过程中暂停的具体位置，包括：
 * - 当前运行的线程
 * - 栈顶帧（当前函数上下文）
 * - 函数返回值（仅在函数返回事件中存在）
 */
data class   StopPlace(
    /** 当前暂停的线程 */
    val thread: LLDBThread,

    /** 当前暂停的栈帧 */
    val frame: LLDBFrame,

    /**
     * 函数返回值（可选）
     * 仅当暂停事件为“函数返回”（StepOut/Return）时才会有值。
     */
    val returnValue: LLDBVariable? = null
) {

    /** 创建一个没有返回值的暂停点 */
    constructor(thread: LLDBThread, frame: LLDBFrame) : this(thread, frame, null)

    /**
     * 将暂停点转换为字符串，通常用于日志输出
     */
    override fun toString(): String = "$thread @ $frame"
}

/**
 * 将 DebugPausePoint 转换为 CangJieSuspendContext
 *
 * 用于向 IDE 提供标准的暂停上下文对象，使调试 UI 能正确显示线程、栈帧等信息。
 */
fun DebugPausePoint.toSuspendContext(facade: DebuggerDriverFacade): CangJieSuspendContext {
    return CangJieSuspendContext(
        activeThread = thread,
        topFrame = frame,
        facade = facade
    )
}