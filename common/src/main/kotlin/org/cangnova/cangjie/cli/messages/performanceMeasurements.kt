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

package org.cangnova.cangjie.cli.messages


interface PerformanceMeasurement {
    fun render(): String
}

class JitCompilationMeasurement(private val milliseconds: Long) : PerformanceMeasurement {
    override fun render(): String = "JIT time is $milliseconds ms"
}

class CompilerInitializationMeasurement(val milliseconds: Long) : PerformanceMeasurement {
    override fun render(): String = "INIT: Compiler initialized in $milliseconds ms"
}

class CodeAnalysisMeasurement(val lines: Int?, val milliseconds: Long) : PerformanceMeasurement {
    override fun render(): String = formatMeasurement("ANALYZE", milliseconds, lines)
}

class CodeGenerationMeasurement(val lines: Int?, val milliseconds: Long) : PerformanceMeasurement {
    override fun render(): String = formatMeasurement("GENERATE", milliseconds, lines)
}

class GarbageCollectionMeasurement(val garbageCollectionKind: String, val milliseconds: Long, val count: Long) : PerformanceMeasurement {
    override fun render(): String = "GC time for $garbageCollectionKind is $milliseconds ms, $count collections"
}

class PerformanceCounterMeasurement(private val counterReport: String) : PerformanceMeasurement {
    override fun render(): String = counterReport
}

class IRMeasurement(val lines: Int?, val milliseconds: Long, val kind: Kind) : PerformanceMeasurement {
    override fun render(): String = formatMeasurement("IR $kind", milliseconds, lines)

    enum class Kind {
        TRANSLATION, LOWERING, GENERATION
    }
}

private fun formatMeasurement(name: String, time: Long, lines: Int?): String =
    "%15s%8s ms".format(name, time) +
            (lines?.let {
                val lps = it.toDouble() * 1000 / time
                "%12.3f loc/s".format(lps)
            } ?: "")
