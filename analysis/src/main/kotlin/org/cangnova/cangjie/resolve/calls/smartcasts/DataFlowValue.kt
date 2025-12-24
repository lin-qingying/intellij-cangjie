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

package org.cangnova.cangjie.resolve.calls.smartcasts

import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.ErrorUtils
import org.cangnova.cangjie.types.TypeUtils

import org.cangnova.cangjie.types.error.ErrorTypeKind

private val CangJieType.immanentNullability: Nullability
    get() = if (TypeUtils.isOptionType(this)) Nullability.UNKNOWN else Nullability.NOT_NULL

class DataFlowValue(
    val identifierInfo: IdentifierInfo,
    val type: CangJieType,
    val immanentNullability: Nullability = type.immanentNullability
) {

    val kind: Kind get() = identifierInfo.kind

    enum class Kind(private val str: String, val description: String = str) {
        // Local value, or parameter, or private / internal member value without open / custom getter,
        // or protected / public member value from the same module without open / custom getter
        // Smart casts are completely safe
        STABLE_VALUE("stable val"),

        // Block, or if / else, or when
        STABLE_COMPLEX_EXPRESSION("complex expression", ""),

        // Should be unstable, but can be used as stable with deprecation warning
        LEGACY_STABLE_LOCAL_DELEGATED_PROPERTY("local delegated property"),

        // Member value with open / custom getter
        // Smart casts are not safe
        PROPERTY_WITH_GETTER("custom getter", "property that has open or custom getter"),

        // Protected / public member value from derived class from another module
        // Should be unstable, but can be used as stable with deprecation warning
        LEGACY_ALIEN_BASE_PROPERTY("alien derived", "property declared in base class from different module"),

        // Protected / public member value from derived class from another module, in case the derived class is non-public API
        // Should be unstable, but can be used as stable with deprecation warning
        LEGACY_ALIEN_BASE_PROPERTY_INHERITED_IN_INVISIBLE_CLASS(
            "alien inherited in invisible",
            "property declared in base class from different module inherited in non-public API class"
        ),

        // Protected / public member value from another module
        // Smart casts are not safe
        ALIEN_PUBLIC_PROPERTY("alien public", "public API property declared in different module"),

        // Local variable not yet captured by a changing closure
        // Smart casts are safe but possible changes in loops / closures ahead must be taken into account
        STABLE_VARIABLE("stable var", "local variable that can be changed since the check in a loop"),

        // Local variable already captured by a changing closure
        // Smart casts are not safe
        CAPTURED_VARIABLE("captured var", "local variable that is captured by a changing closure"),

        // Member variable regardless of its visibility
        // Smart casts are not safe
        MUTABLE_PROPERTY("member", "mutable property that could have been changed by this time"),

        // Some complex expression
        // Smart casts are not safe
        OTHER("other", "complex expression");

        override fun toString() = str
    }

    val canBeBound get() = identifierInfo.canBeBound

    /**
     * Stable means here we do not expect some sudden change of their values,
     * like accessing mutable properties in another thread, so smart casts can be used safely.
     */
    val isStable = kind == Kind.STABLE_VALUE ||
            kind == Kind.STABLE_VARIABLE ||
            kind == Kind.STABLE_COMPLEX_EXPRESSION ||
            kind == Kind.LEGACY_STABLE_LOCAL_DELEGATED_PROPERTY ||
            kind == Kind.LEGACY_ALIEN_BASE_PROPERTY ||
            kind == Kind.LEGACY_ALIEN_BASE_PROPERTY_INHERITED_IN_INVISIBLE_CLASS
//
//    val canBeBound get() = identifierInfo.canBeBound

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DataFlowValue) return false

        if (identifierInfo != other.identifierInfo) return false
        if (type != other.type) return false

        return true
    }

    override fun toString() = "$kind $identifierInfo $immanentNullability"

    private var hashCode = 0

    override fun hashCode(): Int {
        var hashCode = hashCode

        if (hashCode == 0) {
            hashCode = type.hashCode() + 31 * identifierInfo.hashCode()
            this.hashCode = hashCode
        }

        return hashCode
    }

    companion object {

        
        fun nullValue(builtIns: CangJieBuiltIns) =
            DataFlowValue(IdentifierInfo.NULL, builtIns.nothingType, Nullability.NULL)

        
        val ERROR = DataFlowValue(
            IdentifierInfo.ERROR,
            ErrorUtils.createErrorType(ErrorTypeKind.ERROR_DATA_FLOW_TYPE),
            Nullability.IMPOSSIBLE
        )
    }
}
