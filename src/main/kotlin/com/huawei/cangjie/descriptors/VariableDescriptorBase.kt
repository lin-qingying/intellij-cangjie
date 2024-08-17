package com.huawei.cangjie.descriptors

import com.huawei.cangjie.types.TypeSubstitutor

interface VariableDescriptorBase: ValueDescriptor
    /*  CallableMemberDescriptor, PropertySymbolMarker*/ {

//    override fun getOverriddenDescriptors(): Collection< VariableDescriptor>


//    override val original: VariableDescriptorBase
    /**
     * @return true if iff original declaration has appropriate flags and type, e.g. `const` modifier in CangJie.
     * It completely does not means that if isConst then `getCompileTimeInitializer` is not null
     */
    //    @Nullable
    //    FieldDescriptor getBackingField();
    //
    //    @Nullable
    //    FieldDescriptor getDelegateField();
//      override fun substitute(substitutor: TypeSubstitutor): VariableDescriptorBase?
    val isConst:Boolean
    val isVar: Boolean
}
