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

package org.cangnova.cangjie.descriptors.impl

import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.psiUtil.sure
import org.cangnova.cangjie.storage.StorageManager


class ModuleDescriptorImpl(
    /**
     * 所属的仓颉项目
     */
    override val projectDescriptor: ProjectDescriptor,
    moduleName: Name,
    private val storageManager: StorageManager,

    private val capabilities: Map<ModuleCapability<*>, Any?> = emptyMap(),
    override val stableName: Name? = null,



    ) : DeclarationDescriptorImpl(Annotations.EMPTY, moduleName),
    ModuleDescriptor {


    private var packageFragmentProviderForModuleContent: PackageFragmentProvider? = null
    private var dependencies: ModuleDependencies? = null

    private val packageViewDescriptorFactory: PackageViewDescriptorFactory

    init {


        packageViewDescriptorFactory =
            getCapability(PackageViewDescriptorFactory.CAPABILITY) ?: PackageViewDescriptorFactory.Default


    }

    fun setDependencies(vararg descriptors: ModuleDescriptorImpl) {
        setDependencies(descriptors.toList())
    }

    private fun setDependencies(descriptors: List<ModuleDescriptorImpl>) {
        setDependencies(descriptors, emptySet())
    }

    fun setDependencies(dependencies: ModuleDependencies) {
        assert(this.dependencies == null) { "Dependencies of $id were already set" }
        this.dependencies = dependencies
    }

    private fun setDependencies(descriptors: List<ModuleDescriptorImpl>, friends: Set<ModuleDescriptorImpl>) {
        setDependencies(ModuleDependenciesImpl(descriptors, friends, emptyList(), emptySet()))
    }

    private val id: String
        get() = name.toString()
    private val isInitialized: Boolean
        get() = packageFragmentProviderForModuleContent != null

    /*
   * Call initialize() to set module contents. Uninitialized module cannot be queried for its contents.
   */
    fun initialize(providerForModuleContent: PackageFragmentProvider) {
        assert(!isInitialized) { "Attempt to initialize module $id twice" }
        this.packageFragmentProviderForModuleContent = providerForModuleContent
    }


    override var isValid: Boolean = true

    private val packages = storageManager.createMemoizedFunction { fqName: FqName ->
        packageViewDescriptorFactory.compute(this, fqName, storageManager)
    }

    override fun getPackage(fqName: FqName): PackageViewDescriptor {
        assertValid()
        return packages(fqName)
    }

    private val packageFragmentProviderForWholeModuleWithDependencies by lazy {
        getProvider()
    }

    /**
     * 获取模块及其所有依赖的复合包片段提供者
     *
     * 该方法是模块依赖解析的核心，负责聚合当前模块及其所有依赖模块的包片段提供者，
     * 使得在查找类型时可以跨模块搜索。
     *
     * ## 工作流程
     *
     * 1. **验证依赖已设置**: 确保模块的依赖关系已通过 [setDependencies] 配置
     * 2. **获取所有依赖描述符**: 从 [ModuleDependencies.allDependencies] 获取完整依赖列表
     * 3. **自包含验证**: 确保当前模块自身在依赖列表中（模块应该能看到自己的内容）
     * 4. **初始化验证**: 确保所有依赖模块都已通过 [initialize] 初始化
     * 5. **组合提供者**: 将所有依赖的 [packageFragmentProviderForModuleContent] 组合成复合提供者
     *
     * ## 使用场景
     *
     * 当调用 [findClassifierAcrossModuleDependencies] 查找类型时：
     * ```
     * module.getPackage(fqName)
     *   → packages(fqName)
     *   → packageViewDescriptorFactory.compute(...)
     *   → packageFragmentProvider.getPackageFragments(fqName)
     *   → CompositePackageFragmentProvider 聚合所有依赖的包片段
     * ```
     *
     * ## 重要说明
     *
     * - **builtInsModule 依赖**: 如果需要访问基本类型（Int8, Bool 等），
     *   必须确保 builtInsModule 在 [allDependencies] 中
     * - **初始化顺序**: 所有依赖模块必须在查询内容前完成初始化
     * - **延迟计算**: 通过 [packageFragmentProviderForWholeModuleWithDependencies] 延迟初始化，
     *   仅在首次访问时构建
     *
     * ## 断言说明
     *
     * - `dependencies.sure`: 依赖必须已设置，否则无法确定搜索范围
     * - `this in dependenciesDescriptors`: 模块必须能看到自己的内容
     * - `dependency.isInitialized`: 依赖模块必须已初始化，否则无法获取其包内容
     *
     * @return 聚合了当前模块及所有依赖模块包片段的复合提供者
     * @throws IllegalStateException 如果依赖未设置或存在未初始化的依赖
     *
     * @see ModuleDependencies.allDependencies 所有依赖模块列表
     * @see CompositePackageFragmentProvider 复合包片段提供者
     * @see packageFragmentProviderForModuleContent 单个模块的包片段提供者
     */
    private fun getProvider(): CompositePackageFragmentProvider {
        // 1. 确保依赖关系已配置，否则无法确定类型查找的搜索范围
        val moduleDependencies =
            dependencies.sure { "Dependencies of module $id were not set before querying module content" }

        // 2. 获取所有依赖模块（包括当前模块自身、直接依赖和传递依赖）
        val dependenciesDescriptors = moduleDependencies.allDependencies

        // 3. 验证模块状态
        assertValid()

        // 4. 自包含验证：模块必须在自己的依赖列表中，这样才能查找到自身定义的类型
        //    如果模块不在依赖列表中，说明 setDependencies 调用时遗漏了自身
        assert(this in dependenciesDescriptors) { "Module $id is not contained in its own dependencies, this is probably a misconfiguration" }

        // 5. 初始化验证：所有依赖模块必须已完成初始化（调用过 initialize 方法）
        //    未初始化的模块没有 packageFragmentProviderForModuleContent，无法提供包内容
        dependenciesDescriptors.forEach { dependency ->
            assert(dependency.isInitialized) {
                "Dependency module ${dependency.id} was not initialized by the time contents of dependent module ${this.id} were queried"
            }
        }

        // 6. 构建复合提供者：聚合所有依赖模块的包片段提供者
        //    查找类型时会遍历所有这些提供者，实现跨模块的类型解析
        return CompositePackageFragmentProvider(
            dependenciesDescriptors.map {
                it.packageFragmentProviderForModuleContent!!
            },
            "CompositeProvider@ModuleDescriptor for $name"
        )
    }

    val packageFragmentProvider: PackageFragmentProvider
        get() {
            assertValid()
            return packageFragmentProviderForWholeModuleWithDependencies
        }

    override fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean): Collection<FqName> {
        assertValid()
        return packageFragmentProvider.getSubPackagesOf(fqName, nameFilter)
    }

    override fun assertValid() {
//        TODO("Not yet implemented")
    }

    override val expectedByModules: List<ModuleDescriptor>
        get() = this.dependencies.sure { "Dependencies of module $id were not set" }.directExpectedByDependencies

    override fun shouldSeeInternalsOf(targetModule: ModuleDescriptor): Boolean {
        if (this == targetModule) return true
        if (targetModule in dependencies!!.modulesWhoseInternalsAreVisible) return true
        if (targetModule in expectedByModules) return true
        if (this in targetModule.expectedByModules) return true

        return false
    }

    override fun shouldProtectedsOf(targetModule: ModuleDescriptor): Boolean {
        if (this == targetModule) return true
        if (targetModule in dependencies!!.modulesWhoseInternalsAreVisible) return true
        if (targetModule in expectedByModules) return true
        if (this in targetModule.expectedByModules) return true

        return false
    }

    override fun <T> getCapability(capability: ModuleCapability<T>): T? {
        return capabilities[capability] as? T
    }


}

class ModuleDependenciesImpl(
    override val allDependencies: List<ModuleDescriptorImpl>,
    override val modulesWhoseInternalsAreVisible: Set<ModuleDescriptorImpl>,
    override val directExpectedByDependencies: List<ModuleDescriptorImpl>,
    override val allExpectedByDependencies: Set<ModuleDescriptorImpl>,
) : ModuleDependencies

interface ModuleDependencies {
    val allDependencies: List<ModuleDescriptorImpl>
    val modulesWhoseInternalsAreVisible: Set<ModuleDescriptorImpl>
    val directExpectedByDependencies: List<ModuleDescriptorImpl>
    val allExpectedByDependencies: Set<ModuleDescriptorImpl>
}

