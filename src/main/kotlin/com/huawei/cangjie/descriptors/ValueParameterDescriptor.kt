package com.huawei.cangjie.descriptors

import com.huawei.cangjie.mpp.ValueParameterSymbolMarker
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.types.CangJieType

interface ValueParameterDescriptor : VariableDescriptor ,ParameterDescriptor, ValueParameterSymbolMarker   {
    override val original: ValueParameterDescriptor
//    val varargElementType: CangJieType?

    override val containingDeclaration: CallableDescriptor

    val varargElementType: CangJieType? get() = null

    /**
     * Returns the 0-based index of the value parameter in the parameter list of its containing function.

     * @return the parameter index
     */
    val index: Int
    /**
     * Parameter p1 overrides p2 iff
     * a) their respective owners (function declarations) f1 override f2
     * b) p1 and p2 have the same indices in the owners' parameter lists
     */
    override fun getOverriddenDescriptors(): Collection<ValueParameterDescriptor>

    /**
     * @return true iff this parameter belongs to a declared function (not a fake override) and declares the default value,
     * i.e. explicitly specifies it in the function signature. Also see 'hasDefaultValue' extension in DescriptorUtils.kt
     */
    fun declaresDefaultValue(): Boolean
    fun copy(newOwner: CallableDescriptor, newName: Name, newIndex: Int): ValueParameterDescriptor

}
