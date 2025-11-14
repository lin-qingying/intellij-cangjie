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

package org.cangnova.cangjie.protodebugger.breakpoint

import com.intellij.openapi.diagnostic.Logger
import com.intellij.xdebugger.breakpoints.XBreakpointHandler
import com.intellij.xdebugger.breakpoints.XBreakpoint
import org.cangnova.cangjie.protodebugger.core.CangJieDebugProcess
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/**
 * 仓颉语言观察点处理器
 *
 * 负责处理IntelliJ调试框架中的观察点事件，将IDE中的观察点操作转换为对proto调试器服务的调用。
 * 实现观察点的注册、移除、启用、禁用等功能。
 *
 * @param debugProcess 调试进程实例，用于访问调试器服务
 */
class WatchpointBreakpointHandler(
    private val debugProcess: CangJieDebugProcess
) : XBreakpointHandler<WatchpointBreakpoint>(
    WatchpointBreakpointType::class.java
) {

    companion object {
        private val LOG = Logger.getInstance(WatchpointBreakpointHandler::class.java)
    }

    /** 活跃的观察点集合 */
    private val activeBreakpoints = ConcurrentHashMap<String, MutableSet<XBreakpoint<WatchpointBreakpointType.Properties>>>()

    /** 观察点ID映射，用于跟踪调试器返回的观察点ID */
    private val breakpointIds = ConcurrentHashMap<XBreakpoint<*>, Int>()

    /**
     * 注册观察点
     *
     * 当用户在IDE中设置观察点时调用此方法。
     * 将观察点信息发送到调试器服务器。
     */
    override fun registerBreakpoint(breakpoint: WatchpointBreakpoint) {
        val properties = breakpoint.properties
        val expression = properties.getExpression()

        LOG.debug("Registering watchpoint: $expression, enabled: ${breakpoint.isEnabled}")

        // 添加到活跃观察点集合
        activeBreakpoints.computeIfAbsent(expression) { mutableSetOf() }.add(breakpoint)

        // 如果观察点启用，发送到调试器
        if (breakpoint.isEnabled && properties.isValidExpression()) {
            sendBreakpointToDebugger(breakpoint)
        }
    }

    /**
     * 移除观察点
     *
     * 当用户在IDE中移除观察点时调用此方法。
     * 从调试器服务器中移除对应的观察点。
     */
    override fun unregisterBreakpoint(breakpoint: WatchpointBreakpoint, temporary: Boolean) {
        val properties = breakpoint.properties ?: return
        val expression = properties.getExpression()

        LOG.debug("Unregistering watchpoint: $expression (temporary: $temporary)")

        // 从活跃观察点集合中移除
        activeBreakpoints[expression]?.remove(breakpoint)

        // 从调试器中移除观察点
        val breakpointId = breakpointIds.remove(breakpoint)
        if (breakpointId != null) {
            removeBreakpointFromDebugger(breakpointId)
        }
    }

    /**
     * 发送观察到调试器服务器
     */
    private fun sendBreakpointToDebugger(breakpoint: XBreakpoint<WatchpointBreakpointType.Properties>)  {
        val properties = breakpoint.properties ?: return

        if (!properties.isValidExpression()) {
            LOG.error("Invalid expression for watchpoint: ${properties.getExpression()}")
            return
        }

        val expression = properties.getExpression()
        val accessType = properties.toLLAccessType()
        val threadId = properties.getThreadId()

        LOG.debug("Sending watchpoint to debugger: $expression, access: $accessType")

          debugProcess.executeCommand {
            try {
                // 观察点需要在特定线程和帧中设置，这里使用默认值
                // 注意：实际使用时需要在运行时获取正确的LLValue对象
                // 这里暂时跳过观察点创建，因为需要有效的LLValue
                LOG.warn("Cannot create watchpoint without valid LLValue for expression: $expression")

                // 目前跳过观察点创建，因为需要有效的LLValue对象
                LOG.debug("Watchpoint creation skipped - requires valid LLValue")
            } catch (e: Exception) {
                LOG.error("Failed to register watchpoint: $expression", e)
                throw e
            }
        }
    }

    /**
     * 从调试器服务器移除观察点
     */
    private fun removeBreakpointFromDebugger(breakpointId: Int)  {
        LOG.debug("Removing watchpoint from debugger: ID $breakpointId")

          debugProcess.executeCommand {
            try {
                debugProcess.debuggerDriver.breakpointService.removeWatchpoints(listOf(breakpointId))
                LOG.debug("Watchpoint removed successfully: ID $breakpointId")

            } catch (e: Exception) {
                LOG.error("Failed to remove watchpoint: ID $breakpointId", e)
                throw e
            }
        }
    }

    /**
     * 获取观察点ID
     */
    fun getBreakpointId(breakpoint: XBreakpoint<*>): Int? {
        return breakpointIds[breakpoint]
    }

    /**
     * 获取所有活跃观察点
     */
    fun getActiveBreakpoints(): Map<String, Set<XBreakpoint<WatchpointBreakpointType.Properties>>> {
        return activeBreakpoints.toMap()
    }

    /**
     * 启用所有观察点
     */
    fun enableAllBreakpoints() {
        activeBreakpoints.values.flatten().forEach { breakpoint ->
            if (!breakpoint.isEnabled && breakpoint.properties?.isValidExpression() == true) {
                breakpoint.isEnabled = true
                sendBreakpointToDebugger(breakpoint)
            }
        }
    }

    /**
     * 禁用所有观察点
     */
    fun disableAllBreakpoints() {
        val idsToDisable = mutableListOf<Int>()

        activeBreakpoints.values.flatten().forEach { breakpoint ->
            if (breakpoint.isEnabled) {
                breakpoint.isEnabled = false
                breakpointIds[breakpoint]?.let { id ->
                    idsToDisable.add(id)
                }
            }
        }

        if (idsToDisable.isNotEmpty()) {
            debugProcess.executeCommand {
                debugProcess.debuggerDriver.breakpointService.removeWatchpoints(idsToDisable)
                Unit
            }
        }
    }

    /**
     * 切换观察点状态
     */
    fun toggleBreakpoint(breakpoint: XBreakpoint<WatchpointBreakpointType.Properties>) {
        breakpoint.isEnabled = !breakpoint.isEnabled

        if (breakpoint.isEnabled && breakpoint.properties?.isValidExpression() == true) {
            sendBreakpointToDebugger(breakpoint)
        } else {
            breakpointIds.remove(breakpoint)?.let { id ->
                removeBreakpointFromDebugger(id)
            }
        }
    }
}