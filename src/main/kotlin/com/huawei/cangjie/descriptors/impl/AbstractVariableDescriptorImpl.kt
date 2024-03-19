package com.huawei.cangjie.descriptors.impl

import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.descriptors.SourceElement
import com.huawei.cangjie.descriptors.VariableDescriptor
import com.huawei.cangjie.descriptors.annotations.Annotations
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.types.CangJieType

abstract class AbstractVariableDescriptorImpl(
    containingDeclaration: DeclarationDescriptor,
    annotations: Annotations,
    name: Name,
    outType: CangJieType?,
    source: SourceElement

) :  DeclarationDescriptorNonRootImpl (containingDeclaration, annotations, name, source),
    VariableDescriptor {
}