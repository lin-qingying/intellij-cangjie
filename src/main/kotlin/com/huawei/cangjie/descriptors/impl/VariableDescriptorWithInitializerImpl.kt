package com.huawei.cangjie.descriptors.impl

import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.descriptors.SourceElement
import com.huawei.cangjie.descriptors.annotations.Annotations
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.types.CangJieType

abstract class VariableDescriptorWithInitializerImpl (
    containingDeclaration: DeclarationDescriptor,
    annotations: Annotations,
    name: Name,
    outType:CangJieType?,
    override val isVar:Boolean,
    source: SourceElement
):  AbstractVariableDescriptorImpl (containingDeclaration, annotations, name, outType, source){


}
