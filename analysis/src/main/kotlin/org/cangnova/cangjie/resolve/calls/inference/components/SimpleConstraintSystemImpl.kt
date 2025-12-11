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

package org.cangnova.cangjie.resolve.calls.inference.components

import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.TypeParameterDescriptor
import org.cangnova.cangjie.resolve.calls.components.ClassicTypeSystemContextForCS
import org.cangnova.cangjie.resolve.calls.inference.ConstraintSystemBuilder
import org.cangnova.cangjie.resolve.calls.inference.model.NewConstraintSystemImpl
import org.cangnova.cangjie.resolve.calls.inference.model.SimpleConstraintSystemConstraintPosition
import org.cangnova.cangjie.resolve.calls.inference.model.TypeVariableFromCallableDescriptor
import org.cangnova.cangjie.resolve.calls.inference.substitute

import org.cangnova.cangjie.resolve.calls.results.SimpleConstraintSystem
import org.cangnova.cangjie.types.TypeConstructorSubstitution
import org.cangnova.cangjie.types.asTypeProjection
import org.cangnova.cangjie.types.checker.CangJieTypeRefiner
import org.cangnova.cangjie.types.model.*


class SimpleConstraintSystemImpl(
    constraintInjector: ConstraintInjector,
    builtIns: CangJieBuiltIns,
    cangjieTypeRefiner: CangJieTypeRefiner,
    languageVersionSettings: LanguageVersionSettings
) : SimpleConstraintSystem {

    val system = NewConstraintSystemImpl(
        constraintInjector, ClassicTypeSystemContextForCS(builtIns, cangjieTypeRefiner), languageVersionSettings
    )
    val csBuilder: ConstraintSystemBuilder =
        system.getBuilder()

    override fun registerTypeVariables(typeParameters: Collection<TypeParameterMarker>): TypeSubstitutorMarker {
        val substitutionMap = typeParameters.associate {
            requireOrDescribe(it is TypeParameterDescriptor, it)
            val variable = TypeVariableFromCallableDescriptor(it)
            csBuilder.registerVariable(variable)

            it.defaultType.constructor to variable.defaultType.asTypeProjection()
        }
        val substitutor = TypeConstructorSubstitution.createByConstructorsMap(substitutionMap).buildSubstitutor()
        for (typeParameter in typeParameters) {
            requireOrDescribe(typeParameter is TypeParameterDescriptor, typeParameter)
            for (upperBound in typeParameter.upperBounds) {
                addSubtypeConstraint(
                    substitutor.substitute(typeParameter.defaultType),
                    substitutor.substitute(upperBound.unwrap())
                )
            }
        }
        return substitutor
    }

    override fun addSubtypeConstraint(subType: CangJieTypeMarker, superType: CangJieTypeMarker) {
        csBuilder.addSubtypeConstraint(
            subType,
            superType,
            SimpleConstraintSystemConstraintPosition
        )
    }

    override fun hasContradiction(): Boolean = csBuilder.hasContradiction

    override val context: TypeSystemInferenceExtensionContext
        get() = system
}
