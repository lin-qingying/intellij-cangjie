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

import com.intellij.openapi.util.Key
import com.intellij.pom.Navigatable
import com.intellij.xdebugger.XExpression
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.breakpoints.SuspendPolicy
import com.intellij.xdebugger.breakpoints.XBreakpoint
import com.intellij.xdebugger.breakpoints.XBreakpointType
import com.intellij.xdebugger.evaluation.EvaluationMode
import java.util.concurrent.ConcurrentHashMap

/**
 * 仓颉语言观察点
 *
 * 表示对变量或内存表达式的监视点，当变量被读/写时触发。
 *
 * @property properties 观察点属性配置
 */
class WatchpointBreakpoint(
    private val properties: WatchpointBreakpointType.Properties
) : XBreakpoint<WatchpointBreakpointType.Properties> {

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

    override fun getType(): XBreakpointType<*, WatchpointBreakpointType.Properties> = WatchpointBreakpointType

    override fun getProperties(): WatchpointBreakpointType.Properties = properties

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

    // ==================== 观察点特有方法 ====================

    /**
     * 获取变量表达式字符串
     */
    val expression: String
        get() = properties.getExpression()

    /**
     * 获取条件表达式字符串（已废弃，使用 conditionExpression）
     */
    @Deprecated("Use conditionExpression instead", ReplaceWith("conditionExpression?.expression"))
    val condition: String?
        get() = conditionExpressionObj?.expression

    /**
     * 获取访问类型字符串
     */
    val accessType: String
        get() = properties.getAccessType()

    /**
     * 获取线程ID
     */
    val threadId: Long
        get() = properties.getThreadId()

    /**
     * 检查表达式是否有效
     */
    fun isValidExpression(): Boolean = properties.isValidExpression()

    /**
     * 检查是否监视读操作
     */
    fun isReadAccess(): Boolean = properties.isReadAccess()

    /**
     * 检查是否监视写操作
     */
    fun isWriteAccess(): Boolean = properties.isWriteAccess()

    /**
     * 转换为LLWatchpoint.AccessType
     */
    fun toLLAccessType(): org.cangnova.cangjie.protodebugger.data.LLWatchpoint.AccessType = properties.toLLAccessType()

    /**
     * 获取日志消息
     */
    val logMessageText: String?
        get() = properties.getLogMessage()

    /**
     * 获取观察点描述信息
     */
    fun getDescription(): String = buildString {
        append("Watchpoint: $expression")

        val accessTypeDisplay = when (accessType) {
            WatchpointBreakpointType.ACCESS_TYPE_READ -> "read"
            WatchpointBreakpointType.ACCESS_TYPE_WRITE -> "write"
            WatchpointBreakpointType.ACCESS_TYPE_READ_WRITE -> "read/write"
            else -> "write"
        }
        append(" ($accessTypeDisplay)")

        if (!enabled) {
            append(" (disabled)")
        }

        conditionExpressionObj?.let {
            append(" [condition: ${it.expression}]")
        }

        if (threadId != WatchpointBreakpointType.INVALID_THREAD_ID) {
            append(" [thread: $threadId]")
        }
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
        if (other !is WatchpointBreakpoint) return false
        return expression == other.expression &&
               accessType == other.accessType &&
               threadId == other.threadId
    }

    override fun hashCode(): Int {
        var result = expression.hashCode()
        result = 31 * result + accessType.hashCode()
        result = 31 * result + threadId.hashCode()
        return result
    }

    override fun toString(): String = getDescription()

    companion object {
        /**
         * 创建观察点的便捷工厂方法
         */
        fun create(
            expression: String,
            accessType: String = WatchpointBreakpointType.ACCESS_TYPE_WRITE,
            enabled: Boolean = true,
            suspendPolicy: SuspendPolicy = SuspendPolicy.ALL,
            condition: String? = null
        ): WatchpointBreakpoint {
            val properties = WatchpointBreakpointType.Properties().apply {
                setExpression(expression)
                setAccessType(accessType)
                setCondition(condition)
                setEnabled(enabled)
            }
            return WatchpointBreakpoint(properties).apply {
                this.enabled = enabled
                this.suspendPolicy = suspendPolicy
                condition?.let { getConditionExpression()?.expression }
            }
        }

        /**
         * 创建读观察点的便捷工厂方法
         */
        fun createReadWatchpoint(
            expression: String,
            enabled: Boolean = true,
            condition: String? = null
        ): WatchpointBreakpoint {
            return create(expression, WatchpointBreakpointType.ACCESS_TYPE_READ, enabled, SuspendPolicy.ALL, condition)
        }

        /**
         * 创建写观察点的便捷工厂方法
         */
        fun createWriteWatchpoint(
            expression: String,
            enabled: Boolean = true,
            condition: String? = null
        ): WatchpointBreakpoint {
            return create(expression, WatchpointBreakpointType.ACCESS_TYPE_WRITE, enabled, SuspendPolicy.ALL, condition)
        }

        /**
         * 创建读写观察点的便捷工厂方法
         */
        fun createReadWriteWatchpoint(
            expression: String,
            enabled: Boolean = true,
            condition: String? = null
        ): WatchpointBreakpoint {
            return create(expression, WatchpointBreakpointType.ACCESS_TYPE_READ_WRITE, enabled, SuspendPolicy.ALL, condition)
        }
    }
}