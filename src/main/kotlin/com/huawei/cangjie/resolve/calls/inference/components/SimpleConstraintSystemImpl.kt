package com.huawei.cangjie.resolve.calls.inference.components

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.descriptors.TypeParameterDescriptor
import com.huawei.cangjie.resolve.calls.components.ClassicTypeSystemContextForCS
import com.huawei.cangjie.resolve.calls.inference.ConstraintSystemBuilder
import com.huawei.cangjie.resolve.calls.inference.model.NewConstraintSystemImpl
import com.huawei.cangjie.resolve.calls.inference.model.SimpleConstraintSystemConstraintPosition
import com.huawei.cangjie.resolve.calls.inference.model.TypeVariableFromCallableDescriptor
import com.huawei.cangjie.resolve.calls.inference.substitute

import com.huawei.cangjie.resolve.calls.results.SimpleConstraintSystem
import com.huawei.cangjie.types.TypeConstructorSubstitution
import com.huawei.cangjie.types.checker.CangJieTypeRefiner
import com.huawei.cangjie.types.model.*
import com.huawei.cangjie.types.util.asTypeProjection

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
