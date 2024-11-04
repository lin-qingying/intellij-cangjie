package com.linqingying.cangjie.ide.vfilefinder

import com.intellij.util.indexing.DefaultFileTypeSpecificInputFilter
import com.intellij.util.indexing.FileContent
import com.linqingying.cangjie.lang.declarations.CangJieBuiltInFileType
import com.linqingying.cangjie.metadata.BuiltInDefinitionFile
import com.linqingying.cangjie.name.ClassId
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.serialization.deserialization.MetadataPackageFragment
import com.linqingying.cangjie.serialization.deserialization.getClassId


abstract class CangJieMetadataFileIndexBase(indexFunction: (ClassId) -> FqName) : CangJieFileIndexBase() {
    override fun getIndexer() = INDEXER

    override fun getInputFilter() = DefaultFileTypeSpecificInputFilter(CangJieBuiltInFileType)

    override fun getVersion() = 2

    private val INDEXER = indexer { fileContent ->
        val classId = fileContent.classIdFromCangJieMetadata() ?: return@indexer null
        indexFunction(classId)
    }
}

internal fun FileContent.classIdFromCangJieMetadata(): ClassId? {
    val builtIns = readCangJieMetadataDefinition(this) as? BuiltInDefinitionFile ?: return null

    val singleClass = builtIns.proto.class_List.singleOrNull()
    if (singleClass != null) {
        return builtIns.nameResolver.getClassId(singleClass.fqName)
    }

    val facadeName = this.fileName.substringBeforeLast(MetadataPackageFragment.DOT_METADATA_FILE_EXTENSION)
    return ClassId(builtIns.packageFqName, Name.identifier(facadeName))
}
