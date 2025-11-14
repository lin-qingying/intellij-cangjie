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
import com.intellij.xdebugger.breakpoints.XLineBreakpoint
import com.intellij.xdebugger.breakpoints.ui.XBreakpointCustomPropertiesPanel
import org.cangnova.cangjie.messages.DebuggerBundle
import org.cangnova.cangjie.protodebugger.breakpoint.SymbolicBreakpointType.ID
import org.cangnova.cangjie.protodebugger.data.LLSymbolicBreakpoint
import org.jetbrains.annotations.Nls
import javax.swing.Icon

/**
 * 仓颉语言符号断点类型
 *
 * 为 proto_debugger 模块提供 IntelliJ 调试框架中的符号断点支持。
 * 允许用户根据函数名、方法名或其他符号设置断点，而不需要具体的代码行。
 */
object SymbolicBreakpointType : XBreakpointType<SymbolicBreakpointXBreakpoint, SymbolicBreakpointType.Properties>(
    ID,
    DebuggerBundle.message("breakpoint.symbolic.title")
) {

    /**
     * 符号断点属性类
     *
     * 存储符号断点的配置信息，包括符号模式、条件表达式、模块过滤等。
     */
    class Properties : XBreakpointProperties<Properties.State>() {

        /**
         * 符号断点状态数据类
         */
        @Tag("symbolic-breakpoint-state")
        class State {
            @Attribute("symbol-pattern")
            var symbolPattern: String = ""

            @Attribute("is-regexp")
            var isRegexpPattern: Boolean = false

            @Attribute("module")
            var module: String? = null

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
         * 获取符号模式
         */
        fun getSymbolPattern(): String = myState.symbolPattern

        /**
         * 设置符号模式
         */
        fun setSymbolPattern(pattern: String) {
            myState.symbolPattern = pattern
        }

        /**
         * 是否使用正则表达式
         */
        fun isRegexpPattern(): Boolean = myState.isRegexpPattern

        /**
         * 设置正则表达式模式
         */
        fun setRegexpPattern(regexp: Boolean) {
            myState.isRegexpPattern = regexp
        }

        /**
         * 获取模块过滤
         */
        fun getModule(): String? = myState.module

        /**
         * 设置模块过滤
         */
        fun setModule(module: String?) {
            myState.module = module
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
         * 转换为调试器服务的符号断点格式
         * 注意：这里的 id=0 是占位符，实际ID会在服务端分配
         */
        fun toLLSymbolicBreakpoint(): LLSymbolicBreakpoint {
            return LLSymbolicBreakpoint(
                id = 0, // ID will be assigned by the debugger service
                symbolPattern = myState.symbolPattern,
                condition = myState.condition,
                enabled = myState.enabled
            )
        }
    }




    /**
     * 创建默认属性
     */
    override fun createProperties(): Properties {
        return Properties()
    }

    /**
     * 获取断点显示文本
     */
    override fun getDisplayText(breakpoint: SymbolicBreakpointXBreakpoint): @Nls String {
        val props = breakpoint.properties
        return buildString {
            append("Symbolic Breakpoint: ")
            append(props.getSymbolPattern())

            if (props.isRegexpPattern()) {
                append(" (regex)")
            }

            props.getModule()?.let {
                append(" [$it]")
            }

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
    override fun getBreakpointsDialogHelpTopic(): String = "reference.dialogs.breakpoints.symbolic"

    /**
     * 获取短显示文本（用于列表等场景）
     */
    override fun getShortText(breakpoint: SymbolicBreakpointXBreakpoint): String {
        return breakpoint.properties.getSymbolPattern()
    }

    /**
     * 创建自定义属性面板（可选）
     * 如果需要自定义 UI，可以在这里实现
     */
    override fun createCustomPropertiesPanel(project: Project): XBreakpointCustomPropertiesPanel<SymbolicBreakpointXBreakpoint>? {
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
    override fun getGeneralDescription(breakpoint: SymbolicBreakpointXBreakpoint): String {
        val props = breakpoint.properties
        return DebuggerBundle.message(
            "breakpoint.symbolic.description",
            props.getSymbolPattern()
        )
    }



    /**
     * 创建符号断点的便捷工厂方法
     */
    fun createBreakpoint(
        symbolPattern: String,
        isRegexpPattern: Boolean = false,
        module: String? = null,
        condition: String? = null,
        enabled: Boolean = true
    ): SymbolicBreakpointXBreakpoint {
        val properties = Properties().apply {
            setSymbolPattern(symbolPattern)
            setRegexpPattern(isRegexpPattern)
            setModule(module)
            setCondition(condition)
            setEnabled(enabled)
        }
        return SymbolicBreakpointXBreakpoint(properties)
    }






        /** 断点类型ID */
        const val ID = "proto-debugger-cangjie-symbolic"

        /** 无效线程ID常量 */
        const val INVALID_THREAD_ID = 0L

}