/*
 * Copyright 2024 LinQingYing. and contributors.
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

package com.linqingying.cangjie.diagnostics

import com.linqingying.cangjie.name.CallableId
import com.linqingying.cangjie.name.ClassId
import com.linqingying.cangjie.name.IClassId


sealed class MatchMissingCase {
    abstract val branchConditionText: String

    object Unknown : MatchMissingCase() {
        override fun toString(): String = "unknown"

        override val branchConditionText: String = "else"
    }

    sealed class ConditionTypeIsExpect(val typeOfDeclaration: String) : MatchMissingCase() {
        object SealedClass : ConditionTypeIsExpect("sealed class")
        object SealedInterface : ConditionTypeIsExpect("sealed interface")
        object Enum : ConditionTypeIsExpect("enum")

        override val branchConditionText: String = "else"

        override fun toString(): String = "unknown"
    }

    object NullIsMissing : MatchMissingCase() {
        override val branchConditionText: String = "null"
    }

    sealed class BooleanIsMissing(val value: Boolean) : MatchMissingCase() {
        object TrueIsMissing : BooleanIsMissing(true)
        object FalseIsMissing : BooleanIsMissing(false)

        override val branchConditionText: String = value.toString()
    }

    class IsTypeCheckIsMissing(val classId: IClassId, val isSingleton: Boolean) : MatchMissingCase() {
        override val branchConditionText: String = run {
            val fqName = classId.asSingleFqName().toString()
            if (isSingleton) fqName else "is $fqName"
        }

        override fun toString(): String {
            val className = classId.shortClassName
            val name = if (className.isSpecial) className.asString() else className.identifier
            return if (isSingleton) name else "is $name"
        }
    }
    class OtherCheckIsMissing : MatchMissingCase() {
        override val branchConditionText: String
            get() = "Other"
    }

    class EnumCheckIsMissing(val callableId: CallableId) : MatchMissingCase() {
        override val branchConditionText: String = callableId.asSingleFqName().toString()

        override fun toString(): String {
            return callableId.callableName.identifier
        }
    }
    class TupleCheckIsMissing(val callableId: CallableId) : MatchMissingCase() {
        override val branchConditionText: String = callableId.asSingleFqName().toString()

        override fun toString(): String {
            return callableId.callableName.identifier
        }
    }
    override fun toString(): String {
        return branchConditionText
    }
}
