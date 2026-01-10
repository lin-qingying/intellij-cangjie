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

package org.cangnova.cangjie.stubindex

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FileBasedIndex
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile


object CangJiePackageIndexUtils {
    fun getSubPackageFqNames(
        packageFqName: FqName,
        scope: GlobalSearchScope,
        nameFilter: (Name) -> Boolean
    ): Collection<FqName> = getSubpackages(packageFqName, scope, nameFilter)

    fun findFilesWithExactPackage(
        packageFqName: FqName,
        searchScope: GlobalSearchScope,
        project: Project
    ): Collection<CjFile> = CangJieExactPackagesIndex.get(packageFqName.asString(), project, searchScope)


    fun findFilesWithExactPackageByAllScope(
        packageFqName: FqName,

        project: Project
    ): Collection<CjFile> =
        CangJieExactPackagesIndex.get(packageFqName.asString(), project, GlobalSearchScope.allScope(project))

    /**
     * Return all direct subpackages of package [fqName].
     *
     * I.e. if there are packages `a.b`, `a.b.c`, `a.c`, `a.c.b` for `packagefqName` = `a` it returns
     * `a.b` and `a.c`
     *
     * Follow the contract of [com.intellij.psi.PsiElementFinder#getSubPackages]
     */
    fun getSubpackages(fqName: FqName, scope: GlobalSearchScope, nameFilter: (Name) -> Boolean): Collection<FqName> {
        val result = hashSetOf<FqName>()

        FileBasedIndex.getInstance().processValues(
            NAME, fqName, null,
            { _, subPackageName ->
                if (subPackageName != null && nameFilter(subPackageName)) {
                    result.add(fqName.child(subPackageName))
                }
                true
            }, scope
        )

        return result
    }


    fun getSubPackageFqNamesByAllScope(
        packageFqName: FqName,
        project: Project
    ): Collection<FqName> = getSubpackages(packageFqName, GlobalSearchScope.allScope(project)) {
        true
    }

    fun getSubPackageFqNames(
        packageFqName: FqName,
        scope: GlobalSearchScope
    ): Collection<FqName> = getSubpackages(packageFqName, scope) {
        true
    }


    /**
     * Return true if exists package with exact [fqName] OR there are some subpackages of [fqName]
     */
    fun packageExists(fqName: FqName, project: Project): Boolean =
        packageExists(fqName, GlobalSearchScope.allScope(project))


    /**
     * Return true if package [packageFqName] exists or some subpackages of [packageFqName] exist in [searchScope]
     */
    fun packageExists(
        packageFqName: FqName,
        searchScope: GlobalSearchScope
    ): Boolean {
        val a = !FileBasedIndex.getInstance().processValues(
            NAME,
            packageFqName,
            null,
            FileBasedIndex.ValueProcessor { _, _ -> false },
            searchScope
        )
        return a
    }
}
