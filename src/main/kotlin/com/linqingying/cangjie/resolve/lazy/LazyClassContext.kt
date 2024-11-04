package com.linqingying.cangjie.resolve.lazy

import com.linqingying.cangjie.config.LanguageVersionSettings
import com.linqingying.cangjie.descriptors.BindingTrace
import com.linqingying.cangjie.descriptors.ModuleDescriptor
import com.linqingying.cangjie.descriptors.SupertypeLoopChecker
import com.linqingying.cangjie.incremental.components.LookupTracker
import com.linqingying.cangjie.resolve.*
import com.linqingying.cangjie.resolve.calls.components.InferenceSession
import com.linqingying.cangjie.resolve.extensions.SyntheticResolveExtension
import com.linqingying.cangjie.resolve.lazy.declarations.DeclarationProviderFactory
import com.linqingying.cangjie.storage.StorageManager
import com.linqingying.cangjie.types.WrappedTypeFactory
import com.linqingying.cangjie.types.checker.NewCangJieTypeChecker

interface LazyClassContext {

    val inferenceSession: InferenceSession?
    val descriptorResolver: DescriptorResolver
    val lookupTracker: LookupTracker
    val moduleDescriptor: ModuleDescriptor
    val supertypeLoopChecker: SupertypeLoopChecker
    val delegationFilter: DelegationFilter
    val typeResolver: TypeResolver
    val overloadResolver: OverloadResolver
    //    val additionalClassPartsProvider: AdditionalClassPartsProvider
    val syntheticResolveExtension: SyntheticResolveExtension
    val overloadChecker: OverloadChecker
    val trace: BindingTrace
    val declarationProviderFactory: DeclarationProviderFactory
    val languageVersionSettings: LanguageVersionSettings
    val wrappedTypeFactory: WrappedTypeFactory
    val sealedClassInheritorsProvider: SealedClassInheritorsProvider

    val storageManager: StorageManager
    val functionDescriptorResolver: FunctionDescriptorResolver
    val enumDescriptorResolver: EnumDescriptorResolver
    val declarationScopeProvider: DeclarationScopeProvider
    val cangjieTypeCheckerOfOwnerModule: NewCangJieTypeChecker
    val extendDescriptorResolver: ExtendDescriptorResolver
}
