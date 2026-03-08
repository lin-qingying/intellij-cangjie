/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.resolve.extensions

import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.extensions.SyntheticResolveExtension.PackageSyntheticNames
import org.cangnova.cangjie.resolve.lazy.LazyClassContext
import org.cangnova.cangjie.resolve.lazy.descriptors.ClassMemberDeclarationProvider
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.utils.flatMapToNullable
import java.util.ArrayList

/**
 * 将多个 [SyntheticResolveExtension] 实例合并为一个，对每个方法依次调用所有实例。
 *
 * 由 [SyntheticResolveExtension.getInstance] 在注册了多个扩展时自动使用。
 */
internal class CompositeSyntheticResolveExtension(
    private val extensions: List<SyntheticResolveExtension>
) : SyntheticResolveExtension {

    // -------------------------------------------------------------------------
    // 类成员级别
    // -------------------------------------------------------------------------

    override fun getSyntheticNestedClassNames(thisDescriptor: ClassDescriptor): List<Name> =
        extensions.flatMap { withLinkageErrorLogger(it) { getSyntheticNestedClassNames(thisDescriptor) } }

    override fun getPossibleSyntheticNestedClassNames(thisDescriptor: ClassDescriptor): List<Name>? =
        extensions.flatMapToNullable(ArrayList()) {
            withLinkageErrorLogger(it) { getPossibleSyntheticNestedClassNames(thisDescriptor) }
        }

    override fun getSyntheticFunctionNames(thisDescriptor: ClassDescriptor): List<Name> =
        extensions.flatMap { withLinkageErrorLogger(it) { getSyntheticFunctionNames(thisDescriptor) } }

    override fun getSyntheticPropertiesNames(thisDescriptor: ClassDescriptor): List<Name> =
        extensions.flatMap { withLinkageErrorLogger(it) { getSyntheticPropertiesNames(thisDescriptor) } }

    override fun getSyntheticCompanionObjectNameIfNeeded(thisDescriptor: ClassDescriptor): Name? =
        extensions.firstNotNullOfOrNull {
            withLinkageErrorLogger(it) { getSyntheticCompanionObjectNameIfNeeded(thisDescriptor) }
        }

    override fun addSyntheticSupertypes(thisDescriptor: ClassDescriptor, supertypes: MutableList<CangJieType>) =
        extensions.forEach { withLinkageErrorLogger(it) { addSyntheticSupertypes(thisDescriptor, supertypes) } }

    override fun generateSyntheticNestedClasses(
        thisDescriptor: ClassAndEnumDescriptor,
        name: Name,
        ctx: LazyClassContext,
        declarationProvider: ClassMemberDeclarationProvider,
        result: MutableSet<ClassDescriptor>
    ) = extensions.forEach {
        withLinkageErrorLogger(it) { generateSyntheticNestedClasses(thisDescriptor, name, ctx, declarationProvider, result) }
    }

    override fun generateSyntheticMethods(
        thisDescriptor: ClassAndEnumDescriptor, name: Name,
        ctx: LazyClassContext,
        bindingContext: BindingContext,
        fromSupertypes: List<SimpleFunctionDescriptor>,
        result: MutableCollection<SimpleFunctionDescriptor>
    ) = extensions.forEach {
        withLinkageErrorLogger(it) { generateSyntheticMethods(thisDescriptor, name, ctx, bindingContext, fromSupertypes, result) }
    }

    override fun generateSyntheticProperties(
        thisDescriptor: ClassAndEnumDescriptor, name: Name,
        ctx: LazyClassContext,
        bindingContext: BindingContext,
        fromSupertypes: List<PropertyDescriptor>,
        result: MutableSet<PropertyDescriptor>
    ) = extensions.forEach {
        withLinkageErrorLogger(it) { generateSyntheticProperties(thisDescriptor, name, ctx, bindingContext, fromSupertypes, result) }
    }

    override fun generateSyntheticSecondaryConstructors(
        thisDescriptor: ClassDescriptor,
        bindingContext: BindingContext,
        result: MutableCollection<ClassConstructorDescriptor>
    ) = extensions.forEach {
        withLinkageErrorLogger(it) { generateSyntheticSecondaryConstructors(thisDescriptor, bindingContext, result) }
    }

    // -------------------------------------------------------------------------
    // 包级别
    // -------------------------------------------------------------------------

    override fun generateSyntheticTopLevelClasses(
        thisDescriptor: PackageFragmentDescriptor, name: Name,
        ctx: LazyClassContext,
        declarationProvider: PackageMemberDeclarationProvider,
        result: MutableSet<ClassDescriptor>
    ) = extensions.forEach {
        withLinkageErrorLogger(it) { generateSyntheticTopLevelClasses(thisDescriptor, name, ctx, declarationProvider, result) }
    }

    override fun generateSyntheticEnums(
        thisDescriptor: PackageFragmentDescriptor, name: Name,
        ctx: LazyClassContext,
        declarationProvider: PackageMemberDeclarationProvider,
        result: MutableSet<EnumDescriptor>
    ) = extensions.forEach {
        withLinkageErrorLogger(it) { generateSyntheticEnums(thisDescriptor, name, ctx, declarationProvider, result) }
    }

    override fun generateSyntheticFunctions(
        thisDescriptor: PackageFragmentDescriptor, name: Name,
        ctx: LazyClassContext,
        declarationProvider: PackageMemberDeclarationProvider,
        result: MutableSet<SimpleFunctionDescriptor>
    ) = extensions.forEach {
        withLinkageErrorLogger(it) { generateSyntheticFunctions(thisDescriptor, name, ctx, declarationProvider, result) }
    }

    override fun generateSyntheticVariables(
        thisDescriptor: PackageFragmentDescriptor, name: Name,
        ctx: LazyClassContext,
        declarationProvider: PackageMemberDeclarationProvider,
        result: MutableSet<VariableDescriptor>
    ) = extensions.forEach {
        withLinkageErrorLogger(it) { generateSyntheticVariables(thisDescriptor, name, ctx, declarationProvider, result) }
    }

    override fun getSyntheticPackageNames(
        thisDescriptor: PackageFragmentDescriptor,
        declarationProvider: PackageMemberDeclarationProvider
    ): PackageSyntheticNames {
        var result = PackageSyntheticNames.EMPTY
        for (ext in extensions) {
            result = result.merge(
                withLinkageErrorLogger(ext) { getSyntheticPackageNames(thisDescriptor, declarationProvider) }
            )
        }
        return result
    }
}