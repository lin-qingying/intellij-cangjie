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

package org.cangnova.telemetry.performance

import com.intellij.openapi.diagnostic.Logger
import org.cangnova.telemetry.api.EventCategories
import org.cangnova.telemetry.api.TelemetryService
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * 代码索引构建时间遥测工具类，用于收集和发送索引构建相关的性能数据
 */
object IndexingPerformanceTelemetry {
    private val logger = Logger.getInstance(IndexingPerformanceTelemetry::class.java)

    // 性能统计数据
    private val indexingStats = ConcurrentHashMap<String, IndexingStats>()

    // 当前正在进行的索引操作
    private val activeIndexing = ConcurrentHashMap<String, Long>()

    data class IndexingStats(
        val operationType: String,
        val totalTime: AtomicLong = AtomicLong(0),
        val count: AtomicLong = AtomicLong(0),
        val maxTime: AtomicLong = AtomicLong(0),
        val minTime: AtomicLong = AtomicLong(Long.MAX_VALUE)
    )

    /**
     * 开始索引操作计时
     *
     * @param operationType 操作类型（如 "file_indexing", "project_indexing", "psi_building"等）
     * @param operationId 操作唯一标识
     * @return 操作标识，用于后续结束计时
     */
    fun startIndexing(operationType: String, operationId: String = ""): String {
        val id = if (operationId.isEmpty()) {
            "${operationType}_${System.currentTimeMillis()}_${Thread.currentThread().id}"
        } else {
            "${operationType}_$operationId"
        }

        activeIndexing[id] = System.currentTimeMillis()
        return id
    }

    /**
     * 结束索引操作计时并发送遥测事件
     *
     * @param operationId 操作标识（由startIndexing返回）
     * @param fileCount 处理的文件数量
     * @param additionalInfo 附加信息
     * @return 如果成功发送，则返回true；否则返回false
     */
    fun endIndexing(
        operationId: String,
        fileCount: Int = 0,
        additionalInfo: Map<String, String> = emptyMap()
    ): Boolean {
        val startTime = activeIndexing.remove(operationId) ?: return false
        val duration = System.currentTimeMillis() - startTime

        val operationType = operationId.substringBefore('_')

        // 更新统计数据
        updateStats(operationType, duration)

        try {
            val telemetryService = TelemetryService.getInstance()
            if (!telemetryService.isEnabled()) {
                return false
            }

            // 构建属性
            val properties = HashMap<String, String>()
            properties["operation_type"] = operationType
            properties["duration_ms"] = duration.toString()
            properties["file_count"] = fileCount.toString()

            if (fileCount > 0) {
                properties["avg_time_per_file"] = (duration / fileCount).toString()
            }

            // 添加性能等级
            properties["performance_level"] = getPerformanceLevel(operationType, duration)

            // 添加统计信息
            val stats = indexingStats[operationType]
            if (stats != null) {
                properties["avg_duration_ms"] = (stats.totalTime.get() / stats.count.get()).toString()
                properties["max_duration_ms"] = stats.maxTime.get().toString()
                properties["min_duration_ms"] = stats.minTime.get().toString()
                properties["operation_count"] = stats.count.get().toString()
            }

            // 添加附加信息
            properties.putAll(additionalInfo)

            // 发送遥测事件
            return telemetryService.sendEvent(
                category = EventCategories.PERFORMANCE,
                name = "indexing_performance",
                value = duration,
                properties = properties
            )
        } catch (e: Exception) {
            logger.warn("Failed to send indexing performance telemetry", e)
            return false
        }
    }

    /**
     * 发送索引错误遥测事件
     *
     * @param operationType 操作类型
     * @param errorMessage 错误消息
     * @param exception 异常对象
     * @param additionalInfo 附加信息
     * @return 如果成功发送，则返回true；否则返回false
     */
    fun sendIndexingError(
        operationType: String,
        errorMessage: String,
        exception: Throwable? = null,
        additionalInfo: Map<String, String> = emptyMap()
    ): Boolean {
        try {
            val telemetryService = TelemetryService.getInstance()
            if (!telemetryService.isEnabled()) {
                return false
            }

            // 构建属性
            val properties = HashMap<String, String>()
            properties["operation_type"] = operationType
            properties["error_message"] = errorMessage

            exception?.let { ex ->
                properties["exception_class"] = ex.javaClass.simpleName
                properties["exception_message"] = ex.message ?: "No message"

                val stackTrace = ex.stackTrace
                if (stackTrace.isNotEmpty()) {
                    properties["stack_trace_top"] = stackTrace.take(2).joinToString(" -> ") {
                        "${it.className}.${it.methodName}:${it.lineNumber}"
                    }
                }
            }

            // 添加附加信息
            properties.putAll(additionalInfo)

            // 发送遥测事件
            return telemetryService.sendEvent(
                category = EventCategories.ERROR,
                name = "indexing_error",
                value = "indexing_error_${Math.abs(errorMessage.hashCode())}",
                properties = properties
            )
        } catch (e: Exception) {
            logger.warn("Failed to send indexing error telemetry", e)
            return false
        }
    }

    /**
     * 发送索引完成统计遥测事件
     *
     * @param projectName 项目名称
     * @param totalFiles 总文件数
     * @param indexedFiles 已索引文件数
     * @param totalDuration 总耗时
     * @param additionalInfo 附加信息
     * @return 如果成功发送，则返回true；否则返回false
     */
    fun sendIndexingComplete(
        projectName: String,
        totalFiles: Int,
        indexedFiles: Int,
        totalDuration: Long,
        additionalInfo: Map<String, String> = emptyMap()
    ): Boolean {
        try {
            val telemetryService = TelemetryService.getInstance()
            if (!telemetryService.isEnabled()) {
                return false
            }

            // 构建属性
            val properties = HashMap<String, String>()
            properties["project_name_hash"] = Math.abs(projectName.hashCode()).toString()
            properties["total_files"] = totalFiles.toString()
            properties["indexed_files"] = indexedFiles.toString()
            properties["total_duration_ms"] = totalDuration.toString()
            properties["success_rate"] = if (totalFiles > 0) {
                String.format("%.2f", (indexedFiles.toDouble() / totalFiles) * 100)
            } else "0.0"

            if (indexedFiles > 0) {
                properties["avg_time_per_file"] = (totalDuration / indexedFiles).toString()
            }

            // 性能评级
            val avgTimePerFile = if (indexedFiles > 0) totalDuration / indexedFiles else 0
            properties["indexing_performance"] = when {
                avgTimePerFile < 50 -> "EXCELLENT"
                avgTimePerFile < 200 -> "GOOD"
                avgTimePerFile < 500 -> "AVERAGE"
                avgTimePerFile < 1000 -> "SLOW"
                else -> "VERY_SLOW"
            }

            // 添加附加信息
            properties.putAll(additionalInfo)

            // 发送遥测事件
            return telemetryService.sendEvent(
                category = EventCategories.PERFORMANCE,
                name = "indexing_complete",
                value = totalDuration,
                properties = properties
            )
        } catch (e: Exception) {
            logger.warn("Failed to send indexing complete telemetry", e)
            return false
        }
    }

    /**
     * 更新统计数据
     */
    private fun updateStats(operationType: String, duration: Long) {
        val stats = indexingStats.computeIfAbsent(operationType) { IndexingStats(it) }
        stats.totalTime.addAndGet(duration)
        stats.count.incrementAndGet()

        // 更新最大值
        var currentMax = stats.maxTime.get()
        while (duration > currentMax && !stats.maxTime.compareAndSet(currentMax, duration)) {
            currentMax = stats.maxTime.get()
        }

        // 更新最小值
        var currentMin = stats.minTime.get()
        while (duration < currentMin && !stats.minTime.compareAndSet(currentMin, duration)) {
            currentMin = stats.minTime.get()
        }
    }

    /**
     * 获取性能等级
     */
    private fun getPerformanceLevel(operationType: String, duration: Long): String {
        return when (operationType) {
            "file_indexing" -> when {
                duration < 100 -> "EXCELLENT"
                duration < 500 -> "GOOD"
                duration < 1000 -> "AVERAGE"
                duration < 3000 -> "SLOW"
                else -> "VERY_SLOW"
            }

            "project_indexing" -> when {
                duration < 5000 -> "EXCELLENT"
                duration < 15000 -> "GOOD"
                duration < 30000 -> "AVERAGE"
                duration < 60000 -> "SLOW"
                else -> "VERY_SLOW"
            }

            "psi_building" -> when {
                duration < 50 -> "EXCELLENT"
                duration < 200 -> "GOOD"
                duration < 500 -> "AVERAGE"
                duration < 1000 -> "SLOW"
                else -> "VERY_SLOW"
            }

            else -> when {
                duration < 1000 -> "EXCELLENT"
                duration < 5000 -> "GOOD"
                duration < 10000 -> "AVERAGE"
                duration < 30000 -> "SLOW"
                else -> "VERY_SLOW"
            }
        }
    }

    /**
     * 获取统计信息
     */
    fun getStats(): Map<String, IndexingStats> {
        return indexingStats.toMap()
    }

    /**
     * 清除统计信息
     */
    fun clearStats() {
        indexingStats.clear()
        activeIndexing.clear()
    }
}