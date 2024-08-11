package com.huawei.cangjie.descriptors.impl

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.descriptors.annotations.Annotations
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.psi.psiUtil.sure
import com.huawei.cangjie.storage.StorageManager

//


class ModuleDescriptorImpl(
    moduleName: Name,
    private val storageManager: StorageManager,
    override val builtIns: CangJieBuiltIns,

    capabilities: Map<ModuleCapability<*>, Any?> = emptyMap(),

    ) : DeclarationDescriptorImpl(Annotations.EMPTY, moduleName),
    ModuleDescriptor {
    private val capabilities: Map<ModuleCapability<*>, Any?>
    private var packageFragmentProviderForModuleContent: PackageFragmentProvider? = null
    private var dependencies: ModuleDependencies? = null

    private val packageViewDescriptorFactory: PackageViewDescriptorFactory

    init {
//        if (!moduleName.isSpecial) {
//            throw IllegalArgumentException("Module name must be special: $moduleName")
//        }
        this.capabilities = capabilities

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
    fun  test(): CompositePackageFragmentProvider {
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
      return  CompositePackageFragmentProvider(
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

