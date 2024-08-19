package com.huawei.cangjie.descriptors

import com.huawei.cangjie.descriptors.annotations.Annotated

interface DeclarationDescriptor : Annotated,
    Named,
    ValidateableDescriptor, DeclarationSymbolMarker {
    /**
     * @return The descriptor that corresponds to the original declaration of this element.
     * A descriptor can be obtained from its original by substituting type arguments (of the declaring class
     * or of the element itself).
     * returns `this` object if the current descriptor is original itself
     */
    val original: DeclarationDescriptor

    val containingDeclaration: DeclarationDescriptor?

    val visibility: DescriptorVisibility get() = DescriptorVisibilities.PUBLIC
    //    fun getCorrespondingProperty():  PropertyDescriptor

    fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D?): R?

    fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>)
}
