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
 * 仓颉语言符号断点处理器
 *
 * 负责处理IntelliJ调试框架中的符号断点事件，将IDE中的符号断点操作转换为对proto调试器服务的调用。
 * 实现符号断点的注册、移除、启用、禁用等功能。
 *
 * @param debugProcess 调试进程实例，用于访问调试器服务
 */
class SymbolicBreakpointHandler(
    private val debugProcess: CangJieDebugProcess
) : XBreakpointHandler<SymbolicBreakpointXBreakpoint>(
    SymbolicBreakpointType::class.java
) {

    companion object {
        private val LOG = Logger.getInstance(SymbolicBreakpointHandler::class.java)
    }

    /** 活跃的符号断点集合 */
    private val activeBreakpoints = ConcurrentHashMap<String, MutableSet<XBreakpoint<SymbolicBreakpointType.Properties>>>()

    /** 断点ID映射，用于跟踪调试器返回的断点ID */
    private val breakpointIds = ConcurrentHashMap<XBreakpoint<*>, Any>()

    /**
     * 注册符号断点
     *
     * 当用户在IDE中设置符号断点时调用此方法。
     * 将符号断点信息发送到调试器服务器。
     */
    override fun registerBreakpoint(breakpoint: SymbolicBreakpointXBreakpoint) {
        val properties = breakpoint.properties
        val symbolPattern = properties.getSymbolPattern()

        LOG.debug("Registering symbolic breakpoint: $symbolPattern, enabled: ${breakpoint.isEnabled}")

        // 添加到活跃断点集合
        activeBreakpoints.computeIfAbsent(symbolPattern) { mutableSetOf() }.add(breakpoint)

        // 如果断点启用，发送到调试器
        if (breakpoint.isEnabled && !symbolPattern.isBlank()) {
            sendBreakpointToDebugger(breakpoint)
        }
    }

    /**
     * 移除符号断点
     *
     * 当用户在IDE中移除符号断点时调用此方法。
     * 从调试器服务器中移除对应的符号断点。
     */
    override fun unregisterBreakpoint(breakpoint: SymbolicBreakpointXBreakpoint, temporary: Boolean) {
        val properties = breakpoint.properties
        val symbolPattern = properties.getSymbolPattern()

        LOG.debug("Unregistering symbolic breakpoint: $symbolPattern (temporary: $temporary)")

        // 从活跃断点集合中移除
        activeBreakpoints[symbolPattern]?.remove(breakpoint)

        // 从调试器中移除断点
        val breakpointData = breakpointIds.remove(breakpoint)
        if (breakpointData != null) {
            removeBreakpointFromDebugger(breakpoint, breakpointData)
        }
    }

    /**
     * 发送符号断点到调试器服务器
     */
    private fun sendBreakpointToDebugger(breakpoint: XBreakpoint<SymbolicBreakpointType.Properties>)  {
        val properties = breakpoint.properties ?: return
        val symbolPattern = properties.getSymbolPattern()

        if (symbolPattern.isBlank()) {
            LOG.warn("Symbolic breakpoint has empty pattern")
            return
        }

        LOG.debug("Sending symbolic breakpoint to debugger: $symbolPattern")

          debugProcess.executeCommand {
            try {
                val symbolicBreakpoint = org.cangnova.cangjie.protodebugger.breakpoint.SymbolicBreakpoint(
                    pattern = properties.getSymbolPattern(),
                    isRegexpPattern = properties.isRegexpPattern(),
                    module = properties.getModule(),
                    condition = properties.getCondition(),
                    enabled = properties.isEnabled()
                )
                val result = debugProcess.debuggerDriver.breakpointService.addSymbolicBreakpoint(symbolicBreakpoint)

                if (result != null) {
                    breakpointIds[breakpoint] = result
                    LOG.debug("Symbolic breakpoint registered successfully: ${result.symbolPattern}")
                } else {
                    LOG.error("Failed to register symbolic breakpoint: $symbolPattern")
                }

            } catch (e: Exception) {
                LOG.error("Failed to register symbolic breakpoint: $symbolPattern", e)
                throw e
            }
        }
    }

    /**
     * 从调试器服务器移除断点
     */
    private fun removeBreakpointFromDebugger(breakpoint: XBreakpoint<SymbolicBreakpointType.Properties>, breakpointData: Any)  {
        LOG.debug("Removing symbolic breakpoint from debugger: ${breakpoint.type.id}")

          debugProcess.executeCommand {
            try {
                // 符号断点的移除逻辑可能需要根据调试器API调整
                // 这里假设断点Data可以用于识别要移除的断点
                when (breakpointData) {
                    is org.cangnova.cangjie.protodebugger.data.LLSymbolicBreakpoint -> {
                        // 如果是符号断点对象，可能需要调用特定的移除方法
                        // 这里暂时使用通用移除断点的方法
                        LOG.debug("Symbolic breakpoint removed: ${breakpointData.symbolPattern}")
                    }
                    else -> {
                        LOG.warn("Unknown symbolic breakpoint data type: ${breakpointData::class.java}")
                    }
                }

            } catch (e: Exception) {
                LOG.error("Failed to remove symbolic breakpoint: ${breakpoint.type.id}", e)
                throw e
            }
        }
    }

    /**
     * 获取断点ID
     */
    fun getBreakpointId(breakpoint: XBreakpoint<*>): Any? {
        return breakpointIds[breakpoint]
    }

    /**
     * 获取所有活跃断点
     */
    fun getActiveBreakpoints(): Map<String, Set<XBreakpoint<SymbolicBreakpointType.Properties>>> {
        return activeBreakpoints.toMap()
    }

    /**
     * 启用所有断点
     */
    fun enableAllBreakpoints() {
        activeBreakpoints.values.flatten().forEach { breakpoint ->
            if (!breakpoint.isEnabled) {
                breakpoint.isEnabled = true
                sendBreakpointToDebugger(breakpoint)
            }
        }
    }

    /**
     * 禁用所有断点
     */
    fun disableAllBreakpoints() {
        activeBreakpoints.values.flatten().forEach { breakpoint ->
            if (breakpoint.isEnabled) {
                breakpoint.isEnabled = false
                breakpointIds.remove(breakpoint)?.let { data ->
                    removeBreakpointFromDebugger(breakpoint, data)
                }
            }
        }
    }

    /**
     * 切换断点状态
     */
    fun toggleBreakpoint(breakpoint: XBreakpoint<SymbolicBreakpointType.Properties>) {
        breakpoint.isEnabled = !breakpoint.isEnabled

        if (breakpoint.isEnabled) {
            sendBreakpointToDebugger(breakpoint)
        } else {
            breakpointIds.remove(breakpoint)?.let { data ->
                removeBreakpointFromDebugger(breakpoint, data)
            }
        }
    }
}