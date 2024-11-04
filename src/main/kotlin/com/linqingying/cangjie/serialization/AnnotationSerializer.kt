package com.linqingying.cangjie.serialization

import com.linqingying.cangjie.descriptors.annotations.AnnotationDescriptor
import com.linqingying.cangjie.metadata.ProtoBuf


open class AnnotationSerializer(private val stringTable: DescriptorAwareStringTable) {


    fun serializeAnnotation(annotation: AnnotationDescriptor): ProtoBuf.Annotation? =
        ProtoBuf.Annotation.newBuilder().apply {
//        val classId = getAnnotationClassId(annotation) ?: return null
//        id = stringTable.getQualifiedClassNameIndex(classId)
//
//        for ((name, value) in annotation.allValueArguments) {
//            val argument = ProtoBuf.Annotation.Argument.newBuilder()
//            argument.nameId = stringTable.getStringIndex(name.asString())
//            argument.setValue(valueProto(value))
//            addArgument(argument)
//        }
            return null
        }.build()
}
