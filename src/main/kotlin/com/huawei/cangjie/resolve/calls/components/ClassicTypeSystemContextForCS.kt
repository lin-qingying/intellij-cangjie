package com.huawei.cangjie.resolve.calls.components

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.resolve.calls.inference.components.EmptySubstitutor
import com.huawei.cangjie.resolve.calls.inference.components.NewTypeSubstitutor
import com.huawei.cangjie.resolve.calls.inference.components.NewTypeSubstitutorByConstructorMap
import com.huawei.cangjie.resolve.calls.inference.model.NewTypeVariable
import com.huawei.cangjie.types.*
import com.huawei.cangjie.types.checker.*
import com.huawei.cangjie.types.model.*


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
//
//    override fun createCapturedType(
//        constructorProjection: TypeArgumentMarker,
//        constructorSupertypes: List<CangJieTypeMarker>,
//        lowerType: CangJieTypeMarker?,
//        captureStatus: CaptureStatus
//    ): CapturedTypeMarker {
//        require(lowerType is UnwrappedType?, lowerType::errorMessage)
//        require(constructorProjection is TypeProjectionBase, constructorProjection::errorMessage)
//
//        @Suppress("UNCHECKED_CAST")
//        val newCapturedTypeConstructor = NewCapturedTypeConstructor(
//            constructorProjection,
//            constructorSupertypes as List<UnwrappedType>
//        )
//        return NewCapturedType(
//            captureStatus,
//            newCapturedTypeConstructor,
//            lowerType = lowerType
//        )
//    }

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

//    override fun createStubTypeForBuilderInference(typeVariable: TypeVariableMarker): StubTypeMarker {
//        return StubTypeForBuilderInference(
//            typeVariable.freshTypeConstructor() as NewTypeVariableConstructor,
//            typeVariable.defaultType().isMarkedNullable()
//        )
//    }
//
//    override fun createStubTypeForTypeVariablesInSubtyping(typeVariable: TypeVariableMarker): StubTypeMarker {
//        return StubTypeForTypeVariablesInSubtyping(
//            typeVariable.freshTypeConstructor() as NewTypeVariableConstructor,
//            typeVariable.defaultType().isMarkedNullable()
//        )
//    }
//
//    override fun TypeConstructorMarker.isTypeVariable(): Boolean {
//        return this is TypeVariableTypeConstructor
//    }
//
//    override fun TypeVariableTypeConstructorMarker.isContainedInInvariantOrContravariantPositions(): Boolean {
//        require(this is TypeVariableTypeConstructor)
//        return isContainedInInvariantOrContravariantPositions
//    }

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

//@Suppress("FunctionName")
//fun <CangJieBuiltIns> NewConstraintSystemImpl(
//    constraintInjector: ConstraintInjector,
//    builtIns: CangJieBuiltIns,
//    cangjieTypeRefiner: CangJieTypeRefiner,
//    languageVersionSettings: LanguageVersionSettings
//): NewConstraintSystemImpl {
//    return NewConstraintSystemImpl(constraintInjector, ClassicTypeSystemContextForCS(builtIns, cangjieTypeRefiner), languageVersionSettings)
//}
