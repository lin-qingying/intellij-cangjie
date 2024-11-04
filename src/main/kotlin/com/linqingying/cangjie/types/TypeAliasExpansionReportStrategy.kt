package com.linqingying.cangjie.types

import com.linqingying.cangjie.descriptors.TypeAliasDescriptor
import com.linqingying.cangjie.descriptors.TypeParameterDescriptor
import com.linqingying.cangjie.descriptors.annotations.AnnotationDescriptor


interface TypeAliasExpansionReportStrategy {
    fun wrongNumberOfTypeArguments(typeAlias: TypeAliasDescriptor, numberOfParameters: Int)
    fun conflictingProjection(typeAlias: TypeAliasDescriptor, typeParameter: TypeParameterDescriptor?, substitutedArgument: CangJieType)
    fun recursiveTypeAlias(typeAlias: TypeAliasDescriptor)
    fun boundsViolationInSubstitution(
        substitutor: TypeSubstitutor,
        unsubstitutedArgument: CangJieType,
        argument: CangJieType,
        typeParameter: TypeParameterDescriptor
    )

    fun repeatedAnnotation(annotation: AnnotationDescriptor)

    object DO_NOTHING : TypeAliasExpansionReportStrategy {
        override fun wrongNumberOfTypeArguments(typeAlias: TypeAliasDescriptor, numberOfParameters: Int) {}
        override fun conflictingProjection(
            typeAlias: TypeAliasDescriptor,
            typeParameter: TypeParameterDescriptor?,
            substitutedArgument: CangJieType
        ) {
        }

        override fun recursiveTypeAlias(typeAlias: TypeAliasDescriptor) {}
        override fun boundsViolationInSubstitution(
            substitutor: TypeSubstitutor,
            unsubstitutedArgument: CangJieType,
            argument: CangJieType,
            typeParameter: TypeParameterDescriptor
        ) {
        }

        override fun repeatedAnnotation(annotation: AnnotationDescriptor) {}
    }
}
