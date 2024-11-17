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

package com.linqingying.cangjie.utils

import com.linqingying.cangjie.ide.cache.sourceRoot
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.psi.CjFile
import com.intellij.openapi.roots.SingleFileSourcesTracker
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile

fun CjFile.packageMatchesDirectoryOrImplicit() =
    packageFqName == getFqNameByDirectory() /*|| packageFqName == parent?.getFqNameWithImplicitPrefix()*/

fun PsiFile.isChildOf(directory: PsiDirectory): Boolean {
    var currentDirectory: PsiDirectory? = this.parent

    while (currentDirectory != null) {
        if (currentDirectory.virtualFile == directory.virtualFile) {
            return true
        }
        currentDirectory = currentDirectory.parentDirectory
    }
    return false
}

fun PsiFile.getFqNameByDirectory(): FqName {


    val singleFileSourcesTracker = SingleFileSourcesTracker.getInstance(project)
    val singleFileSourcePackageName = singleFileSourcesTracker.getPackageNameForSingleFileSource(virtualFile)
    singleFileSourcePackageName?.let { return FqName(it) }
    //    判断该文件是否在src目录下
//    val srcDirectory = Paths.get(project.basePath, "src")
//    if (srcDirectory != null && this.parent?.virtualFile == srcDirectory.virtualFile) {

//    }


    return parent?.getNonRootFqNameOrNull() ?: FqName.ROOT
//    return FqName.ROOT

}

private fun PsiDirectory.getNonRootFqNameOrNull(): FqName? {
    if (this.sourceRoot == null) return null
    var name = FqName(this.project.name)

    var _this = this

    val namelist = mutableListOf<String>()
    while (_this.virtualFile != sourceRoot) {
        namelist.add(_this.name)
        _this = _this.parentDirectory!!
    }

    if (namelist.isNotEmpty()) {
//        将namelist倒叙
        namelist.reverse()
        namelist.forEach {
            name = name.child(Name.identifier(it))
        }
    }

    return name
}
//fun PsiDirectory.getPackage(): FqName? = JavaDirectoryService.getInstance()!!.getPackage(this)
//fun PsiDirectory.getFqNameWithImplicitPrefix(): FqName? {
//    val packageFqName = getNonRootFqNameOrNull() ?: return null
//    sourceRoot?.takeIf { !it.hasExplicitPackagePrefix(project) }?.let { sourceRoot ->
//        val implicitPrefix = PerModulePackageCacheService.getInstance(project).getImplicitPackagePrefix(sourceRoot)
//        return FqName.fromSegments((implicitPrefix.pathSegments() + packageFqName.pathSegments()).map { it.asString() })
//    }
//
//    return packageFqName
//}
