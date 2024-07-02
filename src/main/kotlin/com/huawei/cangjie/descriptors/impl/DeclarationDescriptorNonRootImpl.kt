package com.huawei.cangjie.descriptors.impl

import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.descriptors.DeclarationDescriptorNonRoot
import com.huawei.cangjie.descriptors.DeclarationDescriptorWithSource
import com.huawei.cangjie.descriptors.SourceElement
import com.huawei.cangjie.descriptors.annotations.Annotations
import com.huawei.cangjie.name.Name

abstract class DeclarationDescriptorNonRootImpl (
    override val  containingDeclaration: DeclarationDescriptor,
    annotations: Annotations,
    name: Name,

  private  val source: SourceElement
): DeclarationDescriptorImpl(annotations, name), DeclarationDescriptorNonRoot {

    override val original: DeclarationDescriptorWithSource
        get() =super.original as DeclarationDescriptorWithSource
    override fun getSource():  SourceElement {
        return source
    }

}