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

import java.util.*

enum class CompilerMessageSeverity {
    EXCEPTION,
    ERROR,

    /**
     * Unlike a normal warning, a strong warning is not discarded when there are compilation errors.
     * Use it for problems related to configuration, not the diagnostics.
     */
    STRONG_WARNING,
    WARNING,
    INFO,
    LOGGING,

    /**
     * Source to output files mapping messages (e.g A.Cj->A.klass).
     * It is needed for incremental compilation.
     */
    OUTPUT;

    val isError: Boolean
        get() = this == EXCEPTION || this == ERROR

    val isWarning: Boolean
        get() = this == STRONG_WARNING || this == WARNING

    val presentableName: String
        get() = when (this) {
            EXCEPTION -> "exception"
            ERROR -> "error"
            STRONG_WARNING, WARNING -> "warning"
            INFO -> "info"
            LOGGING -> "logging"
            OUTPUT -> "output"
        }

    companion object {
        @JvmField
        val VERBOSE: EnumSet<CompilerMessageSeverity> = EnumSet.of(LOGGING)
    }
}
