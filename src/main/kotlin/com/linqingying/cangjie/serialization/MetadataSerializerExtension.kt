package com.linqingying.cangjie.serialization

import com.linqingying.cangjie.metadata.builtins.BuiltInsBinaryVersion
import com.linqingying.cangjie.serialization.deserialization.BuiltInSerializerProtocol


class MetadataSerializerExtension(
    override val metadataVersion: BuiltInsBinaryVersion
) : CangJieSerializerExtensionBase(BuiltInSerializerProtocol) {
    override fun shouldUseTypeTable(): Boolean = true
    override val stringTable = ApproximatingStringTable()
}
