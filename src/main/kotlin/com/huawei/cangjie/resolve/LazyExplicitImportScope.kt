package com.huawei.cangjie.resolve

import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.incremental.components.LookupLocation
import com.huawei.cangjie.incremental.components.NoLookupLocation
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.resolve.scopes.BaseImportingScope
import com.huawei.cangjie.resolve.scopes.DescriptorKindFilter
import com.huawei.cangjie.resolve.scopes.MemberScope
import com.huawei.cangjie.utils.CallOnceFunction
import com.huawei.cangjie.utils.Printer
import com.huawei.cangjie.utils.addIfNotNull
import com.intellij.util.SmartList

class LazyExplicitImportScope(
    private val languageVersionSettings: LanguageVersionSettings,
    private val packageOrClassDescriptor: DeclarationDescriptor,
    private val packageFragmentForVisibilityCheck: PackageFragmentDescriptor?,
    private val declaredName: Name,
    private val aliasName: Name,
    private val storeReferences: CallOnceFunction<Collection<DeclarationDescriptor>, Unit>
) : BaseImportingScope(null) {

    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
        if (name != aliasName) return null

        return when (packageOrClassDescriptor) {
            is PackageViewDescriptor -> packageOrClassDescriptor.memberScope.getContributedClassifier(
                declaredName,
                location
            )

            is ClassDescriptor -> packageOrClassDescriptor.unsubstitutedInnerClassesScope.getContributedClassifier(
                declaredName,
                location
            )

            else -> throw IllegalStateException("Should be class or package: $packageOrClassDescriptor")
        }
    }

    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<FunctionDescriptor> {
        if (name != aliasName) return emptyList()

        return collectCallableMemberDescriptors(location, MemberScope::getContributedFunctions)
    }

    override fun getContributedVariables(name: Name, location: LookupLocation): Collection<VariableDescriptor> {
        if (name != aliasName) return emptyList()

        return collectCallableMemberDescriptors(location, MemberScope::getContributedVariables)
    }

    override fun getContributedPackage(name: Name): PackageViewDescriptor? {

        return when (packageOrClassDescriptor) {
            is PackageViewDescriptor -> {

                val packageViewDescriptor = packageOrClassDescriptor.module.getPackage(
                    packageOrClassDescriptor.fqName.child(
                        aliasName
                    )
                )
                if (!packageViewDescriptor.isEmpty()) {
                    packageViewDescriptor
                } else {
                    null
                }


            }


            else -> throw IllegalStateException("Should be class or package: $packageOrClassDescriptor")
        }
    }

    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean,
        changeNamesForAliased: Boolean
    ): Collection<DeclarationDescriptor> {
        val descriptors = SmartList<DeclarationDescriptor>()

        if (kindFilter.acceptsKinds(DescriptorKindFilter.CLASSIFIERS_MASK)) {
            descriptors.addIfNotNull(getContributedClassifier(aliasName, NoLookupLocation.MATCH_GET_ALL_DESCRIPTORS))
        }
        if (kindFilter.acceptsKinds(DescriptorKindFilter.FUNCTIONS_MASK)) {
            descriptors.addAll(getContributedFunctions(aliasName, NoLookupLocation.MATCH_GET_ALL_DESCRIPTORS))
        }
        if (kindFilter.acceptsKinds(DescriptorKindFilter.VARIABLES_MASK)) {
            descriptors.addAll(getContributedVariables(aliasName, NoLookupLocation.MATCH_GET_ALL_DESCRIPTORS))
        }
        if (kindFilter.acceptsKinds(DescriptorKindFilter.REEXPORT_MASK)) {
            if (packageOrClassDescriptor is PackageViewDescriptor) {
                descriptors.addAll(
                    packageOrClassDescriptor.getContributedDescriptorsByReexportDirective(
                        languageVersionSettings,
                        packageFragmentForVisibilityCheck,
                        declaredName,
                        aliasName
                    )
                )
            }

        }
        if (kindFilter.acceptsKinds(DescriptorKindFilter.PACKAGES_MASK)) {
            getContributedPackage(aliasName)?.let {
                descriptors.add(it)
            }


//            descriptors.addAll(getContributedPackages(aliasName, NoLookupLocation.MATCH_GET_ALL_DESCRIPTORS)))
        }




        if (changeNamesForAliased && aliasName != declaredName) {
            for (i in descriptors.indices) {
                val newDescriptor: DeclarationDescriptor = when (val descriptor = descriptors[i]) {
                    is ClassDescriptor -> {
                        object : ClassDescriptor by descriptor {

                            override val name: Name = aliasName
                        }
                    }

                    is TypeAliasDescriptor -> {
                        object : TypeAliasDescriptor by descriptor {
                            override val name: Name = aliasName
                        }
                    }

                    is CallableMemberDescriptor -> {
                        descriptor
                            .newCopyBuilder()
                            .setName(aliasName)
                            .setOriginal(descriptor)
                            .build()!!
                    }

                    else -> error("Unknown kind of descriptor in import alias: $descriptor")
                }
                descriptors[i] = newDescriptor
            }
        }

        return descriptors
    }

    override fun computeImportedNames() = setOf(aliasName)

    override fun printStructure(p: Printer) {
        p.println(this::class.java.simpleName, ": ", aliasName)
    }

    // should be called only once
    internal fun storeReferencesToDescriptors() = getContributedDescriptors().apply(storeReferences)

    private fun <D : CallableMemberDescriptor> collectCallableMemberDescriptors(
        location: LookupLocation,
        getDescriptors: MemberScope.(Name, LookupLocation) -> Collection<D>
    ): Collection<D> {
        val descriptors = SmartList<D>()

        when (packageOrClassDescriptor) {
            is PackageViewDescriptor -> {
                val packageScope = packageOrClassDescriptor.memberScope
                descriptors.addAll(packageScope.getDescriptors(declaredName, location))
            }

            is ClassDescriptor -> {
                val staticClassScope = packageOrClassDescriptor.staticScope
                descriptors.addAll(staticClassScope.getDescriptors(declaredName, location))

//                if (packageOrClassDescriptor.kind == ClassKind.OBJECT) {
//                    descriptors.addAll(
//                        packageOrClassDescriptor.unsubstitutedMemberScope.getDescriptors(declaredName, location)
//                            .mapNotNull { it.asImportedFromObjectIfPossible() }
//                    )
//                }
            }

            else -> throw IllegalStateException("Should be class or package: $packageOrClassDescriptor")
        }

        return descriptors.choseOnlyVisibleOrAll()
    }

//    @Suppress("UNCHECKED_CAST")
//    private fun <D : CallableMemberDescriptor> D.asImportedFromObjectIfPossible(): D? = when (this) {
//        is PropertyDescriptor -> asImportedFromObject() as D
//        is FunctionDescriptor -> asImportedFromObject() as D
//        else -> null
//    }

    private fun <D : CallableMemberDescriptor> Collection<D>.choseOnlyVisibleOrAll(): Collection<D> =
        filter {
            isVisible(
                it,
                packageFragmentForVisibilityCheck,
                position = QualifierPosition.IMPORT,
                languageVersionSettings
            )
        }
            .takeIf { it.isNotEmpty() }
            ?: this
}
