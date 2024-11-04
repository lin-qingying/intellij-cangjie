package com.linqingying.cangjie.ide.vfilefinder

import com.intellij.util.indexing.DefaultFileTypeSpecificInputFilter
import com.intellij.util.indexing.ID
import com.linqingying.cangjie.lang.declarations.CangJieBuiltInFileType
import com.linqingying.cangjie.metadata.BuiltInDefinitionFile
import com.linqingying.cangjie.name.ClassId
import com.linqingying.cangjie.name.FqName


class CangJieMetadataFileIndex :CangJieMetadataFileIndexBase(ClassId::asSingleFqName) {
    companion object {
        val NAME: ID<FqName, Void> = ID.create("com.linqingying.cangjie.ide.vfilefinder.CangJieMetadataFileIndex")
    }

    override fun getName() = NAME
}

class CangJieMetadataFilePackageIndex : CangJieMetadataFileIndexBase(ClassId::packageFqName) {
    companion object {
        val NAME: ID<FqName, Void> = ID.create("com.linqingying.cangjie.ide.vfilefinder.CangJieMetadataFilePackageIndex")
    }

    override fun getName() = NAME
}
class CangJieBuiltInsMetadataIndex : CangJieFileIndexBase() {
    companion object {
        val NAME: ID<FqName, Void> = ID.create("com.linqingying.cangjie.ide.vfilefinder.CangJieBuiltInsMetadataIndex")
    }

    override fun getName() = NAME

    override fun getIndexer() = INDEXER

    override fun getInputFilter() = DefaultFileTypeSpecificInputFilter(CangJieBuiltInFileType)

    override fun getVersion() = VERSION

    private val VERSION = 4

    private val INDEXER = indexer { fileContent ->
        val packageFqName =
            if (fileContent.fileType == CangJieBuiltInFileType  )
             {
                val builtins = readCangJieMetadataDefinition(fileContent) as? BuiltInDefinitionFile
                builtins?.packageFqName
            } else null
        packageFqName
    }
}
