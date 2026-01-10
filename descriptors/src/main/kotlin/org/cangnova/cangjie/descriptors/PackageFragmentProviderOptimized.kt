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

package org.cangnova.cangjie.descriptors

import org.cangnova.cangjie.name.FqName

/**
 * 优化实现的包片段提供者接口，继承自 `PackageFragmentProvider`，提供更高效的包片段收集和空状态检查。
 */
interface PackageFragmentProviderOptimized : PackageFragmentProvider {
    /**
     * 收集指定完全限定名对应的包片段到传入的集合中（优化实现）。
     *
     * @param fqName 完全限定名
     * @param packageFragments 用于存储收集结果的包片段集合
     */
    fun collectPackageFragments(fqName: FqName, packageFragments: MutableCollection<PackageFragmentDescriptor>)
    /**
     * 检查指定完全限定名的包片段是否为空（优化实现）。
     *
     * @param fqName 完全限定名
     * @return 如果为空则返回 `true`
     */
    fun isEmpty(fqName: FqName): Boolean
}
