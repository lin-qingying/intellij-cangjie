package com.linqingying.cangjie.descriptors.annotations

import com.linqingying.cangjie.descriptors.ClassDescriptor
import com.linqingying.cangjie.descriptors.DeclarationDescriptor
import com.linqingying.cangjie.descriptors.SourceElement
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.name.FqNameUnsafe
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.resolve.DescriptorUtils

import com.linqingying.cangjie.resolve.constants.ConstantValue
import com.linqingying.cangjie.resolve.descriptorUtil.annotationClass
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.ErrorUtils
import com.linqingying.cangjie.types.model.AnnotationMarker



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
