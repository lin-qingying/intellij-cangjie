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

package org.cangnova.cangjie.resolve.calls.results

import java.util.EnumSet

enum class ResolutionStatus(private val success: Boolean = false) {
    UNKNOWN_STATUS,
    UNSAFE_CALL_ERROR,
    WRONG_NUMBER_OF_TYPE_ARGUMENTS_ERROR,
    UNSTABLE_SMARTCAST_FOR_RECEIVER_ERROR,
    INVISIBLE_MEMBER_ERROR,
    NULLABLE_ARGUMENT_TYPE_MISMATCH,
    OTHER_ERROR,
    ARGUMENTS_MAPPING_ERROR,

    // '1.foo()' shouldn't be resolved to 'fun String.foo()'
    // candidates with such error are treated specially
    // (are mentioned in 'unresolved' error, if there are no other options)
    RECEIVER_TYPE_ERROR,

    // 'a.foo()' shouldn't be resolved to package level non-extension 'fun foo()'
    // candidates with such error are thrown away completely
    RECEIVER_PRESENCE_ERROR,
    INCOMPLETE_TYPE_INFERENCE,
    SUCCESS(true);

    private var severityIndex: Int = -1

val isSuccess: Boolean get() = success
    fun possibleTransformToSuccess(): Boolean =
        this == UNKNOWN_STATUS || this == INCOMPLETE_TYPE_INFERENCE || this == SUCCESS

    fun combine(other: ResolutionStatus): ResolutionStatus {
        if (this == UNKNOWN_STATUS) return other
        if (SUCCESS.among(this, other)) {
            return SUCCESS.chooseDifferent(this, other)
        }
        if (INCOMPLETE_TYPE_INFERENCE.among(this, other)) {
            return INCOMPLETE_TYPE_INFERENCE.chooseDifferent(this, other)
        }
        if (this.getSeverityIndex() < other.getSeverityIndex()) return other
        return this
    }

    private fun among(first: ResolutionStatus, second: ResolutionStatus): Boolean =
        this == first || this == second

    private fun chooseDifferent(first: ResolutionStatus, second: ResolutionStatus): ResolutionStatus {
        assert(among(first, second))
        return if (this == first) second else first
    }

    private fun getSeverityIndex(): Int {
        if (severityIndex == -1) {
            for (i in SEVERITY_LEVELS.indices) {
                if (SEVERITY_LEVELS[i].contains(this)) {
                    severityIndex = i
                    break
                }
            }
        }
        assert(severityIndex >= 0)

        return severityIndex
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        val SEVERITY_LEVELS: Array<EnumSet<ResolutionStatus>> = arrayOf(
            EnumSet.of(UNSAFE_CALL_ERROR), // weakest
            EnumSet.of(WRONG_NUMBER_OF_TYPE_ARGUMENTS_ERROR),
            EnumSet.of(UNSTABLE_SMARTCAST_FOR_RECEIVER_ERROR),
            EnumSet.of(INVISIBLE_MEMBER_ERROR),
            EnumSet.of(NULLABLE_ARGUMENT_TYPE_MISMATCH),
            EnumSet.of(OTHER_ERROR),
            EnumSet.of(ARGUMENTS_MAPPING_ERROR),
            EnumSet.of(RECEIVER_TYPE_ERROR),
            EnumSet.of(RECEIVER_PRESENCE_ERROR), // most severe
        )
    }
}