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

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.xdebugger.breakpoints.XBreakpointProperties
import com.intellij.xdebugger.breakpoints.XLineBreakpointType
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Tag
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.messages.DebuggerBundle
import org.cangnova.cangjie.protodebugger.breakpoint.CangJieDebuggerLineBreakpointType.ID

/**
 * 仓颉语言行断点类型
 *
 * 为proto_debugger模块提供IntelliJ调试框架中的行断点支持。
 * 允许用户在仓颉源代码的特定行设置断点。
 */
object CangJieDebuggerLineBreakpointType : XLineBreakpointType<CangJieDebuggerLineBreakpointType.Properties>(
    ID,
    DebuggerBundle.message("breakpoint.line.title")
) {

    /**
     * 断点属性类
     *
     * 存储断点的各种配置信息，包括条件表达式、启用状态等。
     */
    class Properties : XBreakpointProperties<Properties.State>() {

        /**
         * 断点状态数据类
         */
        @Tag("breakpoint-state")
        class State {
            @Attribute("enabled")
            var enabled: Boolean = true

            @Attribute("condition")
            var condition: String? = null

            @Attribute("log-message")
            var logMessage: String? = null

            @Attribute("suspend-policy")
            var suspendPolicy: String = "ALL"  // ALL, THREAD, NONE
        }

        private var myState = State()

        override fun getState(): State = myState

        override fun loadState(state: State) {
            myState = state
        }

        /**
         * 检查断点是否启用
         */
        fun isEnabled(): Boolean = myState.enabled

        /**
         * 设置断点启用状态
         */
        fun setEnabled(enabled: Boolean) {
            myState.enabled = enabled
        }

        /**
         * 获取断点条件表达式
         */
        fun getCondition(): String? = myState.condition

        /**
         * 设置断点条件表达式
         */
        fun setCondition(condition: String?) {
            myState.condition = condition
        }

        /**
         * 获取日志消息
         */
        fun getLogMessage(): String? = myState.logMessage

        /**
         * 设置日志消息
         */
        fun setLogMessage(message: String?) {
            myState.logMessage = message
        }

        /**
         * 获取暂停策略
         */
        fun getSuspendPolicy(): String = myState.suspendPolicy

        /**
         * 设置暂停策略
         */
        fun setSuspendPolicy(policy: String) {
            myState.suspendPolicy = policy
        }
    }

    /**
     * 创建断点属性
     */
    override fun createBreakpointProperties(file: VirtualFile, line: Int): Properties {
        return Properties()
    }

    /**
     * 检查是否可以在指定文件的指定行设置断点
     */
    override fun canPutAt(file: VirtualFile, line: Int, project: Project): Boolean {
        return file.fileType == CangJieFileType.INSTANCE
    }


        /** 断点类型ID */
        const val ID = "proto-debugger-cangjie-line"

}