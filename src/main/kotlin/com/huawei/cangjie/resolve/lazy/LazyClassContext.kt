package com.huawei.cangjie.resolve.lazy

import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.descriptors.BindingTrace
import com.huawei.cangjie.descriptors.ModuleDescriptor
import com.huawei.cangjie.descriptors.SupertypeLoopChecker
import com.huawei.cangjie.incremental.components.LookupTracker
import com.huawei.cangjie.resolve.DescriptorResolver
import com.huawei.cangjie.resolve.FunctionDescriptorResolver
import com.huawei.cangjie.resolve.TypeResolver
import com.huawei.cangjie.resolve.calls.components.InferenceSession
import com.huawei.cangjie.resolve.extensions.SyntheticResolveExtension
import com.huawei.cangjie.resolve.lazy.declarations.DeclarationProviderFactory
import com.huawei.cangjie.storage.StorageManager
import com.huawei.cangjie.types.WrappedTypeFactory
import com.huawei.cangjie.types.checker.NewCangJieTypeChecker

interface LazyClassContext {

    val inferenceSession: InferenceSession
    val descriptorResolver: DescriptorResolver
    val lookupTracker: LookupTracker
    val moduleDescriptor: ModuleDescriptor
    val supertypeLoopChecker: SupertypeLoopChecker
    val delegationFilter: DelegationFilter
    val typeResolver: TypeResolver
//    val additionalClassPartsProvider: AdditionalClassPartsProvider
val syntheticResolveExtension: SyntheticResolveExtension

    val trace: BindingTrace
    val declarationProviderFactory: DeclarationProviderFactory
    val languageVersionSettings: LanguageVersionSettings
    val wrappedTypeFactory: WrappedTypeFactory

    val storageManager: StorageManager
    val functionDescriptorResolver: FunctionDescriptorResolver
    val declarationScopeProvider: DeclarationScopeProvider
    val cangjieTypeCheckerOfOwnerModule: NewCangJieTypeChecker

}
