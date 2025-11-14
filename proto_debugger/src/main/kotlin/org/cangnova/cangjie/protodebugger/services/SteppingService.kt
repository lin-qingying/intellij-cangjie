package org.cangnova.cangjie.protodebugger.services

import org.cangnova.cangjie.protodebugger.breakpoint.StopPlace
import org.cangnova.cangjie.protodebugger.data.LLThread
import org.cangnova.cangjie.protodebugger.execution.ExecutionResult
import org.cangnova.cangjie.protodebugger.memory.Address

/**
 * 步进控制服务接口
 *
 * 负责程序执行控制：单步、继续、中断等
 */
interface SteppingService {
    /**
     * 继续执行程序
     *
     * @return 是否成功
     */
    suspend fun resume(): Boolean

    /**
     * 中断程序执行
     *
     * @return 是否成功
     */
    suspend fun interrupt(): Boolean

    /**
     * 单步进入
     *
     * @param thread 目标线程
     * @param forceStepIntoFramesWithNoDebugInfo 是否强制进入无调试信息的帧
     * @param stepByInstruction 是否按指令单步
     */
    suspend fun stepInto(
        thread: LLThread,
        forceStepIntoFramesWithNoDebugInfo: Boolean = false,
        stepByInstruction: Boolean = false
    )

    /**
     * 单步跨过
     *
     * @param thread 目标线程
     * @param stepByInstruction 是否按指令单步
     */
    suspend fun stepOver(
        thread: LLThread,
        stepByInstruction: Boolean = false
    )

    /**
     * 单步跳出
     *
     * @param thread 目标线程
     * @param stopInFramesWithNoDebugInfo 是否在无调试信息的帧中停止
     */
    suspend fun stepOut(
        thread: LLThread,
        stopInFramesWithNoDebugInfo: Boolean = false
    )

    /**
     * 运行到指定地址
     *
     * @param address 目标地址
     */
    suspend fun runToAddress(address: Address)

    /**
     * 运行到指定行
     *
     * @param path 源文件路径
     * @param line 行号
     */
    suspend fun runToLine(path: String, line: Int)

    /**
     * 跳转到指定地址
     *
     * @param thread 目标线程
     * @param address 目标地址
     * @param canLeaveFunction 是否允许离开当前函数
     * @return 停止位置
     */
    suspend fun jumpToAddress(
        thread: LLThread,
        address: Address,
        canLeaveFunction: Boolean
    ): StopPlace

    /**
     * 跳转到指定行
     *
     * @param thread 目标线程
     * @param path 源文件路径
     * @param line 行号
     * @param canLeaveFunction 是否允许离开当前函数
     * @return 停止位置
     */
    suspend fun jumpToLine(
        thread: LLThread,
        path: String,
        line: Int,
        canLeaveFunction: Boolean
    ): StopPlace

    /**
     * 冻结线程
     *
     * @param thread 要冻结的线程
     */
    suspend fun freezeThread(thread: LLThread)

    /**
     * 解冻线程
     *
     * @param thread 要解冻的线程
     */
    suspend fun unfreezeThread(thread: LLThread)

    /**
     * 冻结其他线程
     *
     * @param thread 不冻结的线程
     */
    suspend fun freezeOtherThreads(thread: LLThread)

    /**
     * 解冻所有线程
     *
     * @param thread 参考线程
     */
    suspend fun unfreezeAllThreads(thread: LLThread)

    /**
     * 是否支持跳转到行
     */
    fun supportsJumpToLine(): Boolean

    /**
     * 是否支持冻结其他线程
     */
    fun supportsFreezeOtherThreads(): Boolean

    /**
     * 是否支持冻结单个线程
     */
    fun supportsFreezeSingleThread(): Boolean
}
