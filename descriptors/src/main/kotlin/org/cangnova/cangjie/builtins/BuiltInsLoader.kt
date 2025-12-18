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

package org.cangnova.cangjie.builtins

import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.descriptors.ModuleDescriptor
import org.cangnova.cangjie.descriptors.PackageFragmentProvider
import org.cangnova.cangjie.storage.StorageManager
import org.cangnova.cangjie.toolchain.api.CjSdk
import java.util.*


interface BuiltInsLoader {

    /**
     * 创建基本类型的包片段提供者
     *
     * 仅包含 cangjie 包下的基本类型（Int8, Bool, Unit 等）。
     * 这些类型是硬编码的，不从文件加载。
     *
     * @param storageManager 存储管理器
     * @param builtInsModule 内置模块描述符
     * @return 基本类型的 PackageFragmentProvider
     */
    fun createBasicTypesPackageFragmentProvider(
        storageManager: StorageManager,
        builtInsModule: ModuleDescriptor
    ): PackageFragmentProvider

    /**
     * 创建标准库的包片段提供者
     *
     * 包含 std.core, std.sync, std.ast 等包。
     * 从 SDK 的 .cjo 文件反序列化获取。
     *
     * @param storageManager 存储管理器
     * @param stdlibModule 标准库模块描述符
     * @param isFallback 是否为回退模式（SDK 不可用时）
     * @param sdk 仓颉 SDK
     * @return 标准库的 PackageFragmentProvider
     */
    fun createStdLibPackageFragmentProvider(
        storageManager: StorageManager,
        stdlibModule: ModuleDescriptor,
        sdk: CjSdk?
    ): PackageFragmentProvider

    /**
     * 创建普通二进制库的包片段提供者
     *
     * 用于加载其他二进制库（非 stdlib）。
     * 从指定的 .cjo 文件反序列化获取。
     *
     * **注意**：此方法暂不实现，预留接口
     *
     * @param storageManager 存储管理器
     * @param libraryModule 库模块描述符
     * @param cjoFiles .cjo 文件列表
     * @return 二进制库的 PackageFragmentProvider
     */
    fun createPackageFragmentProvider(
        storageManager: StorageManager,
        libraryModule: ModuleDescriptor,
        cjoFiles: List<VirtualFile>
    ): PackageFragmentProvider {
        TODO("Not yet implemented - reserved for future binary library loading")
    }

    companion object {
        val Instance: BuiltInsLoader by lazy(LazyThreadSafetyMode.PUBLICATION) {
            val implementations = ServiceLoader.load(BuiltInsLoader::class.java, BuiltInsLoader::class.java.classLoader)
            implementations.firstOrNull() ?: throw IllegalStateException(
                "No BuiltInsLoader implementation was found. Please ensure that the META-INF/services/ is not stripped " +
                        "from your application and that the Java virtual machine is not running under a security manager"
            )
        }
    }
}
