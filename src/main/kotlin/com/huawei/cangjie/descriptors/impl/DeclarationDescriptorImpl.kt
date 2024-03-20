package com.huawei.cangjie.descriptors.impl

import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.descriptors.DeclarationDescriptorVisitor
import com.huawei.cangjie.descriptors.annotations.AnnotatedImpl
import com.huawei.cangjie.descriptors.annotations.Annotations
import com.huawei.cangjie.name.Name

abstract class DeclarationDescriptorImpl(
    annotations: Annotations,
    override val  name: Name
):  AnnotatedImpl(annotations), DeclarationDescriptor {


    override fun acceptVoid(visitor:  DeclarationDescriptorVisitor<Void, Void>) {
        accept(visitor, null)
    }

}