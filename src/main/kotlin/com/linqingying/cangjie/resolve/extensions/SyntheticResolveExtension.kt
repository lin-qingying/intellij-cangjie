package com.linqingying.cangjie.resolve.extensions

import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.extensions.ProjectExtensionDescriptor
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.resolve.BindingContext
import com.linqingying.cangjie.resolve.lazy.LazyClassContext
import com.linqingying.cangjie.resolve.lazy.declarations.PackageMemberDeclarationProvider
import com.linqingying.cangjie.resolve.lazy.descriptors.ClassMemberDeclarationProvider
import com.linqingying.cangjie.types.CangJieType
import com.intellij.openapi.project.Project
import java.util.ArrayList
import com.linqingying.cangjie.utils.flatMapToNullable
//----------------------------------------------------------------
// extension interface

interface SyntheticResolveExtension {
    companion object : ProjectExtensionDescriptor<SyntheticResolveExtension>(
        "com.linqingying.cangjie.syntheticResolveExtension", SyntheticResolveExtension::class.java
    ) {
        fun getInstance(project: Project): SyntheticResolveExtension {
            val instances = getInstances(project)
            if (instances.size == 1) return instances.single()
            // return list combiner here
            return object : SyntheticResolveExtension {
                override fun getSyntheticNestedClassNames(thisDescriptor: ClassDescriptor): List<Name> =
                    instances.flatMap { withLinkageErrorLogger(it) { getSyntheticNestedClassNames(thisDescriptor) } }

                override fun getPossibleSyntheticNestedClassNames(thisDescriptor: ClassDescriptor): List<Name>? =
                    instances.flatMapToNullable(ArrayList<Name>()) {
                        withLinkageErrorLogger(it) {
                            getPossibleSyntheticNestedClassNames(thisDescriptor)
                        }
                    }

                override fun getSyntheticFunctionNames(thisDescriptor: ClassDescriptor): List<Name> =
                    instances.flatMap { withLinkageErrorLogger(it) { getSyntheticFunctionNames(thisDescriptor) } }

                override fun getSyntheticPropertiesNames(thisDescriptor: ClassDescriptor): List<Name> =
                    instances.flatMap { withLinkageErrorLogger(it) { getSyntheticPropertiesNames(thisDescriptor) } }

                override fun generateSyntheticClasses(
                    thisDescriptor: ClassDescriptor, name: Name,
                    ctx: LazyClassContext, declarationProvider: ClassMemberDeclarationProvider,
                    result: MutableSet<ClassDescriptor>
                ) =
                    instances.forEach {
                        withLinkageErrorLogger(it) {
                            generateSyntheticClasses(thisDescriptor, name, ctx, declarationProvider, result)
                        }
                    }

                override fun generateSyntheticClasses(
                    thisDescriptor: PackageFragmentDescriptor, name: Name,
                    ctx: LazyClassContext, declarationProvider: PackageMemberDeclarationProvider,
                    result: MutableSet<ClassDescriptor>
                ) =
                    instances.forEach {
                        withLinkageErrorLogger(it) {
                            generateSyntheticClasses(thisDescriptor, name, ctx, declarationProvider, result)
                        }
                    }

                override fun getSyntheticCompanionObjectNameIfNeeded(thisDescriptor: ClassDescriptor): Name? =
                    instances.firstNotNullOfOrNull { withLinkageErrorLogger(it) { getSyntheticCompanionObjectNameIfNeeded(thisDescriptor) } }

                override fun addSyntheticSupertypes(thisDescriptor: ClassDescriptor, supertypes: MutableList<CangJieType>) =
                    instances.forEach { withLinkageErrorLogger(it) { addSyntheticSupertypes(thisDescriptor, supertypes) } }

                // todo revert
                override fun generateSyntheticMethods(
                    thisDescriptor: ClassDescriptor, name: Name,
                    bindingContext: BindingContext,
                    fromSupertypes: List<SimpleFunctionDescriptor>,
                    result: MutableCollection<SimpleFunctionDescriptor>
                ) =
                    instances.forEach {
                        withLinkageErrorLogger(it) {
                            generateSyntheticMethods(
                                thisDescriptor,
                                name,
                                bindingContext,
                                fromSupertypes,
                                result
                            )
                        }
                    }

                override fun generateSyntheticProperties(
                    thisDescriptor: ClassDescriptor, name: Name,
                    bindingContext: BindingContext,
                    fromSupertypes: ArrayList<PropertyDescriptor>,
                    result: MutableSet<PropertyDescriptor>
                ) =
                    instances.forEach {
                        withLinkageErrorLogger(it) {
                            generateSyntheticProperties(
                                thisDescriptor,
                                name,
                                bindingContext,
                                fromSupertypes,
                                result
                            )
                        }
                    }

                override fun generateSyntheticSecondaryConstructors(
                    thisDescriptor: ClassDescriptor,
                    bindingContext: BindingContext,
                    result: MutableCollection<ClassConstructorDescriptor>
                ) {
                    instances.forEach {
                        withLinkageErrorLogger(it) {
                            generateSyntheticSecondaryConstructors(
                                thisDescriptor,
                                bindingContext,
                                result
                            )
                        }
                    }
                }
            }
        }
    }

    fun getSyntheticCompanionObjectNameIfNeeded(thisDescriptor: ClassDescriptor): Name? = null

    fun getSyntheticFunctionNames(thisDescriptor: ClassDescriptor): List<Name> = emptyList()

    fun getSyntheticPropertiesNames(thisDescriptor: ClassDescriptor): List<Name> = emptyList()

    fun getSyntheticNestedClassNames(thisDescriptor: ClassDescriptor): List<Name> = emptyList()

    /**
     * This method should return either superset of what [getSyntheticNestedClassNames] returns,
     * or null in case it needs to run resolution and inference and/or it is very costly.
     * Override this method if resolution started to fail with recursion.
     */
    fun getPossibleSyntheticNestedClassNames(thisDescriptor: ClassDescriptor): List<Name>? = getSyntheticNestedClassNames(thisDescriptor)

    fun addSyntheticSupertypes(thisDescriptor: ClassDescriptor, supertypes: MutableList<CangJieType>) {}

    fun generateSyntheticClasses(
        thisDescriptor: ClassDescriptor,
        name: Name,
        ctx: LazyClassContext,
        declarationProvider: ClassMemberDeclarationProvider,
        result: MutableSet<ClassDescriptor>
    ) {
    }

    fun generateSyntheticClasses(
        thisDescriptor: PackageFragmentDescriptor,
        name: Name,
        ctx: LazyClassContext,
        declarationProvider: PackageMemberDeclarationProvider,
        result: MutableSet<ClassDescriptor>
    ) {
    }

    fun generateSyntheticMethods(
        thisDescriptor: ClassDescriptor,
        name: Name,
        bindingContext: BindingContext,
        fromSupertypes: List<SimpleFunctionDescriptor>,
        result: MutableCollection<SimpleFunctionDescriptor>
    ) {
    }

    fun generateSyntheticProperties(
        thisDescriptor: ClassDescriptor,
        name: Name,
        bindingContext: BindingContext,
        fromSupertypes: ArrayList<PropertyDescriptor>,
        result: MutableSet<PropertyDescriptor>
    ) {
    }

    fun generateSyntheticSecondaryConstructors(
        thisDescriptor: ClassDescriptor,
        bindingContext: BindingContext,
        result: MutableCollection<ClassConstructorDescriptor>
    ) {
    }
}
