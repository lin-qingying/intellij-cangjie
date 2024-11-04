package com.linqingying.cangjie.serialization.deserialization

import com.google.protobuf.ExtensionRegistryLite
import com.linqingying.cangjie.metadata.builtins.BuiltInsProtoBuf
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.serialization.SerializerExtensionProtocol

object BuiltInSerializerProtocol   : SerializerExtensionProtocol(
ExtensionRegistryLite.newInstance().apply(BuiltInsProtoBuf::registerAllExtensions),
BuiltInsProtoBuf.packageFqName,
BuiltInsProtoBuf.constructorAnnotation,
BuiltInsProtoBuf.classAnnotation,
BuiltInsProtoBuf.functionAnnotation,
functionExtensionReceiverAnnotation = null,
BuiltInsProtoBuf.propertyAnnotation,
BuiltInsProtoBuf.propertyGetterAnnotation,
BuiltInsProtoBuf.propertySetterAnnotation,
propertyExtensionReceiverAnnotation = null,

BuiltInsProtoBuf.enumEntryAnnotation,
BuiltInsProtoBuf.compileTimeValue,
BuiltInsProtoBuf.parameterAnnotation,
BuiltInsProtoBuf.typeAnnotation,
BuiltInsProtoBuf.typeParameterAnnotation
) {
    const val BUILTINS_FILE_EXTENSION = "cangjie_builtins"
    const val DOT_DEFAULT_EXTENSION = ".$BUILTINS_FILE_EXTENSION"

    fun getBuiltInsFilePath(fqName: FqName): String =
        fqName.asString().replace('.', '/') + "/" + getBuiltInsFileName(
            fqName
        )

    fun getBuiltInsFileName(fqName: FqName): String =
        shortName(fqName) + DOT_DEFAULT_EXTENSION

    private fun shortName(fqName: FqName): String =
        if (fqName.isRoot) "default-package" else fqName.shortName().asString()
}
