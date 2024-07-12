package com.huawei.cangjie.resolve.scopes

import com.huawei.cangjie.descriptors.ClassifierDescriptor
import com.huawei.cangjie.descriptors.DescriptorWithDeprecation
import com.huawei.cangjie.incremental.components.LookupLocation
import com.huawei.cangjie.name.Name


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
