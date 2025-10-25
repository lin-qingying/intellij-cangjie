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

    private val  capabilities: Map<ModuleCapability<*>, Any?> = emptyMap(),
    override val stableName: Name? = null,

    isBuiltInsModule: Boolean = false,


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
        test()
    }

    fun test(): CompositePackageFragmentProvider {
        val moduleDependencies =
            dependencies.sure { "Dependencies of module $id were not set before querying module content" }
        val dependenciesDescriptors = moduleDependencies.allDependencies
        assertValid()
        assert(this in dependenciesDescriptors) { "Module $id is not contained in its own dependencies, this is probably a misconfiguration" }
        dependenciesDescriptors.forEach { dependency ->
            assert(dependency.isInitialized) {
                "Dependency module ${dependency.id} was not initialized by the time contents of dependent module ${this.id} were queried"
            }
        }
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

