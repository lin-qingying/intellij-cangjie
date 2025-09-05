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

package org.cangnova.cangjie.serialization.deserialization.descriptors

import org.cangnova.cangjie.descriptors.ClassifierDescriptor
import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.descriptors.PackageFragmentDescriptor
import org.cangnova.cangjie.incremental.components.LookupLocation
import org.cangnova.cangjie.incremental.components.NoLookupLocation
import org.cangnova.cangjie.incremental.record
import org.cangnova.cangjie.metadata.deserialization.BinaryVersion
import org.cangnova.cangjie.metadata.model.Package
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.scopes.DescriptorKindFilter
import org.cangnova.cangjie.serialization.deserialization.DeserializationComponents

open class DeserializedPackageMemberScope(
    private val packageDescriptor: PackageFragmentDescriptor,
    `package`: Package,

    metadataVersion: BinaryVersion,
    containerSource: DeserializedContainerSource?,
    components: DeserializationComponents,
    private val debugName: String,
    classNames: () -> Collection<Name>,
) : DeserializedMemberScope(
    components.createContext(
        packageDescriptor,
        metadataVersion, containerSource
    ),
    `package`.decls
) {
    private val packageFqName = packageDescriptor.fqName

    override fun getContributedDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean) =
        computeDescriptors(kindFilter, nameFilter, NoLookupLocation.MATCH_GET_ALL_DESCRIPTORS) +
                c.components.fictitiousClassDescriptorFactories.flatMap {
                    it.getAllContributedClassesIfPossible(
                        packageFqName
                    )
                }

    override fun hasClass(name: Name) =
        super.hasClass(name) || c.components.fictitiousClassDescriptorFactories.any {
            it.shouldCreateClass(
                packageFqName,
                name
            )
        }

    override fun createClassId(name: Name) = ClassId(packageFqName, name)


    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
        recordLookup(name, location)
        return super.getContributedClassifier(name, location)
    }

    override fun recordLookup(name: Name, location: LookupLocation) {
        c.components.lookupTracker.record(location, packageDescriptor, name)
    }

    override fun getNonDeclaredFunctionNames(): Set<Name> = emptySet()
    override fun getNonDeclaredVariableNames(): Set<Name> = emptySet()


    override fun getNonDeclaredPropertyNames(): Set<Name> = emptySet()

    override fun getNonDeclaredClassifierNames(): Set<Name>? = emptySet()


    override fun toString(): String = debugName
}
