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
import org.cangnova.cangjie.protodebugger.breakpoint.AddressBreakpointType.ID
import org.cangnova.cangjie.protodebugger.memory.Address
import org.jetbrains.annotations.Nls
import javax.swing.Icon

/**
 * 仓颉语言地址断点类型
 *
 * 为 proto_debugger 模块提供 IntelliJ 调试框架中的地址断点支持。
 * 允许用户在特定的内存地址设置断点，用于底层调试和反汇编调试。
 */
object AddressBreakpointType : XBreakpointType<AddressBreakpoint, AddressBreakpointType.Properties>(
    ID,
    DebuggerBundle.message("breakpoint.address.title")
) {

    /**
     * 地址断点属性类
     *
     * 存储地址断点的配置信息，包括内存地址、条件表达式等。
     */
    class Properties(
        initialAddress: String = ""
    ) : XBreakpointProperties<Properties.State>() {

        /**
         * 地址断点状态数据类
         * 用于持久化存储断点配置
         */
        @Tag("address-breakpoint-state")
        data class State(
            @Attribute("address")
            var address: String = "",

            @Attribute("condition")
            var condition: String? = null,

            @Attribute("enabled")
            var enabled: Boolean = true,

            @Attribute("log-message")
            var logMessage: String? = null,

            @Attribute("hit-count")
            var hitCount: Int = 0,

            @Attribute("description")
            var description: String? = null
        )

        private var state = State(address = initialAddress)

        override fun getState(): State = state

        override fun loadState(state: State) {
            this.state = state
        }

        // ==================== 属性访问器 ====================

        /**
         * 内存地址字符串
         */
        var address: String
            get() = state.address
            set(value) {
                state.address = value.trim()
            }

        /**
         * 条件表达式
         */
        var condition: String?
            get() = state.condition
            set(value) {
                state.condition = value?.takeIf { it.isNotBlank() }
            }

        /**
         * 断点启用状态
         */
        var enabled: Boolean
            get() = state.enabled
            set(value) {
                state.enabled = value
            }

        /**
         * 日志消息模板
         */
        var logMessage: String?
            get() = state.logMessage
            set(value) {
                state.logMessage = value?.takeIf { it.isNotBlank() }
            }

        /**
         * 命中次数
         */
        var hitCount: Int
            get() = state.hitCount
            set(value) {
                state.hitCount = value.coerceAtLeast(0)
            }

        /**
         * 断点描述
         */
        var description: String?
            get() = state.description
            set(value) {
                state.description = value?.takeIf { it.isNotBlank() }
            }

        // ==================== 地址解析和验证 ====================

        /**
         * 解析地址字符串为 Address 对象
         *
         * 支持的格式：
         * - 十六进制: 0x1234, 0X1234, 1234h, 1234H
         * - 十进制: 1234
         * - 八进制: 0o1234, 01234
         * - 二进制: 0b1010, 0B1010
         *
         * @return 解析后的 Address 对象，失败返回 null
         */
        fun parseAddress(): Address? {
            return try {
                val cleanAddress = address.trim()
                if (cleanAddress.isBlank()) return null

                val value = when {
                    // 十六进制: 0x... 或 0X...
                    cleanAddress.startsWith("0x", ignoreCase = true) -> {
                        cleanAddress.substring(2).toLong(16)
                    }
                    // 十六进制后缀: ...h 或 ...H
                    cleanAddress.endsWith("h", ignoreCase = true) -> {
                        cleanAddress.dropLast(1).toLong(16)
                    }
                    // 二进制: 0b... 或 0B...
                    cleanAddress.startsWith("0b", ignoreCase = true) -> {
                        cleanAddress.substring(2).toLong(2)
                    }
                    // 八进制: 0o... 或 0O... 或 0...
                    cleanAddress.startsWith("0o", ignoreCase = true) -> {
                        cleanAddress.substring(2).toLong(8)
                    }
                    cleanAddress.startsWith("0") && cleanAddress.length > 1 -> {
                        cleanAddress.toLong(8)
                    }
                    // 十进制
                    else -> {
                        cleanAddress.toLong(10)
                    }
                }

                Address(value)
            } catch (e: NumberFormatException) {
                null
            }
        }

        /**
         * 验证地址格式是否有效
         */
        fun isValidAddress(): Boolean {
            return address.isNotBlank() && parseAddress() != null
        }

        /**
         * 获取格式化的地址字符串（十六进制）
         */
        fun getFormattedAddress(): String {
            val addr = parseAddress() ?: return address
            return String.format("0x%X", addr.unsignedLongValue)
        }

        /**
         * 增加命中次数
         */
        fun incrementHitCount() {
            hitCount++
        }

        /**
         * 重置命中次数
         */
        fun resetHitCount() {
            hitCount = 0
        }

        // ==================== 辅助方法 ====================

        /**
         * 获取属性摘要信息
         */
        fun getSummary(): String = buildString {
            append("Address: ${getFormattedAddress()}")
            condition?.let { append(", Condition: $it") }
            if (hitCount > 0) append(", Hits: $hitCount")
            if (!enabled) append(" [DISABLED]")
        }

        override fun toString(): String = getSummary()
    }

    // ==================== XBreakpointType 实现 ====================

    /**
     * 创建默认属性对象
     */
    override fun createProperties(): Properties = Properties()

    /**
     * 获取断点显示文本
     */
    override fun getDisplayText(breakpoint: AddressBreakpoint): @Nls String {
        val props = breakpoint.properties
        return buildString {
            append("Address Breakpoint: ")
            append(props.getFormattedAddress())

            props.description?.let {
                append(" - $it")
            }

            if (!breakpoint.isEnabled) {
                append(" (disabled)")
            }

            if (props.hitCount > 0) {
                append(" [${props.hitCount} hits]")
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
    override fun getBreakpointsDialogHelpTopic(): String = "reference.dialogs.breakpoints.address"

    /**
     * 获取短显示文本（用于列表等场景）
     */
    override fun getShortText(breakpoint: AddressBreakpoint): String {
        return breakpoint.properties.getFormattedAddress()
    }

    /**
     * 创建自定义属性面板（可选）
     * 如果需要自定义 UI，可以在这里实现
     */
    override fun createCustomPropertiesPanel(project: Project): XBreakpointCustomPropertiesPanel<AddressBreakpoint>? {
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
    override fun getGeneralDescription(breakpoint: AddressBreakpoint): String {
        return DebuggerBundle.message(
            "breakpoint.address.description",
            breakpoint.properties.getFormattedAddress()
        )
    }

    // ==================== 常量定义 ====================

    /** 断点类型唯一标识符 */
    const val ID = "proto-debugger-cangjie-address"

    /** 断点类型名称 */
    const val NAME = "Address Breakpoint"


        /**
         * 创建地址断点属性的便捷工厂方法
         */
        fun createProperties(
            address: String,
            condition: String? = null,
            logMessage: String? = null,
            description: String? = null
        ): Properties {
            return Properties(address).apply {
                this.condition = condition
                this.logMessage = logMessage
                this.description = description
            }
        }

        /**
         * 验证地址字符串格式
         */
        fun isValidAddressFormat(address: String): Boolean {
            return Properties(address).isValidAddress()
        }

        /**
         * 格式化地址为十六进制字符串
         */
        fun formatAddress(address: Long): String {
            return String.format("0x%X", address)
        }

        /**
         * 解析地址字符串
         */
        fun parseAddressString(address: String): Address? {
            return Properties(address).parseAddress()
        }

}