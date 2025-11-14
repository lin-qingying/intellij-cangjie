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
import com.intellij.xdebugger.breakpoints.XLineBreakpoint
import org.cangnova.cangjie.protodebugger.core.CangJieDebugProcess
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/**
 * 仓颉语言行断点处理器
 *
 * 负责处理IntelliJ调试框架中的行断点事件，将IDE中的行断点操作转换为对proto调试器服务的调用。
 * 实现行断点的注册、移除、启用、禁用等功能。
 *
 * @param debugProcess 调试进程实例，用于访问调试器服务
 */
class CangJieDebuggerLineBreakpointHandler(
    private val debugProcess: CangJieDebugProcess
) : XBreakpointHandler<XLineBreakpoint<CangJieDebuggerLineBreakpointType.Properties>>(
    CangJieDebuggerLineBreakpointType::class.java
) {

    companion object {
        private val LOG = Logger.getInstance(CangJieDebuggerLineBreakpointHandler::class.java)
    }

    /** 活跃的行断点映射，按文件路径分组 */
    private val activeBreakpoints =
        ConcurrentHashMap<String, MutableSet<XLineBreakpoint<CangJieDebuggerLineBreakpointType.Properties>>>()

    /** 断点ID映射，用于跟踪调试器返回的断点ID */
    private val breakpointIds = ConcurrentHashMap<XLineBreakpoint<*>, Int>()

    /**
     * 注册行断点
     *
     * 当用户在IDE中设置行断点时调用此方法。
     * 将行断点信息发送到调试器服务器。
     */
    override fun registerBreakpoint(breakpoint: XLineBreakpoint<CangJieDebuggerLineBreakpointType.Properties>) {
        val file = breakpoint.sourcePosition?.file ?: return
        val filePath = file.path
        val line = breakpoint.line

        LOG.debug("Registering line breakpoint at $filePath:$line")

        // 添加到活跃断点集合
        activeBreakpoints.computeIfAbsent(filePath) { mutableSetOf() }.add(breakpoint)

        // 如果断点启用，发送到调试器
        if (breakpoint.isEnabled) {
            sendBreakpointToDebugger(breakpoint)
        }
    }

    /**
     * 移除行断点
     *
     * 当用户在IDE中移除行断点时调用此方法。
     * 从调试器服务器中移除对应的行断点。
     */
    override fun unregisterBreakpoint(
        breakpoint: XLineBreakpoint<CangJieDebuggerLineBreakpointType.Properties>,
        temporary: Boolean
    ) {
        val file = breakpoint.sourcePosition?.file ?: return
        val filePath = file.path
        val line = breakpoint.line

        LOG.debug("Unregistering line breakpoint at $filePath:$line (temporary: $temporary)")

        // 从活跃断点集合中移除
        activeBreakpoints[filePath]?.remove(breakpoint)

        // 从调试器中移除断点
        val breakpointId = breakpointIds.remove(breakpoint)
        if (breakpointId != null) {
            removeBreakpointFromDebugger(breakpointId)
        }
    }

    /**
     * 发送行断点到调试器服务器
     */
    private fun sendBreakpointToDebugger(breakpoint: XLineBreakpoint<CangJieDebuggerLineBreakpointType.Properties>) {

        val file = breakpoint.sourcePosition?.file ?: return
        val filePath = file.path
        val line = breakpoint.line
        val condition = breakpoint.properties?.getCondition()

        LOG.debug("Sending line breakpoint to debugger: $filePath:$line, condition: $condition")

        debugProcess.executeCommand {
            try {
                val result = debugProcess.debuggerDriver.breakpointService.addLineBreakpoint(
                    path = filePath,
                    line = line,
                    condition = condition
                )

                // 存储断点ID
                breakpointIds[breakpoint] = result.breakpoint.id

                LOG.debug("Line breakpoint registered successfully with ID: ${result.breakpoint.id}")

            } catch (e: Exception) {
                LOG.error("Failed to register line breakpoint at $filePath:$line", e)
                throw e
            }
        }
    }

    /**
     * 从调试器服务器移除断点
     */
    private fun removeBreakpointFromDebugger(breakpointId: Int) {
        LOG.debug("Removing line breakpoint from debugger: ID $breakpointId")

        debugProcess.executeCommand {
            try {
                debugProcess.debuggerDriver.breakpointService.removeBreakpoints(listOf(breakpointId))
                LOG.debug("Line breakpoint removed successfully: ID $breakpointId")

            } catch (e: Exception) {
                LOG.error("Failed to remove line breakpoint: ID $breakpointId", e)
                throw e
            }
        }
    }

    /**
     * 获取断点ID
     */
    fun getBreakpointId(breakpoint: XLineBreakpoint<*>): Int? {
        return breakpointIds[breakpoint]
    }

    /**
     * 获取所有活跃断点
     */
    fun getActiveBreakpoints(): Map<String, Set<XLineBreakpoint<CangJieDebuggerLineBreakpointType.Properties>>> {
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
                debugProcess.debuggerDriver.breakpointService.removeBreakpoints(idsToDisable)
                Unit
            }
        }
    }

    /**
     * 切换断点状态
     */
    fun toggleBreakpoint(breakpoint: XLineBreakpoint<CangJieDebuggerLineBreakpointType.Properties>) {
        breakpoint.isEnabled = !breakpoint.isEnabled

        if (breakpoint.isEnabled) {
            sendBreakpointToDebugger(breakpoint)
        } else {
            breakpointIds.remove(breakpoint)?.let { id ->
                removeBreakpointFromDebugger(id)
            }
        }
    }
}