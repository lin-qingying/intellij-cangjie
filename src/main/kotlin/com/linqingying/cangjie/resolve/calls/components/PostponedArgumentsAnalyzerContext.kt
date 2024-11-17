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

package com.linqingying.cangjie.resolve.calls.components

import com.linqingying.cangjie.resolve.calls.inference.ConstraintSystemBuilder
import com.linqingying.cangjie.resolve.calls.inference.model.ConstraintStorage
import com.linqingying.cangjie.resolve.calls.inference.model.VariableWithConstraints
import com.linqingying.cangjie.types.model.*

interface PostponedArgumentsAnalyzerContext : TypeSystemInferenceExtensionContext{

    val notFixedTypeVariables: Map<TypeConstructorMarker, VariableWithConstraints>

    fun buildCurrentSubstitutor(additionalBindings: Map<TypeConstructorMarker, CangJieTypeMarker>): TypeSubstitutorMarker
    fun buildNotFixedVariablesToStubTypesSubstitutor(): TypeSubstitutorMarker
    fun bindingStubsForPostponedVariables(): Map<TypeVariableMarker, StubTypeMarker>

    // type can be proper if it not contains not fixed type variables
    fun canBeProper(type: CangJieTypeMarker): Boolean

    fun hasUpperOrEqualUnitConstraint(type: CangJieTypeMarker): Boolean

    fun removePostponedTypeVariablesFromConstraints(postponedTypeVariables: Set<TypeConstructorMarker>)

    // mutable operations
    fun addOtherSystem(otherSystem: ConstraintStorage)

    fun getBuilder(): ConstraintSystemBuilder
    fun resolveForkPointsConstraints()
}
