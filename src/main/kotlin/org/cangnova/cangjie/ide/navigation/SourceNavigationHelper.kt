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

package org.cangnova.cangjie.ide.navigation

import org.cangnova.cangjie.psi.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbService

object SourceNavigationHelper {
    private val LOG = Logger.getInstance(SourceNavigationHelper::class.java)

    fun getOriginalElement(declaration: CjDeclaration): CjElement {
        return navigateToDeclaration(declaration, NavigationKind.SOURCES_TO_CLASS_FILES)
    }

    fun getNavigationElement(declaration: CjDeclaration): CjElement {
        return navigateToDeclaration(declaration, NavigationKind.CLASS_FILES_TO_SOURCES)
    }

    enum class NavigationKind {
        CLASS_FILES_TO_SOURCES,
        SOURCES_TO_CLASS_FILES,
    }

    private fun navigateToDeclaration(
        from: CjDeclaration,
        navigationKind: NavigationKind,
    ): CjDeclaration {
        if (!from.isValid || DumbService.isDumb(from.project)) return from

        when (navigationKind) {
            NavigationKind.CLASS_FILES_TO_SOURCES -> if (!from.getContainingCjFile().isCompiled) return from
            NavigationKind.SOURCES_TO_CLASS_FILES -> {
                val file = from.containingFile
//                if (!RootKindFilter.librarySources.matches(from)) return from
                if (CjPsiUtil.isLocal(from)) return from
            }
        }

        return from.accept(SourceAndDecompiledConversionVisitor(navigationKind), Unit) ?: from
    }

    private class SourceAndDecompiledConversionVisitor(private val navigationKind: NavigationKind) :
        CjVisitor<CjDeclaration?, Unit>()
}
