package com.linqingying.cangjie.serialization


import com.google.protobuf.ExtensionRegistryLite
import com.google.protobuf.GeneratedMessage
import com.google.protobuf.GeneratedMessage.GeneratedExtension
import com.linqingying.cangjie.metadata.ProtoBuf


open class SerializerExtensionProtocol(
    val extensionRegistry: ExtensionRegistryLite,
    val packageFqName: GeneratedExtension<ProtoBuf.Package, Int>,
    val constructorAnnotation: GeneratedExtension<ProtoBuf.Constructor, List<ProtoBuf.Annotation>>,
    val classAnnotation: GeneratedExtension<ProtoBuf.Class, List<ProtoBuf.Annotation>>,
    val functionAnnotation: GeneratedExtension<ProtoBuf.Function, List<ProtoBuf.Annotation>>,
    val functionExtensionReceiverAnnotation: GeneratedExtension<ProtoBuf.Function, List<ProtoBuf.Annotation>>?,
    val propertyAnnotation: GeneratedExtension<ProtoBuf.Property, List<ProtoBuf.Annotation>>,
    val propertyGetterAnnotation: GeneratedExtension<ProtoBuf.Property, List<ProtoBuf.Annotation>>,
    val propertySetterAnnotation: GeneratedExtension<ProtoBuf.Property, List<ProtoBuf.Annotation>>,
    val propertyExtensionReceiverAnnotation: GeneratedExtension<ProtoBuf.Property, List<ProtoBuf.Annotation>>?,
//    val propertyBackingFieldAnnotation: GeneratedExtension<ProtoBuf.Property, List<ProtoBuf.Annotation>>?,
//    val propertyDelegatedFieldAnnotation: GeneratedExtension<ProtoBuf.Property, List<ProtoBuf.Annotation>>?,
    val enumEntryAnnotation: GeneratedExtension<ProtoBuf.EnumEntry, List<ProtoBuf.Annotation>>,
    val compileTimeValue: GeneratedExtension<ProtoBuf.Property, ProtoBuf.Annotation.Argument.Value>,
    val parameterAnnotation: GeneratedExtension<ProtoBuf.ValueParameter, List<ProtoBuf.Annotation>>,
    val typeAnnotation: GeneratedExtension<ProtoBuf.Type, List<ProtoBuf.Annotation>>,
    val typeParameterAnnotation: GeneratedExtension<ProtoBuf.TypeParameter, List<ProtoBuf.Annotation>>
)
