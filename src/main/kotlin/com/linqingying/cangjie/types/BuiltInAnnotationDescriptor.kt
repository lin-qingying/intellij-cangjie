package com.linqingying.cangjie.types

import com.linqingying.cangjie.builtins.CangJieBuiltIns
import com.linqingying.cangjie.descriptors.SourceElement
import com.linqingying.cangjie.descriptors.annotations.AnnotationDescriptor
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.resolve.constants.ConstantValue

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
