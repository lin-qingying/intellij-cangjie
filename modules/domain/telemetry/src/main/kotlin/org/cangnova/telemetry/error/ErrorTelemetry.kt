/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.telemetry.error

import com.intellij.openapi.diagnostic.Logger
import org.cangnova.telemetry.api.EventCategories
import org.cangnova.telemetry.api.TelemetryService
import java.util.concurrent.ConcurrentHashMap

/**
 * 插件内部错误遥测工具类，用于收集和发送插件运行时发生的错误信息
 */
object ErrorTelemetry {
    private val logger = Logger.getInstance(ErrorTelemetry::class.java)

    // 用于去重，避免重复发送相同的错误
    private val reportedErrors = ConcurrentHashMap<String, Long>()

    // 每个错误的报告间隔时间（毫秒）
    private const val REPORT_INTERVAL_MS = 1800000L // 30分钟

    /**
     * 发送异常错误遥测事件
     *
     * @param exception 发生的异常
     * @param context 错误发生的上下文（如类名、方法名等）
     * @param additionalInfo 附加信息
     * @return 如果成功发送，则返回true；否则返回false
     */
    fun sendException(
        exception: Throwable,
        context: String = "",
        additionalInfo: Map<String, String> = emptyMap()
    ): Boolean {
        try {


            val telemetryService = TelemetryService.getInstance()
            if (!telemetryService.isEnabled()) {
                return false
            }

            // 生成错误唯一标识
            val errorId = generateErrorId(exception, context)

            // 检查是否需要去重
            val now = System.currentTimeMillis()
            val lastReportTime = reportedErrors.getOrDefault(errorId, 0L)
            if (now - lastReportTime < REPORT_INTERVAL_MS) {
                return false
            }

            // 更新最后报告时间
            reportedErrors[errorId] = now

            // 构建属性
            val properties = HashMap<String, String>()
            properties["error_id"] = errorId
            properties["exception_class"] = exception.javaClass.simpleName
            properties["exception_message"] = exception.message ?: "No message"
            properties["context"] = context

            // 添加堆栈跟踪的前几层
            val stackTrace = exception.stackTrace
            if (stackTrace.isNotEmpty()) {
                properties["stack_trace_top"] = stackTrace.take(3).joinToString(" -> ") {
                    "${it.className}.${it.methodName}:${it.lineNumber}"
                }
            }

            // 添加根本原因
            val rootCause = getRootCause(exception)
            if (rootCause != exception) {
                properties["root_cause_class"] = rootCause.javaClass.simpleName
                properties["root_cause_message"] = rootCause.message ?: "No message"
            }

            // 添加附加信息
            properties.putAll(additionalInfo)

            // 发送遥测事件
            return telemetryService.sendEvent(
                category = EventCategories.ERROR,
                name = "plugin_exception",
                value = errorId,
                properties = properties
            )
        } catch (e: Exception) {

            return false
        }
    }

    /**
     * 发送一般性错误遥测事件
     *
     * @param errorMessage 错误消息
     * @param errorCode 错误代码
     * @param context 错误发生的上下文
     * @param additionalInfo 附加信息
     * @return 如果成功发送，则返回true；否则返回false
     */
    fun sendError(
        errorMessage: String,
        errorCode: String = "",
        context: String = "",
        additionalInfo: Map<String, String> = emptyMap()
    ): Boolean {
        try {
            val telemetryService = TelemetryService.getInstance()
            if (!telemetryService.isEnabled()) {
                return false
            }

            // 生成错误唯一标识
            val errorId = if (errorCode.isNotEmpty()) {
                errorCode
            } else {
                "GENERAL_ERROR_${Math.abs(errorMessage.hashCode())}"
            }

            // 检查是否需要去重
            val now = System.currentTimeMillis()
            val lastReportTime = reportedErrors.getOrDefault(errorId, 0L)
            if (now - lastReportTime < REPORT_INTERVAL_MS) {
                return false
            }

            // 更新最后报告时间
            reportedErrors[errorId] = now

            // 构建属性
            val properties = HashMap<String, String>()
            properties["error_id"] = errorId
            properties["error_message"] = errorMessage
            properties["error_code"] = errorCode
            properties["context"] = context

            // 添加附加信息
            properties.putAll(additionalInfo)

            // 发送遥测事件
            return telemetryService.sendEvent(
                category = EventCategories.ERROR,
                name = "plugin_error",
                value = errorId,
                properties = properties
            )
        } catch (e: Exception) {
            logger.warn("Failed to send error telemetry", e)
            return false
        }
    }

    /**
     * 发送性能警告遥测事件（当操作耗时过长时）
     *
     * @param operationName 操作名称
     * @param duration 持续时间（毫秒）
     * @param threshold 警告阈值（毫秒）
     * @param context 操作上下文
     * @param additionalInfo 附加信息
     * @return 如果成功发送，则返回true；否则返回false
     */
    fun sendPerformanceWarning(
        operationName: String,
        duration: Long,
        threshold: Long,
        context: String = "",
        additionalInfo: Map<String, String> = emptyMap()
    ): Boolean {
        if (duration < threshold) {
            return false
        }

        try {
            val telemetryService = TelemetryService.getInstance()
            if (!telemetryService.isEnabled()) {
                return false
            }

            // 构建属性
            val properties = HashMap<String, String>()
            properties["operation_name"] = operationName
            properties["duration_ms"] = duration.toString()
            properties["threshold_ms"] = threshold.toString()
            properties["context"] = context
            properties["severity"] = when {
                duration > threshold * 3 -> "CRITICAL"
                duration > threshold * 2 -> "HIGH"
                else -> "MEDIUM"
            }

            // 添加附加信息
            properties.putAll(additionalInfo)

            // 发送遥测事件
            return telemetryService.sendEvent(
                category = EventCategories.ERROR,
                name = "performance_warning",
                value = duration,
                properties = properties
            )
        } catch (e: Exception) {
            logger.warn("Failed to send performance warning telemetry", e)
            return false
        }
    }

    /**
     * 生成错误唯一标识
     */
    private fun generateErrorId(exception: Throwable, context: String): String {
        val exceptionClass = exception.javaClass.simpleName
        val message = exception.message ?: "NoMessage"
        val contextPart = if (context.isNotEmpty()) "_$context" else ""

        // 使用异常类名、消息和上下文生成ID
        val combined = "$exceptionClass:$message$contextPart"
        return "${exceptionClass}_${Math.abs(combined.hashCode())}"
    }

    /**
     * 获取异常的根本原因
     */
    private fun getRootCause(exception: Throwable): Throwable {
        var cause = exception
        while (cause.cause != null && cause.cause != cause) {
            cause = cause.cause!!
        }
        return cause
    }

    /**
     * 清除错误缓存
     */
    fun clearCache() {
        reportedErrors.clear()
    }
}