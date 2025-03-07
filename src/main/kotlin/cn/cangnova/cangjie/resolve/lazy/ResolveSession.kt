/*
 * Copyright 2024 LinQingYing. and contributors.
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

//package cn.cangnova.cangjie.resolve.lazy
//
//import cn.cangnova.cangjie.context.GlobalContext
//import cn.cangnova.cangjie.descriptors.BindingTrace
//import cn.cangnova.cangjie.descriptors.ModuleDescriptor
//import cn.cangnova.cangjie.incremental.components.LookupTracker
//import cn.cangnova.cangjie.name.FqName
//import cn.cangnova.cangjie.psi.CjFile
//import cn.cangnova.cangjie.resolve.BindingContext
//import cn.cangnova.cangjie.resolve.DescriptorResolver
//import cn.cangnova.cangjie.resolve.FunctionDescriptorResolver
//import cn.cangnova.cangjie.resolve.calls.components.InferenceSession
//import cn.cangnova.cangjie.resolve.lazy.declarations.DeclarationProviderFactory
//import cn.cangnova.cangjie.resolve.lazy.declarations.LazyPackageDescriptor
//import cn.cangnova.cangjie.resolve.lazy.declarations.PackageMemberDeclarationProvider
//import cn.cangnova.cangjie.storage.CacheWithNotNullValues
//import cn.cangnova.cangjie.storage.LazyResolveStorageManager
//import cn.cangnova.cangjie.storage.LockBasedLazyResolveStorageManager
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
