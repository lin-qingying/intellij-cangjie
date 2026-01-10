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

package org.cangnova.cangjie.descriptors.impl

import org.cangnova.cangjie.descriptors.ModuleCapability
import org.cangnova.cangjie.descriptors.PackageViewDescriptor
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.storage.StorageManager

/**
 * 提供 PackageViewDescriptor 实例的工厂接口。
 *
 * 实现此接口可以定制如何为给定模块和包名创建或获取 PackageViewDescriptor。
 */
interface PackageViewDescriptorFactory {
    /**
     * 计算并返回指定模块中给定包的 PackageViewDescriptor。
     *
     * 实现可以选择即时创建、缓存或返回惰性计算的描述器实例。
     *
     * @param module 要创建描述器的模块（实现类型为 ModuleDescriptorImpl）
     * @param fqName 包的全限定名
     * @param storageManager 用于惰性计算或缓存的存储管理器
     * @return 对应的 PackageViewDescriptor 实例
     */
    fun compute(
        module: ModuleDescriptorImpl,
        fqName: FqName,
        storageManager: StorageManager
    ): PackageViewDescriptor

    /**
     * 默认工厂实现，返回 LazyPackageViewDescriptorImpl 的实例。
     */
    object Default: PackageViewDescriptorFactory {
        override fun compute(module: ModuleDescriptorImpl, fqName: FqName, storageManager: StorageManager): PackageViewDescriptor {
            return LazyPackageViewDescriptorImpl(module, fqName, storageManager)
        }
    }

    companion object {
        /**
         * 在 ModuleDescriptor 中注册或查找 PackageViewDescriptorFactory 的能力标识。
         */
        val CAPABILITY = ModuleCapability<PackageViewDescriptorFactory>("PackageViewDescriptorFactory")
    }
}
