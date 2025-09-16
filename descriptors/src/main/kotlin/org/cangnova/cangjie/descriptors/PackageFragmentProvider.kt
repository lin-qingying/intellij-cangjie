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

package org.cangnova.cangjie.descriptors

import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name


/**
 * 包片段提供者接口，用于提供包片段（PackageFragment）的查询功能，包括获取包片段和子包。
 */
interface PackageFragmentProvider {
    /**
     * 获取指定完全限定名对应的包片段列表（已弃用，建议使用 `packageFragments` 或 `collectPackageFragments` 替代）。
     *
     * @param fqName 完全限定名
     * @return 包片段列表
     */
    @Deprecated("for usages use #packageFragments(FqName) at final point, for impl use #collectPackageFragments(FqName, MutableCollection<PackageFragmentDescriptor>)")
    fun getPackageFragments(fqName: FqName): List<PackageFragmentDescriptor>

    /**
     * 获取指定完全限定名的子包集合（过滤后的）。
     *
     * @param fqName 父包的完全限定名
     * @param nameFilter 子包名称过滤器
     * @return 子包的完全限定名集合
     */
    fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean): Collection<FqName>

    /**
     * 空实现的包片段提供者，用于表示不包含任何包片段或子包的提供者。
     */
    object Empty : PackageFragmentProvider {
        @Deprecated("for usages use #packageFragments(FqName) at final point, for impl use #collectPackageFragments(FqName, MutableCollection<PackageFragmentDescriptor>)",
            ReplaceWith("emptyList<PackageFragmentDescriptor>()")
        )
        override fun getPackageFragments(fqName: FqName) = emptyList<PackageFragmentDescriptor>()

        override fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean) = emptySet<FqName>()
    }
}

/**
 * 尽可能优化地收集指定完全限定名对应的包片段（如果提供者是优化实现的）。
 *
 * @param fqName 完全限定名
 * @param packageFragments 用于存储收集结果的包片段集合
 */
fun PackageFragmentProvider.collectPackageFragmentsOptimizedIfPossible(
    fqName: FqName,
    packageFragments: MutableCollection<PackageFragmentDescriptor>
) {
    when (this) {
        is PackageFragmentProviderOptimized -> collectPackageFragments(fqName, packageFragments)
        else -> packageFragments.addAll(@Suppress("DEPRECATION") getPackageFragments(fqName))
    }
}


/**
 * 获取指定完全限定名对应的包片段列表（优化实现优先）。
 *
 * @param fqName 完全限定名
 * @return 包片段列表
 */
fun PackageFragmentProvider.packageFragments(fqName: FqName): List<PackageFragmentDescriptor> {
    val packageFragments = mutableListOf<PackageFragmentDescriptor>()
    collectPackageFragmentsOptimizedIfPossible(fqName, packageFragments)
    return packageFragments
}
/**
 * 检查指定完全限定名的包片段是否为空（优化实现优先）。
 *
 * @param fqName 完全限定名
 * @return 如果为空则返回 `true`
 */
fun PackageFragmentProvider.isEmpty(fqName: FqName): Boolean {
    return when (this) {
        is PackageFragmentProviderOptimized -> isEmpty(fqName)
        else -> packageFragments(fqName).isEmpty()
    }
}
