package com.linqingying.cangjie.serialization.deserialization.descriptors

import com.linqingying.cangjie.descriptors.SourceFile
import com.linqingying.cangjie.metadata.ProtoBuf
import com.linqingying.cangjie.metadata.deserialization.NameResolver
import com.linqingying.cangjie.name.CangJieClassName
import com.linqingying.cangjie.name.ClassId
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.serialization.CangJieMetadataVersion

interface FacadeClassSource {
    val className: CangJieClassName
    val facadeClassName: CangJieClassName?
}
class CangJiePackagePartSource(


    override val className: CangJieClassName,
    override val facadeClassName: CangJieClassName?,
    packageProto: ProtoBuf.Package,
    nameResolver: NameResolver,
    override val incompatibility: IncompatibleVersionErrorData<CangJieMetadataVersion>? = null,
    override val isPreReleaseInvisible: Boolean = false,
    override val abiStability: DeserializedContainerAbiStability = DeserializedContainerAbiStability.STABLE,

    ):DeserializedContainerSource,FacadeClassSource  {
    override val presentableString: String
        get() = "Class '${classId.asSingleFqName().asString()}'"
    val classId: ClassId get() = ClassId(className.packageFqName, simpleName)
    val simpleName: Name get() = Name.identifier(className.internalName.substringAfterLast('/'))
    override fun toString() = "${this::class.java.simpleName}: $className"

    override fun getContainingFile(): SourceFile = SourceFile.NO_SOURCE_FILE

}
