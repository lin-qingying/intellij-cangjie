package org.cangnova.cangjie.protodebugger.services

import org.cangnova.cangjie.protodebugger.breakpoint.AddBreakpointResult
import org.cangnova.cangjie.protodebugger.breakpoint.SymbolicBreakpoint
import org.cangnova.cangjie.protodebugger.data.LLSymbolicBreakpoint
import org.cangnova.cangjie.protodebugger.data.LLValue
import org.cangnova.cangjie.protodebugger.data.LLWatchpoint
import org.cangnova.cangjie.protodebugger.memory.Address

/**
 * 断点服务接口
 *
 * 负责管理所有类型的断点：行断点、地址断点、符号断点和观察点
 */
interface BreakpointService {
    /**
     * 添加行断点
     *
     * @param path 源文件路径
     * @param line 行号
     * @param condition 条件表达式（可选）
     * @return 断点添加结果
     */
    suspend fun addLineBreakpoint(
        path: String,
        line: Int,
        condition: String? = null
    ): AddBreakpointResult

    /**
     * 添加地址断点
     *
     * @param address 内存地址
     * @param condition 条件表达式（可选）
     * @return 断点添加结果
     */
    suspend fun addAddressBreakpoint(
        address: Address,
        condition: String? = null
    ): AddBreakpointResult

    /**
     * 添加符号断点
     *
     * @param symbolPattern 符号模式（函数名等）
     * @param module 模块名（可选）
     * @param condition 条件表达式（可选）
     * @return 符号断点对象，失败返回null
     */
    suspend fun addSymbolicBreakpoint(
        symbolPattern: String,
        module: String? = null,
        condition: String? = null
    ): LLSymbolicBreakpoint?

    /**
     * 添加符号断点（使用SymbolicBreakpoint对象）
     *
     * @param breakpoint 符号断点配置
     * @return 符号断点对象，失败返回null
     */
    suspend fun addSymbolicBreakpoint(breakpoint: SymbolicBreakpoint): LLSymbolicBreakpoint?

    /**
     * 添加观察点（内存监视点）
     *
     * @param address 监视的内存地址
     * @param size 监视的字节数
     * @param read 是否监视读操作
     * @param write 是否监视写操作
     * @param condition 条件表达式（可选）
     * @return 观察点对象
     */
    suspend fun addWatchpoint(
        threadId: Long,
        frameIndex: Int,
        value: LLValue,
        expr: String,
        lifetime: LLWatchpoint.Lifetime?,
        accessType: LLWatchpoint.AccessType
    ): LLWatchpoint
    /**
     * 移除断点
     *
     * @param ids 断点ID集合
     */
    suspend fun removeBreakpoints(ids: Collection<Int>)

    /**
     * 移除观察点
     *
     * @param ids 观察点ID集合
     */
    suspend fun removeWatchpoints(ids: List<Int>)

    /**
     * 是否支持观察点
     */
    fun supportsWatchpoints(): Boolean

    /**
     * 是否支持观察点生命周期管理
     */
    fun supportsWatchpointLifetime(): Boolean
}
