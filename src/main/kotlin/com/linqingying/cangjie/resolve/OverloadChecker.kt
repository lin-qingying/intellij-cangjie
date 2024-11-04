package com.linqingying.cangjie.resolve

import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.resolve.calls.inference.ConstraintSystemBuilderImpl
import com.linqingying.cangjie.resolve.calls.results.*
import com.linqingying.cangjie.types.ErrorUtils

class OverloadChecker(val specificityComparator: TypeSpecificityComparator) {

    private fun checkOverloadability(a: CallableDescriptor, b: CallableDescriptor): Boolean {

//        // NB this makes generic and non-generic declarations with equivalent signatures non-conflicting
//        // E.g., 'fun <T> foo()' and 'fun foo()'.
//        // They can be disambiguated by providing explicit type parameters.
        if (a.typeParameters.isEmpty() != b.typeParameters.isEmpty()) return true
//
        if (a is FunctionDescriptor && ErrorUtils.containsErrorTypeInParameters(a) ||
            b is FunctionDescriptor && ErrorUtils.containsErrorTypeInParameters(b)
        ) return true
//        if (a.varargParameterPosition() != b.varargParameterPosition()) return true
//
        val aSignature = FlatSignature.createFromCallableDescriptor(a)
        val bSignature = FlatSignature.createFromCallableDescriptor(b)
//
        val aIsNotLessSpecificThanB = ConstraintSystemBuilderImpl.forSpecificity()
            .isSignatureNotLessSpecific(aSignature, bSignature, OverloadabilitySpecificityCallbacks, specificityComparator)
        val bIsNotLessSpecificThanA = ConstraintSystemBuilderImpl.forSpecificity()
            .isSignatureNotLessSpecific(bSignature, aSignature, OverloadabilitySpecificityCallbacks, specificityComparator)
//
        return !(aIsNotLessSpecificThanB && bIsNotLessSpecificThanA)

    }

    private enum class DeclarationCategory {
        TYPE_OR_VALUE,
        FUNCTION,
        EXTENSION_PROPERTY
    }

    private fun getDeclarationCategory(a: DeclarationDescriptor): DeclarationCategory =
        when (a) {
            is PropertyDescriptor ->
//                if (a.isExtensionProperty)
//                    DeclarationCategory.EXTENSION_PROPERTY
//                else
                    DeclarationCategory.TYPE_OR_VALUE

            is FunctionDescriptor ->
                DeclarationCategory.FUNCTION
            is VariableDescriptor ->
                DeclarationCategory.TYPE_OR_VALUE

            is ClassifierDescriptor ->
                DeclarationCategory.TYPE_OR_VALUE

            else ->
                error("Unexpected declaration kind: $a")
        }

    /**
     * Does not check names.
     */
    fun isOverloadable(a: DeclarationDescriptor, b: DeclarationDescriptor): Boolean {
        val aCategory = getDeclarationCategory(a)
        val bCategory = getDeclarationCategory(b)

        if (aCategory != bCategory) return false
        if (a !is CallableDescriptor || b !is CallableDescriptor) return false

        if(aCategory != DeclarationCategory.FUNCTION) return false
        return checkOverloadability(a, b)
    }


}
