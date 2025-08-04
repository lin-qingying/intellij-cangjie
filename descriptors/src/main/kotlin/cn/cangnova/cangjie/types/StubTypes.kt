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

package cn.cangnova.cangjie.types

import cn.cangnova.cangjie.resolve.scopes.MemberScope
import cn.cangnova.cangjie.types.checker.CangJieTypeRefiner
import cn.cangnova.cangjie.types.checker.NewTypeVariableConstructor
import cn.cangnova.cangjie.types.error.ErrorScopeKind
import cn.cangnova.cangjie.types.error.ErrorTypeKind
import cn.cangnova.cangjie.types.model.StubTypeMarker


abstract class AbstractStubType(
    val originalTypeVariable: NewTypeVariableConstructor,
    override val isOption: Boolean
) : SimpleType() {
    override val memberScope: MemberScope =
        ErrorUtils.createErrorScope(ErrorScopeKind.STUB_TYPE_SCOPE, originalTypeVariable.toString())

    override val arguments: List<TypeProjection>
        get() = emptyList()

    override val attributes: TypeAttributes
        get() = TypeAttributes.Empty

    override fun replaceAttributes(newAttributes: TypeAttributes): SimpleType = this

    override fun makeOptionalAsSpecified(newOption: Boolean): SimpleType {
        return if (newOption == isOption) this else materialize(newOption)
    }

    @TypeRefinement
    override fun refine(cangjieTypeRefiner: CangJieTypeRefiner) = this

    abstract fun materialize(newOption: Boolean): AbstractStubType

    companion object {
        fun createConstructor(originalTypeVariable: NewTypeVariableConstructor) =
            ErrorUtils.createErrorTypeConstructor(ErrorTypeKind.STUB_TYPE, originalTypeVariable.toString())
    }
}

class StubTypeForTypeVariablesInSubtyping(
    originalTypeVariable: NewTypeVariableConstructor,
    isOption: Boolean,
    override val constructor: TypeConstructor = createConstructor(originalTypeVariable)
) : AbstractStubType(originalTypeVariable, isOption), StubTypeMarker {
    override fun materialize(newOption: Boolean): AbstractStubType =
        StubTypeForTypeVariablesInSubtyping(originalTypeVariable, newOption, constructor)

    override fun toString(): String {
        return "Stub (subtyping): $originalTypeVariable${if (isOption) "?" else ""}"
    }
}


class StubTypeForBuilderInference(
    originalTypeVariable: NewTypeVariableConstructor,
    isOption: Boolean,
    override val constructor: TypeConstructor = createConstructor(originalTypeVariable)
) : AbstractStubType(originalTypeVariable, isOption), StubTypeMarker {
    override fun materialize(newOption: Boolean): AbstractStubType =
        StubTypeForBuilderInference(originalTypeVariable, newOption, constructor)

    override val memberScope: MemberScope = originalTypeVariable.builtIns.anyType.memberScope

    override fun toString(): String {
        // BI means builder inference
        return "Stub (BI): $originalTypeVariable${if (isOption) "?" else ""}"
    }
}
