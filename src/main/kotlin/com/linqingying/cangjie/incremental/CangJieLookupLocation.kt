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

package com.linqingying.cangjie.incremental

import com.linqingying.cangjie.diagnostics.DiagnosticUtils.getLineAndColumnInPsiFile
import com.linqingying.cangjie.incremental.components.LocationInfo
import com.linqingying.cangjie.incremental.components.LookupLocation
import com.linqingying.cangjie.incremental.components.Position
import com.linqingying.cangjie.psi.CjElement
import com.linqingying.cangjie.psi.doNotAnalyze


class CangJieLookupLocation(val element: CjElement) : LookupLocation {
    val cachedLocation : LocationInfo? by lazy {
        val containingCjFile = element.getContainingCjFile()

        if (containingCjFile.doNotAnalyze != null)
            null
        else
            object : LocationInfo {
                override val filePath = containingCjFile.virtualFilePath

                override val position: Position
                    get() = getLineAndColumnInPsiFile(containingCjFile, element.textRange).let { Position(it.line, it.column) }
            }
    }

    override val location: LocationInfo?
        get() = cachedLocation
}
