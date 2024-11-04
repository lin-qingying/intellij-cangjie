package com.linqingying.cangjie.resolve.scopes

import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.incremental.components.LookupLocation
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.resolve.source.MemberScopeImpl
import com.linqingying.cangjie.utils.Printer


class InnerClassesScopeWrapper(val workerScope: MemberScope) : MemberScopeImpl() {
    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? =
        workerScope.getContributedClassifier(name, location)?.let {
            it as? ClassDescriptor ?: it as? TypeAliasDescriptor
        }
    override fun getContributedClassifiers(name: Name, location: LookupLocation): List<ClassifierDescriptor> =
        workerScope.getContributedClassifiers(name, location)
    override fun getContributedDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): List<DeclarationDescriptor> {
        val restrictedFilter = kindFilter.restrictedToKindsOrNull(DescriptorKindFilter.CLASSIFIERS_MASK) ?: return listOf()
        return workerScope.getContributedDescriptors(restrictedFilter, nameFilter) .toList()
    }

    override fun printScopeStructure(p: Printer) {
        p.println("InnerClassesScopeWrapper for scope:")
        workerScope.printScopeStructure(p)
    }

    override fun getFunctionNames() = workerScope.getFunctionNames()
    override fun getVariableNames() = workerScope.getVariableNames()
    override fun getClassifierNames() = workerScope.getClassifierNames()

    override fun definitelyDoesNotContainName(name: Name) = workerScope.definitelyDoesNotContainName(name)

    override fun recordLookup(name: Name, location: LookupLocation) {
        workerScope.recordLookup(name, location)
    }

    override fun toString() = "Classes from $workerScope"
}
