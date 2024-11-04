package com.linqingying.cangjie.types

import com.linqingying.cangjie.builtins.CangJieBuiltIns
import com.linqingying.cangjie.descriptors.ClassifierDescriptorWithTypeParameters
import com.linqingying.cangjie.descriptors.FunctionDescriptor
import com.linqingying.cangjie.descriptors.TypeParameterDescriptor
import com.linqingying.cangjie.resolve.descriptorUtil.builtIns
import com.linqingying.cangjie.types.checker.CangJieTypeRefiner
import com.linqingying.cangjie.types.util.TypeUtils


//class StarProjectionImpl(
//    private val typeParameter: TypeParameterDescriptor
//) : TypeProjectionBase() {
//    override fun isStarProjection() = true
//
//    override fun getProjectionKind() = Variance.OUT_VARIANCE
//
//    // No synchronization here: there's no problem in accidentally computing this twice
//    private val _type: CangJieType by lazy(LazyThreadSafetyMode.PUBLICATION) {
//        typeParameter.starProjectionType()
//    }
//
//    override fun getType() = _type
//
//    @TypeRefinement
//    override fun refine(CangJieTypeRefiner: CangJieTypeRefiner): TypeProjection = this
//
//    override fun replaceType(type: CangJieType): TypeProjection {
//        throw UnsupportedOperationException("Replacing type for star projection is unsupported")
//    }
//}
//
//private fun buildStarProjectionTypeByTypeParameters(
//    typeParameters: List<TypeConstructor>,
//    upperBounds: List<CangJieType>,
//    builtIns: CangJieBuiltIns
//) = TypeSubstitutor.create(
//    object : TypeConstructorSubstitution() {
//        override fun get(key: TypeConstructor) =
//            if (key in typeParameters)
//                TypeUtils.makeStarProjection(key.declarationDescriptor as TypeParameterDescriptor)
//            else null
//
//    }
//).substitute(upperBounds.first(), Variance.OUT_VARIANCE) ?: builtIns.defaultBound
//
//fun TypeParameterDescriptor.starProjectionType(): CangJieType {
//    return when (val descriptor = this.containingDeclaration) {
//        is ClassifierDescriptorWithTypeParameters -> {
//            buildStarProjectionTypeByTypeParameters(
//                typeParameters = descriptor.typeConstructor.parameters.map { it.typeConstructor },
//                upperBounds,
//                builtIns
//            )
//        }
//        is FunctionDescriptor -> {
//            buildStarProjectionTypeByTypeParameters(
//                typeParameters = descriptor.typeParameters.map { it.typeConstructor },
//                upperBounds,
//                builtIns
//            )
//        }
//        else -> throw IllegalArgumentException("Unsupported descriptor type to build star projection type based on type parameters of it")
//    }
//}

// It should only be used in rare cases when type parameter for the relevant argument is not available
//class StarProjectionForAbsentTypeParameter(
//    CangJieBuiltIns: CangJieBuiltIns
//) : TypeProjectionBase() {
//    private val nullableAnyType: CangJieType = CangJieBuiltIns.nullableAnyType
//
//    override fun isStarProjection() = true
//
//    override fun getProjectionKind() = Variance.OUT_VARIANCE
//
//    override fun getType() = nullableAnyType
//
//    @TypeRefinement
//    override fun refine(CangJieTypeRefiner: CangJieTypeRefiner): TypeProjection = this
//
//    override fun replaceType(type: CangJieType): TypeProjection {
//        throw UnsupportedOperationException("Replacing type for star projection is unsupported")
//    }
//}
