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

import com.intellij.openapi.diagnostic.Logger
import org.cangnova.cangjie.debugger.protobuf.core.ProtoDebugProcess

/**
 * 仓颉语言观察点处理器
 *
 * 负责处理IntelliJ调试框架中的观察点事件，将IDE中的观察点操作转换为对proto调试器服务的调用。
 * 实现观察点的注册、移除、启用、禁用等功能。
 *
 * @param debugProcess 调试进程实例，用于访问调试器服务
 */
class WatchpointBreakpointHandler(
    debugProcess: ProtoDebugProcess
) : BaseBreakpointHandler<WatchpointBreakpoint, WatchpointBreakpointType, WatchpointBreakpointType.Properties>(
    WatchpointBreakpointType::class.java,
    debugProcess
) {

    companion object {
        private val LOG = Logger.getInstance(WatchpointBreakpointHandler::class.java)
    }

    override fun getBreakpointIdentifier(breakpoint: WatchpointBreakpoint): String {
        return breakpoint.properties.getExpression()
    }

    override fun isValidBreakpoint(breakpoint: WatchpointBreakpoint): Boolean {
        return breakpoint.properties.isValidExpression()
    }

    override fun sendBreakpointToDebugger(breakpoint: WatchpointBreakpoint): Long {
        val properties = breakpoint.properties

        if (!properties.isValidExpression()) {
            throw IllegalArgumentException("Invalid expression for watchpoint: ${properties.getExpression()}")
        }

        // 注意：观察点创建需要有效的LLDBVariable对象
        // 这里返回0表示暂未创建，实际实现需要在运行时获取正确的变量
        LOG.warn("Cannot create watchpoint without valid LLDBVariable for expression: ${properties.getExpression()}")
        return 0L
    }

    override fun removeBreakpointFromDebugger(debuggerIdentifier: Any) {
        val breakpointId = when (debuggerIdentifier) {
            is Long -> debuggerIdentifier
            is Number -> debuggerIdentifier.toLong()
            else -> throw IllegalArgumentException("Invalid debugger identifier type: ${debuggerIdentifier::class.java}")
        }

        if (breakpointId == 0L) {
            LOG.debug("Skipping removal of watchpoint with ID 0 (not created)")
            return
        }

        LOG.debug("Removing watchpoint from debugger: ID $breakpointId")

        debugProcess.executeCommand {
            debugProcess.facade.breakpointService.removeBreakpoints(listOf(breakpointId))
            Unit
        }.get() // 等待异步结果
    }

    override fun getBreakpointTypeName(): String = "Watchpoint"

    override fun shouldBreakpointBeRemoved(breakpoint: WatchpointBreakpoint): Boolean {
        // 移除表达式无效的观察点
        if (!breakpoint.properties.isValidExpression()) {
            return true
        }

        // 可以添加更多观察点特定的清理逻辑
        // 比如移除指向已删除变量的观察点等

        return false
    }
}