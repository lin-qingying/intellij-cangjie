package com.huawei.cangjie.ide.cdoc

import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.descriptors.annotations.Annotations
import com.huawei.cangjie.ide.base.projectStructure.CangJieSourceFilterScope
import com.huawei.cangjie.ide.indices.CangJiePackageIndexUtils
import com.huawei.cangjie.ide.stubindex.CangJieClassShortNameIndex
import com.huawei.cangjie.ide.stubindex.CangJieFullClassNameIndex
import com.huawei.cangjie.ide.stubindex.CangJieFunctionShortNameIndex
import com.huawei.cangjie.ide.stubindex.CangJieTopLevelFunctionFqnNameIndex
import com.huawei.cangjie.incremental.components.LookupLocation
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.name.isChildOf
import com.huawei.cangjie.name.isOneSegmentFQN
import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.resolve.ResolutionFacade
import com.huawei.cangjie.resolve.caches.resolveToDescriptorIfAny
import com.huawei.cangjie.resolve.caches.unsafeResolveToDescriptor
import com.huawei.cangjie.resolve.lazy.BodyResolveMode
import com.huawei.cangjie.resolve.lazy.descriptors.LazyExtendClassDescriptor
import com.huawei.cangjie.resolve.scopes.DescriptorKindFilter
import com.huawei.cangjie.resolve.scopes.MemberScope
import com.huawei.cangjie.utils.Printer
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope

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

        override fun getFunctionNames(): Set<Name> = shouldNotBeCalled()
        override fun getVariableNames(): Set<Name> = shouldNotBeCalled()
        override fun getClassifierNames(): Set<Name> = shouldNotBeCalled()
        override fun getPropertyNames(): Set<Name> = shouldNotBeCalled()
        override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor =
            shouldNotBeCalled()

        override fun getExtendClass(name: Name): List<LazyExtendClassDescriptor>  = shouldNotBeCalled()

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
    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D?): R =
        shouldNotBeCalled()

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>) = shouldNotBeCalled()

    override val annotations = Annotations.EMPTY
}
