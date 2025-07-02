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

package cn.cangnova.cangjie.resolve.calls.components

import cn.cangnova.cangjie.builtins.CangJieBuiltIns
import cn.cangnova.cangjie.builtins.functions.FunctionTypeKind
import cn.cangnova.cangjie.config.LanguageVersionSettings
import cn.cangnova.cangjie.resolve.calls.inference.components.ConstraintInjector
import cn.cangnova.cangjie.resolve.calls.inference.components.EmptySubstitutor
import cn.cangnova.cangjie.resolve.calls.inference.components.NewTypeSubstitutor
import cn.cangnova.cangjie.resolve.calls.inference.components.NewTypeSubstitutorByConstructorMap
import cn.cangnova.cangjie.resolve.calls.inference.model.NewConstraintSystemImpl
import cn.cangnova.cangjie.resolve.calls.inference.model.NewTypeVariable
import cn.cangnova.cangjie.resolve.calls.inference.model.TypeVariableTypeConstructor
import cn.cangnova.cangjie.types.*
import cn.cangnova.cangjie.types.checker.*
import cn.cangnova.cangjie.types.model.*


class ClassicTypeSystemContextForCS(
    override val builtIns: CangJieBuiltIns,
    val cangjieTypeRefiner: CangJieTypeRefiner
) : TypeSystemInferenceExtensionContextDelegate,
    ClassicTypeSystemContext,
    BuiltInsProvider {

    override fun TypeVariableMarker.defaultType(): SimpleTypeMarker {
        require(this is NewTypeVariable, this::errorMessage)
        return this.defaultType
    }


    override fun TypeVariableMarker.freshTypeConstructor(): TypeConstructorMarker {
        require(this is NewTypeVariable, this::errorMessage)
        return this.freshTypeConstructor
    }

    override fun createCapturedType(
        constructorProjection: TypeArgumentMarker,
        constructorSupertypes: List<CangJieTypeMarker>,
        lowerType: CangJieTypeMarker?,
        captureStatus: CaptureStatus
    ): CapturedTypeMarker {
        require(lowerType is UnwrappedType?, lowerType::errorMessage)
        require(constructorProjection is TypeProjectionBase, constructorProjection::errorMessage)

        @Suppress("UNCHECKED_CAST")
        val newCapturedTypeConstructor = NewCapturedTypeConstructor(
            constructorProjection,
            constructorSupertypes as List<UnwrappedType>
        )
        return NewCapturedType(
            captureStatus,
            newCapturedTypeConstructor,
            lowerType = lowerType
        )
    }

    override fun typeSubstitutorByTypeConstructor(map: Map<TypeConstructorMarker, CangJieTypeMarker>): TypeSubstitutorMarker {
        if (map.isEmpty()) return createEmptySubstitutor()
        @Suppress("UNCHECKED_CAST")
        return NewTypeSubstitutorByConstructorMap(map as Map<TypeConstructor, UnwrappedType>)
    }

    override fun createEmptySubstitutor(): TypeSubstitutorMarker {
        return EmptySubstitutor
    }

    override fun TypeSubstitutorMarker.safeSubstitute(type: CangJieTypeMarker): CangJieTypeMarker {
        require(type is CangJieType, type::errorMessage)
        val unwrappedType = type.unwrap()
        return when (this) {
            is NewTypeSubstitutor -> safeSubstitute(unwrappedType)
            is TypeSubstitutor -> safeSubstitute(unwrappedType, Variance.INVARIANT)
            else -> error(this.errorMessage())
        }
    }

    override fun createStubTypeForBuilderInference(typeVariable: TypeVariableMarker): StubTypeMarker {
        return StubTypeForBuilderInference(
            typeVariable.freshTypeConstructor() as NewTypeVariableConstructor,
            typeVariable.defaultType().isMarkedNullable()
        )
    }

    override fun createStubTypeForTypeVariablesInSubtyping(typeVariable: TypeVariableMarker): StubTypeMarker {
        return StubTypeForTypeVariablesInSubtyping(
            typeVariable.freshTypeConstructor() as NewTypeVariableConstructor,
            typeVariable.defaultType().isMarkedNullable()
        )
    }

    override fun TypeConstructorMarker.isTypeVariable(): Boolean {
        return this is TypeVariableTypeConstructor
    }

    override fun TypeVariableTypeConstructorMarker.isContainedInInvariantOrContravariantPositions(): Boolean {
        require(this is TypeVariableTypeConstructor)
        return isContainedInInvariantOrContravariantPositions
    }



    override fun newTypeCheckerState(errorTypesEqualToAnything: Boolean, stubTypesEqualToAnything: Boolean): TypeCheckerState {
        return createClassicTypeCheckerState(
            errorTypesEqualToAnything,
            stubTypesEqualToAnything,
            typeSystemContext = this,
            cangjieTypeRefiner = cangjieTypeRefiner
        )
    }
}

@Suppress("NOTHING_TO_INLINE")
private inline fun Any?.errorMessage(): String {
    return "ClassicTypeSystemContextForCS couldn't handle: $this, ${this?.let { it::class }}"
}

@Suppress("FunctionName")
fun   NewConstraintSystemImpl(
    constraintInjector: ConstraintInjector,
    builtIns: CangJieBuiltIns,
    cangjieTypeRefiner: CangJieTypeRefiner,
    languageVersionSettings: LanguageVersionSettings
): NewConstraintSystemImpl {
    return NewConstraintSystemImpl(constraintInjector, ClassicTypeSystemContextForCS(builtIns, cangjieTypeRefiner) , languageVersionSettings )
}
