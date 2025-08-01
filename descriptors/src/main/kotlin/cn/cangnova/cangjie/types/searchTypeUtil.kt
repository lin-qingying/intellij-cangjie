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

package cn.cangnova.cangjie.types

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import cn.cangnova.cangjie.descriptors.ClassDescriptor
import cn.cangnova.cangjie.descriptors.ModuleDescriptor
import cn.cangnova.cangjie.descriptors.PackageViewDescriptor
import cn.cangnova.cangjie.ide.projectStructure.moduleInfo
import cn.cangnova.cangjie.ide.stubindex.CangJieExactPackagesIndex
import cn.cangnova.cangjie.incremental.components.NoLookupLocation
import cn.cangnova.cangjie.psi.CjFile
import cn.cangnova.cangjie.resolve.caches.CangJieCacheService
import cn.cangnova.cangjie.resolve.scopes.MemberScope

/**
 * 根据fqname获取类型
 */

fun findClassDescriptorByFqName(project: Project, fqName: FqName): ClassDescriptor? {

    val moduleDescriptor = getModuleDescriptorByFqName(project, fqName) ?: return null

    val memberScope = getMemberScope(moduleDescriptor, fqName)
    val classDescriptor = memberScope.getContributedClassifier(fqName.shortName(), NoLookupLocation.FROM_LIBRARY)

    return classDescriptor as? ClassDescriptor
}

fun findCangJieTypeByFqName(project: Project, fqName: FqName): CangJieType? {

    return findClassDescriptorByFqName(project, fqName)?.defaultType
}

fun getMemberScope(module: ModuleDescriptor, fqName: FqName): MemberScope {

    return module.getPackage(fqName.parent()).memberScope
}



fun getPackageView(project: Project, fqName: FqName): PackageViewDescriptor? {
    return getModuleDescriptorByFqName(project, fqName, true)?.getPackage(fqName)
}

private fun getModuleDescriptorByFqName(
    project: Project,
    fqName: FqName,
    isPackage: Boolean = false
): ModuleDescriptor? {
    val cache = CangJieCacheService.getInstance(project)

    val file = findCjFileByFqName(
        project, if (isPackage) {
            fqName
        } else {
            fqName.parent()
        }
    ) ?: return null


    return cache.getResolutionFacadeByModuleInfo(file.moduleInfo).moduleDescriptor
}

/**
 * 获取文件
 */
fun findCjFileByFqName(project: Project, fqName: FqName): CjFile? = runReadAction {

    val files = CangJieExactPackagesIndex.get(fqName.asString(), project)
    if (files.isEmpty()) return@runReadAction null

    return@runReadAction files[0]
}

//private operator fun <E> Collection<E>.get(i: Int): E {
//    return this[i]
//}
