package com.linqingying.cangjie.resolve.scopes.synthetic

import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.incremental.components.LookupLocation
import com.linqingying.cangjie.incremental.components.LookupTracker
import com.linqingying.cangjie.resolve.scopes.SyntheticScope
import com.linqingying.cangjie.resolve.scopes.SyntheticScopes
import com.linqingying.cangjie.storage.StorageManager


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
