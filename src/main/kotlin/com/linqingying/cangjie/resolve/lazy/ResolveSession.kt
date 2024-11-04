//package com.linqingying.cangjie.resolve.lazy
//
//import com.linqingying.cangjie.context.GlobalContext
//import com.linqingying.cangjie.descriptors.BindingTrace
//import com.linqingying.cangjie.descriptors.ModuleDescriptor
//import com.linqingying.cangjie.incremental.components.LookupTracker
//import com.linqingying.cangjie.name.FqName
//import com.linqingying.cangjie.psi.CjFile
//import com.linqingying.cangjie.resolve.BindingContext
//import com.linqingying.cangjie.resolve.DescriptorResolver
//import com.linqingying.cangjie.resolve.FunctionDescriptorResolver
//import com.linqingying.cangjie.resolve.calls.components.InferenceSession
//import com.linqingying.cangjie.resolve.lazy.declarations.DeclarationProviderFactory
//import com.linqingying.cangjie.resolve.lazy.declarations.LazyPackageDescriptor
//import com.linqingying.cangjie.resolve.lazy.declarations.PackageMemberDeclarationProvider
//import com.linqingying.cangjie.storage.CacheWithNotNullValues
//import com.linqingying.cangjie.storage.LazyResolveStorageManager
//import com.linqingying.cangjie.storage.LockBasedLazyResolveStorageManager
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
