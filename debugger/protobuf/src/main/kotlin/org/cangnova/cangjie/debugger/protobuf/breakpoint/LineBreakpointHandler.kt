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
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.xdebugger.breakpoints.XLineBreakpoint
import org.cangnova.cangjie.debugger.protobuf.core.ProtoDebugProcess

/**
 * 仓颉语言行断点处理器
 *
 * 负责处理IntelliJ调试框架中的行断点事件，将IDE中的行断点操作转换为对proto调试器服务的调用。
 * 实现行断点的注册、移除、启用、禁用等功能。
 *
 * @param debugProcess 调试进程实例，用于访问调试器服务
 */
class LineBreakpointHandler(
    debugProcess: ProtoDebugProcess
) : BaseBreakpointHandler<XLineBreakpoint<LineBreakpointType.Properties>, LineBreakpointType, LineBreakpointType.Properties>(
    LineBreakpointType::class.java,
    debugProcess
) {

    companion object {
        private val LOG = Logger.getInstance(LineBreakpointHandler::class.java)
    }

    override fun getBreakpointIdentifier(breakpoint: XLineBreakpoint<LineBreakpointType.Properties>): String {
        val file = breakpoint.sourcePosition?.file ?: return "unknown"
        return "${file.path}:${breakpoint.line}"
    }

    override fun isValidBreakpoint(breakpoint: XLineBreakpoint<LineBreakpointType.Properties>): Boolean {
        return breakpoint.sourcePosition?.file?.path?.isNotBlank() == true
    }

    override fun sendBreakpointToDebugger(breakpoint: XLineBreakpoint<LineBreakpointType.Properties>): Long {
        val file = breakpoint.sourcePosition?.file
            ?: throw IllegalArgumentException("Invalid breakpoint: no source file")
        val filePath = file.path
        val line = breakpoint.line
        val condition = breakpoint.conditionExpression?.expression

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
    override fun shouldBreakpointBeRemoved(breakpoint: XLineBreakpoint<LineBreakpointType.Properties>): Boolean {
        val file = breakpoint.sourcePosition?.file
        val line = breakpoint.line

        // 移除文件不存在或行号无效的断点
        if (file == null || !file.exists()) {
            return true
        }

        if (line <= 0) {
            return true
        }

        // 可以添加更多清理逻辑，比如：
        // - 移除超出文件行数范围的断点
        try {
            val document = FileDocumentManager.getInstance().getDocument(file)
            if (document != null) {
                val lineCount = document.lineCount
                if (line > lineCount) {
                    return true
                }
            }
        } catch (e: Exception) {
            LOG.debug("Error checking line count for file ${file.path}: ${e.message}")
            // 如果无法检查行数，保留断点
        }


        return false
    }

    /**
     * 更新行断点的验证状态
     *
     * 当调试器后端报告断点解析状态时，更新 IDE 中的断点图标
     */
    override fun updateVerificationStatus(breakpointId: Long, verified: Boolean) {
        val breakpoint = findBreakpointById(breakpointId) as? XLineBreakpoint<LineBreakpointType.Properties> ?: run {
            LOG.warn("Cannot update line breakpoint verification status: breakpoint not found for ID $breakpointId")
            return
        }

        if (verified) {
            session.setBreakpointVerified(breakpoint)
            LOG.debug("Line breakpoint verified: ID=$breakpointId at ${breakpoint.sourcePosition?.file?.path}:${breakpoint.line}")
        } else {
            session.setBreakpointInvalid(breakpoint, "Breakpoint locations lost")
            LOG.debug("Line breakpoint marked as invalid: ID=$breakpointId")
        }
    }
}