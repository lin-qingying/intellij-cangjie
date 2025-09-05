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

package org.cangnova.cangjie.serialization.deserialization

import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.metadata.model.*
import org.cangnova.cangjie.metadata.deserialization.BinaryVersion
import org.cangnova.cangjie.descriptors.SourceElement

/**
 * FlatBuffers-based implementation of ClassDataFinder.
 * This class finds class data from FlatBuffers serialized metadata instead of Protocol Buffers.
 */
class FlatBuffersBasedClassDataFinder(
    private val packageData: Package,
    private val metadataVersion: BinaryVersion,
    private val sourceElementProvider: () -> SourceElement = {
        SourceElement.NO_SOURCE
    }
) : ClassDataFinder {

    private val classDeclarations: Map<ClassId, Decl> by lazy {
        buildClassDeclarationMap()
    }

    override val allClassIds: Collection<ClassId> by lazy {
        classDeclarations.keys
    }

    override fun findClassData(classId: ClassId): ClassData? {
        val classDecl = classDeclarations[classId] ?: return null
        return ClassData(
            classDecl = classDecl,
            metadataVersion = metadataVersion,
            sourceElement = sourceElementProvider()
        )
    }

    private fun buildClassDeclarationMap(): Map<ClassId, Decl> {
        val result = mutableMapOf<ClassId, Decl>()
        
        // Extract class declarations from the package
        for (decl in packageData.decls) {
            when (decl.kind) {
                DeclKind.ClassDecl, 
                DeclKind.InterfaceDecl, 
                DeclKind.StructDecl, 
                DeclKind.EnumDecl -> {
                    val classId = createClassId(decl)
                    result[classId] = decl
                }
                else -> {
                    // Skip non-class declarations
                }
            }
        }
        
        return result
    }

    private fun createClassId(decl: Decl): ClassId {
        val packageFqName = org.cangnova.cangjie.name.FqName(packageData.fullPkgName)
        val className = Name.identifier(decl.identifier)
        return ClassId(packageFqName, className)
    }
}