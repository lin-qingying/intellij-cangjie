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

package cn.cangnova.cangjie.ide

import cn.cangnova.cangjie.lang.CangJieFileType
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.psi.search.SearchScope
fun PsiFile.fileScope(): GlobalSearchScope = GlobalSearchScope.fileScope(this)

fun PsiElement.useScope(): SearchScope = PsiSearchHelper.getInstance(project).getUseScope(this)
fun GlobalSearchScope.restrictToCangJieSources() = restrictByFileType(CangJieFileType.INSTANCE)
fun GlobalSearchScope.restrictByFileType(fileType: FileType) = GlobalSearchScope.getScopeRestrictedByFileTypes(this, fileType)
fun Project.everythingScopeExcludeFileTypes(vararg fileTypes: FileType): GlobalSearchScope {
    return GlobalSearchScope.getScopeRestrictedByFileTypes(GlobalSearchScope.everythingScope(this), *fileTypes).not()
}
operator fun GlobalSearchScope.not(): GlobalSearchScope = GlobalSearchScope.notScope(this)
fun SearchScope.restrictToCangJieSources() = restrictByFileType(CangJieFileType.INSTANCE)
fun SearchScope.restrictByFileType(fileType: FileType): SearchScope = when (this) {
    is GlobalSearchScope -> restrictByFileType(fileType)
    is LocalSearchScope -> {
        val elements = scope.filter { it.containingFile.fileType == fileType }
        when (elements.size) {
            0 -> LocalSearchScope.EMPTY
            scope.size -> this
            else -> LocalSearchScope(elements.toTypedArray())
        }
    }
    else -> this
}
fun Project.allScope(): GlobalSearchScope = GlobalSearchScope.allScope(this)
