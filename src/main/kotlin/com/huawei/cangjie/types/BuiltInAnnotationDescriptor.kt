package com.huawei.cangjie.types

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.descriptors.SourceElement
import com.huawei.cangjie.descriptors.annotations.AnnotationDescriptor
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.resolve.constants.ConstantValue

class BuiltInAnnotationDescriptor(
    private val builtIns: CangJieBuiltIns,
    override val fqName: FqName,
    override val allValueArguments: Map<Name, ConstantValue<*>>,
    val forcePropagationDeprecationToOverrides: Boolean = false,
) : AnnotationDescriptor {
    override val type: CangJieType by lazy(LazyThreadSafetyMode.PUBLICATION) {
        builtIns.getBuiltInClassByFqName(fqName).defaultType
    }

    override val source: SourceElement
        get() = SourceElement.NO_SOURCE
}
