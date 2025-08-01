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

package cn.cangnova.cangjie.resolve.scopes.synthetic

import cn.cangnova.cangjie.descriptors.*
import cn.cangnova.cangjie.incremental.components.LookupLocation
import cn.cangnova.cangjie.incremental.components.LookupTracker
import cn.cangnova.cangjie.resolve.scopes.SyntheticScope
import cn.cangnova.cangjie.resolve.scopes.SyntheticScopes


class FunInterfaceConstructorsScopeProvider(
    storageManager: StorageManager,
    lookupTracker: LookupTracker,
//    samResolver: SamConversionResolver,
//    samConversionOracle: SamConversionOracle
) : SyntheticScopes {
    override val scopes: Collection<SyntheticScope> = listOf(
//        FunInterfaceConstructorsSyntheticScope(storageManager, lookupTracker, samResolver, samConversionOracle)
    )
}
//
//class FunInterfaceConstructorsSyntheticScope(
//    storageManager: StorageManager,
//    private val lookupTracker: LookupTracker,
//    private val samResolver: SamConversionResolver,
//    private val samConversionOracle: SamConversionOracle
//) : SyntheticScope.Default() {
//
//    private val samConstructorForClassifier =
//        storageManager.createMemoizedFunction<ClassDescriptor, SamConstructorDescriptor> { classifier ->
//            createSamConstructorFunction(classifier.containingDeclaration, classifier, samResolver, samConversionOracle)
//        }
//
//    override fun getSyntheticConstructors(
//        contributedClassifier: ClassifierDescriptor,
//        location: LookupLocation
//    ): Collection<FunctionDescriptor> {
//        recordSamLookupsToClassifier(contributedClassifier, location)
//
//        return listOfNotNull(getSamConstructor(contributedClassifier))
//    }
//
//    override fun getSyntheticConstructors(classifierDescriptors: Collection<DeclarationDescriptor>): Collection<FunctionDescriptor> =
//        classifierDescriptors.filterIsInstanceMapNotNull<ClassifierDescriptor, FunctionDescriptor> { getSamConstructor(it) }
//
//    private fun getSamConstructor(classifier: ClassifierDescriptor): SamConstructorDescriptor? {
//        if (classifier is TypeAliasDescriptor) {
//            return getTypeAliasSamConstructor(classifier)
//        }
//
//        val classDescriptor = checkIfClassifierApplicable(classifier) ?: return null
//        return samConstructorForClassifier(classDescriptor)
//    }
//
//    private fun getTypeAliasSamConstructor(classifier: TypeAliasDescriptor): SamConstructorDescriptor? {
//        val classDescriptor = checkIfClassifierApplicable(classifier.classDescriptor ?: return null) ?: return null
//
//        return createTypeAliasSamConstructorFunction(
//            classifier, samConstructorForClassifier(classDescriptor), samResolver, samConversionOracle
//        )
//    }
//
//    private fun checkIfClassifierApplicable(classifier: ClassifierDescriptor): ClassDescriptor? {
//        if (classifier !is ClassDescriptor) return null
//        if (!classifier.isFun) return null
//        if (getSingleAbstractMethodOrNull(classifier) == null) return null
//
//        return classifier
//    }
//
//    private fun recordSamLookupsToClassifier(classifier: ClassifierDescriptor, location: LookupLocation) {
//        if (classifier !is ClassDescriptor || !classifier.isFun) return
//        lookupTracker.record(location, classifier, SAM_LOOKUP_NAME)
//    }
//}
