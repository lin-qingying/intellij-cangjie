package com.linqingying.cangjie.descriptors

import com.linqingying.cangjie.types.TypeSubstitutor

interface Substitutable<out T : DeclarationDescriptorNonRoot> {
    fun substitute(substitutor: TypeSubstitutor): T
}
