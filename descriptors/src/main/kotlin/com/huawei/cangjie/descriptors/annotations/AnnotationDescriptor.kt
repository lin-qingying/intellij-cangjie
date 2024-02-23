package com.huawei.cangjie.descriptors.annotations

interface AnnotationMarker

//interface AnnotationDescriptor : AnnotationMarker {
//    val type: CangJieType
//
//    val fqName: FqName?
//        get() = annotationClass?.takeUnless(ErrorUtils::isError)?.fqNameOrNull()
//
//    val allValueArguments: Map<Name, ConstantValue<*>>
//
//    val source: SourceElement
//
//}