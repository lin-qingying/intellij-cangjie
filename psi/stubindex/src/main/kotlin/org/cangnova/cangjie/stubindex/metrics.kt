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

package org.cangnova.cangjie.stubindex


import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.stubs.StubIndexKey
import org.cangnova.cangjie.utils.isInternal
import org.cangnova.cangjie.utils.isUnitTestMode
import org.cangnova.telemetry.performance.IndexingPerformanceTelemetry
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.TimeSource
import kotlin.time.toDuration

fun getByKeyMaxDuration(): Duration =
    Registry.intValue("cangjie.indices.timing.threshold.single").toDuration(DurationUnit.MILLISECONDS)


inline fun <T> getByKeyAndMeasure(index: StubIndexKey<*, *>, log: Logger, crossinline block: () -> T): T =
    measureIndexCall(index, "getByKey", getByKeyMaxDuration(), log, block)


inline fun <T> measureIndexCall(
    index: StubIndexKey<*, *>,
    prefix: String,
    threshold: Duration,
    log: Logger,
    crossinline block: () -> T
): T {
    val operationId = IndexingPerformanceTelemetry.startIndexing("stub_index_${prefix}")
    val mark = TimeSource.Monotonic.markNow()

    try {
        val t = block()
        val elapsed = mark.elapsedNow()
        val elapsedMs = elapsed.inWholeMilliseconds

        // 发送性能遥测事件
        IndexingPerformanceTelemetry.endIndexing(
            operationId,
            // Stub索引操作通常不直接对应文件数量
            additionalInfo = mapOf(
                "index_name" to index.name,
                "operation_type" to prefix,
                "threshold_ms" to threshold.inWholeMilliseconds.toString()
            )
        )

        if (elapsed > threshold) {
            if (isInternal && !isUnitTestMode && Registry.`is`("cangjie.indices.timing.enabled")) {
                log.error("${index.name} $prefix took $elapsed more than expected $threshold")
            }

            // 发送性能警告遥测事件
            org.cangnova.telemetry.error.ErrorTelemetry.sendPerformanceWarning(
                "stub_index_operation",
                elapsedMs,
                threshold.inWholeMilliseconds,
                "stub_index_${prefix}",
                mapOf(
                    "index_name" to index.name,
                    "operation_type" to prefix
                )
            )
        }

        return t
    } catch (e: Exception) {
        // 发送索引错误遥测事件
        IndexingPerformanceTelemetry.sendIndexingError(
            "stub_index_${prefix}",
            "Error during ${index.name} $prefix operation",
            e,
            mapOf(
                "index_name" to index.name,
                "operation_type" to prefix
            )
        )
        throw e
    }
}

inline fun <T> processElementsAndMeasure(index: StubIndexKey<*, *>, log: Logger, crossinline block: () -> T): T =
    measureIndexCall(
        index,
        "processElements",
        processElementsMaxDuration(),
        log,
        block
    )


inline fun <T> getAllKeysAndMeasure(index: StubIndexKey<*, *>, log: Logger, crossinline block: () -> T): T =
    measureIndexCall(index, "getAllKeys", processElementsMaxDuration(), log, block)


fun processElementsMaxDuration(): Duration =
    Registry.intValue("cangjie.indices.timing.threshold.batch").toDuration(DurationUnit.MILLISECONDS)


inline fun <T> processAllKeysAndMeasure(index: StubIndexKey<*, *>, log: Logger, crossinline block: () -> T): T =
    measureIndexCall(index, "processAllKeys", processElementsMaxDuration(), log, block)


