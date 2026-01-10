/*
 * Copyright 2026 LinQingYing. and contributors.
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
import org.cangnova.cangjie.descriptors.extend.ExtendDescriptor
import org.cangnova.cangjie.descriptors.packageFragments
import org.cangnova.cangjie.metadata.deserialization.BinaryVersion
import org.cangnova.cangjie.metadata.model.wrapper.ExtendWrapper
import org.cangnova.cangjie.metadata.model.wrapper.PackageWrapper
import org.cangnova.cangjie.serialization.deserialization.descriptors.DeserializedExtendDescriptor


data class ExtendData(
    val extendDecl: ExtendWrapper,
    val `package`: PackageWrapper,
    val metadataVersion: BinaryVersion,
    val sourceElement: SourceElement
)

internal class ExtendKey(val extendID: String, val extendData: ExtendData?) {
    // classData *intentionally* not used in equals() / hashCode()
    override fun equals(other: Any?) = other is ExtendKey && extendID == other.extendID

    override fun hashCode() = extendID.hashCode()
}

class ExtendDeserializer(private val components: DeserializationComponents) {


    private val extend: (ExtendKey) -> ExtendDescriptor? =
        components.storageManager.createMemoizedFunctionWithNullableValues { key -> createExtend(key) }

    // Additional ClassData parameter is needed to avoid calling ClassDataFinder#findClassData()
    // if it is already computed at the call site
    fun deserializeExtend(
        extendID: String,
        classData: ExtendData? = null
    ): ExtendDescriptor? =
        extend(ExtendKey(extendID, classData))

    private fun createExtend(key: ExtendKey): ExtendDescriptor? {
        val extendID = key.extendID


        val (decl, `package`, metadataVersion, sourceElement) = key.extendData
            ?: components.classDataFinder.findExtendData(extendID)
            ?: return null


        val fragments = components.packageFragmentProvider.packageFragments(decl.packageFqName)
        val fragment =
            fragments.firstOrNull()
                ?: return null

        val outerContext = components.createContext(
            fragment, `package`,


            metadataVersion,
            containerSource = null
        )


        return DeserializedExtendDescriptor(outerContext, decl, metadataVersion, sourceElement)

    }

}
