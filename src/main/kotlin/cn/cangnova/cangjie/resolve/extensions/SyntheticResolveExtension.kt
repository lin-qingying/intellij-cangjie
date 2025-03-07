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

package cn.cangnova.cangjie.resolve.extensions

import cn.cangnova.cangjie.descriptors.*
import cn.cangnova.cangjie.extensions.ProjectExtensionDescriptor
import cn.cangnova.cangjie.name.Name
import cn.cangnova.cangjie.resolve.BindingContext
import cn.cangnova.cangjie.resolve.lazy.LazyClassContext
import cn.cangnova.cangjie.resolve.lazy.declarations.PackageMemberDeclarationProvider
import cn.cangnova.cangjie.resolve.lazy.descriptors.ClassMemberDeclarationProvider
import cn.cangnova.cangjie.types.CangJieType
import com.intellij.openapi.project.Project
import java.util.ArrayList
import cn.cangnova.cangjie.utils.flatMapToNullable
//----------------------------------------------------------------
// extension interface

interface SyntheticResolveExtension {
    companion object : ProjectExtensionDescriptor<SyntheticResolveExtension>(
        "cn.cangnova.cangjie.syntheticResolveExtension", SyntheticResolveExtension::class.java
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
