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

package org.cangnova.cangjie.resolve.scopes

import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.macro.MacroDescriptor
import org.cangnova.cangjie.incremental.components.LookupLocation
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.utils.Printer


/**
 * Introduces a simple wrapper for internal scope.
 */
abstract class AbstractScopeAdapter : MemberScope {
    protected abstract val workerScope: MemberScope


    override val functionNames: Set<Name>
        get() = workerScope.functionNames
    override val variableNames: Set<Name>
        get() = workerScope.variableNames
    override val classifierNames
        get() = workerScope.classifierNames
    override val propertyNames: Set<Name>
        get() = workerScope.propertyNames
    fun getActualScope(): MemberScope =
        if (workerScope is AbstractScopeAdapter)
            (workerScope as AbstractScopeAdapter).getActualScope()
        else
            workerScope

    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<SimpleFunctionDescriptor> {
        return workerScope.getContributedFunctions(name, location)
    }

    override fun getContributedMacros(name: Name, location: LookupLocation): Collection<MacroDescriptor> {
        return workerScope.getContributedMacros(name, location)

    }
    override fun getContributedEnumEntrys(name: Name, location: LookupLocation): List<ClassifierDescriptor> {
        return workerScope.getContributedEnumEntrys(name, location)
    }
    override fun getContributedClassifiers(name: Name, location: LookupLocation): List<ClassifierDescriptor> {
        return workerScope.getContributedClassifiers(name, location)
    }
    override fun getContributedPackageView(name: Name, location: LookupLocation): PackageViewDescriptor? {
        return workerScope.getContributedPackageView(name, location)
    }
    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
        return workerScope.getContributedClassifier(name, location)
    }
    override fun getContributedClassifierByExportId(exportId: String, location: LookupLocation): ClassifierDescriptor? {
        return workerScope.getContributedClassifierByExportId(exportId, location)
    }
    override fun getContributedClassifierByIndex(index: Int, location: LookupLocation): ClassifierDescriptor? {
        return workerScope.getContributedClassifierByIndex(index, location)
    }
    override fun getContributedVariables(name: Name, location: LookupLocation): Collection<@JvmWildcard VariableDescriptor> {
        return workerScope.getContributedVariables(name, location)
    }
    override fun getContributedPropertys(name: Name, location: LookupLocation): Collection<@JvmWildcard PropertyDescriptor> {
        return workerScope.getContributedPropertys(name, location)
    }
    override fun getContributedDescriptors(kindFilter: DescriptorKindFilter,
                                           nameFilter: (Name) -> Boolean): Collection<DeclarationDescriptor> {
        return workerScope.getContributedDescriptors(kindFilter, nameFilter)
    }

/*
    override fun getFunctionNames() = workerScope.getFunctionNames()
    override fun getVariableNames() = workerScope.getVariableNames()
    override fun classifierNames() = workerScope.classifierNames()
*/

    override fun recordLookup(name: Name, location: LookupLocation) {
        workerScope.recordLookup(name, location)
    }

    override fun printScopeStructure(p: Printer) {
        p.println(this::class.java.simpleName, " {")
        p.pushIndent()

        p.print("worker =")
        workerScope.printScopeStructure(p.withholdIndentOnce())

        p.popIndent()
        p.println("}")
    }
}
