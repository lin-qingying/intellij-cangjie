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
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.xdebugger.breakpoints.XLineBreakpoint
import org.cangnova.cangjie.debugger.protobuf.core.ProtoDebugProcess
import org.cangnova.cangjie.debugger.protobuf.memory.vfs.MemoryViewFile

/**
 * 仓颉语言地址断点处理器
 *
 * 负责处理IntelliJ调试框架中的地址断点事件，将IDE中的地址断点操作转换为对proto调试器服务的调用。
 * 实现地址断点的注册、移除、启用、禁用等功能。
 *
 * @param debugProcess 调试进程实例，用于访问调试器服务
 */
class AddressBreakpointHandler(
    debugProcess: ProtoDebugProcess
) : BaseBreakpointHandler<XLineBreakpoint<AddressBreakpointType.Properties>, AddressBreakpointType, AddressBreakpointType.Properties>(
    AddressBreakpointType::class.java,
    debugProcess
) {

    companion object {
        private val LOG = Logger.getInstance(AddressBreakpointHandler::class.java)
    }

    override fun getBreakpointIdentifier(breakpoint: XLineBreakpoint<AddressBreakpointType.Properties>): String {
        val file = getFileFromBreakpoint(breakpoint) ?: return "unknown"
        return "${file.path}:${breakpoint.line}"
    }

    override fun isValidBreakpoint(breakpoint: XLineBreakpoint<AddressBreakpointType.Properties>): Boolean {
        return getAddressFromBreakpoint(breakpoint) != null
    }

    override fun sendBreakpointToDebugger(breakpoint: XLineBreakpoint<AddressBreakpointType.Properties>): Any {
        val address = getAddressFromBreakpoint(breakpoint)
            ?: throw IllegalArgumentException("Failed to get address from breakpoint")

        val condition = breakpoint.conditionExpression?.expression

        LOG.debug("Sending address breakpoint to debugger: ${address.toString()}")

        return debugProcess.executeCommand {
            val result = debugProcess.facade.breakpointService.addAddressBreakpoint(
                address = address,
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

        LOG.debug("Removing address breakpoint from debugger: ID $breakpointId")

        debugProcess.executeCommand {
            debugProcess.facade.breakpointService.removeBreakpoints(listOf(breakpointId))
            Unit
        }.get() // 等待异步结果
    }

    override fun getBreakpointTypeName(): String = "Address Breakpoint"

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

    override fun shouldBreakpointBeRemoved(breakpoint: XLineBreakpoint<AddressBreakpointType.Properties>): Boolean {
        val file = getFileFromBreakpoint(breakpoint)
        val line = breakpoint.line

        // 移除文件不存在或行号无效的断点
        if (file == null || !file.exists()) {
            return true
        }

        if (line < 0) {
            return true
        }

        // 如果无法获取地址，移除断点
        if (getAddressFromBreakpoint(breakpoint) == null) {
            return true
        }

        return false
    }

    /**
     * 从断点获取虚拟文件
     *
     * 使用 fileUrl 和 VirtualFileManager 获取文件，
     * 因为地址断点的 sourcePosition 始终为 null
     */
    private fun getFileFromBreakpoint(breakpoint: XLineBreakpoint<AddressBreakpointType.Properties>): VirtualFile? {
        val fileUrl = breakpoint.fileUrl ?: return null
        return VirtualFileManager.getInstance().findFileByUrl(fileUrl)
    }

    /**
     * 从断点获取地址
     *
     * 通过 MemoryViewFile 的 store 获取断点所在行对应的内存地址
     */
    private fun getAddressFromBreakpoint(breakpoint: XLineBreakpoint<AddressBreakpointType.Properties>): org.cangnova.cangjie.debugger.protobuf.memory.Address? {
        val file = getFileFromBreakpoint(breakpoint) ?: return null
        val line = breakpoint.line

        // 从 MemoryViewFile 获取 MemoryStore
        if (file !is MemoryViewFile<*>) {
            return null
        }

        @Suppress("UNCHECKED_CAST")
        val store =
            file.store as? org.cangnova.cangjie.debugger.protobuf.memory.state.MemoryStore<org.cangnova.cangjie.debugger.protobuf.memory.MemoryCell.InstructionCell>
                ?: return null

        // 通过 store 的行号映射获取地址
        return store.getAddressForLine(line)
    }
}