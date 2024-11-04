package com.linqingying.cangjie.resolve.calls.inference.components

import com.linqingying.cangjie.builtins.CangJieBuiltIns
import com.linqingying.cangjie.config.LanguageVersionSettings
import com.linqingying.cangjie.descriptors.TypeParameterDescriptor
import com.linqingying.cangjie.resolve.calls.components.ClassicTypeSystemContextForCS
import com.linqingying.cangjie.resolve.calls.inference.ConstraintSystemBuilder
import com.linqingying.cangjie.resolve.calls.inference.model.NewConstraintSystemImpl
import com.linqingying.cangjie.resolve.calls.inference.model.SimpleConstraintSystemConstraintPosition
import com.linqingying.cangjie.resolve.calls.inference.model.TypeVariableFromCallableDescriptor
import com.linqingying.cangjie.resolve.calls.inference.substitute

import com.linqingying.cangjie.resolve.calls.results.SimpleConstraintSystem
import com.linqingying.cangjie.types.TypeConstructorSubstitution
import com.linqingying.cangjie.types.checker.CangJieTypeRefiner
import com.linqingying.cangjie.types.model.*
import com.linqingying.cangjie.types.util.asTypeProjection

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
                addSubtypeConstraint(substitutor.substitute(typeParameter.defaultType), substitutor.substitute(upperBound.unwrap()))
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
