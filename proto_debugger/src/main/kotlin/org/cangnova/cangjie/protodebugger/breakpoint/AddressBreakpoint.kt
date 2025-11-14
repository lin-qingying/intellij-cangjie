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

import com.intellij.openapi.util.Key
import com.intellij.pom.Navigatable
import com.intellij.xdebugger.XExpression
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.breakpoints.SuspendPolicy
import com.intellij.xdebugger.breakpoints.XBreakpoint
import com.intellij.xdebugger.breakpoints.XBreakpointType
import com.intellij.xdebugger.evaluation.EvaluationMode
import org.cangnova.cangjie.protodebugger.memory.Address
import java.util.concurrent.ConcurrentHashMap

/**
 * 仓颉语言地址断点
 *
 * 表示对特定内存地址的断点，当程序访问该地址时触发。
 * 用于底层调试和反汇编调试。
 *
 * @property properties 断点属性配置
 */
class AddressBreakpoint(
    private val properties: AddressBreakpointType.Properties
) : XBreakpoint<AddressBreakpointType.Properties> {

    // 使用属性委托简化代码
    private var enabled: Boolean = true
    private var suspendPolicy: SuspendPolicy = SuspendPolicy.ALL
    private var logMessage: Boolean = false
    private var logStack: Boolean = false
    private var logExpressionObj: XExpression? = null
    private var conditionExpressionObj: XExpression? = null
    private val timeStamp: Long = System.currentTimeMillis()

    // 使用线程安全的 ConcurrentHashMap
    private val userData = ConcurrentHashMap<Key<*>, Any>()

    // ==================== XBreakpoint 接口实现 ====================

    override fun isEnabled(): Boolean = enabled

    override fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    override fun getType(): XBreakpointType<*, AddressBreakpointType.Properties> = AddressBreakpointType

    override fun getProperties(): AddressBreakpointType.Properties = properties

    override fun getSourcePosition(): XSourcePosition? = null

    override fun getNavigatable(): Navigatable? = null

    override fun getSuspendPolicy(): SuspendPolicy = suspendPolicy

    override fun setSuspendPolicy(suspendPolicy: SuspendPolicy) {
        this.suspendPolicy = suspendPolicy
    }

    override fun isLogMessage(): Boolean = logMessage

    override fun setLogMessage(logMessage: Boolean) {
        this.logMessage = logMessage
    }

    override fun isLogStack(): Boolean = logStack

    override fun setLogStack(logStack: Boolean) {
        this.logStack = logStack
    }

    @Deprecated("Use getLogExpressionObject() instead", ReplaceWith("getLogExpressionObject()?.expression"))
    override fun setLogExpression(logExpression: String?) {
        logExpressionObj = logExpression?.let { createExpression(it) }
    }

    override fun getLogExpressionObject(): XExpression? = logExpressionObj

    override fun setLogExpressionObject(expression: XExpression?) {
        logExpressionObj = expression
    }

    @Deprecated("Use getConditionExpression() instead", ReplaceWith("getConditionExpression()?.expression"))
    override fun setCondition(condition: String?) {
        conditionExpressionObj = condition?.let { createExpression(it) }
    }

    override fun getConditionExpression(): XExpression? = conditionExpressionObj

    override fun setConditionExpression(condition: XExpression?) {
        conditionExpressionObj = condition
    }

    override fun getTimeStamp(): Long = timeStamp

    override fun <T : Any?> getUserData(key: Key<T>): T? {
        @Suppress("UNCHECKED_CAST")
        return userData[key] as? T
    }

    override fun <T : Any?> putUserData(key: Key<T>, value: T?) {
        if (value != null) {
            userData[key] = value
        } else {
            userData.remove(key)
        }
    }

    // ==================== 地址断点特有方法 ====================

    /**
     * 获取内存地址对象
     * @return 解析后的地址对象，如果地址无效则返回 null
     */
    val address: Address?
        get() = properties.parseAddress()

    /**
     * 获取地址字符串表示
     */
    val addressString: String
        get() = properties.address

    /**
     * 获取条件表达式字符串（已废弃，使用 conditionExpression）
     */
    @Deprecated("Use conditionExpression instead", ReplaceWith("conditionExpression?.expression"))
    val condition: String?
        get() = conditionExpressionObj?.expression

    /**
     * 检查地址是否有效
     */
    fun isValidAddress(): Boolean = properties.isValidAddress()


    /**
     * 获取断点描述信息
     */
    fun getDescription(): String = buildString {
        append("Address Breakpoint at ${properties.address}")
        if (!enabled) {
            append(" (disabled)")
        }
        conditionExpressionObj?.let {
            append(" [condition: ${it.expression}]")
        }
    }

    /**
     * 检查给定地址是否匹配此断点
     */
    fun matches(targetAddress: Address): Boolean {
        val breakpointAddress = address ?: return false
        return breakpointAddress == targetAddress
    }

    /**
     * 检查给定地址字符串是否匹配此断点
     */
    fun matches(targetAddressString: String): Boolean {
        return addressString.equals(targetAddressString, ignoreCase = true)
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建表达式对象
     */
    private fun createExpression(expression: String): XExpression {
        return object : XExpression {
            override fun getExpression(): String = expression
            override fun getLanguage() = null
            override fun getCustomInfo() = null
            override fun getMode() = EvaluationMode.EXPRESSION
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AddressBreakpoint) return false
        return addressString == other.addressString
    }

    override fun hashCode(): Int = addressString.hashCode()

    override fun toString(): String = getDescription()

    companion object {
        /**
         * 创建地址断点的便捷工厂方法
         */
        fun create(
            address: String,
            enabled: Boolean = true,
            suspendPolicy: SuspendPolicy = SuspendPolicy.ALL,
            condition: String? = null
        ): AddressBreakpoint {
            val properties = AddressBreakpointType.Properties(address)
            return AddressBreakpoint(properties).apply {
                this.enabled = enabled
                this.suspendPolicy = suspendPolicy
                condition?.let { getConditionExpression()?.expression }
            }
        }
    }
}