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
 * 仓颉语言符号断点数据类（内部协议使用）
 *
 * 用于在调试器服务之间传递符号断点配置信息。
 *
 * @param pattern 符号模式（函数名、方法名等）
 * @param isRegexpPattern 是否使用正则表达式模式
 * @param module 模块过滤（可选）
 * @param condition 断点条件表达式（可选）
 * @param threadId 线程ID过滤
 * @param enabled 断点是否启用
 */
data class SymbolicBreakpoint(
    val pattern: String,
    val isRegexpPattern: Boolean = false,
    val module: String? = null,
    val condition: String? = null,
    val threadId: Long = SymbolicBreakpointType.INVALID_THREAD_ID,
    val enabled: Boolean = true
)

/**
 * 仓颉语言符号断点
 *
 * 表示对特定符号（如函数名）的断点，当程序执行到该符号时触发。
 */
class SymbolicBreakpointXBreakpoint(
    private val properties: SymbolicBreakpointType.Properties
) : XBreakpoint<SymbolicBreakpointType.Properties> {

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

    override fun getType(): XBreakpointType<*, SymbolicBreakpointType.Properties> = SymbolicBreakpointType

    override fun getProperties(): SymbolicBreakpointType.Properties = properties

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

    // ==================== 符号断点特有方法 ====================

    /**
     * 获取符号模式字符串
     */
    val symbolPattern: String
        get() = properties.getSymbolPattern()

    /**
     * 获取条件表达式字符串（已废弃，使用 conditionExpression）
     */
    @Deprecated("Use conditionExpression instead", ReplaceWith("conditionExpression?.expression"))
    val condition: String?
        get() = conditionExpressionObj?.expression

    /**
     * 检查符号模式是否有效
     */
    fun isValidSymbolPattern(): Boolean = symbolPattern.isNotBlank()

    /**
     * 检查是否使用正则表达式模式
     */
    fun isRegexpPattern(): Boolean = properties.isRegexpPattern()

    /**
     * 获取模块过滤
     */
    fun getModule(): String? = properties.getModule()

    /**
     * 获取线程ID
     */
    fun getThreadId(): Long = properties.getThreadId()

    /**
     * 获取断点描述信息
     */
    fun getDescription(): String = buildString {
        append("Symbolic Breakpoint: $symbolPattern")
        if (!enabled) {
            append(" (disabled)")
        }
        conditionExpressionObj?.let {
            append(" [condition: ${it.expression}]")
        }
        properties.getModule()?.let {
            append(" [module: $it]")
        }
        if (properties.isRegexpPattern()) {
            append(" [regex]")
        }
    }

    /**
     * 转换为调试器服务的符号断点格式
     */
    fun toLLDBSymbolicBreakpoint(): org.cangnova.cangjie.protodebugger.data.LLDBSymbolicBreakpoint {
        return org.cangnova.cangjie.protodebugger.data.LLDBSymbolicBreakpoint(
            id = 0, // ID will be assigned by the debugger service
            symbolPattern = symbolPattern,
            condition = conditionExpressionObj?.expression,
            enabled = enabled
        )
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
        if (other !is SymbolicBreakpointXBreakpoint) return false
        return symbolPattern == other.symbolPattern &&
               getModule() == other.getModule() &&
               isRegexpPattern() == other.isRegexpPattern()
    }

    override fun hashCode(): Int {
        var result = symbolPattern.hashCode()
        result = 31 * result + (getModule()?.hashCode() ?: 0)
        result = 31 * result + isRegexpPattern().hashCode()
        return result
    }

    override fun toString(): String = getDescription()

    companion object {
        /**
         * 创建符号断点的便捷工厂方法
         */
        fun create(
            symbolPattern: String,
            enabled: Boolean = true,
            suspendPolicy: SuspendPolicy = SuspendPolicy.ALL,
            condition: String? = null,
            module: String? = null,
            isRegexpPattern: Boolean = false
        ): SymbolicBreakpointXBreakpoint {
            val properties = SymbolicBreakpointType.Properties().apply {
                setSymbolPattern(symbolPattern)
                setCondition(condition)
                setModule(module)
                setRegexpPattern(isRegexpPattern)
                setEnabled(enabled)
            }
            return SymbolicBreakpointXBreakpoint(properties).apply {
                this.enabled = enabled
                this.suspendPolicy = suspendPolicy
                condition?.let { getConditionExpression()?.expression }
            }
        }
    }
}