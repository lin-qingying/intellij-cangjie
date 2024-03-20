package com.huawei.cangjie.descriptors

import com.huawei.cangjie.types.TypeSubstitutor

interface Substitutable<out T : DeclarationDescriptorNonRoot> {
    fun substitute(substitutor: TypeSubstitutor): T
}
