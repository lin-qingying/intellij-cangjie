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

package org.cangnova.cangjie.utils

import com.intellij.openapi.extensions.ExtensionPointName
import org.cangnova.cangjie.descriptors.ClassKind
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.descriptors.Visibility
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjFile

/** Filter that can block classes from being automatically imported as a side-effect of other actions. */
fun interface ClassImportFilter {
    /**
     * This class holds information that implementations of ClassImportFilter might need to decide whether to import a given class.
     * By packaging this information in a data class, we avoid the need to change the API when we need to add more/different information.
     */
    data class ClassInfo(val fqName: FqName, val classKind: ClassKind, val modality: Modality, val visibility: Visibility, val isNested: Boolean)

    /** Returns whether to allow this class to be imported. */
    fun allowClassImport(classInfo: ClassInfo, contextFile: CjFile) : Boolean

    companion object {
        val EP_NAME = ExtensionPointName.create<ClassImportFilter>("org.cangnova.cangjie.classImportFilter")
        fun allowClassImport(classInfo: ClassInfo, contextFile: CjFile) =
            EP_NAME.extensions.all { it.allowClassImport(classInfo, contextFile) }
    }
}