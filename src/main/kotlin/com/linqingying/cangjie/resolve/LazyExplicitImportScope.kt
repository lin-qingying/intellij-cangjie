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

package com.linqingying.cangjie.resolve

import com.linqingying.cangjie.config.LanguageVersionSettings
import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.incremental.components.LookupLocation
import com.linqingying.cangjie.incremental.components.NoLookupLocation
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.resolve.lazy.descriptors.LazyClassDescriptor
import com.linqingying.cangjie.resolve.lazy.descriptors.LazyExtendClassDescriptor
import com.linqingying.cangjie.resolve.scopes.BaseImportingScope
import com.linqingying.cangjie.resolve.scopes.DescriptorKindFilter
import com.linqingying.cangjie.resolve.scopes.MemberScope
import com.linqingying.cangjie.utils.CallOnceFunction
import com.linqingying.cangjie.utils.Printer
import com.linqingying.cangjie.utils.addIfNotNull
import com.intellij.util.SmartList
import com.linqingying.cangjie.descriptors.macro.MacroDescriptor

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

    override fun getExtendClass(name: Name): List<LazyExtendClassDescriptor> {
        return if (packageOrClassDescriptor is PackageViewDescriptor) {
            packageOrClassDescriptor.memberScope.getExtendClass(
                declaredName,

                )
        } else {
            emptyList()
        }
    }

    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<FunctionDescriptor> {
        if (name != aliasName) return emptyList()

        return collectCallableMemberDescriptors(location, MemberScope::getContributedFunctions)
    }

    override fun getContributedMacros(name: Name, location: LookupLocation): Collection<MacroDescriptor> {
        if (name != aliasName) return emptyList()

        return collectCallableMemberDescriptors(location, MemberScope::getContributedMacros)
    }
    override fun getContributedVariables(name: Name, location: LookupLocation): Collection<@JvmWildcard VariableDescriptor> {
        if (name != aliasName) return emptyList()

        return collectCallableMemberDescriptors(location, MemberScope::getContributedVariables)
    }

    override fun getContributedPackage(name: Name): PackageViewDescriptor? {

        return when (packageOrClassDescriptor) {
            is LazyClassDescriptor -> {
                packageOrClassDescriptor.containingDeclaration as? PackageViewDescriptor
            }
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

//    CallableMemberDescriptor
    private fun <D : /*CallableMemberDescriptor*/CallableDescriptor> collectCallableMemberDescriptors(
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

    private fun <D : CallableDescriptor> Collection<D>.choseOnlyVisibleOrAll(): Collection<D> =
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
