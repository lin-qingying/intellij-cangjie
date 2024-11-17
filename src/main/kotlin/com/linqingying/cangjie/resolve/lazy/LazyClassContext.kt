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
