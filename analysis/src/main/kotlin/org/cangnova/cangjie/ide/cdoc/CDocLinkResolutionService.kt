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

package org.cangnova.cangjie.ide.cdoc

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.descriptors.macro.MacroDescriptor
import org.cangnova.cangjie.incremental.components.LookupLocation
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.isChildOf
import org.cangnova.cangjie.name.isOneSegmentFQN
import org.cangnova.cangjie.projectStructure.CangJieSourceFilterScope
import org.cangnova.cangjie.resolve.ResolutionFacade
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.caches.resolveToDescriptorIfAny
import org.cangnova.cangjie.resolve.caches.unsafeResolveToDescriptor
import org.cangnova.cangjie.resolve.lazy.BodyResolveMode
import org.cangnova.cangjie.resolve.scopes.DescriptorKindFilter
import org.cangnova.cangjie.resolve.scopes.MemberScope
import org.cangnova.cangjie.stubindex.*
import org.cangnova.cangjie.utils.Printer

interface CDocLinkResolutionService {
    fun resolveCDocLink(
        context: BindingContext,
        fromDescriptor: DeclarationDescriptor,
        resolutionFacade: ResolutionFacade,
        qualifiedName: List<String>
    ): Collection<DeclarationDescriptor>
}

class IdeCDocLinkResolutionService(val project: Project) : CDocLinkResolutionService {
    override fun resolveCDocLink(
        context: BindingContext,
        fromDescriptor: DeclarationDescriptor,
        resolutionFacade: ResolutionFacade,
        qualifiedName: List<String>
    ): Collection<DeclarationDescriptor> {

        val scope = CangJieSourceFilterScope.projectAndLibrarySources(GlobalSearchScope.projectScope(project), project)

        val shortName = qualifiedName.lastOrNull() ?: return emptyList()

        val targetFqName = FqName.fromSegments(qualifiedName)

        val functions = CangJieFunctionShortNameIndex[shortName, project, scope].asSequence()
        val classes = CangJieClassShortNameIndex[shortName, project, scope].asSequence()

        val descriptors = (functions + classes).filter { it.fqName == targetFqName }
            .map { it.unsafeResolveToDescriptor(BodyResolveMode.PARTIAL) } // TODO Filter out not visible due dependencies config descriptors
            .toList()
        if (descriptors.isNotEmpty())
            return descriptors






        if (!targetFqName.isRoot && CangJiePackageIndexUtils.packageExists(targetFqName, scope))
            return listOf(GlobalSyntheticPackageViewDescriptor(targetFqName, project, scope))
        return emptyList()
    }
}

private fun shouldNotBeCalled(): Nothing = throw UnsupportedOperationException("Synthetic PVD for CDoc link resolution")

private class GlobalSyntheticPackageViewDescriptor(
    override val fqName: FqName,
    private val project: Project,
    private val scope: GlobalSearchScope
) : PackageViewDescriptor {


    override val containingDeclaration: PackageViewDescriptor?
        get() = if (fqName.isOneSegmentFQN()) null else GlobalSyntheticPackageViewDescriptor(
            fqName.parent(),
            project,
            scope
        )
    override val visibility: DescriptorVisibility = DescriptorVisibilities.PUBLIC
    override val memberScope: MemberScope = object : MemberScope {

        override fun getContributedVariables(name: Name, location: LookupLocation): Collection<VariableDescriptor> =
            shouldNotBeCalled()

        override fun getContributedPropertys(name: Name, location: LookupLocation): Collection<PropertyDescriptor> =
            shouldNotBeCalled()

        override fun getContributedFunctions(
            name: Name,
            location: LookupLocation
        ): Collection<SimpleFunctionDescriptor> =
            shouldNotBeCalled()

        override fun getContributedMacros(name: Name, location: LookupLocation): Collection<MacroDescriptor> =
            shouldNotBeCalled()

        override val functionNames : Set<Name> = shouldNotBeCalled()
        override val variableNames : Set<Name> = shouldNotBeCalled()
        override val classifierNames : Set<Name> = shouldNotBeCalled()
        override val propertyNames : Set<Name> = shouldNotBeCalled()
        override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor =
            shouldNotBeCalled()


        override fun printScopeStructure(p: Printer) {
            p.printIndent()
            p.print("GlobalSyntheticPackageViewDescriptorMemberScope (INDEX)")
        }


        fun getClassesByNameFilter(nameFilter: (Name) -> Boolean) = CangJieFullClassNameIndex
            .getAllKeys(project)
            .asSequence()
            .filter { it.startsWith(fqName.asString()) }
            .map(::FqName)
            .filter { it.isChildOf(fqName) }
            .filter { nameFilter(it.shortName()) }
            .flatMap { CangJieFullClassNameIndex[it.asString(), project, scope].asSequence() }
            .map { it.resolveToDescriptorIfAny() }

        fun getFunctionsByNameFilter(nameFilter: (Name) -> Boolean) = CangJieTopLevelFunctionFqnNameIndex
            .getAllKeys(project)
            .asSequence()
            .filter { it.startsWith(fqName.asString()) }
            .map(::FqName)
            .filter { it.isChildOf(fqName) }
            .filter { nameFilter(it.shortName()) }
            .flatMap { CangJieTopLevelFunctionFqnNameIndex[it.asString(), project, scope].asSequence() }
            .map { it.resolveToDescriptorIfAny() }

        fun getSubpackages(nameFilter: (Name) -> Boolean) =
            CangJiePackageIndexUtils.getSubPackageFqNames(fqName, scope, nameFilter)
                .map { GlobalSyntheticPackageViewDescriptor(it, project, scope) }

        override fun getContributedDescriptors(
            kindFilter: DescriptorKindFilter,
            nameFilter: (Name) -> Boolean
        ): Collection<DeclarationDescriptor> = (getClassesByNameFilter(nameFilter) +
                getFunctionsByNameFilter(nameFilter) +
                getSubpackages(nameFilter)
                ).filterNotNull().toList()

    }
    override val module: ModuleDescriptor
        get() = shouldNotBeCalled()
    override val fragments: List<PackageFragmentDescriptor>
        get() = shouldNotBeCalled()


    override val original: DeclarationDescriptor = this

    override val name: Name = fqName.shortName()
    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R? =
        shouldNotBeCalled()

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Unit, Unit>) = shouldNotBeCalled()

    override val annotations = Annotations.EMPTY
}
