/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.resolve.scopes

import com.intellij.util.SmartList
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.macro.MacroDescriptor
import org.cangnova.cangjie.incremental.components.LookupLocation
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.lazy.descriptors.LazyExtendClassDescriptor
import org.cangnova.cangjie.utils.Printer


class ChainedMemberScope private constructor(
    private val debugName: String,
    private val scopes: Array<out MemberScope>
) : MemberScope {
    override fun getFunctionNames() = scopes.flatMapTo(mutableSetOf()) { it.getFunctionNames() }
    override fun getVariableNames() = scopes.flatMapTo(mutableSetOf()) { it.getVariableNames() }
    override fun getPropertyNames() = scopes.flatMapTo(mutableSetOf()) { it.getPropertyNames() }
    override fun getClassifierNames(): Set<Name>? = scopes.asIterable().flatMapClassifierNamesOrNull()
    override fun getContributedPackageView(name: Name, location: LookupLocation): PackageViewDescriptor? =
        getFirstFromAllScopes(scopes) { it.getContributedPackageView(name, location) }

    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? =
        getFirstClassifierDiscriminateHeaders(scopes) { it.getContributedClassifier(name, location) }

    override fun getContributedClassifiers(name: Name, location: LookupLocation): List<ClassifierDescriptor> =
        getListClassifierDiscriminateHeaders(scopes) { it.getContributedClassifiers(name, location) }

    override fun getContributedEnumEntrys(name: Name, location: LookupLocation): List<ClassifierDescriptor> =
        getListClassifierDiscriminateHeaders(scopes) { it.getContributedEnumEntrys(name, location) }

    override fun getExtendClass(name: Name): List<LazyExtendClassDescriptor> =
        getListClassifierDiscriminateHeaders(scopes) { it.getExtendClass(name) }

    override fun getContributedVariables(
        name: Name,
        location: LookupLocation
    ): Collection<@JvmWildcard VariableDescriptor> =
        getFromAllScopes(scopes) { it.getContributedVariables(name, location) }

    override fun getContributedPropertys(name: Name, location: LookupLocation): Collection<PropertyDescriptor> =
        getFromAllScopes(scopes) { it.getContributedPropertys(name, location) }

    override fun getContributedMacros(name: Name, location: LookupLocation): Collection<MacroDescriptor> =
        getFromAllScopes(scopes) { it.getContributedMacros(name, location) }

    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<SimpleFunctionDescriptor> =
        getFromAllScopes(scopes) { it.getContributedFunctions(name, location) }

    override fun getContributedDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean) =
        getFromAllScopes(scopes) { it.getContributedDescriptors(kindFilter, nameFilter) }

//    override fun getFunctionNames() = scopes.flatMapTo(mutableSetOf()) { it.getFunctionNames() }
//    override fun getVariableNames() = scopes.flatMapTo(mutableSetOf()) { it.getVariableNames() }
//    override fun getClassifierNames(): Set<Name>? = scopes.asIterable().flatMapClassifierNamesOrNull()

    override fun recordLookup(name: Name, location: LookupLocation) {
        scopes.forEach { it.recordLookup(name, location) }
    }

    override fun toString() = debugName

    //
    override fun printScopeStructure(p: Printer) {
        p.println(this::class.java.simpleName, ": ", debugName, " {")
        p.pushIndent()

        for (scope in scopes) {
            scope.printScopeStructure(p)
        }

        p.popIndent()
        p.println("}")
    }

    companion object {
        fun create(debugName: String, vararg scopes: MemberScope): MemberScope = create(debugName, scopes.asIterable())

        fun create(debugName: String, scopes: Iterable<MemberScope>): MemberScope {
            val flattenedNonEmptyScopes = SmartList<MemberScope>()
            for (scope in scopes) {
                when {
                    scope === MemberScope.Empty -> {}
                    scope is ChainedMemberScope -> flattenedNonEmptyScopes.addAll(scope.scopes)
                    else -> flattenedNonEmptyScopes.add(scope)
                }
            }
            return createOrSingle(debugName, flattenedNonEmptyScopes)
        }

        internal fun createOrSingle(debugName: String, scopes: List<MemberScope>): MemberScope =
            when (scopes.size) {
                0 -> MemberScope.Empty
                1 -> scopes[0]
                else -> ChainedMemberScope(debugName, scopes.toTypedArray())
            }
    }
}
