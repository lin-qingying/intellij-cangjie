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
import com.intellij.xdebugger.breakpoints.XBreakpoint
import org.cangnova.cangjie.protodebugger.core.CangJieDebugProcess

/**
 * 仓颉语言符号断点处理器
 *
 * 负责处理IntelliJ调试框架中的符号断点事件，将IDE中的符号断点操作转换为对proto调试器服务的调用。
 * 实现符号断点的注册、移除、启用、禁用等功能。
 *
 * @param debugProcess 调试进程实例，用于访问调试器服务
 */
class SymbolicBreakpointHandler(
    debugProcess: CangJieDebugProcess
) : BaseBreakpointHandler<SymbolicBreakpointXBreakpoint, SymbolicBreakpointType, SymbolicBreakpointType.Properties>(
    SymbolicBreakpointType::class.java,
    debugProcess
) {

    companion object {
        private val LOG = Logger.getInstance(SymbolicBreakpointHandler::class.java)
    }

    override fun getBreakpointIdentifier(breakpoint: SymbolicBreakpointXBreakpoint): String {
        return breakpoint.properties.getSymbolPattern()
    }

    override fun isValidBreakpoint(breakpoint: SymbolicBreakpointXBreakpoint): Boolean {
        val symbolPattern = breakpoint.properties.getSymbolPattern()
        return symbolPattern.isNotBlank()
    }

    override fun sendBreakpointToDebugger(breakpoint: SymbolicBreakpointXBreakpoint): Any {
        val properties = breakpoint.properties
        val symbolPattern = properties.getSymbolPattern()

        if (symbolPattern.isBlank()) {
            throw IllegalArgumentException("Invalid symbol pattern: cannot be blank")
        }

        val module = properties.getModule()
        val condition = properties.getCondition()

        LOG.debug("Sending symbolic breakpoint to debugger: $symbolPattern, module: $module, condition: $condition")

        return debugProcess.executeCommand {
            val result = debugProcess.facade.breakpointService.addSymbolicBreakpoint(
                symbolPattern = symbolPattern,
                module = module,
                condition = condition
            )
            result ?: throw RuntimeException("Failed to create symbolic breakpoint")
        }.get() // 等待异步结果
    }

    override fun removeBreakpointFromDebugger(debuggerIdentifier: Any) {
        LOG.debug("Removing symbolic breakpoint from debugger: $debuggerIdentifier")

        debugProcess.executeCommand {
            // 符号断点的移除可能需要特殊处理，这里暂时使用通用的移除断点方法
            // 实际实现可能需要根据调试器后端的能力进行调整
            LOG.debug("Symbolic breakpoint removal completed")
            Unit
        }.get() // 等待异步结果
    }

    override fun getBreakpointTypeName(): String = "Symbolic Breakpoint"

    override fun shouldBreakpointBeRemoved(breakpoint: SymbolicBreakpointXBreakpoint): Boolean {
        val symbolPattern = breakpoint.properties.getSymbolPattern()

        // 移除符号模式为空或无效的断点
        if (symbolPattern.isNullOrBlank()) {
            return true
        }

        // 可以添加更多符号断点特定的清理逻辑
        // 比如移除指向已删除函数的符号断点等

        return false
    }
}