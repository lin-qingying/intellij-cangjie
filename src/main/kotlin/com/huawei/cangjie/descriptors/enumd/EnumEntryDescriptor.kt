package com.huawei.cangjie.descriptors.enumd

import com.huawei.cangjie.descriptors.ClassConstructorDescriptor
import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.descriptors.DescriptorVisibility
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.resolve.lazy.LazyClassContext
import com.huawei.cangjie.resolve.lazy.data.CjEnmuEntryInfo
import com.huawei.cangjie.resolve.lazy.descriptors.LazyClassDescriptor
import com.huawei.cangjie.resolve.lazy.descriptors.LazyClassMemberScope

class EnumEntryDescriptor(
    c: LazyClassContext,
    thisDescriptor: DeclarationDescriptor,
    name: Name,
    info: CjEnmuEntryInfo,

    ) : LazyClassDescriptor(c, thisDescriptor, name, info, false) {


    override val classLikeInfo: CjEnmuEntryInfo
        get() = super.classLikeInfo as CjEnmuEntryInfo

    //    是否有无参构造
    fun hasUnsubstitutedPrimaryConstructor(): Boolean {
        return unsubstitutedPrimaryConstructor.valueParameters.isEmpty() /*&&
                containingDeclaration is LazyEnumDescriptor &&
                (containingDeclaration as LazyEnumDescriptor).declaredTypeParameters.isEmpty()*/

    }

    override val isStatic: Boolean
        get() = true
    override val visibility: DescriptorVisibility = containingDeclaration.visibility
    override fun getEndConstructors(): Collection<ClassConstructorDescriptor> = emptySet()

    override fun getUnsubstitutedPrimaryConstructor(): ClassConstructorDescriptor {
        return (unsubstitutedMemberScope as LazyClassMemberScope).getEnumEntryPrimaryConstructor()!!

    }

    override fun getConstructors(): List<ClassConstructorDescriptor> {
        return emptyList()
    }
}
