//package com.huawei.cangjie.resolve.lazy
//
//import com.huawei.cangjie.context.GlobalContext
//import com.huawei.cangjie.descriptors.BindingTrace
//import com.huawei.cangjie.descriptors.ModuleDescriptor
//import com.huawei.cangjie.incremental.components.LookupTracker
//import com.huawei.cangjie.name.FqName
//import com.huawei.cangjie.psi.CjFile
//import com.huawei.cangjie.resolve.BindingContext
//import com.huawei.cangjie.resolve.DescriptorResolver
//import com.huawei.cangjie.resolve.FunctionDescriptorResolver
//import com.huawei.cangjie.resolve.calls.components.InferenceSession
//import com.huawei.cangjie.resolve.lazy.declarations.DeclarationProviderFactory
//import com.huawei.cangjie.resolve.lazy.declarations.LazyPackageDescriptor
//import com.huawei.cangjie.resolve.lazy.declarations.PackageMemberDeclarationProvider
//import com.huawei.cangjie.storage.CacheWithNotNullValues
//import com.huawei.cangjie.storage.LazyResolveStorageManager
//import com.huawei.cangjie.storage.LockBasedLazyResolveStorageManager
//import com.intellij.openapi.project.Project
//
//class ResolveSession(
//    project: Project,
//    globalContext: GlobalContext,
//    val module: ModuleDescriptor,
//    val declarationProviderFactory: DeclarationProviderFactory,
//    delegationTrace: BindingTrace,
//) : CangJieCodeAnalyzer, LazyClassContext {
//
//    private var myDescriptorResolver: DescriptorResolver? = null
//    val trace: BindingTrace
//    private val packages: CacheWithNotNullValues<FqName, LazyPackageDescriptor>
//    override val storageManager: LazyResolveStorageManager =
//        LockBasedLazyResolveStorageManager(globalContext.storageManager)
//    var myFunctionDescriptorResolver: FunctionDescriptorResolver? = null
//    var myDeclarationScopeProvider: DeclarationScopeProvider? = null
//    var fileScopeProvider: FileScopeProvider? = null
//
//    init {
//
//
//        this.trace = storageManager.createSafeTrace(delegationTrace)
//
//
//
//
//        this.packages =
//            storageManager.createCacheWithNotNullValues()
//
//    }
//
//    override fun getPackageFragment(fqName: FqName): LazyPackageDescriptor? {
//         val provider: PackageMemberDeclarationProvider =
//            declarationProviderFactory.getPackageMemberDeclarationProvider(fqName)
//                ?: return null
//
//        return packages.computeIfAbsent(
//            fqName
//        ) {
//            LazyPackageDescriptor(
//                module,
//                fqName,
//                this,
//                provider
//            )
//        }
//    }
//
//    override val bindingContext: BindingContext
//        get() = trace.bindingContext
//
//    override fun getPackageFragmentOrDiagnoseFailure(fqName: FqName, from: CjFile?): LazyPackageDescriptor {
//        val packageDescriptor = getPackageFragment(fqName)
//        if (packageDescriptor == null) {
//            declarationProviderFactory.diagnoseMissingPackageFragment(fqName, from)
//            assert(false) { "diagnoseMissingPackageFragment should throw!" }
//        }
//        return packageDescriptor!!
//    }
//
//    override fun assertValid() {
//        module.assertValid()
//
//    }
//
//    override val inferenceSession: InferenceSession? = null
//    override var descriptorResolver: DescriptorResolver
//        get() =   myDescriptorResolver!!
//        set(value) {
//            myDescriptorResolver = value
//        }
//    override var lookupTracker: LookupTracker? = LookupTracker.DO_NOTHING
//
//    override var functionDescriptorResolver: FunctionDescriptorResolver
//        get() = myFunctionDescriptorResolver!!
//        set(value) {
//            myFunctionDescriptorResolver = value
//        }
//    override var declarationScopeProvider: DeclarationScopeProvider
//        get() = myDeclarationScopeProvider!!
//        set(value) {
//            myDeclarationScopeProvider = value
//        }
//
//
//}