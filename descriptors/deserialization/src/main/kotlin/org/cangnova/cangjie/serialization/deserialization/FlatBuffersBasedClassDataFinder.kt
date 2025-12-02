/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.serialization.deserialization

import org.cangnova.cangjie.descriptors.SourceElement
import org.cangnova.cangjie.metadata.deserialization.BinaryVersion
import org.cangnova.cangjie.metadata.model.wrapper.ClassDeclWrapper
import org.cangnova.cangjie.metadata.model.wrapper.ExtendWrapper
import org.cangnova.cangjie.metadata.model.wrapper.PackageWrapper
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.utils.valuesToMap

/**
 * FlatBuffers-based implementation of ClassDataFinder.
 * This class finds class data from FlatBuffers serialized metadata instead of Protocol Buffers.
 */
class FlatBuffersBasedClassDataFinder(
    private val packageData: PackageWrapper,
    private val metadataVersion: BinaryVersion,
    private val sourceElementProvider: () -> SourceElement = {
        SourceElement.NO_SOURCE
    }
) : ClassDataFinder {
    private val extendDeclarations: Map<String, ExtendWrapper> by lazy {
        buildExtendDeclarationMap()
    }
    private val classDeclarations: Map<ClassId, ClassDeclWrapper> by lazy {
        buildClassDeclarationMap()
    }

    override val allClassIds: Collection<ClassId> by lazy {
        classDeclarations.keys
    }

    override fun findClassData(classId: ClassId): ClassData? {
        val classDecl = classDeclarations[classId] ?: return null
        return ClassData(
            classDecl = classDecl,
            `package` = packageData,
            metadataVersion = metadataVersion,
            sourceElement = sourceElementProvider()
        )
    }

    private fun buildExtendDeclarationMap(): Map<String, ExtendWrapper> {

        return packageData.extends.valuesToMap {
            it.id
        }
    }

    private fun buildClassDeclarationMap(): Map<ClassId, ClassDeclWrapper> {

        return packageData.allClassDecls.valuesToMap {
            it.classId
        }
    }

    override fun findExtendData(extendID: String): ExtendData? {
        val extendDecl = extendDeclarations[extendID] ?: return null
        return ExtendData(
            extendDecl = extendDecl,
            `package` = packageData,
            metadataVersion = metadataVersion,
            sourceElement = sourceElementProvider()
        )
    }

    override val allExtendIds: Collection<String> by lazy {
        extendDeclarations.keys
    }


}