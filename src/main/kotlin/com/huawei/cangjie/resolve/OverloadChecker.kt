package com.huawei.cangjie.resolve

import com.huawei.cangjie.descriptors.CallableDescriptor
import com.huawei.cangjie.descriptors.DeclarationDescriptor

class OverloadChecker {
//    private fun checkOverloadability(a: CallableDescriptor, b: CallableDescriptor): Boolean {
//        if (a.hasLowPriorityInOverloadResolution() != b.hasLowPriorityInOverloadResolution()) return true
//
//        // NB this makes generic and non-generic declarations with equivalent signatures non-conflicting
//        // E.g., 'fun <T> foo()' and 'fun foo()'.
//        // They can be disambiguated by providing explicit type parameters.
//        if (a.typeParameters.isEmpty() != b.typeParameters.isEmpty()) return true
//
//        if (a is FunctionDescriptor && ErrorUtils.containsErrorTypeInParameters(a) ||
//            b is FunctionDescriptor && ErrorUtils.containsErrorTypeInParameters(b)
//        ) return true
//        if (a.varargParameterPosition() != b.varargParameterPosition()) return true
//
//        val aSignature = FlatSignature.createFromCallableDescriptor(a)
//        val bSignature = FlatSignature.createFromCallableDescriptor(b)
//
//        val aIsNotLessSpecificThanB = ConstraintSystemBuilderImpl.forSpecificity()
//            .isSignatureNotLessSpecific(aSignature, bSignature, OverloadabilitySpecificityCallbacks, specificityComparator)
//        val bIsNotLessSpecificThanA = ConstraintSystemBuilderImpl.forSpecificity()
//            .isSignatureNotLessSpecific(bSignature, aSignature, OverloadabilitySpecificityCallbacks, specificityComparator)
//
//        return !(aIsNotLessSpecificThanB && bIsNotLessSpecificThanA)
//    }


    /**
     * Does not check names.
     */
//    fun isOverloadable(a: DeclarationDescriptor, b: DeclarationDescriptor): Boolean {
//        val aCategory = getDeclarationCategory(a)
//        val bCategory = getDeclarationCategory(b)
//
//        if (aCategory != bCategory) return true
//        if (a !is CallableDescriptor || b !is CallableDescriptor) return false
//
//        return checkOverloadability(a, b)
//    }
}
