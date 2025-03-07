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

package cn.cangnova.cangjie.utils.roots

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.Query


/**
 * Provides a possibility to query the directories corresponding to a specific Java package name.
 *
 * @see ModulePackageIndex
 */
abstract class PackageIndex {
    /**
     * Returns all directories in content sources and libraries (and optionally library sources)
     * corresponding to the given package name.
     *
     * @param packageName           the name of the package for which directories are requested.
     * @param includeLibrarySources if true, directories under library sources are included in the returned list.
     * @return the array of directories.
     */
    abstract fun getDirectoriesByPackageName(packageName: String, includeLibrarySources: Boolean): Array<VirtualFile>

    /**
     * @return all directories in the given scope corresponding to the given package name.
     */
    open fun getDirsByPackageName(packageName: String, scope: GlobalSearchScope): Query<VirtualFile > {
        return getDirsByPackageName(packageName, true).filtering { file: VirtualFile  ->
            scope.contains(
                file
            )
        }
    }

    /**
     * Returns all directories in content sources and libraries (and optionally library sources)
     * corresponding to the given package name as a query object (allowing to perform partial iteration of the results).
     *
     * @param packageName           the name of the package for which directories are requested.
     * @param includeLibrarySources if true, directories under library sources are included in the returned list.
     * @return the query returning the list of directories.
     */
    abstract fun getDirsByPackageName(packageName: String, includeLibrarySources: Boolean): Query<VirtualFile>

    /**
     * Returns the name of the package corresponding to the specified directory.
     *
     * @return the package name, or null if the directory does not correspond to any package.
     */
    abstract fun getPackageNameByDirectory(dir: VirtualFile): String?

    companion object {
        fun getInstance(project: Project): PackageIndex {
            return project.getService(PackageIndex::class.java)
        }
    }
}

//
//class CangJiePakcageIndex : PackageIndex(){
//    override fun getDirectoriesByPackageName(packageName: String, includeLibrarySources: Boolean): Array<VirtualFile> {
//        TODO("Not yet implemented")
//    }
//
//    override fun getDirsByPackageName(packageName: String, includeLibrarySources: Boolean): Query<VirtualFile> {
//        TODO("Not yet implemented")
//    }
//
//    override fun getPackageNameByDirectory(dir: VirtualFile): String? {
//        TODO("Not yet implemented")
//    }
//
//}