package com.huawei.cangjie.descriptors.annotations

import com.huawei.cangjie.descriptors.ClassDescriptor
import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.descriptors.SourceElement
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.name.FqNameUnsafe
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.resolve.DescriptorUtils
import com.huawei.cangjie.resolve.constants.ConstantValue
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.ErrorUtils
import com.huawei.cangjie.types.model.AnnotationMarker


val AnnotationDescriptor.annotationClass: ClassDescriptor?
    get() = type.constructor.declarationDescriptor as? ClassDescriptor


val DeclarationDescriptor.fqNameUnsafe: FqNameUnsafe
    get() = DescriptorUtils.getFqName(this)

fun DeclarationDescriptor.fqNameOrNull(): FqName? = fqNameUnsafe.takeIf { it.isSafe }?.toSafe()

interface AnnotationDescriptor : AnnotationMarker {
    val type: CangJieType

    val fqName: FqName?
        get() = annotationClass?.takeUnless(ErrorUtils::isError)?.fqNameOrNull()

    val allValueArguments: Map<Name, ConstantValue<*>>

    val source: SourceElement

}
//fun CangJieType.getAbbreviation(): SimpleType? = getAbbreviatedType()?.abbreviation
//fun CangJieType.getAbbreviatedType(): AbbreviatedType? = unwrap() as? AbbreviatedType
//
//val AnnotationDescriptor.abbreviationFqName: FqName?
//    get() = type.getAbbreviation()?.constructor?.declarationDescriptor?.fqNameOrNull()
