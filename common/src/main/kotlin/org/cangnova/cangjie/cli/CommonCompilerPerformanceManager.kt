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

package org.cangnova.cangjie.cli

import org.cangnova.cangjie.cli.messages.*
import org.cangnova.cangjie.utils.PerformanceCounter
import java.io.File
import java.lang.management.GarbageCollectorMXBean
import java.lang.management.ManagementFactory
import java.util.concurrent.TimeUnit

abstract class CommonCompilerPerformanceManager(private val presentableName: String) {
    @Suppress("MemberVisibilityCanBePrivate")
    protected val measurements: MutableList<PerformanceMeasurement> = mutableListOf()
    protected var isEnabled: Boolean = false
    private var initStartNanos = PerformanceCounter.Companion.currentTime()
    private var analysisStart: Long = 0
    private var generationStart: Long = 0

    private var startGCData = mutableMapOf<String, GCData>()

    private var irTranslationStart: Long = 0
    private var irLoweringStart: Long = 0
    private var irGenerationStart: Long = 0

    private var targetDescription: String? = null
    protected var files: Int? = null
    protected var lines: Int? = null

    fun getTargetInfo(): String =
        "$targetDescription, $files files ($lines lines)"

    fun getMeasurementResults(): List<PerformanceMeasurement> = measurements

    fun enableCollectingPerformanceStatistics() {
        isEnabled = true
        PerformanceCounter.Companion.setTimeCounterEnabled(true)
        ManagementFactory.getGarbageCollectorMXBeans().associateTo(startGCData) { it.name to GCData(it) }
    }

    private fun deltaTime(start: Long): Long = PerformanceCounter.Companion.currentTime() - start

    open fun notifyCompilerInitialized(files: Int, lines: Int, targetDescription: String) {
        if (!isEnabled) return
        recordInitializationTime()

        this.files = files
        this.lines = lines
        this.targetDescription = targetDescription
    }

    open fun notifyCompilationFinished() {
        if (!isEnabled) return
        recordGcTime()
        recordJitCompilationTime()
        recordPerfCountersMeasurements()
    }

    open fun addSourcesStats(files: Int, lines: Int) {
        if (!isEnabled) return
        this.files = this.files?.plus(files) ?: files
        this.lines = this.lines?.plus(lines) ?: lines
    }

    open fun notifyAnalysisStarted() {
        analysisStart = PerformanceCounter.Companion.currentTime()
    }

    open fun notifyAnalysisFinished() {
        val time = PerformanceCounter.Companion.currentTime() - analysisStart
        measurements .plusAssign (CodeAnalysisMeasurement(lines, TimeUnit.NANOSECONDS.toMillis(time)))
    }

    open fun notifyGenerationStarted() {
        generationStart = PerformanceCounter.Companion.currentTime()
    }

    open fun notifyGenerationFinished() {
        val time = PerformanceCounter.Companion.currentTime() - generationStart
        measurements .plusAssign( CodeGenerationMeasurement(lines, TimeUnit.NANOSECONDS.toMillis(time)))
    }

    open fun notifyIRTranslationStarted() {
        irTranslationStart = PerformanceCounter.Companion.currentTime()
    }

    open fun notifyIRTranslationFinished() {
        val time = deltaTime(irTranslationStart)
        measurements. plusAssign (IRMeasurement(
            lines,
            TimeUnit.NANOSECONDS.toMillis(time),
            IRMeasurement.Kind.TRANSLATION
        ))
    }

    open fun notifyIRLoweringStarted() {
        irLoweringStart = PerformanceCounter.Companion.currentTime()
    }

    open fun notifyIRLoweringFinished() {
        val time = deltaTime(irLoweringStart)
        measurements .plusAssign (IRMeasurement(
            lines,
            TimeUnit.NANOSECONDS.toMillis(time),
            IRMeasurement.Kind.LOWERING
        ))
    }

    open fun notifyIRGenerationStarted() {
        irGenerationStart = PerformanceCounter.Companion.currentTime()
    }

    open fun notifyIRGenerationFinished() {
        val time = deltaTime(irGenerationStart)
        measurements .plusAssign (
                IRMeasurement(
                    lines,
                    TimeUnit.NANOSECONDS.toMillis(time),
                    IRMeasurement.Kind.GENERATION
                )
        )
    }

    fun dumpPerformanceReport(destination: File) {
        destination.writeBytes(createPerformanceReport())
    }

    private fun recordGcTime() {
        if (!isEnabled) return

        ManagementFactory.getGarbageCollectorMXBeans().forEach {
            val startCounts = startGCData[it.name]
            val startCollectionTime = startCounts?.collectionTime ?: 0
            val startCollectionCount = startCounts?.collectionCount ?: 0
            measurements. plusAssign (GarbageCollectionMeasurement(
                it.name,
                it.collectionTime - startCollectionTime,
                it.collectionCount - startCollectionCount
            ))
        }
    }

    private fun recordJitCompilationTime() {
        if (!isEnabled) return

        val bean = ManagementFactory.getCompilationMXBean() ?: return
        measurements .plusAssign (JitCompilationMeasurement(bean.totalCompilationTime))
    }

    private fun recordInitializationTime() {
        val time = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - initStartNanos)
        measurements .plusAssign (CompilerInitializationMeasurement(time))
    }

    private fun recordPerfCountersMeasurements() {
        PerformanceCounter.Companion.report { s -> measurements. plusAssign(PerformanceCounterMeasurement(s) ) }
    }

    private fun createPerformanceReport(): ByteArray = buildString {
        append("$presentableName performance report\n")
        measurements.map { it.render() }.sorted().forEach { append("$it\n") }
    }.toByteArray()

    private data class GCData(val name: String, val collectionTime: Long, val collectionCount: Long) {
        constructor(bean: GarbageCollectorMXBean) : this(bean.name, bean.collectionTime, bean.collectionCount)
    }

    fun renderCompilerPerformance(): String {
        val relevantMeasurements = getMeasurementResults().filter {
            it is CompilerInitializationMeasurement || it is CodeAnalysisMeasurement || it is CodeGenerationMeasurement || it is PerformanceCounterMeasurement
        }

        return "Compiler perf stats:\n" + relevantMeasurements.joinToString(separator = "\n") { "  ${it.render()}" }
    }
}