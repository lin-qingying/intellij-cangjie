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

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.xdebugger.breakpoints.XBreakpointProperties
import com.intellij.xdebugger.breakpoints.XBreakpointType
import com.intellij.xdebugger.breakpoints.ui.XBreakpointCustomPropertiesPanel
import org.cangnova.cangjie.messages.DebuggerBundle
import org.cangnova.cangjie.protodebugger.breakpoint.WatchpointBreakpointType.ID
import org.cangnova.cangjie.protodebugger.data.LLWatchpoint
import org.jetbrains.annotations.Nls
import javax.swing.Icon

/**
 * 仓颉语言观察点类型
 *
 * 为 proto_debugger 模块提供 IntelliJ 调试框架中的观察点支持。
 * 允许用户监视变量的读/写操作，当变量被访问或修改时触发断点。
 */
object WatchpointBreakpointType : XBreakpointType<WatchpointBreakpoint, WatchpointBreakpointType.Properties>(
    ID,
    DebuggerBundle.message("breakpoint.watchpoint.title")
) {

    /**
     * 观察点属性类
     *
     * 存储观察点的配置信息，包括变量表达式、访问类型、条件表达式等。
     */
    class Properties : XBreakpointProperties<Properties.State>() {

        /**
         * 观察点状态数据类
         */
        @Tag("watchpoint-state")
        class State {
            @Attribute("expression")
            var expression: String = ""

            @Attribute("access-type")
            var accessType: String = ACCESS_TYPE_WRITE // read, write, read_write

            @Attribute("condition")
            var condition: String? = null

            @Attribute("thread-id")
            var threadId: Long = INVALID_THREAD_ID

            @Attribute("enabled")
            var enabled: Boolean = true

            @Attribute("log-message")
            var logMessage: String? = null
        }

        private var myState = State()

        override fun getState(): State = myState

        override fun loadState(state: State) {
            myState = state
        }

        /**
         * 获取变量表达式
         */
        fun getExpression(): String = myState.expression

        /**
         * 设置变量表达式
         */
        fun setExpression(expression: String) {
            myState.expression = expression
        }

        /**
         * 获取访问类型
         */
        fun getAccessType(): String = myState.accessType

        /**
         * 设置访问类型
         */
        fun setAccessType(accessType: String) {
            myState.accessType = when (accessType) {
                ACCESS_TYPE_READ, ACCESS_TYPE_WRITE, ACCESS_TYPE_READ_WRITE -> accessType
                else -> ACCESS_TYPE_WRITE
            }
        }

        /**
         * 获取条件表达式
         */
        fun getCondition(): String? = myState.condition

        /**
         * 设置条件表达式
         */
        fun setCondition(condition: String?) {
            myState.condition = condition
        }

        /**
         * 获取线程ID
         */
        fun getThreadId(): Long = myState.threadId

        /**
         * 设置线程ID
         */
        fun setThreadId(threadId: Long) {
            myState.threadId = threadId
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
         * 检查是否监视读操作
         */
        fun isReadAccess(): Boolean {
            return myState.accessType == ACCESS_TYPE_READ || myState.accessType == ACCESS_TYPE_READ_WRITE
        }

        /**
         * 检查是否监视写操作
         */
        fun isWriteAccess(): Boolean {
            return myState.accessType == ACCESS_TYPE_WRITE || myState.accessType == ACCESS_TYPE_READ_WRITE
        }

        /**
         * 转换为LLWatchpoint.AccessType
         */
        fun toLLAccessType(): LLWatchpoint.AccessType {
            return when (myState.accessType) {
                ACCESS_TYPE_READ -> LLWatchpoint.AccessType.READ
                ACCESS_TYPE_WRITE -> LLWatchpoint.AccessType.WRITE
                ACCESS_TYPE_READ_WRITE -> LLWatchpoint.AccessType.ANY
                else -> LLWatchpoint.AccessType.WRITE
            }
        }

        /**
         * 验证观察点表达式
         */
        fun isValidExpression(): Boolean {
            return myState.expression.isNotBlank()
        }
    }

    // ==================== XBreakpointType 实现 ====================



    /**
     * 创建默认属性
     */
    override fun createProperties(): Properties = Properties()

    /**
     * 获取断点显示文本
     */
    override fun getDisplayText(breakpoint: WatchpointBreakpoint): @Nls String {
        val props = breakpoint.properties
        return buildString {
            append("Watchpoint: ")
            append(props.getExpression())

            val accessType = when (props.getAccessType()) {
                ACCESS_TYPE_READ -> "read"
                ACCESS_TYPE_WRITE -> "write"
                ACCESS_TYPE_READ_WRITE -> "read/write"
                else -> "write"
            }
            append(" ($accessType)")

            if (!breakpoint.isEnabled) {
                append(" (disabled)")
            }

            props.getCondition()?.let {
                append(" [condition: $it]")
            }
        }
    }

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
    override fun getBreakpointsDialogHelpTopic(): String = "reference.dialogs.breakpoints.watchpoint"

    /**
     * 获取短显示文本（用于列表等场景）
     */
    override fun getShortText(breakpoint: WatchpointBreakpoint): String {
        return breakpoint.properties.getExpression()
    }

    /**
     * 创建自定义属性面板（可选）
     * 如果需要自定义 UI，可以在这里实现
     */
    override fun createCustomPropertiesPanel(project: Project): XBreakpointCustomPropertiesPanel<WatchpointBreakpoint>? {
        // 返回 null 使用默认面板，或实现自定义面板
        return null
    }

    /**
     * 判断是否支持条件
     */
    override fun isSuspendThreadSupported(): Boolean = true

    /**
     * 获取断点类型的一般描述
     */
    override fun getGeneralDescription(breakpoint: WatchpointBreakpoint): String {
        val props = breakpoint.properties
        return DebuggerBundle.message(
            "breakpoint.watchpoint.description",
            props.getExpression()
        )
    }



    /**
     * 创建观察点的便捷工厂方法
     */
    fun createWatchpoint(
        expression: String,
        accessType: String = ACCESS_TYPE_WRITE,
        condition: String? = null,
        enabled: Boolean = true
    ): WatchpointBreakpoint {
        val properties = Properties().apply {
            setExpression(expression)
            setAccessType(accessType)
            setCondition(condition)
            setEnabled(enabled)
        }
        return WatchpointBreakpoint(properties)
    }

    /**
     * 创建读观察点的便捷工厂方法
     */
    fun createReadWatchpoint(
        expression: String,
        condition: String? = null,
        enabled: Boolean = true
    ): WatchpointBreakpoint {
        return createWatchpoint(expression, ACCESS_TYPE_READ, condition, enabled)
    }

    /**
     * 创建写观察点的便捷工厂方法
     */
    fun createWriteWatchpoint(
        expression: String,
        condition: String? = null,
        enabled: Boolean = true
    ): WatchpointBreakpoint {
        return createWatchpoint(expression, ACCESS_TYPE_WRITE, condition, enabled)
    }

    /**
     * 创建读写观察点的便捷工厂方法
     */
    fun createReadWriteWatchpoint(
        expression: String,
        condition: String? = null,
        enabled: Boolean = true
    ): WatchpointBreakpoint {
        return createWatchpoint(expression, ACCESS_TYPE_READ_WRITE, condition, enabled)
    }


        /** 断点类型ID */
        const val ID = "proto-debugger-cangjie-watchpoint"

        /** 无效线程ID常量 */
        const val INVALID_THREAD_ID = 0L

        /** 访问类型常量 */
        const val ACCESS_TYPE_READ = "read"
        const val ACCESS_TYPE_WRITE = "write"
        const val ACCESS_TYPE_READ_WRITE = "read_write"

}