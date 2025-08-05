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

package cn.cangnova.cangjie.types.error

import cn.cangnova.cangjie.descriptors.*
import cn.cangnova.cangjie.descriptors.macro.ErrorMacroDescriptor
import cn.cangnova.cangjie.descriptors.macro.MacroDescriptor
import cn.cangnova.cangjie.incremental.components.LookupLocation
import cn.cangnova.cangjie.name.Name
import cn.cangnova.cangjie.resolve.scopes.DescriptorKindFilter
import cn.cangnova.cangjie.resolve.scopes.MemberScope
import cn.cangnova.cangjie.types.ErrorUtils
import cn.cangnova.cangjie.utils.Printer


open class ErrorScope(val kind: ErrorScopeKind, vararg formatParams: String) : MemberScope {
    protected val debugMessage = kind.debugMessage.format(*formatParams)

    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor =
        ErrorClassDescriptor(Name.special(ErrorEntity.ERROR_CLASS.debugText.format(name)))


    override fun getContributedClassifierIncludeDeprecated(
        name: Name, location: LookupLocation
    ): DescriptorWithDeprecation<ClassifierDescriptor>? = null

    override fun getContributedVariables(name: Name, location: LookupLocation): Collection<@JvmWildcard VariableDescriptor> =
        ErrorUtils.errorVariableGroup

    override fun getContributedPropertys (name: Name, location: LookupLocation): Set<PropertyDescriptor> =
        ErrorUtils.errorPropertyGroup

    override fun getContributedFunctions(name: Name, location: LookupLocation): Set<SimpleFunctionDescriptor> =
        setOf(ErrorFunctionDescriptor(ErrorUtils.errorClass))



    override fun getContributedMacros(name: Name, location: LookupLocation): Collection<MacroDescriptor> =
    setOf( ErrorMacroDescriptor(ErrorUtils.errorClass))
    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter, nameFilter: Function1<Name, Boolean>
    ): Collection<DeclarationDescriptor> = emptyList()

    override fun getFunctionNames(): Set<Name> = emptySet()
    override fun getVariableNames(): Set<Name> = emptySet()
    override fun getClassifierNames(): Set<Name> = emptySet()
    override fun getPropertyNames(): Set<Name> = emptySet()
    override fun recordLookup(name: Name, location: LookupLocation) {}
    override fun definitelyDoesNotContainName(name: Name): Boolean = false

    override fun toString(): String = "ErrorScope{$debugMessage}"

    override fun printScopeStructure(p: Printer) {
        p.println(javaClass.simpleName, ": ", debugMessage)
    }
}
