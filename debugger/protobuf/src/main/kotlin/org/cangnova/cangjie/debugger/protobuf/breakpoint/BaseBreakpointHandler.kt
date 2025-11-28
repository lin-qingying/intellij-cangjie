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

package org.cangnova.cangjie.debugger.protobuf.breakpoint

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction.nonBlocking
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.breakpoints.XBreakpoint
import com.intellij.xdebugger.breakpoints.XBreakpointHandler
import com.intellij.xdebugger.breakpoints.XBreakpointProperties
import com.intellij.xdebugger.breakpoints.XBreakpointType
import org.cangnova.cangjie.debugger.protobuf.core.ProtoDebugProcess
import java.util.concurrent.ConcurrentHashMap

/**
 * 断点处理器基类
 *
 * 提供断点处理的通用功能，包括活跃断点管理、断点ID映射、
 * 断点状态管理和通用的断点操作模板方法。
 *
 * @param B 断点类型
 * @param P 断点属性类型
 * @param breakpointTypeClass 断点类型Class对象
 * @param debugProcess 调试进程实例
 */
abstract class BaseBreakpointHandler<B : XBreakpoint<P>, T : XBreakpointType<B, P>, P : XBreakpointProperties<*>>(
    breakpointTypeClass: Class<T>,
    protected val debugProcess: ProtoDebugProcess
) : XBreakpointHandler<B>(breakpointTypeClass) {

    companion object {
        private val LOG = Logger.getInstance(BaseBreakpointHandler::class.java)
    }

    /** 活跃断点集合，按标识符分组 */
    protected val activeBreakpoints = ConcurrentHashMap<String, MutableSet<B>>()

    /** 断点ID映射，用于跟踪调试器返回的断点ID */
    protected val breakpointIds = ConcurrentHashMap<B, Any>()
    protected val session get() = debugProcess.session

    /**
     * 获取断点的标识符（用于分组管理）
     *
     * @param breakpoint 断点对象
     * @return 断点标识符
     */
    protected abstract fun getBreakpointIdentifier(breakpoint: B): String

    /**
     * 验证断点是否有效
     *
     * @param breakpoint 断点对象
     * @return 是否有效
     */
    protected abstract fun isValidBreakpoint(breakpoint: B): Boolean

    /**
     * 发送断点到调试器服务器
     *
     * @param breakpoint 断点对象
     * @return 调试器返回的断点标识符（ID或其他对象）
     */
    protected abstract fun sendBreakpointToDebugger(breakpoint: B): Any

    /**
     * 从调试器服务器移除断点
     *
     * @param debuggerIdentifier 调试器中的断点标识符
     */
    protected abstract fun removeBreakpointFromDebugger(debuggerIdentifier: Any)

    /**
     * 注册断点的通用实现
     */
    override fun registerBreakpoint(breakpoint: B) {
        val identifier = getBreakpointIdentifier(breakpoint)

        LOG.debug("Registering ${getBreakpointTypeName()}: $identifier, enabled: ${breakpoint.isEnabled}")

        // 添加到活跃断点集合
        activeBreakpoints.computeIfAbsent(identifier) { mutableSetOf() }.add(breakpoint)

        // 如果断点启用且有效，发送到调试器
        if (breakpoint.isEnabled && isValidBreakpoint(breakpoint)) {
            sendBreakpointToDebuggerSafe(breakpoint)
        }
    }

    /**
     * 移除断点的通用实现
     */
    override fun unregisterBreakpoint(breakpoint: B, temporary: Boolean) {
        val identifier = getBreakpointIdentifier(breakpoint)

        LOG.debug("Unregistering ${getBreakpointTypeName()}: $identifier (temporary: $temporary)")

        // 从活跃断点集合中移除
        activeBreakpoints[identifier]?.remove(breakpoint)

        // 从调试器中移除断点
        val debuggerIdentifier = breakpointIds.remove(breakpoint)
        if (debuggerIdentifier != null) {
            removeBreakpointFromDebuggerSafe(debuggerIdentifier)
        }
    }

    /**
     * 安全地发送断点到调试器
     */
    private fun sendBreakpointToDebuggerSafe(breakpoint: B) {
        try {
            val debuggerIdentifier = sendBreakpointToDebugger(breakpoint)
            breakpointIds[breakpoint] = debuggerIdentifier
            LOG.debug("${getBreakpointTypeName()} registered successfully")
        } catch (e: Exception) {
            LOG.error("Failed to register ${getBreakpointTypeName()}", e)
            // 不重新抛出异常，避免影响整个调试流程
        }
    }

    /**
     * 安全地从调试器移除断点
     */
    private fun removeBreakpointFromDebuggerSafe(debuggerIdentifier: Any) {
        try {
            removeBreakpointFromDebugger(debuggerIdentifier)
            LOG.debug("${getBreakpointTypeName()} removed successfully")
        } catch (e: Exception) {
            LOG.error("Failed to remove ${getBreakpointTypeName()}", e)
            // 不重新抛出异常，避免影响整个调试流程
        }
    }

    /**
     * 获取断点类型名称（用于日志）
     */
    protected abstract fun getBreakpointTypeName(): String

    /**
     * 获取断点ID
     */
    fun getBreakpointId(breakpoint: B): Any? {
        return breakpointIds[breakpoint]
    }

    /**
     * 获取所有活跃断点
     */
    fun getActiveBreakpoints(): Map<String, Set<B>> {
        return activeBreakpoints.toMap()
    }

    /**
     * 启用所有断点
     */
    fun enableAllBreakpoints() {
        activeBreakpoints.values.flatten().forEach { breakpoint ->
            if (!breakpoint.isEnabled && isValidBreakpoint(breakpoint)) {
                breakpoint.isEnabled = true
                sendBreakpointToDebuggerSafe(breakpoint)
            }
        }
    }

    /**
     * 禁用所有断点
     */
    fun disableAllBreakpoints() {
        val identifiersToDisable = mutableListOf<Any>()

        activeBreakpoints.values.flatten().forEach { breakpoint ->
            if (breakpoint.isEnabled) {
                breakpoint.isEnabled = false
                breakpointIds[breakpoint]?.let { id ->
                    identifiersToDisable.add(id)
                }
            }
        }

        if (identifiersToDisable.isNotEmpty()) {
            debugProcess.executeCommand {
                identifiersToDisable.forEach { identifier ->
                    removeBreakpointFromDebuggerSafe(identifier)
                }
                Unit
            }
        }
    }

    /**
     * 切换断点状态
     */
    fun toggleBreakpoint(breakpoint: B) {
        breakpoint.isEnabled = !breakpoint.isEnabled

        if (breakpoint.isEnabled && isValidBreakpoint(breakpoint)) {
            sendBreakpointToDebuggerSafe(breakpoint)
        } else {
            breakpointIds.remove(breakpoint)?.let { id ->
                removeBreakpointFromDebuggerSafe(id)
            }
        }
    }

    /**
     * 清理所有断点
     */
    fun clearAllBreakpoints() {
        LOG.debug("Clearing all ${getBreakpointTypeName()}s")

        // 移除所有调试器中的断点
        val allDebuggerIdentifiers = breakpointIds.values.toList()
        allDebuggerIdentifiers.forEach { identifier ->
            removeBreakpointFromDebuggerSafe(identifier)
        }

        // 清理本地状态
        activeBreakpoints.clear()
        breakpointIds.clear()
    }

    /**
     * 获取断点统计信息
     */
    fun getBreakpointStats(): BreakpointStats {
        val totalBreakpoints = activeBreakpoints.values.sumOf { it.size }
        val enabledBreakpoints = activeBreakpoints.values.flatten().count { it.isEnabled }
        val disabledBreakpoints = totalBreakpoints - enabledBreakpoints

        return BreakpointStats(
            total = totalBreakpoints,
            enabled = enabledBreakpoints,
            disabled = disabledBreakpoints,
            type = getBreakpointTypeName()
        )
    }

    /**
     * 清理调试器相关的断点
     *
     * 异步查找并移除满足条件的断点。该方法会：
     * 1. 在后台线程中查找需要移除的断点
     * 2. 在UI线程中移除断点（需要写操作）
     * 3. 提供可扩展的断点过滤逻辑
     */
    fun clear() {
        val manager = XDebuggerManager.getInstance(debugProcess.project).breakpointManager
        nonBlocking<List<B>> {
            manager.getBreakpoints(this.breakpointTypeClass).filter {
                shouldBreakpointBeRemoved(it)
            }


        }.finishOnUiThread(ModalityState.nonModal()) { breakpoints ->

            if (breakpoints.isNotEmpty()) {
                val application = ApplicationManager.getApplication()
                val manager = XDebuggerManager.getInstance(debugProcess.project).breakpointManager
                application.runWriteAction {
                    breakpoints.forEach { manager.removeBreakpoint(it) }
                }
            }
        }
            .submit(AppExecutorUtil.getAppExecutorService())


    }

    /**
     * 根据断点ID查找断点
     *
     * @param breakpointId 断点ID（调试器返回的标识符）
     * @return 找到的断点，如果没有找到则返回 null
     */
    fun findBreakpointById(breakpointId: Long): XBreakpoint<*>? {
        return breakpointIds.entries.find { (_, debuggerId) ->
            // 处理不同类型的调试器ID
            when (debuggerId) {
                is Long -> debuggerId == breakpointId
                is Int -> debuggerId.toLong() == breakpointId
                else -> false
            }
        }?.key
    }

    /**
     * 在指定位置查找断点
     *
     * @param filePosition 文件位置信息
     * @return 找到的断点，如果没有找到则返回 null
     */
    fun findBreakpointAt(filePosition: XSourcePosition?): XBreakpoint<*>? {
        if (filePosition == null) return null

        val file = filePosition.file
        val line = filePosition.line

        return activeBreakpoints.values.flatten().firstNotNullOfOrNull { breakpoint ->
            when (breakpoint) {
                is com.intellij.xdebugger.breakpoints.XLineBreakpoint<*> -> {
                    val breakpointSourcePosition = breakpoint.sourcePosition
                    if (breakpointSourcePosition != null &&
                        breakpointSourcePosition.file == file &&
                        breakpointSourcePosition.line == line
                    ) {
                        breakpoint
                    } else {
                        null
                    }
                }

                else -> null
            }
        }
    }

    /**
     * 判断断点是否应该被移除
     *
     * 子类需要重写此方法来实现具体的清理逻辑。
     * 例如：移除无效的断点、过期的断点、或特定条件下的断点。
     *
     * @param breakpoint 要检查的断点
     * @return true 如果断点应该被移除，false 否则
     */
    protected abstract fun shouldBreakpointBeRemoved(breakpoint: B): Boolean

    /**
     * 更新断点验证状态
     *
     * 子类需要实现此方法来根据调试器后端的事件更新断点的验证状态。
     * 不同类型的断点可能有不同的更新机制：
     * - 行断点：使用 session.setBreakpointVerified() / setBreakpointInvalid()
     * - 其他类型断点：可能有自己的状态管理机制
     *
     * @param breakpointId 后端返回的断点ID
     * @param verified 是否已验证
     */
    open fun updateVerificationStatus(breakpointId: Long, verified: Boolean) {
        // 查找对应的断点
        val breakpoint = findBreakpointById(breakpointId) as? B ?: run {
            LOG.warn("Cannot update verification status: breakpoint not found for ID $breakpointId")
            return
        }

        // 子类可以重写此方法来实现特定的更新逻辑
        LOG.debug("Breakpoint verification status updated: ID=$breakpointId, verified=$verified")
    }

    /**
     * 断点统计信息
     */
    data class BreakpointStats(
        val total: Int,
        val enabled: Int,
        val disabled: Int,
        val type: String
    )
}