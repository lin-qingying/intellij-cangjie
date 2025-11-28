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

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.breakpoints.XBreakpointProperties
import com.intellij.xdebugger.breakpoints.XLineBreakpointType
import org.cangnova.cangjie.debugger.protobuf.breakpoint.AddressBreakpointType.ID
import org.cangnova.cangjie.debugger.protobuf.core.ProtoDebugProcess
import org.cangnova.cangjie.debugger.protobuf.memory.vfs.DisasmFileType
import org.cangnova.cangjie.debugger.protobuf.messages.ProtoDebuggerBundle
import javax.swing.Icon

/**
 * 仓颉语言地址断点类型
 *
 * 为 proto_debugger 模块提供 IntelliJ 调试框架中的地址断点支持。
 * 允许用户在特定的内存地址设置断点，用于底层调试和反汇编调试。
 */
object AddressBreakpointType : XLineBreakpointType<AddressBreakpointType.Properties>(
    ID,
    ProtoDebuggerBundle.message("proto.breakpoint.address.title")
) {
    /**
     * 地址断点属性类
     *
     * 存储地址断点的配置信息。
     */
    class Properties : XBreakpointProperties<Properties.State>() {

        /**
         * 地址断点状态数据类
         * 用于持久化存储断点的命中次数
         */
        @Tag("address-breakpoint-state")
        data class State(
            @Attribute("hit-count")
            var hitCount: Int = 0
        )

        private var myState = State()

        override fun getState(): State = myState

        override fun loadState(state: State) {
            myState = state
        }

        /**
         * 命中次数
         */
        var hitCount: Int
            get() = myState.hitCount
            set(value) {
                myState.hitCount = value.coerceAtLeast(0)
            }
    }

    // ==================== XBreakpointType 实现 ====================

    /**
     * 创建默认属性对象
     */
    override fun createProperties(): Properties = Properties()


    /**
     * 获取断点图标
     */
    override fun getEnabledIcon(): Icon = AllIcons.Debugger.Db_set_breakpoint

    /**
     * 获取禁用状态的图标
     */
    override fun getDisabledIcon(): Icon = AllIcons.Debugger.Db_disabled_breakpoint

    /**
     * 获取已验证断点的图标
     */
    override fun getMutedEnabledIcon(): Icon = AllIcons.Debugger.Db_muted_breakpoint

    /**
     * 获取已验证但禁用的断点图标
     */
    override fun getMutedDisabledIcon(): Icon = AllIcons.Debugger.Db_muted_disabled_breakpoint


    /**
     * 获取断点类型的描述文本
     */
    override fun getBreakpointsDialogHelpTopic(): String = "reference.dialogs.breakpoints.address"


    /**
     * 判断是否支持条件
     */
    override fun isSuspendThreadSupported(): Boolean = true
    override fun canPutAt(file: VirtualFile, line: Int, project: Project): Boolean {

        val currentSession = XDebuggerManager.getInstance(project).currentSession ?: return false
        val currentDebugProcess = currentSession.debugProcess
        if (currentDebugProcess !is ProtoDebugProcess) return false

        return file.fileType == DisasmFileType && currentDebugProcess.facade.memoryViewFacade.getDisasmStore().virtualFile == file
    }

    // ==================== 常量定义 ====================

    /** 断点类型唯一标识符 */
    const val ID = "proto-debugger-cangjie-address"

    /** 断点类型名称 */
    const val NAME = "Address Breakpoint"

    override fun createBreakpointProperties(
        file: VirtualFile,
        line: Int
    ): Properties = Properties()

}