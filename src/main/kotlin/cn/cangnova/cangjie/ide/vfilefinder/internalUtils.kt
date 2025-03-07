/*
 * Copyright 2024 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package cn.cangnova.cangjie.ide.vfilefinder

import com.intellij.util.indexing.FileContent
import cn.cangnova.cangjie.lang.declarations.CangJieBuiltInFileType
import cn.cangnova.cangjie.metadata.BuiltInDefinitionFile
import cn.cangnova.cangjie.serialization.deserialization.MetadataPackageFragment
import cn.cangnova.cangjie.serialization.deserialization.MetadataPackageFragment.Companion.DOT_METADATA_FILE_EXTENSION
import cn.cangnova.cangjie.metadata.CangJieMetadataStubBuilder.FileWithMetadata.Compatible as CompatibleMetadata

private val ALLOWED_METADATA_EXTENSIONS = listOf(
    DOT_METADATA_FILE_EXTENSION
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
