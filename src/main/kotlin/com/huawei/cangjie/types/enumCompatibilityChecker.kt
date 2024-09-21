package com.huawei.cangjie.types

import com.huawei.cangjie.config.LanguageFeature
import com.huawei.cangjie.descriptors.ClassDescriptor
import com.huawei.cangjie.descriptors.TypeParameterDescriptor
import com.huawei.cangjie.diagnostics.Errors
import com.huawei.cangjie.psi.CjElement
import com.huawei.cangjie.resolve.DescriptorUtils
import com.huawei.cangjie.resolve.calls.context.ResolutionContext
import com.huawei.cangjie.types.checker.SimpleClassicTypeSystemContext.isNothing
import com.huawei.cangjie.types.util.TypeUtils
import com.huawei.cangjie.types.util.isEnum
import com.huawei.cangjie.types.util.representativeUpperBound

fun checkEnumsForCompatibility(context: ResolutionContext<*>, reportOn: CjElement, typeA: CangJieType, typeB: CangJieType) {
    if (isIncompatibleEnums(typeA, typeB)) {
//        val diagnostic = if (context.languageVersionSettings.supportsFeature(LanguageFeature.ProhibitComparisonOfIncompatibleEnums)) {
//            Errors.INCOMPATIBLE_ENUM_COMPARISON_ERROR
//        } else {
//            Errors.INCOMPATIBLE_ENUM_COMPARISON
//        }
        val diagnostic  = Errors.INCOMPATIBLE_ENUM_COMPARISON
        context.trace.report(diagnostic.on(reportOn, typeA, typeB))
    }
}
private fun isIncompatibleEnums(typeA: CangJieType, typeB: CangJieType): Boolean {
    if (!typeA.isEnum() && !typeB.isEnum()) return false
    if (TypeUtils.isNullableType(typeA) && TypeUtils.isNullableType(typeB)) return false

    // TODO: remove this line once KT-30266 will be fixed
    // For now, this check is needed as isSubClass contains bug wrt Nothing
    if (typeA.isNothing() || typeB.isNothing()) return false

    val representativeTypeA = typeA.representativeTypeForTypeParameter()
    val representativeTypeB = typeB.representativeTypeForTypeParameter()

    val classA = representativeTypeA.constructor.declarationDescriptor as? ClassDescriptor ?: return false
    val classB = representativeTypeB.constructor.declarationDescriptor as? ClassDescriptor ?: return false

    return !DescriptorUtils.isSubclass(classA, classB) && !DescriptorUtils.isSubclass(classB, classA)
}
private fun CangJieType.representativeTypeForTypeParameter(): CangJieType {
    val descriptor = constructor.declarationDescriptor
    return if (descriptor is TypeParameterDescriptor) descriptor.representativeUpperBound else this
}
