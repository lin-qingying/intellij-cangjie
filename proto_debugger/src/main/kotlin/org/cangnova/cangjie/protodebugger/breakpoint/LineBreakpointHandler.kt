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
import com.intellij.xdebugger.breakpoints.XLineBreakpoint
import org.cangnova.cangjie.protodebugger.core.CangJieDebugProcess

/**
 * 仓颉语言行断点处理器
 *
 * 负责处理IntelliJ调试框架中的行断点事件，将IDE中的行断点操作转换为对proto调试器服务的调用。
 * 实现行断点的注册、移除、启用、禁用等功能。
 *
 * @param debugProcess 调试进程实例，用于访问调试器服务
 */
class CangJieDebuggerLineBreakpointHandler(
    debugProcess: CangJieDebugProcess
) : BaseBreakpointHandler<XLineBreakpoint<CangJieDebuggerLineBreakpointType.Properties>, CangJieDebuggerLineBreakpointType, CangJieDebuggerLineBreakpointType.Properties>(
    CangJieDebuggerLineBreakpointType::class.java,
    debugProcess
) {

    companion object {
        private val LOG = Logger.getInstance(CangJieDebuggerLineBreakpointHandler::class.java)
    }

    override fun getBreakpointIdentifier(breakpoint: XLineBreakpoint<CangJieDebuggerLineBreakpointType.Properties>): String {
        val file = breakpoint.sourcePosition?.file ?: return "unknown"
        return "${file.path}:${breakpoint.line}"
    }

    override fun isValidBreakpoint(breakpoint: XLineBreakpoint<CangJieDebuggerLineBreakpointType.Properties>): Boolean {
        return breakpoint.sourcePosition?.file?.path?.isNotBlank() == true
    }

    override fun sendBreakpointToDebugger(breakpoint: XLineBreakpoint<CangJieDebuggerLineBreakpointType.Properties>): Long {
        val file = breakpoint.sourcePosition?.file
            ?: throw IllegalArgumentException("Invalid breakpoint: no source file")
        val filePath = file.path
        val line = breakpoint.line
        val condition = breakpoint.properties?.getCondition()

        LOG.debug("Sending line breakpoint to debugger: $filePath:$line, condition: $condition")

        return debugProcess.executeCommand {
            val result = debugProcess.facade.breakpointService.addLineBreakpoint(
                path = filePath,
                line = line,
                condition = condition
            )
            result.breakpoint.id
        }.get() // 等待异步结果
    }

    override fun removeBreakpointFromDebugger(debuggerIdentifier: Any) {
        val breakpointId = when (debuggerIdentifier) {
            is Long -> debuggerIdentifier
            is Number -> debuggerIdentifier.toLong()
            else -> throw IllegalArgumentException("Invalid debugger identifier type: ${debuggerIdentifier::class.java}")
        }

        LOG.debug("Removing line breakpoint from debugger: ID $breakpointId")

        debugProcess.executeCommand {
            debugProcess.facade.breakpointService.removeBreakpoints(listOf(breakpointId))
            Unit
        }.get() // 等待异步结果
    }

    override fun getBreakpointTypeName(): String = "Line Breakpoint"
}