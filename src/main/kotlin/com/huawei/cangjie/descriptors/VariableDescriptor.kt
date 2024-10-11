package com.huawei.cangjie.descriptors

import com.huawei.cangjie.resolve.constants.ConstantValue
import com.huawei.cangjie.types.TypeSubstitutor

interface VariableDescriptor : ValueDescriptor ,MemberDescriptor/*,
   CallableMemberDescriptor, VariableSymbolMarker*/ {


    fun getCompileTimeInitializer(): ConstantValue<*>?  = null

    /**
     * ONLY FOR IDE USE! Please don't use the method inside the compiler
     */
    fun cleanCompileTimeInitializerCache()  {}


    /**
     * @return true if iff original declaration has appropriate flags and type, e.g. `const` modifier in CangJie.
     * It completely does not means that if isConst then `getCompileTimeInitializer` is not null
     */
    //    @Nullable
    //    FieldDescriptor getBackingField();
    //
    //    @Nullable
    //    FieldDescriptor getDelegateField();
    override fun substitute(substitutor: TypeSubstitutor): VariableDescriptor?
    val isConst: Boolean

    //    bool isActual();
    //
    //    bool isExternal();
    val isVar: Boolean
}
