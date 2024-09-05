package com.huawei.cangjie.resolve.scopes

import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.incremental.components.LookupLocation
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.resolve.lazy.descriptors.LazyExtendClassDescriptor
import com.huawei.cangjie.utils.Printer


class LexicalChainedScope private constructor(
    parent: LexicalScope,
    override val ownerDescriptor: DeclarationDescriptor,
    override val isOwnerDescriptorAccessibleByLabel: Boolean,
    override val implicitReceiver: ReceiverParameterDescriptor?,
    override val contextReceiversGroup: List<ReceiverParameterDescriptor>,
    override val kind: LexicalScopeKind,
    // NB. Here can be very special subtypes of MemberScope (e.g., DeprecatedMemberScope).
    // Please, do not leak them outside of LexicalChainedScope, because other parts of compiler are not ready to work with them
    private val memberScopes: Array<MemberScope>,
    @Deprecated("This value is temporary hack for resolve -- don't use it!")
    val isStaticScope: Boolean = false
) : LexicalScope {
    override val parent = parent.takeSnapshot()
//    override fun getContributedPackageFqName(name: Name, location: LookupLocation): List<FqName> {
//        implicitReceiver ?:  return emptyList()
//
//
//        TODO()
//    }

    override fun getContributedDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean) =
        getFromAllScopes(memberScopes) { it.getContributedDescriptors() }

    override fun getContributedClassifier(name: Name, location: LookupLocation) =
        getFirstClassifierDiscriminateHeaders(memberScopes) { it.getContributedClassifier(name, location) }

    override fun getExtendClass(name: Name): List<LazyExtendClassDescriptor> {

  return  getListClassifierDiscriminateHeaders(memberScopes) { it.getExtendClass(name) }
    }

    override fun getContributedClassifierIncludeDeprecated(
        name: Name,
        location: LookupLocation
    ): DescriptorWithDeprecation<ClassifierDescriptor>? {
        val (firstClassifier, isFirstDeprecated) = memberScopes.firstNotNullOfOrNull {
            it.getContributedClassifierIncludeDeprecated(name, location)
        } ?: return null

        if (!isFirstDeprecated) return DescriptorWithDeprecation.createNonDeprecated(firstClassifier)

        // Slow-path: try to find the same classifier, but without deprecation
        for (scope in memberScopes) {
            val (descriptor, isDeprecated) = scope.getContributedClassifierIncludeDeprecated(name, location) ?: continue
            if (descriptor == firstClassifier && !isDeprecated) return DescriptorWithDeprecation.createNonDeprecated(
                descriptor
            )
        }

        return DescriptorWithDeprecation.createDeprecated(firstClassifier)
    }

    override fun getContributedVariables(name: Name, location: LookupLocation): Collection<@JvmWildcard VariableDescriptor> =
        getFromAllScopes(memberScopes) { it.getContributedVariables(name, location) }

    override fun getContributedPropertys(name: Name, location: LookupLocation): Collection<PropertyDescriptor> =
        getFromAllScopes(memberScopes) { it.getContributedPropertys(name, location) }

    override fun getContributedFunctions(name: Name, location: LookupLocation) =
        getFromAllScopes(memberScopes) { it.getContributedFunctions(name, location) }

    override fun toString(): String = kind.toString()

    override fun printStructure(p: Printer) {
        p.println(
            this::class.java.simpleName,
            ": ",
            kind,
            "; for descriptor: ",
            ownerDescriptor.name,
            " with implicitReceiver: ",
            implicitReceiver?.value ?: "NONE",
            " with contextReceiversGroup: ",
            if (contextReceiversGroup.isEmpty()) "NONE" else contextReceiversGroup.joinToString { it.value.toString() },
            " {"
        )
        p.pushIndent()

        for (scope in memberScopes) {
            scope.printScopeStructure(p)
        }

        p.print("parent = ")
        parent.printStructure(p.withholdIndentOnce())

        p.popIndent()
        p.println("}")
    }

    override fun definitelyDoesNotContainName(name: Name): Boolean {
        return memberScopes.all { it.definitelyDoesNotContainName(name) }
    }

    override fun recordLookup(name: Name, location: LookupLocation) {
        memberScopes.forEach { it.recordLookup(name, location) }
    }

    companion object {

        @JvmOverloads
        fun create(
            parent: LexicalScope,
            ownerDescriptor: DeclarationDescriptor,
            isOwnerDescriptorAccessibleByLabel: Boolean,
            implicitReceiver: ReceiverParameterDescriptor?,
            contextReceiversGroup: List<ReceiverParameterDescriptor>,
            kind: LexicalScopeKind,
            vararg memberScopes: MemberScope?,
            isStaticScope: Boolean = false
        ): LexicalScope =
            LexicalChainedScope(
                parent,
                ownerDescriptor,
                isOwnerDescriptorAccessibleByLabel,
                implicitReceiver,
                contextReceiversGroup,
                kind,
                listOfNonEmptyScopes(*memberScopes).toTypedArray(),
                isStaticScope
            )
    }
}
