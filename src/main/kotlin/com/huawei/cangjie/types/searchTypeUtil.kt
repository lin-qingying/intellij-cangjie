package com.huawei.cangjie.types

import com.huawei.cangjie.descriptors.ClassDescriptor
import com.huawei.cangjie.descriptors.ClassifierDescriptor
import com.huawei.cangjie.descriptors.ModuleDescriptor
import com.huawei.cangjie.ide.projectStructure.moduleInfo
import com.huawei.cangjie.ide.stubindex.CangJieExactPackagesIndex
import com.huawei.cangjie.incremental.components.NoLookupLocation
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.psi.CjFile
import com.huawei.cangjie.resolve.caches.CangJieCacheService
import com.huawei.cangjie.resolve.scopes.MemberScope
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project

/**
 * 根据fqname获取类型
 */

fun findClassDescriptorByFqName(project: Project, fqName: FqName): ClassDescriptor? {

    val moduleDescriptor = getModuleDescriptorByFqName(project, fqName) ?: return null

    val memberScope = getMemberScope(moduleDescriptor, fqName)
    val classDescriptor = memberScope.getContributedClassifier(fqName.shortName(), NoLookupLocation.FROM_LIBRARY)

    return classDescriptor as? ClassDescriptor
}

fun getMemberScope(module: ModuleDescriptor, fqName: FqName): MemberScope {

    return module.getPackage(fqName.parent()).memberScope
}

private fun getModuleDescriptorByFqName(project: Project, fqName: FqName): ModuleDescriptor? {
    val cache = CangJieCacheService.getInstance(project)

    val file = findCjFileByFqName(project, fqName.parent()) ?: return null


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
