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

package com.linqingying.cangjie.types

import com.linqingying.cangjie.resolve.scopes.MemberScope
import com.linqingying.cangjie.types.checker.CangJieTypeRefiner
import com.linqingying.cangjie.types.checker.NewTypeVariableConstructor
import com.linqingying.cangjie.types.error.ErrorScopeKind
import com.linqingying.cangjie.types.error.ErrorTypeKind
import com.linqingying.cangjie.types.model.StubTypeMarker


abstract class AbstractStubType(
    val originalTypeVariable: NewTypeVariableConstructor,
    override val isMarkedOption: Boolean
) : SimpleType() {
    override val memberScope: MemberScope =
        ErrorUtils.createErrorScope(ErrorScopeKind.STUB_TYPE_SCOPE, originalTypeVariable.toString())

    override val arguments: List<TypeProjection>
        get() = emptyList()

    override val attributes: TypeAttributes
        get() = TypeAttributes.Empty

    override fun replaceAttributes(newAttributes: TypeAttributes): SimpleType = this

    override fun makeOptionalAsSpecified(newNullability: Boolean): SimpleType {
        return if (newNullability == isMarkedOption) this else materialize(newNullability)
    }

    @TypeRefinement
    override fun refine(cangjieTypeRefiner: CangJieTypeRefiner) = this

    abstract fun materialize(newNullability: Boolean): AbstractStubType

    companion object {
        fun createConstructor(originalTypeVariable: NewTypeVariableConstructor) =
            ErrorUtils.createErrorTypeConstructor(ErrorTypeKind.STUB_TYPE, originalTypeVariable.toString())
    }
}

class StubTypeForTypeVariablesInSubtyping(
    originalTypeVariable: NewTypeVariableConstructor,
    isMarkedNullable: Boolean,
    override val constructor: TypeConstructor = createConstructor(originalTypeVariable)
) : AbstractStubType(originalTypeVariable, isMarkedNullable), StubTypeMarker {
    override fun materialize(newNullability: Boolean): AbstractStubType =
        StubTypeForTypeVariablesInSubtyping(originalTypeVariable, newNullability, constructor)

    override fun toString(): String {
        return "Stub (subtyping): $originalTypeVariable${if (isMarkedOption) "?" else ""}"
    }
}


class StubTypeForBuilderInference(
    originalTypeVariable: NewTypeVariableConstructor,
    isMarkedNullable: Boolean,
    override val constructor: TypeConstructor = createConstructor(originalTypeVariable)
) : AbstractStubType(originalTypeVariable, isMarkedNullable), StubTypeMarker {
    override fun materialize(newNullability: Boolean): AbstractStubType =
        StubTypeForBuilderInference(originalTypeVariable, newNullability, constructor)

    override val memberScope: MemberScope = originalTypeVariable.builtIns.anyType.memberScope

    override fun toString(): String {
        // BI means builder inference
        return "Stub (BI): $originalTypeVariable${if (isMarkedOption) "?" else ""}"
    }
}
