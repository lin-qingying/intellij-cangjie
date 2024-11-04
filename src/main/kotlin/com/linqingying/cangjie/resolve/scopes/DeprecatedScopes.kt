package com.linqingying.cangjie.resolve.scopes

import com.linqingying.cangjie.descriptors.ClassifierDescriptor
import com.linqingying.cangjie.descriptors.DescriptorWithDeprecation
import com.linqingying.cangjie.incremental.components.LookupLocation
import com.linqingying.cangjie.name.Name


class DeprecatedLexicalScope(private val workerScope: LexicalScope) : LexicalScope by workerScope {
    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? = null

    override fun getContributedClassifierIncludeDeprecated(
        name: Name,
        location: LookupLocation
    ): DescriptorWithDeprecation<ClassifierDescriptor>? {
        return workerScope.getContributedClassifier(name, location)?.let { DescriptorWithDeprecation.createDeprecated(it) }
    }
}

class DeprecatedMemberScope(private val workerScope: MemberScope) : MemberScope by workerScope {
    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? = null

    override fun getContributedClassifierIncludeDeprecated(
        name: Name,
        location: LookupLocation
    ): DescriptorWithDeprecation<ClassifierDescriptor>? {
        return workerScope.getContributedClassifier(name, location)?.let { DescriptorWithDeprecation.createDeprecated(it) }
    }
}
