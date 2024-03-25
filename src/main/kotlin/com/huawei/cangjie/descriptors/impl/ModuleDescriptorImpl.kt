//package com.huawei.cangjie.descriptors.impl
//
//import com.huawei.cangjie.descriptors.*
//import com.huawei.cangjie.descriptors.annotations.Annotations
//import com.huawei.cangjie.name.Name
//import com.huawei.cangjie.storage.StorageManager
//
//
//class ModuleDescriptorImpl @JvmOverloads constructor(
//    moduleName: Name,
//    private val storageManager: StorageManager,
////    override val builtIns: CangJieBuiltIns,
//    // May be null in compiler context, should be not-null in IDE context
////    override val platform: TargetPlatform? = null,
//    capabilities: Map<ModuleCapability<*>, Any?> = emptyMap(),
////    override val stableName: Name? = null,
//) :/* DeclarationDescriptorImpl(Annotations.EMPTY, moduleName),*/ ModuleDescriptor {
//    override val original: DeclarationDescriptor
//        get() = TODO("Not yet implemented")
//    override val containingDeclaration: DeclarationDescriptor
//        get() = TODO("Not yet implemented")
//
//    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D?): R {
//        TODO("Not yet implemented")
//    }
//
//    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>) {
//        TODO("Not yet implemented")
//    }
//
//    private val id: String
//        get() = name.toString()
//
//
//    override val annotations: Annotations
//        get() = TODO("Not yet implemented")
//    private var packageFragmentProviderForModuleContent: PackageFragmentProvider? = null
//
//
//    private val isInitialized: Boolean
//        get() = packageFragmentProviderForModuleContent != null
//
//    /*
//     * Call initialize() to set module contents. Uninitialized module cannot be queried for its contents.
//     */
//    fun initialize(providerForModuleContent: PackageFragmentProvider) {
//        assert(!isInitialized) { "Attempt to initialize module $id twice" }
//        this.packageFragmentProviderForModuleContent = providerForModuleContent
//    }
//
//    private var dependencies: ModuleDependencies? = null
//
//
//    fun setDependencies(dependencies: ModuleDependencies) {
//        assert(this.dependencies == null) { "Dependencies of $id were already set" }
//        this.dependencies = dependencies
//    }
//
//    override var isValid: Boolean = true
//
//    override fun <T> getCapability(capability: ModuleCapability<T>): T? {
//        TODO("Not yet implemented")
//    }
//
//    override val name: Name
//        get() = TODO("Not yet implemented")
//}
//
//interface ModuleDependencies {
//    val allDependencies: List<ModuleDescriptorImpl>
//    val modulesWhoseInternalsAreVisible: Set<ModuleDescriptorImpl>
//    val directExpectedByDependencies: List<ModuleDescriptorImpl>
//    val allExpectedByDependencies: Set<ModuleDescriptorImpl>
//}