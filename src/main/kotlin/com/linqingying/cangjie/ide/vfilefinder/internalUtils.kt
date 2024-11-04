package com.linqingying.cangjie.ide.vfilefinder

import com.intellij.util.indexing.FileContent
import com.linqingying.cangjie.lang.declarations.CangJieBuiltInFileType
import com.linqingying.cangjie.metadata.BuiltInDefinitionFile
import com.linqingying.cangjie.serialization.deserialization.MetadataPackageFragment
import com.linqingying.cangjie.metadata.CangJieMetadataStubBuilder.FileWithMetadata.Compatible as CompatibleMetadata

private val ALLOWED_METADATA_EXTENSIONS = listOf(
    MetadataPackageFragment.DOT_METADATA_FILE_EXTENSION
)
internal fun readCangJieMetadataDefinition(fileContent: FileContent): CompatibleMetadata ? {
    if (fileContent.fileType != CangJieBuiltInFileType) {
        return null
    }

    val fileName = fileContent.fileName
    if (ALLOWED_METADATA_EXTENSIONS.none { fileName.endsWith(it) }) {
        return null
    }

    val definition = BuiltInDefinitionFile.read(fileContent.content, fileContent.file.parent) as? CompatibleMetadata ?: return null

    // '.cangjie_builtins' files sometimes appear in random libraries.
    // Below there's an additional check that the file is likely to be an actual part of built-ins.

    val nestingLevel = definition.packageFqName.pathSegments().size
    val rootPackageDirectory = generateSequence(fileContent.file) { it.parent }.drop(nestingLevel + 1).firstOrNull() ?: return null
    val metaInfDirectory = rootPackageDirectory.findChild("META-INF") ?: return null

    if (metaInfDirectory.children.none { it.extension == "cangjie_module" }) {
        // Here can be a more strict check.
        // For instance, we can check if the manifest file has a 'CangJie-Runtime-Component' attribute.
        // It's unclear if it would break use-cases when the standard library is embedded, though.
        return null
    }

    return definition
}
