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

import com.intellij.openapi.project.Project
import org.cangnova.cangjie.builtins.BuiltInsLoader
import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.builtins.StandardNames.STD_PACKAGE_NAME
import org.cangnova.cangjie.builtins.StdlibTypes
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.storage.StorageManager
import org.cangnova.cangjie.toolchain.api.CjProjectSdkConfig
import org.cangnova.cangjie.storage.NotNullLazyValue
import java.util.concurrent.ConcurrentHashMap

/**
 * 仓颉项目描述符的默认实现
 *
 * 提供了 [ProjectDescriptor] 接口的标准实现，管理项目中的所有模块。
 *
 * ## 特性
 *
 * - **线程安全**: 使用 [ConcurrentHashMap] 确保多线程环境下的安全性
 * - **懒加载**: 利用 [StorageManager] 实现模块的懒加载和缓存
 * - **有效性检查**: 支持项目和模块的有效性验证
 *
 * @param projectName 项目名称
 * @param intellijProject 关联的 IntelliJ 项目实例
 * @param storageManager 存储管理器，用于管理缓存和懒加载
 *
 * @see ProjectDescriptor
 * @see ModuleDescriptor
 */
class ProjectDescriptorImpl(
    private val projectName: Name,
    override val project: Project,
    private val storageManager: StorageManager
) : ProjectDescriptor {

    /**
     * 模块映射表，使用线程安全的 ConcurrentHashMap
     * Key: 模块名称
     * Value: 模块描述符
     */
    private val moduleMap = ConcurrentHashMap<Name, ModuleDescriptor>()

    override val name: Name
        get() = projectName

    /**
     * 全局唯一的 BuiltIns 实例，基于 SDK 创建
     *
     * 仅包含编译器内置类型（Int8, Bool, Unit 等）
     */
    override val builtIns: CangJieBuiltIns by lazy {
        CangJieBuiltIns(this, storageManager)
    }

    /**
     * 标准库模块懒加载提供者
     *
     * 使用 StorageManager.createLazyValue 确保线程安全和单次初始化。
     */
    private val stdlibModuleProvider: NotNullLazyValue<StdlibModuleDescriptorImpl> =
        storageManager.createLazyValue {
            createStdlibModule()
        }

    /**
     * 获取标准库模块
     *
     * 注意: 此属性会触发 stdlib 模块的创建。
     * 第一次访问时，会:
     * 1. 创建 StdlibModuleDescriptorImpl
     * 2. 设置依赖为 [builtInsModule]
     * 3. 从 SDK 加载 .cjo 文件
     * 4. 添加到 moduleMap
     */
    override val stdlibModule: StdlibModuleDescriptorImpl
        get() = stdlibModuleProvider()

    /**
     * 标准库类型访问器
     *
     * 提供对标准库（std.*）类型的访问。
     * 依赖于 stdlibModule，因此访问前会自动创建 stdlib 模块。
     */
    override val stdlibTypes: StdlibTypes by lazy {
        StdlibTypes.create(stdlibModule, storageManager)
    }

    init {
        // 确保 stdlib 模块在 ProjectDescriptor 初始化时就创建
        // 这样可以避免懒加载带来的时序问题
        val sdk = CjProjectSdkConfig.getInstance(project).getProjectSdk()
        if (sdk != null) {
            try {
                // 触发 stdlib 模块创建
                stdlibModuleProvider()
            } catch (e: Exception) {
                // 如果创建失败（如 SDK 配置错误），记录但不中断初始化
                // stdlib 会在后续首次访问时再次尝试创建
                System.err.println("Warning: Failed to create stdlib module during ProjectDescriptor initialization: ${e.message}")
            }
        }
    }

    /**
     * 创建标准库模块
     *
     * 此方法仅在第一次访问 stdlibModule 时调用一次（通过 StorageManager 保证）。
     *
     * @return 标准库模块描述符
     * @throws IllegalStateException 如果 SDK 未配置
     */
    private fun createStdlibModule(): StdlibModuleDescriptorImpl {
        val sdk = CjProjectSdkConfig.getInstance(project).getProjectSdk()
            ?: error("Cannot create stdlib module: SDK not configured for project '$projectName'")

        val stdlibModule = StdlibModuleDescriptorImpl(
            projectDescriptor = this,
            moduleName = STD_PACKAGE_NAME,
            displayName = "stdlib",
            storageManager = storageManager,
            capabilities = emptyMap(),
            stableName = STD_PACKAGE_NAME
        )

        // 设置依赖: stdlib 依赖 builtInsModule
        stdlibModule.setDependencies(stdlibModule, builtIns.builtInsModule)

        // 从 SDK 加载标准库
        stdlibModule.initialize(
            BuiltInsLoader.Instance.createStdLibPackageFragmentProvider(
                storageManager,
                stdlibModule,
                sdk
            )
        )

        // 添加到模块映射（如果已存在则覆盖）
        moduleMap[STD_PACKAGE_NAME] = stdlibModule

        return stdlibModule
    }

    override val modules: List<ModuleDescriptor>
        get() {
            assertValid()
            return moduleMap.values.toList()
        }

    override fun getModule(moduleName: Name): ModuleDescriptor? {
        assertValid()
        return moduleMap[moduleName]
    }

    override var isValid: Boolean = true
        private set

    override fun assertValid() {
        if (!isValid) {
            throw InvalidModuleException("CangJie project '$projectName' is invalid")
        }
    }

    /**
     * 添加模块到项目中
     *
     * @param module 要添加的模块描述符
     * @throws IllegalArgumentException 如果已存在同名模块
     */
    override fun addModule(module: ModuleDescriptor) {
        assertValid()
        val existingModule = moduleMap.putIfAbsent(module.name, module)
        require(existingModule == null) {
            "Module '${module.name}' already exists in project '$projectName'"
        }
    }

    /**
     * 移除项目中的模块
     *
     * @param moduleName 要移除的模块名称
     * @return 被移除的模块描述符，如果不存在则返回 null
     */
    fun removeModule(moduleName: Name): ModuleDescriptor? {
        assertValid()
        return moduleMap.remove(moduleName)
    }

    /**
     * 检查项目中是否包含指定名称的模块
     *
     * @param moduleName 模块名称
     * @return 如果存在则返回 true，否则返回 false
     */
    fun hasModule(moduleName: Name): Boolean {
        assertValid()
        return moduleMap.containsKey(moduleName)
    }

    /**
     * 清空项目中的所有模块
     */
    fun clearModules() {
        assertValid()
        moduleMap.clear()
    }

    /**
     * 使项目失效
     * 调用此方法后，项目将不再可用
     */
    fun invalidate() {
        isValid = false
        moduleMap.clear()
    }

    override fun toString(): String {
        return "ProjectDescriptor($projectName, modules=${moduleMap.keys.joinToString()})"
    }

    override val original: DeclarationDescriptor
        get() = this
    override val containingDeclaration: DeclarationDescriptor?
        get() = null

    override fun <R, D> accept(
        visitor: DeclarationDescriptorVisitor<R, D>,
        data: D
    ): R? {
        return null
    }

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Unit, Unit>) {

    }
}
