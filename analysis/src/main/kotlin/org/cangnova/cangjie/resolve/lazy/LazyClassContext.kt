/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.resolve.lazy

import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.ModuleDescriptor
import org.cangnova.cangjie.descriptors.SupertypeLoopChecker
import org.cangnova.cangjie.incremental.components.LookupTracker
import org.cangnova.cangjie.resolve.*
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.calls.components.InferenceSession
import org.cangnova.cangjie.resolve.extensions.SyntheticResolveExtension
import org.cangnova.cangjie.resolve.lazy.declarations.DeclarationProviderFactory
import org.cangnova.cangjie.storage.StorageManager
import org.cangnova.cangjie.types.WrappedTypeFactory
import org.cangnova.cangjie.types.checker.CangJieTypeChecker

interface LazyClassContext {

    val inferenceSession: InferenceSession?
    val descriptorResolver: DescriptorResolver
    val lookupTracker: LookupTracker
    val moduleDescriptor: ModuleDescriptor
    val supertypeLoopChecker: SupertypeLoopChecker
    val delegationFilter: DelegationFilter
    val typeResolver: TypeResolver
    val overloadResolver: OverloadResolver
    val annotationResolver: AnnotationResolver

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
    val fileScopeProvider: FileScopeProvider
    val cangjieTypeCheckerOfOwnerModule: CangJieTypeChecker
}
